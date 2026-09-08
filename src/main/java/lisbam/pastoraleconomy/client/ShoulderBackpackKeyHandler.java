package lisbam.pastoraleconomy.client;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.equipment.ShoulderEquipmentService;
import lisbam.pastoraleconomy.item.ItemBackpack;
import lisbam.pastoraleconomy.network.ModNetwork;
import lisbam.pastoraleconomy.network.message.RequestOpenBackpackMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Keyboard;

/** Physical-client shortcut for opening the server-owned shoulder backpack. */
@Mod.EventBusSubscriber(modid = LisBamPastoralEconomy.MODID, value = Side.CLIENT)
public final class ShoulderBackpackKeyHandler {
    private static final KeyBinding OPEN_SHOULDER_BACKPACK = new KeyBinding(
            "key.lisbam_pastoral_economy.open_shoulder_backpack",
            Keyboard.KEY_B,
            "key.categories.lisbam_pastoral_economy"
    );
    private static boolean registered;

    private ShoulderBackpackKeyHandler() {
    }

    /** ClientRegistry is client-only, so ClientProxy calls this during pre-initialization. */
    public static void registerKeyBinding() {
        if (!registered) {
            ClientRegistry.registerKeyBinding(OPEN_SHOULDER_BACKPACK);
            registered = true;
        }
    }

    @SubscribeEvent
    public static void openShoulderBackpack(InputEvent.KeyInputEvent event) {
        if (!OPEN_SHOULDER_BACKPACK.isPressed()) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        boolean allowedScreen = minecraft.currentScreen == null || minecraft.currentScreen instanceof GuiInventory;
        boolean hasBackpack = minecraft.player != null && ItemBackpack.isBackpack(
                ShoulderEquipmentService.getShoulderStack(minecraft.player));
        if (isOpenRequestAllowed(true, allowedScreen, hasBackpack)) {
            // The existing payload-free C2S request derives both player and
            // equipped stack server-side; it cannot open arbitrary storage.
            ModNetwork.CHANNEL.sendToServer(new RequestOpenBackpackMessage());
        }
    }

    static boolean isOpenRequestAllowed(boolean keyPressed, boolean allowedScreen, boolean hasBackpack) {
        return keyPressed && allowedScreen && hasBackpack;
    }
}
