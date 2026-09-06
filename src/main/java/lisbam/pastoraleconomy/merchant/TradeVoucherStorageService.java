package lisbam.pastoraleconomy.merchant;

import lisbam.pastoraleconomy.item.ItemTradeVoucher;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ILockableContainer;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Finds voucher-authorized loaded vanilla chests and exposes one rollback-safe sale inventory. */
final class TradeVoucherStorageService {
    private TradeVoucherStorageService() {
    }

    static SaleInventory findSaleInventory(EntityPlayerMP player) {
        return createForInventories(player.inventory, player.inventory.mainInventory.size(),
                findAuthorizedChests(player));
    }

    /** Package-private deterministic seam used by the standalone inventory regression test. */
    static SaleInventory createForInventories(IInventory playerInventory, int playerSlotCount,
                                               List<IInventory> authorizedChests) {
        List<SlotReference> slots = new ArrayList<SlotReference>();
        int safePlayerSlots = Math.max(0, Math.min(playerSlotCount, playerInventory.getSizeInventory()));
        addSlots(slots, playerInventory, safePlayerSlots);
        if (authorizedChests != null) {
            for (IInventory chest : authorizedChests) {
                if (chest != null && chest != playerInventory) {
                    addSlots(slots, chest, chest.getSizeInventory());
                }
            }
        }
        return new SaleInventory(slots, safePlayerSlots);
    }

    static boolean containsVoucher(IInventory inventory, UUID ownerId) {
        if (inventory == null || ownerId == null) {
            return false;
        }
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            if (ItemTradeVoucher.isBoundTo(inventory.getStackInSlot(slot), ownerId)) {
                return true;
            }
        }
        return false;
    }

    private static List<IInventory> findAuthorizedChests(EntityPlayerMP player) {
        MinecraftServer server = player.getServerWorld().getMinecraftServer();
        if (server == null || server.worlds == null) {
            return Collections.emptyList();
        }
        List<ChestCandidate> candidates = new ArrayList<ChestCandidate>();
        for (WorldServer world : server.worlds) {
            if (world == null) {
                continue;
            }
            for (TileEntity tile : new ArrayList<TileEntity>(world.loadedTileEntityList)) {
                if (tile instanceof TileEntityChest) {
                    candidates.add(new ChestCandidate(world, (TileEntityChest) tile));
                }
            }
        }
        Collections.sort(candidates, ChestCandidate.ORDER);

        Set<TileEntityChest> visited = Collections.newSetFromMap(
                new IdentityHashMap<TileEntityChest, Boolean>());
        List<IInventory> result = new ArrayList<IInventory>();
        for (ChestCandidate candidate : candidates) {
            TileEntityChest chest = candidate.chest;
            if (!visited.add(chest) || chest.isInvalid()) {
                continue;
            }
            BlockPos pos = chest.getPos();
            Block block = candidate.world.getBlockState(pos).getBlock();
            if (!(block instanceof BlockChest)) {
                continue;
            }

            List<TileEntityChest> halves = collectChestHalves(candidate.world, pos, block, chest);
            visited.addAll(halves);
            if (hasUnresolvedLootTable(halves)) {
                continue;
            }
            ILockableContainer inventory = ((BlockChest) block).getContainer(candidate.world, pos, true);
            if (inventory == null || inventory.isLocked() || !containsVoucher(inventory, player.getUniqueID())) {
                continue;
            }
            result.add(inventory);
        }
        return result;
    }

    private static List<TileEntityChest> collectChestHalves(WorldServer world, BlockPos pos, Block block,
                                                            TileEntityChest origin) {
        List<TileEntityChest> halves = new ArrayList<TileEntityChest>(2);
        halves.add(origin);
        for (EnumFacing facing : EnumFacing.Plane.HORIZONTAL) {
            BlockPos neighborPos = pos.offset(facing);
            if (world.getBlockState(neighborPos).getBlock() != block) {
                continue;
            }
            TileEntity neighbor = world.getTileEntity(neighborPos);
            if (neighbor instanceof TileEntityChest) {
                halves.add((TileEntityChest) neighbor);
                break;
            }
        }
        return halves;
    }

    /** Do not make remote stock discovery generate unopened vanilla loot chests. */
    private static boolean hasUnresolvedLootTable(List<TileEntityChest> halves) {
        for (TileEntityChest half : halves) {
            if (half.getLootTable() != null) {
                return true;
            }
        }
        return false;
    }

    private static void addSlots(List<SlotReference> slots, IInventory inventory, int count) {
        for (int index = 0; index < count; index++) {
            slots.add(new SlotReference(inventory, index));
        }
    }

    static final class SaleInventory {
        private final List<SlotReference> slots;
        private final int playerSlotCount;

        private SaleInventory(List<SlotReference> slots, int playerSlotCount) {
            this.slots = slots;
            this.playerSlotCount = playerSlotCount;
        }

        int count(TradeCatalogEntry entry) {
            return count(entry, 0);
        }

        /** Sent to the GUI separately so its live local-player count does not become stale. */
        int countLinkedChests(TradeCatalogEntry entry) {
            return count(entry, playerSlotCount);
        }

        private int count(TradeCatalogEntry entry, int firstSlot) {
            int total = 0;
            for (int index = firstSlot; index < slots.size(); index++) {
                SlotReference slot = slots.get(index);
                ItemStack stack = slot.get();
                if (!entry.matches(stack)) {
                    continue;
                }
                if (total > Integer.MAX_VALUE - stack.getCount()) {
                    return Integer.MAX_VALUE;
                }
                total += stack.getCount();
            }
            return total;
        }

        Snapshot snapshot() {
            List<ItemStack> contents = new ArrayList<ItemStack>(slots.size());
            for (SlotReference slot : slots) {
                ItemStack stack = slot.get();
                contents.add(stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
            }
            return new Snapshot(contents);
        }

        void restore(Snapshot snapshot) {
            if (snapshot == null || snapshot.contents.size() != slots.size()) {
                throw new IllegalArgumentException("Mismatched sale inventory snapshot.");
            }
            for (int index = 0; index < slots.size(); index++) {
                ItemStack stack = snapshot.contents.get(index);
                slots.get(index).set(stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
            }
            markDirty();
        }

        int remove(TradeCatalogEntry entry, int quantity) {
            int remaining = quantity;
            for (SlotReference slot : slots) {
                if (remaining <= 0) {
                    break;
                }
                ItemStack stack = slot.get();
                if (!entry.matches(stack)) {
                    continue;
                }
                int removed = Math.min(remaining, stack.getCount());
                stack.shrink(removed);
                remaining -= removed;
                if (stack.isEmpty()) {
                    slot.set(ItemStack.EMPTY);
                }
            }
            return quantity - remaining;
        }

        boolean insert(ItemStack output) {
            if (output == null || output.isEmpty()) {
                return true;
            }
            ItemStack remaining = output.copy();
            for (SlotReference slot : slots) {
                ItemStack existing = slot.get();
                if (existing == null || existing.isEmpty() || !existing.isItemEqual(remaining)
                        || !ItemStack.areItemStackTagsEqual(existing, remaining)) {
                    continue;
                }
                int maximum = Math.min(existing.getMaxStackSize(), slot.inventory.getInventoryStackLimit());
                int room = maximum - existing.getCount();
                if (room > 0) {
                    int moved = Math.min(room, remaining.getCount());
                    existing.grow(moved);
                    remaining.shrink(moved);
                }
                if (remaining.isEmpty()) {
                    return true;
                }
            }
            for (SlotReference slot : slots) {
                ItemStack existing = slot.get();
                if ((existing != null && !existing.isEmpty())
                        || !slot.inventory.isItemValidForSlot(slot.index, remaining)) {
                    continue;
                }
                int maximum = Math.min(remaining.getMaxStackSize(), slot.inventory.getInventoryStackLimit());
                int moved = Math.min(maximum, remaining.getCount());
                ItemStack placed = remaining.copy();
                placed.setCount(moved);
                slot.set(placed);
                remaining.shrink(moved);
                if (remaining.isEmpty()) {
                    return true;
                }
            }
            return false;
        }

        void markDirty() {
            Set<IInventory> inventories = Collections.newSetFromMap(new IdentityHashMap<IInventory, Boolean>());
            for (SlotReference slot : slots) {
                if (inventories.add(slot.inventory)) {
                    slot.inventory.markDirty();
                }
            }
        }
    }

    static final class Snapshot {
        private final List<ItemStack> contents;

        private Snapshot(List<ItemStack> contents) {
            this.contents = contents;
        }
    }

    private static final class SlotReference {
        private final IInventory inventory;
        private final int index;

        private SlotReference(IInventory inventory, int index) {
            this.inventory = inventory;
            this.index = index;
        }

        private ItemStack get() {
            return inventory.getStackInSlot(index);
        }

        private void set(ItemStack stack) {
            inventory.setInventorySlotContents(index, stack);
        }
    }

    private static final class ChestCandidate {
        private static final Comparator<ChestCandidate> ORDER = new Comparator<ChestCandidate>() {
            @Override
            public int compare(ChestCandidate left, ChestCandidate right) {
                int dimension = Integer.compare(left.world.provider.getDimension(), right.world.provider.getDimension());
                return dimension != 0 ? dimension : Long.compare(left.chest.getPos().toLong(), right.chest.getPos().toLong());
            }
        };

        private final WorldServer world;
        private final TileEntityChest chest;

        private ChestCandidate(WorldServer world, TileEntityChest chest) {
            this.world = world;
            this.chest = chest;
        }
    }
}
