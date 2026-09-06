package lisbam.pastoraleconomy.equipment;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/** Visible shoulder slot; only Elytra can be placed here. */
public final class SlotShoulderEquipment extends Slot {
    /** Vanilla survival offhand is at 77,62; this cell sits directly above it. */
    public static final int SURVIVAL_X = 77;
    public static final int SURVIVAL_Y = 44;
    /** Vanilla creative-inventory offhand is remapped to 35,20. */
    public static final int CREATIVE_X = 35;
    public static final int CREATIVE_Y = 2;

    public SlotShoulderEquipment(EntityPlayer player) {
        super(new ShoulderEquipmentInventory(player), 0, SURVIVAL_X, SURVIVAL_Y);
    }

    /** CreativeSlot keeps the wrapped slot's IInventory, so no private-field reflection is needed. */
    public static boolean isShoulderSlot(Slot slot) {
        return slot instanceof SlotShoulderEquipment
                || slot != null && slot.inventory instanceof ShoulderEquipmentInventory;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return stack.getItem() == Items.ELYTRA;
    }

    @Override
    public int getSlotStackLimit() {
        return 1;
    }
}
