package lisbam.pastoraleconomy.network.message;

import io.netty.buffer.ByteBuf;
import lisbam.pastoraleconomy.equipment.ShoulderEquipmentService;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** S2C shoulder snapshot; backpack NBT is sent only to its owning client. */
public final class SyncShoulderEquipmentMessage implements IMessage {
    private int entityId = -1;
    private ItemStack stack = ItemStack.EMPTY;
    private boolean valid;

    public SyncShoulderEquipmentMessage() {
    }

    public SyncShoulderEquipmentMessage(int entityId, ItemStack stack) {
        if (entityId < 0 || stack == null || !stack.isEmpty()
                && !ShoulderEquipmentService.isValidShoulderStack(stack)) {
            throw new IllegalArgumentException("Invalid shoulder equipment snapshot.");
        }
        this.entityId = entityId;
        this.stack = normalize(stack);
        this.valid = true;
    }

    public int getEntityId() {
        return entityId;
    }

    public ItemStack getStack() {
        return stack;
    }

    public boolean isValid() {
        return valid;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        valid = false;
        entityId = -1;
        stack = ItemStack.EMPTY;
        if (buffer.readableBytes() < 4) {
            return;
        }
        int decodedEntityId = buffer.readInt();
        ItemStack decodedStack = ByteBufUtils.readItemStack(buffer);
        if (decodedEntityId < 0 || decodedStack == null || !decodedStack.isEmpty()
                && !ShoulderEquipmentService.isValidShoulderStack(decodedStack)) {
            return;
        }
        entityId = decodedEntityId;
        stack = normalize(decodedStack);
        valid = true;
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        if (!valid) {
            throw new IllegalStateException("Cannot encode an invalid shoulder equipment snapshot.");
        }
        buffer.writeInt(entityId);
        ByteBufUtils.writeItemStack(buffer, stack);
    }

    private static ItemStack normalize(ItemStack value) {
        if (value.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack normalized = value.copy();
        normalized.setCount(1);
        return normalized;
    }
}
