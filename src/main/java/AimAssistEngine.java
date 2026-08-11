import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class AimAssistEngine {

    public static boolean ENABLED = true;
    public static boolean CHECK_WALLS = true;
    public static double MAX_DISTANCE = 30.0;
    public static double FOV = 60.0;
    public static double AIM_SPEED = 0.25;
    public static double PREDICTION_OFFSET = 0.5; // Сила випередження/зміщення при русі

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
        if (currentTarget != null && isValidTarget(mc, currentTarget)) {
            return currentTarget;
        }

        PlayerEntity bestTarget = null;
        double closestFov = FOV;

        for (PlayerEntity entity : mc.world.getPlayers()) {
            if (entity == mc.player || !isValidTarget(mc, entity)) continue;

            double fovToEntity = getFovToEntity(mc, entity);
            if (fovToEntity < closestFov) {
                closestFov = fovToEntity;
                bestTarget = entity;
            }
        }
        return bestTarget;
    }

    private static boolean isValidTarget(MinecraftClient mc, PlayerEntity entity) {
        if (!entity.isAlive() || entity.isInvisible() || mc.player.distanceTo(entity) > MAX_DISTANCE) {
            return false;
        }

        if (CHECK_WALLS) {
            Vec3d eyePos = mc.player.getEyePos();
            Vec3d targetPos = entity.getEyePos();
            HitResult result = mc.world.raycast(new RaycastContext(
                    eyePos, targetPos,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    mc.player
            ));
            if (result.getType() != HitResult.Type.MISS) {
                return false;
            }
        }
        return true;
    }

    private static void aimAtTarget(MinecraftClient mc, Entity target) {
        // Базова позиція цілі
        Vec3d targetHitboxPos = new Vec3d(target.getX(), target.getEyeY() - 0.2, target.getZ());

        // Отримуємо швидкість руху цілі
        Vec3d velocity = target.getVelocity();
        double horizontalSpeed = Math.hypot(velocity.x, velocity.z);

        // Якщо ціль рухається (швидкість більша за мінімальний поріг)
        if (horizontalSpeed > 0.01 && PREDICTION_OFFSET > 0) {
            // 1. Стандартне випередження по напрямку руху
            targetHitboxPos = targetHitboxPos.add(velocity.multiply(PREDICTION_OFFSET * 4.0));

            // 2. Визначаємо, куди саме рухається гравець відносно нашого екрану (вправо чи вліво)
            Vec3d playerLookVec = mc.player.getRotationVector();
            // Перпендикулярний вектор (праворуч від нашого погляду)
            Vec3d rightVector = new Vec3d(-playerLookVec.z, 0, playerLookVec.x).normalize();

            // Скалярний добуток визначає, чи біжить ціль праворуч чи ліворуч від нас
            Vec3d moveDir = new Vec3d(velocity.x, 0, velocity.z).normalize();
            double sideMultiplier = moveDir.dotProduct(rightVector);

            // Зміщуємо приціл в той бік, куди він рухається (вправо або вліво)
            targetHitboxPos = targetHitboxPos.add(rightVector.multiply(sideMultiplier * PREDICTION_OFFSET));
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

        float newYaw = currentYaw + (yawDelta * (float) AIM_SPEED);
        float newPitch = currentPitch + (pitchDelta * (float) AIM_SPEED);

        mc.player.setYaw(newYaw);
        mc.player.setPitch(MathHelper.clamp(newPitch, -90.0F, 90.0F));
    }

    private static double getFovToEntity(MinecraftClient mc, Entity entity) {
        Vec3d eyePos = mc.player.getEyePos();
        double dx = entity.getX() - eyePos.x;
        double dz = entity.getZ() - eyePos.z;

        float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        return Math.abs(MathHelper.wrapDegrees(targetYaw - mc.player.getYaw()));
    }
}
