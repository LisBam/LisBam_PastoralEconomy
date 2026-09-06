package lisbam.pastoraleconomy.equipment;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/** Generic-descriptor Coremod hook used while each vanilla ContainerPlayer is constructed. */
public final class ShoulderEquipmentContainerHooks {
    private ShoulderEquipmentContainerHooks() {
    }

    public static void addShoulderSlot(Object containerObject, Object playerObject) {
        if (!(containerObject instanceof Container) || !(playerObject instanceof EntityPlayer)) {
            return;
        }
        Container container = (Container) containerObject;
        for (Slot slot : container.inventorySlots) {
            if (slot instanceof SlotShoulderEquipment) {
                return;
            }
        }
        // Container#addSlotToContainer is protected.  This mirrors its tiny
        // 1.12.2 implementation so the injected late constructor hook keeps
        // slot numbering and tracked-stack synchronization identical.
        Slot shoulder = new SlotShoulderEquipment((EntityPlayer) playerObject);
        shoulder.slotNumber = container.inventorySlots.size();
        container.inventorySlots.add(shoulder);
        container.inventoryItemStacks.add(ItemStack.EMPTY);
    }

    /** Preserves shift-click ergonomics for every supported shoulder item. */
    public static boolean tryMoveElytraToShoulder(Object containerObject, Object stackObject, int sourceIndex) {
        if (!(containerObject instanceof Container) || !(stackObject instanceof ItemStack)
                || !ShoulderEquipmentService.isValidShoulderStack((ItemStack) stackObject)) {
            return false;
        }
        Container container = (Container) containerObject;
        Slot shoulder = null;
        for (Slot slot : container.inventorySlots) {
            if (slot instanceof SlotShoulderEquipment) {
                shoulder = slot;
                break;
            }
        }
        if (shoulder == null || shoulder.getHasStack() || sourceIndex < 0
                || sourceIndex >= container.inventorySlots.size()) {
            return false;
        }
        shoulder.putStack((ItemStack) stackObject);
        Slot source = container.inventorySlots.get(sourceIndex);
        source.putStack(ItemStack.EMPTY);
        source.onSlotChanged();
        return true;
    }
}
