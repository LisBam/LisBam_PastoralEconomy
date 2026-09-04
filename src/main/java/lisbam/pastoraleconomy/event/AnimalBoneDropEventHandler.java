package lisbam.pastoraleconomy.event;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.config.ModSettings;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Adds animal bones before lower-priority drop modifiers such as Slaughter run. */
@Mod.EventBusSubscriber(modid = LisBamPastoralEconomy.MODID)
public final class AnimalBoneDropEventHandler {
    private AnimalBoneDropEventHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void addAnimalBoneDrop(LivingDropsEvent event) {
        EntityLivingBase animal = event.getEntityLiving();
        if (animal.world.isRemote || ModSettings.isDisableAnimalBoneDrops()) {
            return;
        }

        int count = AnimalBoneDropRules.rollDropCount(animal, event.getLootingLevel(), animal.world.rand);
        if (count <= 0) {
            return;
        }

        EntityItem drop = new EntityItem(animal.world, animal.posX, animal.posY, animal.posZ,
                new ItemStack(Items.BONE, count));
        drop.setDefaultPickupDelay();
        event.getDrops().add(drop);
    }
}
