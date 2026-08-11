package com.example.aimassist;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AimAssistEngine {

    public static boolean ENABLED = true;
    public static double MAX_DISTANCE = 30.0;
    public static double FOV = 60.0;
    public static double AIM_SPEED = 0.25;
    public static double PREDICTION_DISTANCE_OFFSET = 1.0;
    public static double BULLET_SPEED = 3.5;

    private static PlayerEntity currentTarget = null;

    public static void onClientTick(MinecraftClient mc) {
        if (!ENABLED || mc.player == null || mc.world == null || mc.currentScreen != null) {
            currentTarget = null;
            return;
        }

        currentTarget = getOrUpdateTarget(mc);

        if (currentTarget != null) {
            aimAtTarget(mc, currentTarget);
        }
    }

    private static PlayerEntity getOrUpdateTarget(MinecraftClient mc) {
        if (currentTarget != null) {
            if (currentTarget.isAlive() 
                    && !currentTarget.isInvisible() 
                    && mc.player.distanceTo(currentTarget) <= MAX_DISTANCE) {
                return currentTarget;
            }
        }

        PlayerEntity bestTarget = null;
        double closestFov = FOV;

        for (PlayerEntity entity : mc.world.getPlayers()) {
            if (entity == mc.player || !entity.isAlive() || entity.isInvisible()) continue;

            if (mc.player.distanceTo(entity) > MAX_DISTANCE) continue;

            double fovToEntity = getFovToEntity(mc, entity);
            if (fovToEntity < closestFov) {
                closestFov = fovToEntity;
                bestTarget = entity;
            }
        }
        return bestTarget;
    }

    private static void aimAtTarget(MinecraftClient mc, Entity target) {
        // У 1.21.4 використовуємо getEyeY() для точної висоти очей
        Vec3d targetHitboxPos = new Vec3d(target.getX(), target.getEyeY() - 0.2, target.getZ());

        if (PREDICTION_DISTANCE_OFFSET > 0) {
            double distance = mc.player.distanceTo(target);
            double timeToTarget = distance / Math.max(0.1, BULLET_SPEED);
            
            Vec3d targetVelocity = target.getVelocity();
            Vec3d predictedMovement = targetVelocity.multiply(timeToTarget * 20.0);
            
            targetHitboxPos = targetHitboxPos.add(predictedMovement.multiply(PREDICTION_DISTANCE_OFFSET));
        }

        Vec3d eyePos = mc.player.getEyePos();
        double dx = targetHitboxPos.x - eyePos.x;
        double dy = targetHitboxPos.y - eyePos.y;
        double dz = targetHitboxPos.z - eyePos.z;

        double horizontalDistance = Math.hypot(dx, dz);

        float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, horizontalDistance));

        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();

        float yawDelta = MathHelper.wrapDegrees(targetYaw - currentYaw);
        float pitchDelta = targetPitch - currentPitch;

        float newYaw = currentYaw + (float) (yawDelta * AIM_SPEED);
        float newPitch = currentPitch + (float) (pitchDelta * AIM_SPEED);

        mc.player.setYaw(newYaw);
        mc.player.setPitch(MathHelper.clamp(newPitch, -90.0F, 90.0F));
    }

    private static double getFovToEntity(MinecraftClient mc, Entity entity) {
        Vec3d eyePos = mc.player.getEyePos();
        double dx = entity.getX() - eyePos.x;
        double dz = entity.getZ() - eyePos.z;

        float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float yawDelta = MathHelper.wrapDegrees(targetYaw - mc.player.getYaw());

        return Math.abs(yawDelta);
    }
}
