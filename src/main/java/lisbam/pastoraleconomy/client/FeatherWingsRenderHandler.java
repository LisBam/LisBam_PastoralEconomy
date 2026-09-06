package lisbam.pastoraleconomy.client;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.equipment.ShoulderEquipmentService;
import lisbam.pastoraleconomy.item.ItemFeatherWings;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelElytra;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Adds a feather-textured Elytra geometry layer to the two vanilla player renderers. */
@Mod.EventBusSubscriber(modid = LisBamPastoralEconomy.MODID, value = Side.CLIENT)
public final class FeatherWingsRenderHandler {
    private static final Set<RenderPlayer> ATTACHED_RENDERERS = Collections.newSetFromMap(
            new IdentityHashMap<RenderPlayer, Boolean>()
    );

    private FeatherWingsRenderHandler() {
    }

    @SubscribeEvent
    public static void attachLayer(RenderPlayerEvent.Pre event) {
        RenderPlayer renderer = event.getRenderer();
        if (ATTACHED_RENDERERS.add(renderer)) {
            renderer.addLayer(new FeatherWingsLayer(renderer));
        }
    }

    private static final class FeatherWingsLayer implements LayerRenderer<AbstractClientPlayer> {
        private static final ResourceLocation TEXTURE = new ResourceLocation(LisBamPastoralEconomy.MODID,
                "textures/entity/feather_wings.png");

        private final RenderPlayer renderer;
        private final ModelElytra model = new ModelElytra();

        private FeatherWingsLayer(RenderPlayer renderer) {
            this.renderer = renderer;
        }

        @Override
        public void doRenderLayer(AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                                  float partialTicks, float ageInTicks, float netHeadYaw, float headPitch,
                                  float scale) {
            if (!ItemFeatherWings.isFeatherWings(ShoulderEquipmentService.getShoulderStack(player))) {
                return;
            }
            renderer.bindTexture(TEXTURE);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            model.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, player);
            model.render(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
            GlStateManager.disableBlend();
        }

        @Override
        public boolean shouldCombineTextures() {
            return false;
        }
    }
}
