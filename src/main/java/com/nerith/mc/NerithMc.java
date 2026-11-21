
package com.nerith.mc;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class NerithMc extends JavaPlugin implements Listener {

    private final Map<UUID, SwingData> swings = new HashMap<>();

    private boolean active;
    private boolean alerts;
    private double reach;
    private int trackingTicks;

    private static final String PREFIX =
            ChatColor.GOLD + "[NerithMc] " + ChatColor.RESET;

    private static class SwingData {

        final UUID target;
        final long tick;
        final float cooldown;

        SwingData(UUID target, long tick, float cooldown) {
            this.target = target;
            this.tick = tick;
            this.cooldown = cooldown;
        }
    }

    @Override
    public void onEnable() {

        saveDefaultConfig();
        loadSettings();

        Bukkit.getPluginManager().registerEvents(this, this);

        Bukkit.getScheduler().runTaskTimer(
                this,
                () -> {
                    long now = Bukkit.getCurrentTick();

                    swings.entrySet().removeIf(
                            entry -> now - entry.getValue().tick
                                    > trackingTicks
                    );
                },
                1L,
                1L
        );

        getLogger().info("NerithMc enabled successfully.");
    }

    @Override
    public void onDisable() {

        swings.clear();

        getLogger().info("NerithMc disabled.");
    }

    private void loadSettings() {

        active = getConfig().getBoolean("enabled", true);

        alerts = getConfig().getBoolean("alerts", true);

        reach = getConfig().getDouble("reach", 4.5);

        trackingTicks = getConfig().getInt("tracking-ticks", 5);

        trackingTicks = Math.max(1, trackingTicks);

        reach = Math.max(1.0, Math.min(6.0, reach));
    }

    @EventHandler
    public void onSwing(PlayerAnimationEvent event) {

        if (!active) return;

        if (event.getAnimationType()
                != PlayerAnimationType.ARM_SWING) {
            return;
        }

        Player attacker = event.getPlayer();

        if (!attacker.isOnline() || attacker.isDead()) return;

        RayTraceResult result = attacker.getWorld().rayTraceEntities(
                attacker.getEyeLocation(),
                attacker.getEyeLocation().getDirection(),
                reach,
                0.15,
                entity -> entity instanceof Player
                        && entity != attacker
                        && entity.isValid()
                        && !entity.isDead()
        );

        if (result == null || result.getHitEntity() == null) {
            swings.remove(attacker.getUniqueId());
            return;
        }

        Entity target = result.getHitEntity();

        if (!(target instanceof Player)) return;

        swings.put(
                attacker.getUniqueId(),
                new SwingData(
                        target.getUniqueId(),
                        Bukkit.getCurrentTick(),
                        attacker.getAttackCooldown()
                )
        );
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {

        if (!active) return;

        if (!(event.getDamager() instanceof Player attacker)) return;

        if (!(event.getEntity() instanceof Player target)) return;

        UUID attackerId = attacker.getUniqueId();

        SwingData data = swings.get(attackerId);

        if (data == null) return;

        long elapsed = Bukkit.getCurrentTick() - data.tick;

        if (elapsed > trackingTicks) {
            swings.remove(attackerId);
            return;
        }

        if (!data.target.equals(target.getUniqueId())) {
            return;
        }

        swings.remove(attackerId);

        if (alerts) {
            sendTrackedAlert(attacker, target, data, event);
        }
    }

    private void sendTrackedAlert(
            Player attacker,
            Player target,
            SwingData data,
            EntityDamageByEntityEvent event
    ) {

        String message =
                ChatColor.GOLD + "[NerithMc] "
                + ChatColor.GREEN + "HIT CONFIRMED "
                + ChatColor.GRAY + attacker.getName()
                + ChatColor.WHITE + " -> "
                + ChatColor.AQUA + target.getName()
                + ChatColor.GRAY + " | Damage: "
                + ChatColor.YELLOW
                + String.format("%.2f", event.getFinalDamage())
                + ChatColor.GRAY + " | Cooldown: "
                + ChatColor.YELLOW
                + String.format("%.2f", data.cooldown);

        for (Player player : Bukkit.getOnlinePlayers()) {

            if (player.hasPermission("nerithmc.alerts")) {
                player.sendMessage(message);
            }
        }
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (!command.getName().equalsIgnoreCase("nerithmc")) {
            return false;
        }

        if (!sender.hasPermission("nerithmc.admin")) {

            sender.sendMessage(
                    PREFIX + ChatColor.RED
                    + "You don't have permission."
            );

            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {

            sender.sendMessage(PREFIX + ChatColor.YELLOW
                    + "Version: " + getDescription().getVersion());

            sender.sendMessage(PREFIX + ChatColor.YELLOW
                    + "Enabled: " + active);

            sender.sendMessage(PREFIX + ChatColor.YELLOW
                    + "Alerts: " + alerts);

            sender.sendMessage(PREFIX + ChatColor.YELLOW
                    + "Tracking: " + swings.size());

            return true;
        }

        switch (args[0].toLowerCase()) {

            case "on" -> {

                active = true;

                getConfig().set("enabled", true);

                saveConfig();

                swings.clear();

                sender.sendMessage(
                        PREFIX + ChatColor.GREEN
                        + "NerithMc enabled."
                );
            }

            case "off" -> {

                active = false;

                getConfig().set("enabled", false);

                saveConfig();

                swings.clear();

                sender.sendMessage(
                        PREFIX + ChatColor.RED
                        + "NerithMc disabled."
                );
            }

            case "alerts" -> {

                alerts = !alerts;

                getConfig().set("alerts", alerts);

                saveConfig();

                sender.sendMessage(
                        PREFIX + ChatColor.YELLOW
                        + "Alerts: " + alerts
                );
            }

            case "reload" -> {

                reloadConfig();

                loadSettings();

                swings.clear();

                sender.sendMessage(
                        PREFIX + ChatColor.GREEN
                        + "Configuration reloaded."
                );
            }

            default -> sender.sendMessage(
                    PREFIX + ChatColor.YELLOW
                    + "/nerithmc [status|on|off|alerts|reload]"
            );
        }

        return true;
    }
}
