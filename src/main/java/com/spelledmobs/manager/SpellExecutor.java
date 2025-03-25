package com.spelledmobs.manager;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import com.spelledmobs.SpelledMobs;
import com.spelledmobs.config.EntitySpellConfig.SpellEntry;
import com.spelledmobs.config.SpelledMobsConfig;
import com.spelledmobs.util.SpellcastingHelper;

/**
 * 负责执行法术施放的类
 */
public class SpellExecutor {
    private static final int MAX_SPELL_LEVEL = 10;
    private static final int MIN_SPELL_LEVEL = 1;
    private static final double MIN_CAST_DISTANCE = 1.0;
    private static final double MAX_CAST_DISTANCE = 64.0;
    private static final double MIN_CAST_ANGLE = -0.5;

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
     * 检查实体是否处于无法施法状态
     */
    private static boolean isEntityInInvalidState(LivingEntity entity, boolean isCaster) {
        if (entity == null) return false;

        String entityType = isCaster ? "施法者" : "目标";
        String entityId = entity.getUuid().toString();

        if (entity.isFrozen()) {
            logDebug("{}处于无法施法状态: {}", entityType, entityId);
            return true;
        }

        // if (entity.isSubmergedInWater()) {
        //     logDebug("{}处于水中，无法施法: {}", entityType, entityId);
        //     return true;
        // }

        // if (entity.isInLava()) {
        //     logDebug("{}处于岩浆中，无法施法: {}", entityType, entityId);
        //     return true;
        // }

        // if (entity.isFallFlying()) {
        //     logDebug("{}正在飞行，无法施法: {}", entityType, entityId);
        //     return true;
        // }

        // if (entity.isCrawling()) {
        //     logDebug("{}正在爬行，无法施法: {}", entityType, entityId);
        //     return true;
        // }

        // if (entity.isSneaking()) {
        //     logDebug("{}正在潜行，无法施法: {}", entityType, entityId);
        //     return true;
        // }

        // if (entity.isSprinting()) {
        //     logDebug("{}正在疾跑，无法施法: {}", entityType, entityId);
        //     return true;
        // }

        return false;
    }

    /**
     * 尝试施放法术
     * 
     * @param caster 施法者
     * @param target 目标
     * @param spellId 法术ID
     * @param level 法术等级
     * @return 是否成功施放
     */
    public static boolean castSpell(LivingEntity caster, LivingEntity target, String spellId, int level) {
        try {
            // 基本参数验证
            if (!validateSpellParameters(caster, target, spellId, level)) {
                return false;
            }

            // 检查施法距离
            if (!isValidCastDistance(caster, target)) {
                return false;
            }

            // 检查施法角度
            if (!isValidCastAngle(caster, target)) {
                return false;
            }

            // 记录详细的调试信息
            logSpellCastAttempt(caster, target, spellId, level);

            // 检查法术ID是否有效
            if (!SpellcastingHelper.isValidSpell(spellId)) {
                SpelledMobs.LOGGER.warn("无效的法术ID: {}", spellId);
                return false;
            }

            // 检查目标是否有效
            if (!isValidTarget(target)) {
                return false;
            }

            // 执行法术施放
            boolean success = SpellcastingHelper.castSpell(caster, target, spellId, level);
            
            if (success) {
                logSpellCastSuccess(caster, target, spellId, level);
            } else {
                logDebug("法术施放失败 - 施法者: {}, 目标: {}, 法术: {}, 等级: {}", 
                    caster.getUuid(), target.getUuid(), spellId, level);
            }
            
            return success;
        } catch (Exception e) {
            logSpellCastError(e, caster, target, spellId, level);
            return false;
        }
    }

    /**
     * 验证法术参数
     */
    private static boolean validateSpellParameters(LivingEntity caster, LivingEntity target, String spellId, int level) {
        if (!isValidEntity(caster)) {
            SpelledMobs.LOGGER.warn("无效的施法者");
            return false;
        }

        if (spellId == null || spellId.isEmpty()) {
            SpelledMobs.LOGGER.warn("无效的法术ID");
            return false;
        }

        if (level < MIN_SPELL_LEVEL || level > MAX_SPELL_LEVEL) {
            SpelledMobs.LOGGER.warn("法术等级 {} 超出范围 [{}, {}]", level, MIN_SPELL_LEVEL, MAX_SPELL_LEVEL);
            return false;
        }

        if (isEntityInInvalidState(caster, true)) {
            return false;
        }

        if (target != null && isEntityInInvalidState(target, false)) {
            return false;
        }

        return true;
    }

    /**
     * 检查施法距离是否有效
     */
    private static boolean isValidCastDistance(LivingEntity caster, LivingEntity target) {
        if (target == null) return false;
        
        double distance = caster.distanceTo(target);
        boolean isValid = distance >= MIN_CAST_DISTANCE && distance <= MAX_CAST_DISTANCE;
        
        if (!isValid) {
            logDebug("施法距离无效 - 施法者: {}, 目标: {}, 距离: {}, 有效范围: [{}, {}]", 
                caster.getUuid(), target.getUuid(), distance, MIN_CAST_DISTANCE, MAX_CAST_DISTANCE);
        }
        
        return isValid;
    }

    /**
     * 检查施法角度是否有效
     */
    private static boolean isValidCastAngle(LivingEntity caster, LivingEntity target) {
        if (target == null) return false;
        
        Vec3d casterPos = caster.getEyePos();
        Vec3d targetPos = target.getEyePos();
        Vec3d direction = targetPos.subtract(casterPos);
        Vec3d lookVec = caster.getRotationVec(1.0f);
        double dotProduct = lookVec.dotProduct(direction.normalize());
        
        boolean isValid = dotProduct >= MIN_CAST_ANGLE;
        
        if (!isValid) {
            logDebug("施法角度无效 - 施法者: {}, 目标: {}, 点积: {}", 
                caster.getUuid(), target.getUuid(), dotProduct);
        }
        
        return isValid;
    }

    /**
     * 检查目标是否有效
     */
    private static boolean isValidTarget(LivingEntity target) {
        return isValidEntity(target);
    }

    /**
     * 记录法术施放尝试
     */
    private static void logSpellCastAttempt(LivingEntity caster, LivingEntity target, String spellId, int level) {
        logDebug("实体 {} 尝试施放法术 {} (等级 {}) 对 {}", 
            caster.getUuid(), spellId, level, target != null ? target.getUuid() : "无目标");
        
        logEntityInfo(caster, "施法者");
        
        if (target != null) {
            logEntityInfo(target, "目标");
            logDebug("施法者到目标的距离: {}", caster.distanceTo(target));
        }
    }

    /**
     * 记录实体信息
     */
    private static void logEntityInfo(LivingEntity entity, String prefix) {
        if (entity.hasCustomName()) {
            logDebug("{}: {} ({})", prefix, entity.getCustomName().getString(), entity.getType().toString());
        } else {
            logDebug("{}: {}", prefix, entity.getType().toString());
        }
    }

    /**
     * 记录法术施放成功
     */
    private static void logSpellCastSuccess(LivingEntity caster, LivingEntity target, String spellId, int level) {
        logDebug("实体 {} 成功施放法术 {} (等级 {}) 对 {}", 
            caster.getUuid(), spellId, level, target != null ? target.getUuid() : "无目标");
    }

    /**
     * 记录法术施放错误
     */
    private static void logSpellCastError(Exception e, LivingEntity caster, LivingEntity target, String spellId, int level) {
        SpelledMobs.LOGGER.error("施放法术时出错: {}", e.getMessage());
        if (SpelledMobsConfig.isDebugLoggingEnabled()) {
            SpelledMobs.LOGGER.error("施法者: {}, 目标: {}, 法术: {}, 等级: {}", 
                caster != null ? caster.getUuid() : "null",
                target != null ? target.getUuid() : "null",
                spellId,
                level);
            e.printStackTrace();
        }
    }

    /**
     * 确定法术等级
     * 
     * @param spellEntry 法术配置
     * @return 法术等级
     */
    public static int determineSpellLevel(SpellEntry spellEntry) {
        int minLevel = Math.max(MIN_SPELL_LEVEL, spellEntry.getMinLevel());
        int maxLevel = Math.min(MAX_SPELL_LEVEL, spellEntry.getMaxLevel());

        if (minLevel == maxLevel) {
            return minLevel;
        }

        return minLevel + (int)(Math.random() * (maxLevel - minLevel + 1));
    }

    /**
     * 确定法术冷却时间
     * 
     * @param spellEntry 法术配置
     * @return 冷却时间（以tick为单位）
     */
    public static int determineSpellCooldown(SpellEntry spellEntry) {
        int minCastTime = spellEntry.getMinCastTime();
        int maxCastTime = spellEntry.getMaxCastTime();

        if (minCastTime == maxCastTime) {
            return minCastTime;
        }

        return minCastTime + (int)(Math.random() * (maxCastTime - minCastTime + 1));
    }
} 