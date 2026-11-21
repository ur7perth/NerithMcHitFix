
package com.nerith.mc;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

public class AimDetector {

    public Player findTarget(Player attacker, double reach) {

        RayTraceResult result = attacker.getWorld().rayTraceEntities(
                attacker.getEyeLocation(),
                attacker.getEyeLocation().getDirection(),
                reach,
                0.3,
                entity -> entity instanceof Player
                        && entity != attacker
                        && entity.isValid()
                        && !entity.isDead()
        );

        if (result == null) return null;

        Entity entity = result.getHitEntity();

        return entity instanceof Player player ? player : null;
    }
}
