package lisbam.pastoraleconomy.client;

import lisbam.pastoraleconomy.entity.EntityMerchant;
import lisbam.pastoraleconomy.entity.MerchantSkinCatalog;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.util.ResourceLocation;

/** Steve-proportioned merchant renderer; loaded only on the physical client. */
public final class RenderMerchant extends RenderLiving<EntityMerchant> {
    private static final ResourceLocation[] TEXTURES = new ResourceLocation[] {
            new ResourceLocation("minecraft", "textures/entity/steve.png"),
            new ResourceLocation("lisbam_pastoral_economy", "textures/entity/merchant/farmer_straw_hat.png"),
            new ResourceLocation("lisbam_pastoral_economy", "textures/entity/merchant/farmer_green_plaid.png"),
            new ResourceLocation("lisbam_pastoral_economy", "textures/entity/merchant/farmer_blue_apron.png"),
            new ResourceLocation("lisbam_pastoral_economy", "textures/entity/merchant/farmer_harvest_vest.png")
    };

    public RenderMerchant(RenderManager manager) {
        super(manager, new ModelPlayer(0.0F, false), 0.5F);
        addLayer(new LayerBipedArmor(this));
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityMerchant entity) {
        return TEXTURES[MerchantSkinCatalog.normalize(entity.getSkinVariant())];
    }
}
