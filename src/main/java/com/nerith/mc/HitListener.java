
package com.nerith.mc;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Locale;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class HitListener implements Listener {

    private final NerithMc plugin;
    private final HitTracker tracker;

    private final Map<UUID, HitTracker.AttackAttempt> pending =
            new ConcurrentHashMap<>();

    public HitListener(NerithMc plugin, HitTracker tracker) {
        this.plugin = plugin;
        this.tracker = tracker;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPreAttack(PrePlayerAttackEntityEvent event) {

        if (!plugin.isEnabledSystem()) return;
        if (!event.willAttack()) return;

        if (!(event.getAttacked() instanceof Player target)) return;

        Player attacker = event.getPlayer();

        if (attacker.equals(target)) return;
        if (!attacker.getWorld().equals(target.getWorld())) return;

        HitTracker.AttackAttempt attempt =
                tracker.registerAttack(attacker, target);

        pending.put(attacker.getUniqueId(), attempt);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {

            if (pending.get(attacker.getUniqueId()) != attempt) return;

            pending.remove(attacker.getUniqueId());
            tracker.finishAttempt(attacker, attempt);

            if (!attacker.isOnline() || !target.isOnline()) return;

            if (attempt.wasDamageEventSeen()) {

                if (attempt.wasDamageCancelled()) {
                    alert(ChatColor.RED + "DAMAGE CANCELLED "
                            + ChatColor.GRAY + attacker.getName()
                            + " -> " + target.getName());
                }

                return;
            }

            if (!attacker.isGliding() || !target.isGliding()) return;

            double distance = attacker.getLocation()
                    .distance(target.getLocation());

            double reach = plugin.getConfig().getDouble(
                    "combat.reach", 4.5
            );

            if (distance > reach + 1.5) return;

            alert(ChatColor.RED + "POSSIBLE ELYTRA GHOST HIT "
                    + ChatColor.GRAY + attacker.getName()
                    + " -> " + target.getName()
                    + ChatColor.YELLOW
                    + " | Distance: "
                    + String.format(Locale.US, "%.2f", distance));

        }, 1L);
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
        UUID id = event.getPlayer().getUniqueId();

        pending.remove(id);
        tracker.remove(event.getPlayer());
    }

    private void alert(String message) {

        String fullMessage =
                ChatColor.GOLD + "[NerithMc] " + message;

        plugin.getLogger().info(
                ChatColor.stripColor(fullMessage)
        );

        plugin.sendAlert(message);
    }
}
