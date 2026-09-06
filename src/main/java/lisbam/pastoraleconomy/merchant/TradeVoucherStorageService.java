package lisbam.pastoraleconomy.merchant;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.data.world.PastoralWorldData;
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
import net.minecraft.world.World;
import net.minecraft.util.math.ChunkPos;
import net.minecraftforge.common.ForgeChunkManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Finds voucher-authorized vanilla chests and exposes one rollback-safe sale
 * inventory. A bound voucher makes each chest half retain one dedicated Forge
 * chunk ticket, so remote stock survives normal player-distance unloading.
 */
public final class TradeVoucherStorageService {
    private static final String TICKET_POSITION_KEY = "voucherChestPosition";
    private static final Map<VoucherChestRegistry.Location, ForgeChunkManager.Ticket> CHEST_TICKETS =
            new HashMap<VoucherChestRegistry.Location, ForgeChunkManager.Ticket>();
    private static final ForgeChunkManager.LoadingCallback CHUNK_LOADING_CALLBACK =
            new ForgeChunkManager.LoadingCallback() {
                @Override
                public void ticketsLoaded(List<ForgeChunkManager.Ticket> tickets, World world) {
                    if (!(world instanceof WorldServer)) {
                        return;
                    }
                    WorldServer serverWorld = (WorldServer) world;
                    for (ForgeChunkManager.Ticket ticket : tickets) {
                        if (!ticket.getModData().hasKey(TICKET_POSITION_KEY)) {
                            ForgeChunkManager.releaseTicket(ticket);
                            continue;
                        }
                        VoucherChestRegistry.Location location = new VoucherChestRegistry.Location(
                                serverWorld.provider.getDimension(),
                                ticket.getModData().getLong(TICKET_POSITION_KEY));
                        ForgeChunkManager.Ticket oldTicket = CHEST_TICKETS.get(location);
                        if (oldTicket != null && oldTicket != ticket) {
                            ForgeChunkManager.releaseTicket(ticket);
                            continue;
                        }
                        CHEST_TICKETS.put(location, ticket);
                        ForgeChunkManager.forceChunk(ticket, new ChunkPos(location.getPosition()));
                    }
                }
            };

    private TradeVoucherStorageService() {
    }

    /** Register before any world requests a ticket; called from the common pre-init lifecycle. */
    public static void registerChunkLoadingCallback() {
        ForgeChunkManager.setForcedChunkLoadingCallback(LisBamPastoralEconomy.INSTANCE, CHUNK_LOADING_CALLBACK);
    }

    /** Observes loaded chest contents when a player has finished moving items in a vanilla chest. */
    public static void observeLoadedVoucherChests(WorldServer world) {
        if (world == null || world.isRemote) {
            return;
        }
        for (ChestCandidate candidate : collectLoadedChestCandidates(world)) {
            ChestAccess chest = openChest(candidate.world, candidate.chest);
            if (chest != null && containsBoundVoucher(chest.inventory)) {
                trackChestHalves(candidate.world, chest);
            }
        }
    }

    /** Revalidates only the already indexed, ticket-loaded chests once per overworld tick cycle. */
    public static void reconcile(WorldServer overworld) {
        if (overworld == null || overworld.isRemote || overworld.provider.getDimension() != 0) {
            return;
        }
        MinecraftServer server = overworld.getMinecraftServer();
        if (server == null) {
            return;
        }
        PastoralWorldData data = PastoralWorldData.get(overworld);
        VoucherChestRegistry registry = data.getVoucherChestRegistry();
        boolean changed = false;
        for (VoucherChestRegistry.Location location : registry.getLocations()) {
            WorldServer world = server.getWorld(location.getDimension());
            if (world == null) {
                continue;
            }
            ensureTicket(world, location);
            BlockPos position = location.getPosition();
            // forceChunk is asynchronous with respect to this tick; never
            // discard a valid record merely because its chunk has not arrived.
            if (!world.isBlockLoaded(position)) {
                continue;
            }
            TileEntity tile = world.getTileEntity(position);
            ChestAccess chest = tile instanceof TileEntityChest
                    ? openChest(world, (TileEntityChest) tile) : null;
            if (chest == null || !containsBoundVoucher(chest.inventory)) {
                changed |= registry.remove(location);
                releaseTrackedTicket(location);
            } else {
                trackChestHalves(world, chest);
            }
        }
        if (changed) {
            data.markDirty();
        }
    }

    /** Removes only runtime references; Forge persists active tickets during a normal world unload. */
    public static void forgetWorld(World world) {
        if (world == null) {
            return;
        }
        java.util.Iterator<Map.Entry<VoucherChestRegistry.Location, ForgeChunkManager.Ticket>> iterator =
                CHEST_TICKETS.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().world == world) {
                iterator.remove();
            }
        }
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
            candidates.addAll(collectLoadedChestCandidates(world));
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
            ChestAccess openedChest = openChest(candidate.world, chest);
            if (openedChest == null) {
                continue;
            }
            visited.addAll(openedChest.halves);
            if (containsBoundVoucher(openedChest.inventory)) {
                trackChestHalves(candidate.world, openedChest);
            }
            if (containsVoucher(openedChest.inventory, player.getUniqueID())) {
                result.add(openedChest.inventory);
            }
        }
        return result;
    }

    private static List<ChestCandidate> collectLoadedChestCandidates(WorldServer world) {
        List<ChestCandidate> candidates = new ArrayList<ChestCandidate>();
        for (TileEntity tile : new ArrayList<TileEntity>(world.loadedTileEntityList)) {
            if (tile instanceof TileEntityChest) {
                candidates.add(new ChestCandidate(world, (TileEntityChest) tile));
            }
        }
        Collections.sort(candidates, ChestCandidate.ORDER);
        return candidates;
    }

    private static ChestAccess openChest(WorldServer world, TileEntityChest origin) {
        if (world == null || origin == null || origin.isInvalid() || !world.isBlockLoaded(origin.getPos())) {
            return null;
        }
        BlockPos pos = origin.getPos();
        Block block = world.getBlockState(pos).getBlock();
        if (!(block instanceof BlockChest)) {
            return null;
        }
        List<TileEntityChest> halves = collectChestHalves(world, pos, block, origin);
        if (hasUnresolvedLootTable(halves)) {
            return null;
        }
        ILockableContainer inventory = ((BlockChest) block).getContainer(world, pos, true);
        return inventory == null || inventory.isLocked() ? null : new ChestAccess(inventory, halves);
    }

    private static List<TileEntityChest> collectChestHalves(WorldServer world, BlockPos pos, Block block,
                                                            TileEntityChest origin) {
        List<TileEntityChest> halves = new ArrayList<TileEntityChest>(2);
        halves.add(origin);
        for (EnumFacing facing : EnumFacing.Plane.HORIZONTAL) {
            BlockPos neighborPos = pos.offset(facing);
            if (!world.isBlockLoaded(neighborPos) || world.getBlockState(neighborPos).getBlock() != block) {
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

    private static boolean containsBoundVoucher(IInventory inventory) {
        if (inventory == null) {
            return false;
        }
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            if (ItemTradeVoucher.isBound(inventory.getStackInSlot(slot))) {
                return true;
            }
        }
        return false;
    }

    private static void trackChestHalves(WorldServer world, ChestAccess chest) {
        PastoralWorldData data = PastoralWorldData.get(world);
        VoucherChestRegistry registry = data.getVoucherChestRegistry();
        boolean changed = false;
        for (TileEntityChest half : chest.halves) {
            VoucherChestRegistry.Location location = new VoucherChestRegistry.Location(
                    world.provider.getDimension(), half.getPos().toLong());
            changed |= registry.add(location.getDimension(), location.getPosition());
            ensureTicket(world, location);
        }
        if (changed) {
            data.markDirty();
        }
    }

    private static boolean ensureTicket(WorldServer world, VoucherChestRegistry.Location location) {
        ForgeChunkManager.Ticket ticket = CHEST_TICKETS.get(location);
        if (ticket != null && ticket.world == world) {
            ForgeChunkManager.forceChunk(ticket, new ChunkPos(location.getPosition()));
            return true;
        }
        if (ticket != null) {
            CHEST_TICKETS.remove(location);
            ForgeChunkManager.releaseTicket(ticket);
        }
        ticket = ForgeChunkManager.requestTicket(LisBamPastoralEconomy.INSTANCE, world, ForgeChunkManager.Type.NORMAL);
        if (ticket == null) {
            return false;
        }
        ticket.getModData().setLong(TICKET_POSITION_KEY, location.getPackedPosition());
        CHEST_TICKETS.put(location, ticket);
        ForgeChunkManager.forceChunk(ticket, new ChunkPos(location.getPosition()));
        return true;
    }

    private static void releaseTrackedTicket(VoucherChestRegistry.Location location) {
        ForgeChunkManager.Ticket ticket = CHEST_TICKETS.remove(location);
        if (ticket != null) {
            ForgeChunkManager.releaseTicket(ticket);
        }
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

    private static final class ChestAccess {
        private final ILockableContainer inventory;
        private final List<TileEntityChest> halves;

        private ChestAccess(ILockableContainer inventory, List<TileEntityChest> halves) {
            this.inventory = inventory;
            this.halves = halves;
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
