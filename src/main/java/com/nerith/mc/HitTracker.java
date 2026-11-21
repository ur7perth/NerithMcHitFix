
package com.nerith.mc;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HitTracker {

    private final NerithMc plugin;
    private final MovementTracker movementTracker;

    private final Map<UUID, Long> swings = new HashMap<>();

    private BukkitTask task;
    private long tick;

    public HitTracker(NerithMc plugin) {
        this.plugin = plugin;
        this.movementTracker = new MovementTracker(plugin);
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(
                plugin,
                () -> {
                    if (!plugin.isEnabledSystem()) return;

                    tick++;

                    movementTracker.update(tick);

                    swings.entrySet().removeIf(
                            entry -> tick - entry.getValue() > 5
                    );
                },
                1L,
                1L
        );
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }

        clear();
    }

    public void registerSwing(Player player) {
        swings.put(player.getUniqueId(), tick);
    }

    public boolean hasRecentSwing(Player player) {
        Long time = swings.get(player.getUniqueId());

        return time != null && tick - time <= 5;
    }

    public void consumeSwing(Player player) {
        swings.remove(player.getUniqueId());
    }

    public CombatSnapshot getSnapshot(Player player) {
        return movementTracker.get(player.getUniqueId());
    }

    public long getTick() {
        return tick;
    }

    public int size() {
        return swings.size();
    }

    public void clear() {
        swings.clear();
        movementTracker.clear();
    }
}
