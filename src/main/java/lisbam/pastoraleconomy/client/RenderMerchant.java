package lisbam.pastoraleconomy.client;

import lisbam.pastoraleconomy.entity.EntityMerchant;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ResourceLocation;

/** Steve-style placeholder renderer; loaded only on the physical client. */
public final class RenderMerchant extends RenderLiving<EntityMerchant> {
    public RenderMerchant(RenderManager manager) {
        super(manager, new ModelPlayer(0.0F, false), 0.5F);
        addLayer(new LayerBipedArmor(this));
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityMerchant entity) {
        return DefaultPlayerSkin.getDefaultSkin(entity.getUniqueID());
    }
}
