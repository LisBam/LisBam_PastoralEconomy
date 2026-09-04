package lisbam.pastoraleconomy.event;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.entity.PastoralMobRules;
import lisbam.pastoraleconomy.entity.RetaliationTracker;
import lisbam.pastoraleconomy.entity.ai.EntityAIFarmHarassment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/** Server-only monster behavior hooks for the third batch. */
@Mod.EventBusSubscriber(modid = LisBamPastoralEconomy.MODID)
public final class MobBehaviorEventHandler {
    private static final int FARM_HARASSMENT_PRIORITY = 5;

    private static final Field TASK_ENTRIES_FIELD = ReflectionHelper.findField(
            EntityAITasks.class,
            "taskEntries",
            "field_75782_a"
    );
    private static final Field TASK_ACTION_FIELD = ReflectionHelper.findField(
            EntityAITasks.EntityAITaskEntry.class,
            "action",
            "field_75733_a"
    );
    private static final Field TARGET_CLASS_FIELD = ReflectionHelper.findField(
            EntityAINearestAttackableTarget.class,
            "targetClass",
            "field_75307_b"
    );

    /** Runtime-only: joining the same entity object twice must not duplicate its task. */
    private static final Set<EntityLiving> FARM_AI_ATTACHED =
            Collections.newSetFromMap(new WeakHashMap<EntityLiving, Boolean>());

    private MobBehaviorEventHandler() {
    }

    @SubscribeEvent
    public static void configureMonster(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote || !(event.getEntity() instanceof EntityLiving)) {
            return;
        }

        EntityLiving mob = (EntityLiving) event.getEntity();
        if (!PastoralMobRules.isPassiveToPlayers(mob)) {
            return;
        }

        removeVanillaPlayerTargetSelectors(mob);
        if (PastoralMobRules.canHarassFarmland(mob) && FARM_AI_ATTACHED.add(mob)) {
            mob.tasks.addTask(FARM_HARASSMENT_PRIORITY, new EntityAIFarmHarassment(mob));
        }
    }

    @SubscribeEvent
    public static void authorizeDirectPlayerRetaliation(LivingDamageEvent event) {
        if (event.getEntityLiving().world.isRemote || event.getAmount() <= 0.0F
                || !(event.getEntityLiving() instanceof EntityLiving)) {
            return;
        }

        EntityLiving mob = (EntityLiving) event.getEntityLiving();
        Entity trueSource = event.getSource().getTrueSource();
        if (PastoralMobRules.isPassiveToPlayers(mob) && trueSource instanceof EntityPlayer) {
            RetaliationTracker.authorize(mob, (EntityPlayer) trueSource);
        }
    }

    @SubscribeEvent
    public static void enforcePassivePlayerTargets(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase living = event.getEntityLiving();
        if (!living.world.isRemote && living instanceof EntityLiving
                && PastoralMobRules.isPassiveToPlayers((EntityLiving) living)) {
            RetaliationTracker.enforceTargetPolicy((EntityLiving) living);
        }
    }

    @SubscribeEvent
    public static void preserveCreeperEntityExplosionEffects(ExplosionEvent.Detonate event) {
        if (event.getWorld().isRemote) {
            return;
        }

        EntityLivingBase explosiveSource = event.getExplosion().getExplosivePlacedBy();
        if (explosiveSource != null && explosiveSource.getClass() == EntityCreeper.class) {
            event.getAffectedBlocks().clear();
        }
    }

    private static void removeVanillaPlayerTargetSelectors(EntityLiving mob) {
        for (EntityAIBase task : getTargetTasks(mob.targetTasks)) {
            if (isNearestPlayerTargetTask(task) || isEndermanPlayerTargetTask(mob, task)) {
                mob.targetTasks.removeTask(task);
            }
        }
    }

    private static boolean isNearestPlayerTargetTask(EntityAIBase task) {
        if (!(task instanceof EntityAINearestAttackableTarget)) {
            return false;
        }

        try {
            return TARGET_CLASS_FIELD.get(task) == EntityPlayer.class;
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Unable to inspect the 1.12.2 nearest-target task.", exception);
        }
    }

    /**
     * In vanilla 1.12.2 the only EntityEnderman-owned target task is
     * AIFindPlayer. It is kept separate from its Endermite selector and
     * HurtByTarget task so stare aggro alone is removed.
     */
    private static boolean isEndermanPlayerTargetTask(EntityLiving mob, EntityAIBase task) {
        return mob.getClass() == EntityEnderman.class
                && !(task instanceof EntityAIHurtByTarget)
                && task.getClass().getEnclosingClass() == EntityEnderman.class;
    }

    private static Iterable<EntityAIBase> getTargetTasks(EntityAITasks tasks) {
        try {
            Set<?> entries = (Set<?>) TASK_ENTRIES_FIELD.get(tasks);
            ArrayList<EntityAIBase> result = new ArrayList<EntityAIBase>();
            for (Object entry : new ArrayList<Object>(entries)) {
                Object action = TASK_ACTION_FIELD.get(entry);
                if (action instanceof EntityAIBase) {
                    result.add((EntityAIBase) action);
                }
            }
            return result;
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Unable to inspect the 1.12.2 target-task list.", exception);
        }
    }
}
