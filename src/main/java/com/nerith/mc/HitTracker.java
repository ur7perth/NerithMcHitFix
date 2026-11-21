
package com.nerith.mc;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class HitTracker {

    private final NerithMc plugin;
    private final MovementTracker movementTracker;
    private final Map<UUID, AttackAttempt> attempts = new HashMap<>();

    private BukkitTask task;
    private long tick;

    public HitTracker(NerithMc plugin) {
        this.plugin = plugin;
        this.movementTracker = new MovementTracker(plugin);
    }

    public void start() {
        if (task != null) task.cancel();

        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!plugin.isEnabledSystem()) return;

            tick++;
            movementTracker.update(tick);

        }, 1L, 1L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }

        clear();
    }

    public AttackAttempt registerAttack(Player attacker, Player target) {
        AttackAttempt attempt = new AttackAttempt(
                target.getUniqueId(),
                tick,
                System.nanoTime()
        );

        attempts.put(attacker.getUniqueId(), attempt);
        return attempt;
    }

    public AttackAttempt getAttempt(Player attacker) {
        return attempts.get(attacker.getUniqueId());
    }

    public boolean markDamage(
            Player attacker,
            Player target,
            boolean cancelled,
            double finalDamage
    ) {
        AttackAttempt attempt = attempts.get(attacker.getUniqueId());

        if (attempt == null ||
                !attempt.targetId.equals(target.getUniqueId())) {
            return false;
        }

        attempt.damageEventSeen = true;
        attempt.damageCancelled = cancelled;
        attempt.finalDamage = finalDamage;

        return true;
    }

    public boolean finishAttempt(Player attacker, AttackAttempt expected) {
        if (attempts.get(attacker.getUniqueId()) != expected) {
            return false;
        }

        attempts.remove(attacker.getUniqueId());
        return true;
    }

    public void remove(Player player) {
        attempts.remove(player.getUniqueId());
        movementTracker.remove(player.getUniqueId());
    }

    public CombatSnapshot getSnapshot(Player player) {
        return movementTracker.get(player.getUniqueId());
    }

    public long getTick() {
        return tick;
    }

    public int size() {
        return attempts.size();
    }

    public void clear() {
        attempts.clear();
        movementTracker.clear();
    }

    public static final class AttackAttempt {

        private final UUID targetId;
        private final long createdTick;
        private final long createdNanos;

        private boolean damageEventSeen;
        private boolean damageCancelled;
        private double finalDamage;

        private AttackAttempt(
                UUID targetId,
                long createdTick,
                long createdNanos
        ) {
            this.targetId = targetId;
            this.createdTick = createdTick;
            this.createdNanos = createdNanos;
        }

        public UUID getTargetId() {
            return targetId;
        }

        public long getCreatedTick() {
            return createdTick;
        }

        public long getCreatedNanos() {
            return createdNanos;
        }

        public boolean wasDamageEventSeen() {
            return damageEventSeen;
        }

        public boolean wasDamageCancelled() {
            return damageCancelled;
        }

        public double getFinalDamage() {
            return finalDamage;
        }
    }
}
