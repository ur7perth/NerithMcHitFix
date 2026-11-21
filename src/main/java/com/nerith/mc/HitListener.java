
package com.nerith.mc;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HitListener implements Listener {

    private final NerithMc plugin;
    private final HitTracker tracker;
    private final AimDetector aimDetector = new AimDetector();

    private final Map<UUID, PendingHit> pending = new HashMap<>();

    private static class PendingHit {
        final UUID target;
        final long createdAt;

        PendingHit(UUID target, long createdAt) {
            this.target = target;
            this.createdAt = createdAt;
        }
    }

    public HitListener(NerithMc plugin, HitTracker tracker) {
        this.plugin = plugin;
        this.tracker = tracker;
    }

    @EventHandler
    public void onSwing(PlayerAnimationEvent event) {

        if (!plugin.isEnabledSystem()) return;

        if (event.getAnimationType()
                != PlayerAnimationType.ARM_SWING) {
            return;
        }

        Player attacker = event.getPlayer();

        tracker.registerSwing(attacker);

        double reach = plugin.getConfig().getDouble(
                "combat.reach", 4.5
        );

        Player target = aimDetector.findTarget(attacker, reach);

        if (target == null) {
            return;
        }

        PendingHit hit = new PendingHit(
                target.getUniqueId(),
                tracker.getTick()
        );

        pending.put(attacker.getUniqueId(), hit);

        alert(
                ChatColor.YELLOW + "SWING TARGET "
                + ChatColor.GRAY + attacker.getName()
                + " -> " + target.getName()
        );

        Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> {
                    PendingHit current =
                            pending.get(attacker.getUniqueId());

                    if (current != hit) return;

                    pending.remove(attacker.getUniqueId());

                    alert(
                            ChatColor.RED + "POSSIBLE GHOST HIT "
                            + ChatColor.GRAY + attacker.getName()
                            + " -> " + target.getName()
                            + " | No matching damage event"
                    );
                },
                2L
        );
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDamage(EntityDamageByEntityEvent event) {

        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player target)) return;

        PendingHit hit = pending.get(attacker.getUniqueId());

        if (hit == null) return;

        if (!hit.target.equals(target.getUniqueId())) return;

        pending.remove(attacker.getUniqueId());

        tracker.consumeSwing(attacker);

        if (event.isCancelled()) {
            alert(
                    ChatColor.RED + "DAMAGE CANCELLED "
                    + ChatColor.GRAY + attacker.getName()
                    + " -> " + target.getName()
            );
            return;
        }

        alert(
                ChatColor.GREEN + "HIT CONFIRMED "
                + ChatColor.GRAY + attacker.getName()
                + " -> " + target.getName()
                + " | Damage: "
                + String.format("%.2f", event.getFinalDamage())
        );
    }

    private void alert(String message) {

        String prefix = ChatColor.GOLD + "[NerithMc] ";

        plugin.getLogger().info(
                ChatColor.stripColor(message)
        );

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("nerithmc.alerts")) {
                player.sendMessage(prefix + message);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();

        pending.remove(id);
        tracker.consumeSwing(event.getPlayer());
    }
}
