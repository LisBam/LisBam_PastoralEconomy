package lisbam.pastoraleconomy.entity;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Runtime-only permission for the one player that directly hurt a modified
 * mob. Vanilla AI still decides when combat ends; this tracker only prevents
 * unrelated players from becoming a target.
 */
public final class RetaliationTracker {
    private static final long START_GRACE_TICKS = 40L;
    private static final Map<EntityLiving, RetaliationPermission> PERMISSIONS =
            Collections.synchronizedMap(new WeakHashMap<EntityLiving, RetaliationPermission>());

    private RetaliationTracker() {
    }

    public static void authorize(EntityLiving mob, EntityPlayer attacker) {
        PERMISSIONS.put(
                mob,
                new RetaliationPermission(attacker.getUniqueID(), mob.world.getTotalWorldTime() + START_GRACE_TICKS)
        );
    }

    /**
     * Runs before the entity's server-side AI update. It clears only a player
     * target that is not the direct attacker, leaving villagers, golems, and
     * every other original target relationship untouched.
     */
    public static void enforceTargetPolicy(EntityLiving mob) {
        RetaliationPermission permission = PERMISSIONS.get(mob);
        EntityLivingBase target = mob.getAttackTarget();

        if (target instanceof EntityPlayer) {
            EntityPlayer playerTarget = (EntityPlayer) target;
            if (permission != null && permission.playerId.equals(playerTarget.getUniqueID())) {
                permission.started = true;
                return;
            }

            mob.setAttackTarget(null);
            if (permission != null && permission.started) {
                PERMISSIONS.remove(mob);
            }
            return;
        }

        if (permission != null && (permission.started || mob.world.getTotalWorldTime() > permission.startDeadline)) {
            PERMISSIONS.remove(mob);
        }
    }

    private static final class RetaliationPermission {
        private final UUID playerId;
        private final long startDeadline;
        private boolean started;

        private RetaliationPermission(UUID playerId, long startDeadline) {
            this.playerId = playerId;
            this.startDeadline = startDeadline;
        }
    }
}
