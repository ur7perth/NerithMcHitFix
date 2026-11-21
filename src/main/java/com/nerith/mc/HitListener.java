
package com.nerith.mc;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Locale;

public final class HitListener implements Listener {

    private final NerithMc plugin;
    private final HitTracker tracker;

    public HitListener(NerithMc plugin, HitTracker tracker) {
        this.plugin = plugin;
        this.tracker = tracker;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPreAttack(PrePlayerAttackEntityEvent event) {

        if (!plugin.isEnabledSystem()) return;
        if (!event.willAttack()) return;

        if (!(event.getAttacked() instanceof Player target)) return;

        Player attacker = event.getPlayer();

        if (attacker.equals(target)) return;
        if (!attacker.getWorld().equals(target.getWorld())) return;

        HitTracker.AttackAttempt attempt =
                tracker.registerAttack(attacker, target);

        int delay = Math.max(1, Math.min(3,
                plugin.getConfig().getInt(
                        "combat.tracking-ticks", 1
                )
        ));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {

            if (!tracker.finishAttempt(attacker, attempt)) return;

            if (!plugin.isEnabledSystem()) return;
            if (!attacker.isOnline() || !target.isOnline()) return;

            if (attempt.wasDamageEventSeen()) {
                if (attempt.wasDamageCancelled()) {
                    debug("DAMAGE_EVENT_CANCELLED "
                            + attacker.getName() + " -> "
                            + target.getName());
                }

                return;
            }

            if (!plugin.getConfig().getBoolean("debug", false)) return;

            boolean bothGliding =
                    attacker.isGliding() && target.isGliding();

            double distance =
                    attacker.getLocation().distance(target.getLocation());

            debug(String.format(
                    Locale.US,
                    "ATTACK_WITHOUT_DAMAGE_EVENT attacker=%s target=%s " +
                    "elytra=%s distance=%.3f " +
                    "attackerCooldown=%.3f targetCooldown=%.3f " +
                    "attackerVelocity=(%.3f,%.3f,%.3f) " +
                    "targetVelocity=(%.3f,%.3f,%.3f) tick=%d",

                    attacker.getName(),
                    target.getName(),
                    bothGliding,
                    distance,

                    attacker.getAttackCooldown(),
                    target.getAttackCooldown(),

                    attacker.getVelocity().getX(),
                    attacker.getVelocity().getY(),
                    attacker.getVelocity().getZ(),

                    target.getVelocity().getX(),
                    target.getVelocity().getY(),
                    target.getVelocity().getZ(),

                    attempt.getCreatedTick()
            ));

        }, delay);
    }

    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = false
    )
    public void onDamage(EntityDamageByEntityEvent event) {

        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player target)) return;

        tracker.markDamage(
                attacker,
                target,
                event.isCancelled(),
                event.getFinalDamage()
        );
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        tracker.remove(event.getPlayer());
    }

    private void debug(String message) {
        if (!plugin.getConfig().getBoolean("debug", false)) return;

        plugin.getLogger().info("[CombatDebug] " + message);
    }
}
