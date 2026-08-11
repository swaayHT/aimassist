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
    public static double PREDICTION_OFFSET = 0.5; // Співвідношення випередження / зсуву

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
        // Базова позиція хітбокса (центр тіла/очей)
        Vec3d targetHitboxPos = new Vec3d(target.getX(), target.getEyeY() - 0.2, target.getZ());

        // Додаємо випередження (Prediction) та зміщення вправо відносно вектора погляду цілі або руху
        if (PREDICTION_OFFSET > 0) {
            // Враховуємо швидкість руху цілі (швидкість бігу/польоту)
            Vec3d targetVelocity = target.getVelocity();
            targetHitboxPos = targetHitboxPos.add(targetVelocity.multiply(PREDICTION_OFFSET * 5.0));

            // Зміщуємо трохи вправо від цілі (перпендикулярно погляду) за допомогою rotation vector
            Vec3d rotationVec = target.getRotationVector();
            Vec3d rightVector = new Vec3d(-rotationVec.z, 0, rotationVec.x).normalize();
            targetHitboxPos = targetHitboxPos.add(rightVector.multiply(PREDICTION_OFFSET));
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

        // Плавне згладжування (множимо на AIM_SPEED) і обмеження ривків
        float newYaw = currentYaw + (yawDelta * (float) AIM_SPEED);
        float newPitch = currentPitch + (pitchDelta * (float) AIM_SPEED);

        // Застосовуємо значення до камери гравця без зайвого тремтіння
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
