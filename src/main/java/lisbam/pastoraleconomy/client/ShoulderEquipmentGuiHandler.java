package lisbam.pastoraleconomy.client;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.equipment.SlotShoulderEquipment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiContainerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/** Draws the shoulder slot from the native player-inventory texture in the vanilla inventory foreground. */
@Mod.EventBusSubscriber(modid = LisBamPastoralEconomy.MODID, value = Side.CLIENT)
public final class ShoulderEquipmentGuiHandler {
    private static final ResourceLocation INVENTORY_TEXTURE =
            new ResourceLocation("minecraft", "textures/gui/container/inventory.png");

    private ShoulderEquipmentGuiHandler() {
    }

    @SubscribeEvent
    public static void drawShoulderSlotBackground(GuiContainerEvent.DrawForeground event) {
        if (!(event.getGuiContainer() instanceof GuiInventory)) {
            return;
        }
        GuiContainer gui = event.getGuiContainer();
        int guiLeft = ObfuscationReflectionHelper.getPrivateValue(GuiContainer.class, gui,
                "guiLeft", "field_147003_i");
        int guiTop = ObfuscationReflectionHelper.getPrivateValue(GuiContainer.class, gui,
                "guiTop", "field_147009_r");
        Minecraft.getMinecraft().getTextureManager().bindTexture(INVENTORY_TEXTURE);
        // This is the unmodified 18x18 crafting-slot cell from the native 1.12.2 inventory texture.
        Gui.drawModalRectWithCustomSizedTexture(guiLeft + SlotShoulderEquipment.X, guiTop + SlotShoulderEquipment.Y,
                97, 17, 18, 18, 256, 256);
        for (Slot slot : gui.inventorySlots.inventorySlots) {
            if (slot instanceof SlotShoulderEquipment && slot.getHasStack()) {
                Minecraft.getMinecraft().getRenderItem().renderItemAndEffectIntoGUI(slot.getStack(),
                        guiLeft + SlotShoulderEquipment.X, guiTop + SlotShoulderEquipment.Y);
                Minecraft.getMinecraft().getRenderItem().renderItemOverlayIntoGUI(Minecraft.getMinecraft().fontRenderer,
                        slot.getStack(), guiLeft + SlotShoulderEquipment.X, guiTop + SlotShoulderEquipment.Y, null);
                break;
            }
        }
    }
}
