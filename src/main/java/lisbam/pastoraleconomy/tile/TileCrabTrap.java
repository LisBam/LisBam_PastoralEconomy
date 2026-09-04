package lisbam.pastoraleconomy.tile;

import lisbam.pastoraleconomy.block.ModBlocks;
import lisbam.pastoraleconomy.crabtrap.CrabTrapRules;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Enchantments;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.material.Material;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTableList;

import java.util.List;

/**
 * Server-authoritative 20-slot crab-trap inventory and fishing state machine.
 * Slot 0 is read as the rod module, slot 1 as raw-meat bait, and slots 2-19
 * receive catches. The two functional slots validate their specific modules;
 * all harvest slots remain general-purpose storage.
 */
public final class TileCrabTrap extends TileEntity implements ISidedInventory, ITickable {
    public static final int ROD_SLOT = 0;
    public static final int BAIT_SLOT = 1;
    public static final int FIRST_HARVEST_SLOT = 2;
    public static final int HARVEST_SLOT_COUNT = 18;
    public static final int SLOT_COUNT = FIRST_HARVEST_SLOT + HARVEST_SLOT_COUNT;
    private static final int COUNTDOWN_SAVE_INTERVAL_TICKS = 20;

    private static final int[] ALL_SLOTS = new int[] {
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19
    };
    private static final String KEY_INVENTORY = "inventory";
    private static final String KEY_SLOT = "slot";
    private static final String KEY_ROUND_ACTIVE = "roundActive";
    private static final String KEY_REMAINING_TICKS = "remainingTicks";
    private static final String KEY_ROUND_LURE = "roundLure";
    private static final String KEY_ROUND_LUCK = "roundLuck";
    private static final String KEY_ROUND_HAS_BAIT = "roundHasBait";
    private static final String KEY_PENDING_LOOT = "pendingLoot";

    private NonNullList<ItemStack> inventory = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private boolean roundActive;
    private int remainingTicks;
    private int roundLureLevel;
    private int roundLuckLevel;
    private boolean roundHasBait;
    private ItemStack pendingLoot = ItemStack.EMPTY;
    private boolean pendingOutputForDisplay;

    @Override
    public void update() {
        if (world == null || world.isRemote) {
            return;
        }

        if (!pendingLoot.isEmpty()) {
            tryCompletePendingLoot();
            return;
        }

        if (!hasEffectiveWaterEnvironment()) {
            return;
        }

        if (!roundActive) {
            startNextRound();
            return;
        }

        if (remainingTicks > 0) {
            remainingTicks--;
            // Persisting all loaded traps every tick continually dirties their
            // chunks. The countdown is still exact in memory and is saved at
            // least once per second (and on every state transition).
            if (remainingTicks % COUNTDOWN_SAVE_INTERVAL_TICKS == 0) {
                markDirty();
            }
            if (remainingTicks > 0) {
                return;
            }
        }

        generateCurrentRoundLoot();
    }

    /**
     * A 1.12.2 TileEntity block cannot share a BlockPos with water. The
     * compatible in-water expression is direct contact with water or flowing
     * water on any adjacent block, without modern waterlogging state.
     */
    public boolean hasEffectiveWaterEnvironment() {
        if (world == null || pos == null) {
            return false;
        }
        for (EnumFacing facing : EnumFacing.values()) {
            BlockPos adjacent = pos.offset(facing);
            if (world.getBlockState(adjacent).getMaterial() == Material.WATER) {
                return true;
            }
        }
        return false;
    }

    private void startNextRound() {
        ItemStack rod = getStackInSlot(ROD_SLOT);
        roundLureLevel = CrabTrapRules.clampFishingEnchantmentLevel(
                CrabTrapRules.isFishingRod(rod) ? EnchantmentHelper.getEnchantmentLevel(Enchantments.LURE, rod) : 0
        );
        roundLuckLevel = CrabTrapRules.clampFishingEnchantmentLevel(
                CrabTrapRules.isFishingRod(rod) ? EnchantmentHelper.getEnchantmentLevel(Enchantments.LUCK_OF_THE_SEA, rod) : 0
        );
        roundHasBait = CrabTrapRules.isRawMeatBait(getStackInSlot(BAIT_SLOT));
        remainingTicks = CrabTrapRules.rollRoundWaitTicks(roundLureLevel, roundHasBait,
                () -> CrabTrapRules.MIN_BASE_WAIT_TICKS + world.rand.nextInt(
                        CrabTrapRules.MAX_BASE_WAIT_TICKS - CrabTrapRules.MIN_BASE_WAIT_TICKS + 1));
        roundActive = true;
        markDirty();
    }

    private void generateCurrentRoundLoot() {
        List<ItemStack> generated = ((WorldServer) world).getLootTableManager()
                .getLootTableFromLocation(LootTableList.GAMEPLAY_FISHING)
                .generateLootForPools(world.rand, new LootContext.Builder((WorldServer) world)
                        .withLuck((float) roundLuckLevel).build());

        if (generated.isEmpty() || generated.get(0).isEmpty()) {
            // The unmodified 1.12.2 fishing table always has one roll. Should a
            // datapack-like external modification make it empty, preserve the
            // current round and retry rather than charging bait or deleting state.
            return;
        }

        ItemStack result = generated.get(0).copy();
        if (canFullyInsertIntoHarvest(result)) {
            insertIntoHarvest(result);
            finishSuccessfulRound();
            return;
        }

        pendingLoot = result;
        remainingTicks = 0;
        markDirty();
    }

    private void tryCompletePendingLoot() {
        if (!canFullyInsertIntoHarvest(pendingLoot)) {
            return;
        }
        insertIntoHarvest(pendingLoot);
        pendingLoot = ItemStack.EMPTY;
        finishSuccessfulRound();
    }

    private void finishSuccessfulRound() {
        if (roundHasBait && world.rand.nextBoolean()) {
            consumeOneBaitIfPresent();
        }
        roundActive = false;
        remainingTicks = 0;
        roundLureLevel = 0;
        roundLuckLevel = 0;
        roundHasBait = false;
        markDirty();
    }

    private void consumeOneBaitIfPresent() {
        ItemStack bait = getStackInSlot(BAIT_SLOT);
        if (!CrabTrapRules.isRawMeatBait(bait)) {
            return;
        }
        bait.shrink(1);
        if (bait.isEmpty()) {
            inventory.set(BAIT_SLOT, ItemStack.EMPTY);
        }
    }

    public boolean canFullyInsertIntoHarvest(ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        int remaining = stack.getCount();
        for (int index = FIRST_HARVEST_SLOT; index < SLOT_COUNT && remaining > 0; index++) {
            ItemStack existing = inventory.get(index);
            if (!canStacksMerge(existing, stack)) {
                continue;
            }
            int limit = Math.min(getInventoryStackLimit(), existing.getMaxStackSize());
            remaining -= Math.max(0, limit - existing.getCount());
        }
        int maxStack = Math.min(getInventoryStackLimit(), stack.getMaxStackSize());
        for (int index = FIRST_HARVEST_SLOT; index < SLOT_COUNT && remaining > 0; index++) {
            if (inventory.get(index).isEmpty()) {
                remaining -= maxStack;
            }
        }
        return remaining <= 0;
    }

    private void insertIntoHarvest(ItemStack output) {
        ItemStack remaining = output.copy();
        for (int index = FIRST_HARVEST_SLOT; index < SLOT_COUNT && !remaining.isEmpty(); index++) {
            ItemStack existing = inventory.get(index);
            if (!canStacksMerge(existing, remaining)) {
                continue;
            }
            int limit = Math.min(getInventoryStackLimit(), existing.getMaxStackSize());
            int moved = Math.min(limit - existing.getCount(), remaining.getCount());
            if (moved > 0) {
                existing.grow(moved);
                remaining.shrink(moved);
            }
        }
        for (int index = FIRST_HARVEST_SLOT; index < SLOT_COUNT && !remaining.isEmpty(); index++) {
            if (!inventory.get(index).isEmpty()) {
                continue;
            }
            int moved = Math.min(Math.min(getInventoryStackLimit(), remaining.getMaxStackSize()), remaining.getCount());
            ItemStack inserted = remaining.copy();
            inserted.setCount(moved);
            inventory.set(index, inserted);
            remaining.shrink(moved);
        }
        if (!remaining.isEmpty()) {
            throw new IllegalStateException("Crab trap accepted loot without enough harvest capacity.");
        }
    }

    private static boolean canStacksMerge(ItemStack first, ItemStack second) {
        return !first.isEmpty() && first.isItemEqual(second) && ItemStack.areItemStackTagsEqual(first, second);
    }

    public ItemStack getPendingLoot() {
        return pendingLoot.copy();
    }

    public boolean hasPendingLoot() {
        return !pendingLoot.isEmpty() || pendingOutputForDisplay;
    }

    public boolean isRoundActive() {
        return roundActive;
    }

    public int getRemainingTicks() {
        return remainingTicks;
    }

    public int getRoundLureLevel() {
        return roundLureLevel;
    }

    public int getRoundLuckLevel() {
        return roundLuckLevel;
    }

    public boolean isRoundUsingBait() {
        return roundHasBait;
    }

    @Override
    public int[] getSlotsForFace(EnumFacing side) {
        return ALL_SLOTS;
    }

    @Override
    public boolean canInsertItem(int index, ItemStack stack, EnumFacing direction) {
        return isItemValidForSlot(index, stack);
    }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, EnumFacing direction) {
        return isValidSlot(index);
    }

    @Override
    public int getSizeInventory() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : inventory) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return isValidSlot(index) ? inventory.get(index) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        if (!isValidSlot(index) || count <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = inventory.get(index);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = stack.splitStack(count);
        if (stack.isEmpty()) {
            inventory.set(index, ItemStack.EMPTY);
        }
        markDirty();
        return removed;
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        if (!isValidSlot(index)) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = inventory.get(index);
        inventory.set(index, ItemStack.EMPTY);
        if (!removed.isEmpty()) {
            markDirty();
        }
        return removed;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        if (!isValidSlot(index)) {
            return;
        }
        if (stack != null && !stack.isEmpty() && !isItemValidForSlot(index, stack)) {
            return;
        }
        setStoredStack(index, stack);
        markDirty();
    }

    /**
     * Loads already-saved inventory verbatim so old worlds can retrieve items
     * that were placed in functional slots before their validation was fixed.
     */
    private void setStoredStack(int index, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            inventory.set(index, ItemStack.EMPTY);
        } else {
            ItemStack copy = stack.copy();
            copy.setCount(Math.min(copy.getCount(), Math.min(getInventoryStackLimit(), copy.getMaxStackSize())));
            inventory.set(index, copy);
        }
    }

    @Override
    public String getName() {
        return "container.lisbam_pastoral_economy.crab_trap";
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUsableByPlayer(net.minecraft.entity.player.EntityPlayer player) {
        return world != null && world.getBlockState(pos).getBlock() == ModBlocks.CRAB_TRAP
                && world.getTileEntity(pos) == this && player.getDistanceSq(
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void openInventory(net.minecraft.entity.player.EntityPlayer player) {
        // No viewer counter is needed for this non-animated block.
    }

    @Override
    public void closeInventory(net.minecraft.entity.player.EntityPlayer player) {
        // No viewer counter is needed for this non-animated block.
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        if (index == ROD_SLOT) {
            return CrabTrapRules.isFishingRod(stack);
        }
        if (index == BAIT_SLOT) {
            return CrabTrapRules.isRawMeatBait(stack);
        }
        return index >= FIRST_HARVEST_SLOT && index < SLOT_COUNT;
    }

    @Override
    public int getField(int id) {
        switch (id) {
            case 0: return remainingTicks;
            case 1: return roundLureLevel;
            case 2: return roundLuckLevel;
            case 3: return roundActive ? 1 : 0;
            case 4: return roundHasBait ? 1 : 0;
            case 5: return hasPendingLoot() ? 1 : 0;
            default: return 0;
        }
    }

    @Override
    public void setField(int id, int value) {
        switch (id) {
            case 0: remainingTicks = Math.max(0, value); break;
            case 1: roundLureLevel = CrabTrapRules.clampFishingEnchantmentLevel(value); break;
            case 2: roundLuckLevel = CrabTrapRules.clampFishingEnchantmentLevel(value); break;
            case 3: roundActive = value != 0; break;
            case 4: roundHasBait = value != 0; break;
            case 5: pendingOutputForDisplay = value != 0; break;
            default: break;
        }
    }

    @Override
    public int getFieldCount() {
        return 6;
    }

    @Override
    public void clear() {
        for (int index = 0; index < SLOT_COUNT; index++) {
            inventory.set(index, ItemStack.EMPTY);
        }
        markDirty();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        NBTTagList serializedInventory = new NBTTagList();
        for (int index = 0; index < SLOT_COUNT; index++) {
            ItemStack stack = inventory.get(index);
            if (stack.isEmpty()) {
                continue;
            }
            NBTTagCompound stackTag = new NBTTagCompound();
            stackTag.setByte(KEY_SLOT, (byte) index);
            stack.writeToNBT(stackTag);
            serializedInventory.appendTag(stackTag);
        }
        compound.setTag(KEY_INVENTORY, serializedInventory);
        compound.setBoolean(KEY_ROUND_ACTIVE, roundActive);
        compound.setInteger(KEY_REMAINING_TICKS, Math.max(0, remainingTicks));
        compound.setInteger(KEY_ROUND_LURE, CrabTrapRules.clampFishingEnchantmentLevel(roundLureLevel));
        compound.setInteger(KEY_ROUND_LUCK, CrabTrapRules.clampFishingEnchantmentLevel(roundLuckLevel));
        compound.setBoolean(KEY_ROUND_HAS_BAIT, roundHasBait);
        if (!pendingLoot.isEmpty()) {
            NBTTagCompound pendingTag = new NBTTagCompound();
            pendingLoot.writeToNBT(pendingTag);
            compound.setTag(KEY_PENDING_LOOT, pendingTag);
        }
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        inventory = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        NBTTagList serializedInventory = compound.getTagList(KEY_INVENTORY, 10);
        for (int index = 0; index < serializedInventory.tagCount(); index++) {
            NBTTagCompound stackTag = serializedInventory.getCompoundTagAt(index);
            int slot = stackTag.getByte(KEY_SLOT) & 255;
            if (isValidSlot(slot)) {
                ItemStack stack = new ItemStack(stackTag);
                if (!stack.isEmpty()) {
                    setStoredStack(slot, stack);
                }
            }
        }
        roundActive = compound.getBoolean(KEY_ROUND_ACTIVE);
        remainingTicks = Math.max(0, compound.getInteger(KEY_REMAINING_TICKS));
        roundLureLevel = CrabTrapRules.clampFishingEnchantmentLevel(compound.getInteger(KEY_ROUND_LURE));
        roundLuckLevel = CrabTrapRules.clampFishingEnchantmentLevel(compound.getInteger(KEY_ROUND_LUCK));
        roundHasBait = compound.getBoolean(KEY_ROUND_HAS_BAIT);
        pendingLoot = compound.hasKey(KEY_PENDING_LOOT, 10)
                ? new ItemStack(compound.getCompoundTag(KEY_PENDING_LOOT)) : ItemStack.EMPTY;
        if (pendingLoot.isEmpty()) {
            pendingLoot = ItemStack.EMPTY;
        } else {
            roundActive = true;
            remainingTicks = 0;
        }
        pendingOutputForDisplay = !pendingLoot.isEmpty();
    }

    private static boolean isValidSlot(int index) {
        return index >= 0 && index < SLOT_COUNT;
    }
}
