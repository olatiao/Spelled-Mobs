package com.spelledmobs.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * 目标查找器工具类，用于查找附近的实体目标
 */
public class TargetFinder {

    /**
     * 查找给定实体周围的最适合目标
     * 
     * @param entity 寻找目标的实体
     * @param range  查找半径
     * @return 找到的目标实体，如果没有找到则返回null
     */
    public static LivingEntity findTarget(LivingEntity entity, double range) {
        if (entity == null || entity.level() == null) {
            return null;
        }

        // 创建一个以实体为中心的AABB
        AABB boundingBox = entity.getBoundingBox().inflate(range);

        // 获取边界内的所有LivingEntity
        List<LivingEntity> nearbyEntities = entity.level().getEntitiesOfClass(
                LivingEntity.class,
                boundingBox,
                target -> isValidTarget(entity, target));

        // 如果没有找到任何目标，返回null
        if (nearbyEntities.isEmpty()) {
            return null;
        }

        // 按照优先级和距离排序 - 玩家优先，然后是生物
        Optional<LivingEntity> target = nearbyEntities.stream()
                .min(Comparator
                        .<LivingEntity>comparingInt(e -> (e instanceof Player) ? 0 : ((e instanceof Mob) ? 1 : 2))
                        .thenComparingDouble(e -> e.distanceToSqr(entity)));

        return target.orElse(null);
    }

    /**
     * 根据类型查找给定实体周围的所有目标
     * 
     * @param entity      寻找目标的实体
     * @param range       查找半径
     * @param targetClass 目标实体类型
     * @param maxCount    最大返回数量
     * @return 找到的目标实体列表
     */
    public static <T extends LivingEntity> List<T> findTargetsByType(
            LivingEntity entity,
            double range,
            Class<T> targetClass,
            int maxCount) {

        if (entity == null || entity.level() == null) {
            return new ArrayList<>();
        }

        // 创建一个以实体为中心的AABB
        AABB boundingBox = entity.getBoundingBox().inflate(range);

        // 获取边界内的所有指定类型的实体
        List<T> nearbyEntities = entity.level().getEntitiesOfClass(
                targetClass,
                boundingBox,
                target -> isValidTarget(entity, target));

        // 按距离排序
        nearbyEntities.sort(Comparator.comparingDouble(e -> e.distanceToSqr(entity)));

        // 如果超过最大数量，截取列表
        if (nearbyEntities.size() > maxCount) {
            return nearbyEntities.subList(0, maxCount);
        }

        return nearbyEntities;
    }

    /**
     * 判断目标是否是有效的
     * 
     * @param entity 寻找目标的实体
     * @param target 潜在的目标
     * @return 如果目标有效则返回true
     */
    private static boolean isValidTarget(LivingEntity entity, LivingEntity target) {
        // 避免自我定位
        if (entity == target) {
            return false;
        }

        // 确保目标存活
        if (!target.isAlive()) {
            return false;
        }

        // 如果实体是Mob，检查其目标
        if (entity instanceof Mob mob) {
            // 如果已经有目标且目标不是当前检查的实体，则返回false
            LivingEntity existingTarget = mob.getTarget();
            if (existingTarget != null && existingTarget != target) {
                return false;
            }

            // 检查Mob是否可以看到目标
            return mob.getSensing().hasLineOfSight(target);
        }

        // 如果是玩家，避免定位创造模式玩家
        if (target instanceof Player player && player.isCreative()) {
            return false;
        }

        // 确保可以看见目标（使用hasLineOfSight方法代替canSee）
        return entity.hasLineOfSight(target);
    }

    /**
     * 使用自定义条件查找目标
     * 
     * @param entity 寻找目标的实体
     * @param range  查找半径
     * @param filter 自定义过滤条件
     * @return 找到的目标实体，如果没有找到则返回null
     */
    public static LivingEntity findTargetWithFilter(LivingEntity entity, double range, Predicate<LivingEntity> filter) {
        if (entity == null || entity.level() == null) {
            return null;
        }

        // 创建一个以实体为中心的AABB
        AABB boundingBox = entity.getBoundingBox().inflate(range);

        // 获取边界内的所有符合条件的LivingEntity
        List<LivingEntity> nearbyEntities = entity.level().getEntitiesOfClass(
                LivingEntity.class,
                boundingBox,
                target -> isValidTarget(entity, target) && filter.test(target));

        // 如果没有找到任何目标，返回null
        if (nearbyEntities.isEmpty()) {
            return null;
        }

        // 按照距离排序
        Optional<LivingEntity> target = nearbyEntities.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(entity)));

        return target.orElse(null);
    }
}