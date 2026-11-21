
package com.nerith.mc;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class NerithMc extends JavaPlugin {

    private HitTracker hitTracker;
    private HitListener hitListener;

    private boolean enabled;
    private boolean alerts;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        enabled = getConfig().getBoolean("enabled", true);
        alerts = getConfig().getBoolean("alerts", true);

        hitTracker = new HitTracker(this);
        hitListener = new HitListener(this, hitTracker);

        Bukkit.getPluginManager().registerEvents(hitListener, this);

        hitTracker.start();

        getLogger().info("NerithMc enabled.");
    }

    @Override
    public void onDisable() {
        if (hitTracker != null) {
            hitTracker.stop();
        }

        getLogger().info("NerithMc disabled.");
    }

    public boolean isEnabledSystem() {
        return enabled;
    }

    public boolean areAlertsEnabled() {
        return alerts;
    }

    public void setEnabledSystem(boolean value) {
        enabled = value;
        getConfig().set("enabled", value);
        saveConfig();

        if (!value) {
            hitTracker.clear();
        }
    }

    public void setAlerts(boolean value) {
        alerts = value;
        getConfig().set("alerts", value);
        saveConfig();
    }

    public void sendAlert(String message) {
        if (!alerts) return;

        String prefix = ChatColor.GOLD + "[NerithMc] ";

        for (var player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("nerithmc.alerts")) {
                player.sendMessage(prefix + message);
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
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            sender.sendMessage(ChatColor.GOLD + "NerithMc");
            sender.sendMessage("Enabled: " + enabled);
            sender.sendMessage("Alerts: " + alerts);
            sender.sendMessage("Tracked: " + hitTracker.size());
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "on" -> setEnabledSystem(true);
            case "off" -> setEnabledSystem(false);
            case "alerts" -> setAlerts(!alerts);

            case "reload" -> {
                reloadConfig();
                setEnabledSystem(getConfig().getBoolean("enabled", true));
                setAlerts(getConfig().getBoolean("alerts", true));
                hitTracker.clear();
            }

            default -> sender.sendMessage(
                    "/nerithmc [status|on|off|alerts|reload]"
            );
        }

        return true;
    }
}
