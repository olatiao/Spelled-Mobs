package com.spelledmobs.manager;

import java.util.List;
import java.util.Comparator;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.item.ItemStack;
import com.spelledmobs.SpelledMobs;
import com.spelledmobs.config.SpelledMobsConfig;

/**
 * 负责寻找施法目标的类
 */
public class TargetFinder {
    private static final double MIN_DISTANCE = 3.0;
    private static final double MAX_DISTANCE = 32.0;
    private static final double MAX_HEIGHT_DIFF = 8.0;
    private static final double VISIBILITY_THRESHOLD = 0.5;

    /**
     * 记录调试日志
     */
    private static void logDebug(String message, Object... args) {
        if (SpelledMobsConfig.isDebugLoggingEnabled()) {
            SpelledMobs.LOGGER.debug(message, args);
        }
    }

    /**
     * 验证实体是否有效
     */
    private static boolean isValidEntity(LivingEntity entity) {
        return entity != null && !entity.isRemoved() && !entity.isDead();
    }

    /**
     * 计算两点之间的距离
     */
    private static double calculateDistance(Vec3d pos1, Vec3d pos2) {
        return pos1.distanceTo(pos2);
    }

    /**
     * 计算两点之间的高度差
     */
    private static double calculateHeightDifference(Vec3d pos1, Vec3d pos2) {
        return Math.abs(pos1.y - pos2.y);
    }

    /**
     * 计算视野角度
     */
    private static double calculateViewAngle(Vec3d lookVec, Vec3d direction) {
        return lookVec.dotProduct(direction.normalize());
    }

    /**
     * 检查目标是否在视野范围内
     */
    private static boolean isInViewRange(LivingEntity caster, LivingEntity target) {
        Vec3d casterPos = caster.getEyePos();
        Vec3d targetPos = target.getEyePos();
        Vec3d direction = targetPos.subtract(casterPos);
        Vec3d lookVec = caster.getRotationVec(1.0f);

        double distance = calculateDistance(casterPos, targetPos);
        double heightDiff = calculateHeightDifference(casterPos, targetPos);
        double viewAngle = calculateViewAngle(lookVec, direction);

        return distance >= MIN_DISTANCE &&
                distance <= MAX_DISTANCE &&
                heightDiff <= MAX_HEIGHT_DIFF &&
                viewAngle >= VISIBILITY_THRESHOLD;
    }

    /**
     * 计算目标的护甲因子
     */
    private static double calculateArmorFactor(LivingEntity target) {
        double armorFactor = 0.0;
        int armorCount = 0;

        for (ItemStack stack : target.getArmorItems()) {
            if (!stack.isEmpty()) {
                armorCount++;
            }
        }

        if (armorCount > 0) {
            armorFactor = 0.1 * armorCount;
        }

        return armorFactor;
    }

    /**
     * 计算目标的速度因子
     */
    private static double calculateSpeedFactor(LivingEntity target) {
        return target.getVelocity().length() > 0.1 ? 0.2 : 0.0;
    }

    /**
     * 计算目标的高度因子
     */
    private static double calculateHeightFactor(LivingEntity target, LivingEntity caster) {
        double heightDiff = target.getY() - caster.getY();
        return heightDiff > 0 ? 0.2 : (heightDiff < 0 ? -0.1 : 0.0);
    }

    /**
     * 计算目标的可见性因子
     */
    private static double calculateVisibilityFactor(LivingEntity target, LivingEntity caster) {
        if (caster.canSee(target)) {
            return 0.3;
        }

        Vec3d casterPos = caster.getEyePos();
        Vec3d targetPos = target.getEyePos();
        Vec3d direction = targetPos.subtract(casterPos);
        Vec3d lookVec = caster.getRotationVec(1.0f);
        double viewAngle = calculateViewAngle(lookVec, direction);

        return viewAngle > VISIBILITY_THRESHOLD ? 0.1 : 0.0;
    }

    /**
     * 计算目标的伤害因子
     */
    private static double calculateDamageFactor(LivingEntity target, LivingEntity caster) {
        double damageFactor = 0.0;

        if (target.getAttacking() == caster) {
            damageFactor += 0.3;
        }

        if (caster.getAttacking() == target) {
            damageFactor -= 0.1;
        }

        if (!target.getMainHandStack().isEmpty()) {
            damageFactor += 0.2;
        }

        return damageFactor;
    }

    /**
     * 计算目标的防御因子
     */
    private static double calculateDefenseFactor(LivingEntity target) {
        double defenseFactor = 0.0;

        int armorValue = target.getArmor();
        if (armorValue > 0) {
            defenseFactor += 0.1 * armorValue;
        }

        if (!target.getOffHandStack().isEmpty()) {
            defenseFactor += 0.2;
        }

        return defenseFactor;
    }

    /**
     * 计算目标的威胁因子
     */
    private static double calculateThreatFactor(LivingEntity target, LivingEntity caster) {
        double threatFactor = 0.0;

        // 如果目标是玩家，增加威胁因子
        if (target instanceof PlayerEntity) {
            threatFactor += 0.3;
        }

        // 如果目标正在攻击施法者，增加威胁因子
        if (target.getAttacking() == caster) {
            threatFactor += 0.2;
        }

        // 如果目标正在被施法者攻击，降低威胁因子
        if (caster.getAttacking() == target) {
            threatFactor -= 0.1;
        }

        // 考虑目标的装备情况
        boolean hasArmor = false;
        for (ItemStack stack : target.getArmorItems()) {
            if (!stack.isEmpty()) {
                hasArmor = true;
                break;
            }
        }
        if (hasArmor) {
            threatFactor += 0.1;
        }

        return threatFactor;
    }

    /**
     * 计算目标的优先级
     */
    private static double calculateTargetPriority(LivingEntity target, LivingEntity caster) {
        double distance = caster.distanceTo(target);
        double healthFactor = 1.0 - (target.getHealth() / target.getMaxHealth());
        double playerBonus = target instanceof PlayerEntity ? 0.5 : 0.0;
        double threatFactor = calculateThreatFactor(target, caster);
        double armorFactor = calculateArmorFactor(target);
        double speedFactor = calculateSpeedFactor(target);
        double heightFactor = calculateHeightFactor(target, caster);
        double visibilityFactor = calculateVisibilityFactor(target, caster);
        double damageFactor = calculateDamageFactor(target, caster);
        double defenseFactor = calculateDefenseFactor(target);

        double priority = (1.0 / (distance + 1.0)) *
                (1.0 + healthFactor + playerBonus + threatFactor + armorFactor + speedFactor +
                        heightFactor + visibilityFactor + damageFactor + defenseFactor);

        logDebug("目标优先级计算 - 实体: {}, 距离: {}, 健康: {}, 威胁: {}, 护甲: {}, 速度: {}, 高度: {}, 可见性: {}, 伤害: {}, 防御: {}, 最终优先级: {}",
                target.getUuid(), distance, healthFactor, threatFactor, armorFactor, speedFactor,
                heightFactor, visibilityFactor, damageFactor, defenseFactor, priority);

        return priority;
    }

    /**
     * 寻找施法目标
     */
    public static LivingEntity findTarget(LivingEntity caster, double searchRadius) {
        if (!isValidEntity(caster)) {
            logDebug("无效的施法者: {}", caster != null ? caster.getUuid() : "null");
            return null;
        }

        // 获取搜索范围内的所有实体
        Box searchBox = new Box(
                caster.getX() - searchRadius, caster.getY() - MAX_HEIGHT_DIFF, caster.getZ() - searchRadius,
                caster.getX() + searchRadius, caster.getY() + MAX_HEIGHT_DIFF, caster.getZ() + searchRadius);

        List<LivingEntity> potentialTargets = caster.getWorld()
                .getEntitiesByClass(LivingEntity.class, searchBox,
                        entity -> isValidEntity(entity) && entity != caster && isInViewRange(caster, entity));

        if (potentialTargets.isEmpty()) {
            logDebug("未找到合适的目标，施法者: {}", caster.getUuid());
            return null;
        }

        // 按优先级排序并选择最佳目标
        LivingEntity bestTarget = potentialTargets.stream()
                .max(Comparator.comparingDouble(target -> calculateTargetPriority(target, caster)))
                .orElse(null);

        if (bestTarget != null) {
            logDebug("找到最佳目标: {}, 优先级: {}",
                    bestTarget.getUuid(), calculateTargetPriority(bestTarget, caster));
        }

        return bestTarget;
    }
}