package lisbam.pastoraleconomy.event;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.enchantment.ModEnchantments;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntityDonkey;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityLlama;
import net.minecraft.entity.passive.EntityMooshroom;
import net.minecraft.entity.passive.EntityMule;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.entity.passive.EntityParrot;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntityRabbit;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Direct-melee animal rules for Slaughter; no projectiles, XP edits, or new drops. */
@Mod.EventBusSubscriber(modid = LisBamPastoralEconomy.MODID)
public final class SlaughterEnchantmentEventHandler {
    private SlaughterEnchantmentEventHandler() {
    }

    @SubscribeEvent
    public static void increaseDirectMeleeDamage(LivingHurtEvent event) {
        if (event.getEntityLiving().world.isRemote || !isSlaughterAnimal(event.getEntityLiving())) {
            return;
        }
        EntityPlayer player = getDirectPlayer(event.getSource());
        if (player == null) {
            return;
        }
        ItemStack weapon = player.getHeldItemMainhand();
        int level = ModEnchantments.isSwordOrAxe(weapon)
                ? EnchantmentHelper.getEnchantmentLevel(ModEnchantments.SLAUGHTER, weapon) : 0;
        if (level > 0) {
            event.setAmount(event.getAmount() + 2.5F * level);
        }
    }

    @SubscribeEvent
    public static void increaseExistingDeathDrops(LivingDropsEvent event) {
        EntityLivingBase animal = event.getEntityLiving();
        if (animal.world.isRemote || !isSlaughterAnimal(animal)) {
            return;
        }
        EntityPlayer player = getDirectPlayer(event.getSource());
        if (player == null) {
            return;
        }
        ItemStack weapon = player.getHeldItemMainhand();
        int level = ModEnchantments.isSwordOrAxe(weapon)
                ? EnchantmentHelper.getEnchantmentLevel(ModEnchantments.SLAUGHTER, weapon) : 0;
        if (level <= 0) {
            return;
        }

        double multiplier = 1.0D + level * 0.5D;
        for (EntityItem entityItem : event.getDrops()) {
            ItemStack stack = entityItem.getItem();
            if (!stack.isEmpty()) {
                stack.setCount(scaleCount(stack.getCount(), multiplier, animal.world.rand.nextDouble()));
            }
        }
    }

    private static int scaleCount(int original, double multiplier, double fractionRoll) {
        double scaled = original * multiplier;
        int floor = (int) Math.floor(scaled);
        return floor + (fractionRoll < scaled - floor ? 1 : 0);
    }

    private static EntityPlayer getDirectPlayer(DamageSource source) {
        Entity immediate = source.getImmediateSource();
        Entity trueSource = source.getTrueSource();
        return immediate == trueSource && immediate instanceof EntityPlayer ? (EntityPlayer) immediate : null;
    }

    private static boolean isSlaughterAnimal(EntityLivingBase entity) {
        Class<?> entityClass = entity.getClass();
        return entityClass == EntityCow.class
                || entityClass == EntityMooshroom.class
                || entityClass == EntityPig.class
                || entityClass == EntitySheep.class
                || entityClass == EntityChicken.class
                || entityClass == EntityRabbit.class
                || entityClass == EntityHorse.class
                || entityClass == EntityDonkey.class
                || entityClass == EntityMule.class
                || entityClass == EntityLlama.class
                || entityClass == EntityWolf.class
                || entityClass == EntityOcelot.class
                || entityClass == EntityParrot.class;
    }
}
