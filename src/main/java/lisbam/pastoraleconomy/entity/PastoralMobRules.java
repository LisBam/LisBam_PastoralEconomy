package lisbam.pastoraleconomy.entity;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityHusk;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.monster.EntityStray;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.monster.EntityZombieVillager;

/**
 * Exact vanilla-class policy for the third-batch monster changes. Using class
 * equality intentionally excludes Cave Spider, Zombie Pigman, and mod-added
 * subclasses from this batch's rules.
 */
public final class PastoralMobRules {
    private PastoralMobRules() {
    }

    public static boolean isPassiveToPlayers(EntityLiving mob) {
        return isZombieFamily(mob)
                || isSkeletonFamily(mob)
                || isRegularSpider(mob)
                || mob.getClass() == EntityCreeper.class
                || mob.getClass() == EntityEnderman.class;
    }

    public static boolean canHarassFarmland(EntityLiving mob) {
        return isZombieFamily(mob)
                || isSkeletonFamily(mob)
                || isRegularSpider(mob)
                || mob.getClass() == EntityEnderman.class;
    }

    public static boolean isCreeper(EntityLiving mob) {
        return mob.getClass() == EntityCreeper.class;
    }

    private static boolean isZombieFamily(EntityLiving mob) {
        Class<?> mobClass = mob.getClass();
        return mobClass == EntityZombie.class
                || mobClass == EntityZombieVillager.class
                || mobClass == EntityHusk.class;
    }

    private static boolean isSkeletonFamily(EntityLiving mob) {
        Class<?> mobClass = mob.getClass();
        return mobClass == EntitySkeleton.class || mobClass == EntityStray.class;
    }

    private static boolean isRegularSpider(EntityLiving mob) {
        return mob.getClass() == EntitySpider.class;
    }
}
