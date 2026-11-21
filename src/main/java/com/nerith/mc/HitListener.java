
package com.nerith.mc;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class HitListener implements Listener {

    private final NerithMc plugin;
    private final HitTracker tracker;
    private final AimDetector aimDetector = new AimDetector();

    public HitListener(NerithMc plugin, HitTracker tracker) {
        this.plugin = plugin;
        this.tracker = tracker;
    }

    @EventHandler
    public void onSwing(PlayerAnimationEvent event) {

        if (!plugin.isEnabledSystem()) return;

        Player attacker = event.getPlayer();

        tracker.registerSwing(attacker);

        double reach = plugin.getConfig().getDouble(
                "combat.reach",
                4.5
        );

        Player target = aimDetector.findTarget(attacker, reach);

        if (target == null) return;

        CombatSnapshot snapshot = tracker.getSnapshot(target);

        if (snapshot == null) return;

        if (snapshot.airborne() && !target.isOnGround()) {
            // Air-to-air tracking information is available.
            // No artificial damage is applied here.
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {

        if (!plugin.isEnabledSystem()) return;

        if (!(event.getDamager() instanceof Player attacker)) return;

        if (!(event.getEntity() instanceof Player target)) return;

        if (!tracker.hasRecentSwing(attacker)) return;

        tracker.consumeSwing(attacker);

        if (event.isCancelled()) return;

        String message =
                ChatColor.GREEN + "HIT CONFIRMED "
                + ChatColor.GRAY + attacker.getName()
                + ChatColor.WHITE + " -> "
                + ChatColor.AQUA + target.getName()
                + ChatColor.GRAY + " | Damage: "
                + ChatColor.YELLOW
                + String.format("%.2f", event.getFinalDamage());

        plugin.sendAlert(message);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        tracker.consumeSwing(event.getPlayer());
    }
}
