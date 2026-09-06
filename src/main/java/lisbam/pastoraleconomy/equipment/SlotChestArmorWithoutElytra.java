package lisbam.pastoraleconomy.equipment;

import net.minecraft.entity.player.EntityPlayer;
import lisbam.pastoraleconomy.item.ItemFeatherWings;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/** Replacement for ContainerPlayer's anonymous armor slots; shoulder flight items are rejected from chest. */
public final class SlotChestArmorWithoutElytra extends Slot {
    private final EntityEquipmentSlot equipmentSlot;
    private final EntityPlayer player;

    /**
     * The first parameter mirrors ContainerPlayer$1 exactly so the Coremod can
     * retain the original constructor descriptor on both MCP and SRG runtimes.
     */
    public SlotChestArmorWithoutElytra(ContainerPlayer ignoredContainer, IInventory inventory, int index,
                                       int xPosition, int yPosition, EntityEquipmentSlot equipmentSlot) {
        super(inventory, index, xPosition, yPosition);
        this.equipmentSlot = equipmentSlot;
        this.player = inventory instanceof InventoryPlayer ? ((InventoryPlayer) inventory).player : null;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        if (equipmentSlot == EntityEquipmentSlot.CHEST
                && (stack.getItem() == Items.ELYTRA || ItemFeatherWings.isFeatherWings(stack))) {
            return false;
        }
        return stack.getItem().isValidArmor(stack, equipmentSlot, player);
    }

    @Override
    public int getSlotStackLimit() {
        return 1;
    }

    @Override
    public boolean canTakeStack(EntityPlayer playerIn) {
        return super.canTakeStack(playerIn);
    }
}
