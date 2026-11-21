
package com.nerith.mc;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MovementTracker {

    private final NerithMc plugin;

    private final Map<UUID, CombatSnapshot> snapshots =
            new HashMap<>();

    public MovementTracker(NerithMc plugin) {
        this.plugin = plugin;
    }

    public void update(long tick) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isValid() || player.isDead()) continue;

            snapshots.put(
                    player.getUniqueId(),
                    CombatSnapshot.capture(player, tick)
            );
        }
    }

    public CombatSnapshot get(UUID uuid) {
        return snapshots.get(uuid);
    }

    public void remove(UUID uuid) {
        snapshots.remove(uuid);
    }

    public void clear() {
        snapshots.clear();
    }

    public int size() {
        return snapshots.size();
    }
}
