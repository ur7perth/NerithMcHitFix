
package com.nerith.mc;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public record CombatSnapshot(
        UUID playerId,
        Location location,
        Location eyeLocation,
        double velocityX,
        double velocityY,
        double velocityZ,
        boolean airborne,
        float attackCooldown,
        long tick
) {
    public static CombatSnapshot capture(Player player, long tick) {
        var velocity = player.getVelocity();

        return new CombatSnapshot(
                player.getUniqueId(),
                player.getLocation().clone(),
                player.getEyeLocation().clone(),
                velocity.getX(),
                velocity.getY(),
                velocity.getZ(),
                !player.isOnGround(),
                player.getAttackCooldown(),
                tick
        );
    }
}
