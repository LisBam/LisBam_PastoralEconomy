package lisbam.pastoraleconomy.equipment;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/** Visible shoulder slot; only Elytra can be placed here. */
public final class SlotShoulderEquipment extends Slot {
    public static final int X = 70;
    public static final int Y = 35;

    public SlotShoulderEquipment(EntityPlayer player) {
        super(new ShoulderEquipmentInventory(player), 0, X, Y);
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
