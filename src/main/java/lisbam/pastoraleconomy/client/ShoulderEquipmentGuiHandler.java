package lisbam.pastoraleconomy.client;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.equipment.SlotShoulderEquipment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiContainerEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/** Aligns and draws the shoulder cell with the native survival and creative inventory artwork. */
@Mod.EventBusSubscriber(modid = LisBamPastoralEconomy.MODID, value = Side.CLIENT)
public final class ShoulderEquipmentGuiHandler {
    private static final ResourceLocation INVENTORY_TEXTURE =
            new ResourceLocation("minecraft", "textures/gui/container/inventory.png");
    private static final ResourceLocation CREATIVE_INVENTORY_TEXTURE =
            new ResourceLocation("minecraft", "textures/gui/container/creative_inventory/tab_inventory.png");

    private ShoulderEquipmentGuiHandler() {
    }

    /** Creative rebuilds wrapped player slots whenever its inventory tab opens, so align before every frame. */
    @SubscribeEvent
    public static void alignShoulderSlot(GuiScreenEvent.DrawScreenEvent.Pre event) {
        if (event.getGui() instanceof GuiContainer) {
            findAndAlignShoulderSlot((GuiContainer) event.getGui());
        }
    }

    @SubscribeEvent
    public static void drawShoulderSlotBackground(GuiContainerEvent.DrawForeground event) {
        GuiContainer gui = event.getGuiContainer();
        Slot shoulder = findAndAlignShoulderSlot(gui);
        if (shoulder == null) {
            return;
        }
        int guiLeft = ObfuscationReflectionHelper.getPrivateValue(GuiContainer.class, gui,
                "guiLeft", "field_147003_i");
        int guiTop = ObfuscationReflectionHelper.getPrivateValue(GuiContainer.class, gui,
                "guiTop", "field_147009_r");

        boolean creative = gui instanceof GuiContainerCreative;
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.getTextureManager().bindTexture(creative ? CREATIVE_INVENTORY_TEXTURE : INVENTORY_TEXTURE);
        // Source pixels are untouched vanilla slot cells. Slot x/y are the 16x16 item origin,
        // while the surrounding native cell begins one pixel earlier on each axis.
        // DrawForeground runs while GuiContainer's matrix is already translated by guiLeft/guiTop.
        Gui.drawModalRectWithCustomSizedTexture(shoulder.xPos - 1, shoulder.yPos - 1,
                creative ? 8 : 97, creative ? 53 : 17, 18, 18, 256, 256);
        if (shoulder.getHasStack()) {
            minecraft.getRenderItem().renderItemAndEffectIntoGUI(shoulder.getStack(),
                    shoulder.xPos, shoulder.yPos);
            minecraft.getRenderItem().renderItemOverlayIntoGUI(minecraft.fontRenderer,
                    shoulder.getStack(), shoulder.xPos, shoulder.yPos, null);
        }

        if (isMouseOver(event, guiLeft, guiTop, shoulder)) {
            // Reapply GuiContainer's exact translucent hover overlay because this event fires
            // after vanilla drew it and the native cell above necessarily covered it.
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            GlStateManager.colorMask(true, true, true, false);
            Gui.drawRect(shoulder.xPos, shoulder.yPos,
                    shoulder.xPos + 16, shoulder.yPos + 16, -2130706433);
            GlStateManager.colorMask(true, true, true, true);
            GlStateManager.enableLighting();
            GlStateManager.enableDepth();
        }
    }

    private static Slot findAndAlignShoulderSlot(GuiContainer gui) {
        if (!(gui instanceof GuiInventory) && !(gui instanceof GuiContainerCreative)) {
            return null;
        }
        for (Slot slot : gui.inventorySlots.inventorySlots) {
            if (!SlotShoulderEquipment.isShoulderSlot(slot)) {
                continue;
            }
            if (gui instanceof GuiContainerCreative) {
                slot.xPos = SlotShoulderEquipment.CREATIVE_X;
                slot.yPos = SlotShoulderEquipment.CREATIVE_Y;
            } else {
                slot.xPos = SlotShoulderEquipment.SURVIVAL_X;
                slot.yPos = SlotShoulderEquipment.SURVIVAL_Y;
            }
            return slot;
        }
        return null;
    }

    private static boolean isMouseOver(GuiContainerEvent.DrawForeground event, int guiLeft, int guiTop, Slot slot) {
        int relativeX = event.getMouseX() - guiLeft;
        int relativeY = event.getMouseY() - guiTop;
        return slot.isEnabled() && relativeX >= slot.xPos - 1 && relativeX < slot.xPos + 17
                && relativeY >= slot.yPos - 1 && relativeY < slot.yPos + 17;
    }
}
