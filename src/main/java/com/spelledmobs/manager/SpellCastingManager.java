package com.spelledmobs.manager;

import com.spelledmobs.SpelledMobs;
import com.spelledmobs.compatibility.IronsSpellsCompat;
import com.spelledmobs.config.SpelledMobsConfig;
import com.spelledmobs.data.SpellCastingData;
import com.spelledmobs.data.SpellEntry;
import com.spelledmobs.util.TargetFinder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 管理实体的法术施放
 */
public class SpellCastingManager {
    private static final double DEFAULT_SEARCH_RADIUS = 16.0;
    private static final Random RANDOM = new Random();

    // 记录实体的冷却时间
    private final Map<LivingEntity, Map<String, Integer>> cooldowns = new HashMap<>();

    // 实体施法数据
    private final SpellCastingData spellCastingData;

    /**
     * 创建法术施放管理器
     * 
     * @param spellCastingData 实体施法数据
     */
    public SpellCastingManager(SpellCastingData spellCastingData) {
        this.spellCastingData = spellCastingData;
    }

    /**
     * 服务器tick处理，每tick调用一次
     * 
     * @param level 服务器世界
     */
    public void onServerTick(ServerLevel level) {
        // 如果Iron's Spells未加载，跳过处理
        if (!IronsSpellsCompat.isIronsSpellsLoaded() || !IronsSpellsCompat.isInitialized()) {
            return;
        }

        // 遍历所有实体
        int entityCount = 0;
        int entityWithSpellsCount = 0;

        for (var entity : level.getAllEntities()) {
            if (entity instanceof LivingEntity livingEntity) {
                entityCount++;
                if (spellCastingData.hasSpells(livingEntity)) {
                    entityWithSpellsCount++;
                    updateEntitySpellCasting(livingEntity);
                }
            }
        }

        // 每100刻（5秒）记录一次统计信息
        if (level.getGameTime() % 100 == 0 && entityWithSpellsCount > 0) {
            SpelledMobs.LOGGER.info(
                    "[SpelledMobs] [施法统计] 世界: {}, 检查生物: {}, 可施法生物: {}",
                    level.dimension().location(), entityCount, entityWithSpellsCount);
        }

        // 清理不再存在的实体的冷却数据
        cooldowns.entrySet().removeIf(entry -> !entry.getKey().isAlive());
    }

    /**
     * 更新实体的法术施放状态
     * 
     * @param entity 需要更新的实体
     */
    private void updateEntitySpellCasting(LivingEntity entity) {
        // 检查是否已加载Iron's Spells
        if (!IronsSpellsCompat.isIronsSpellsLoaded() || !IronsSpellsCompat.isInitialized()) {
            return;
        }

        // 获取实体名称，用于日志
        String entityName = entity.getName().getString();
        String entityType = entity.getType().toString();

        // 检查实体是否可以施法
        if (!spellCastingData.hasSpells(entity)) {
            return;
        }

        // 获取实体的冷却时间Map
        Map<String, Integer> entityCooldowns = cooldowns.computeIfAbsent(entity, k -> new HashMap<>());

        // 更新所有法术的冷却时间
        entityCooldowns.replaceAll((spell, cooldown) -> Math.max(0, cooldown - 1));

        // 检查是否有法术可以施放
        SpellEntry spellEntry = spellCastingData.getNextSpellToCast(entity);
        if (spellEntry == null) {
            if (SpelledMobsConfig.isDebugLoggingEnabled() && entity.level().getGameTime() % 100 == 0) {
                SpelledMobs.LOGGER.debug("[SpelledMobs] [{}:{}] 没有可施放的法术", entityType, entityName);
            }
            return;
        }

        // 检查法术冷却时间
        if (entityCooldowns.getOrDefault(spellEntry.getSpellId(), 0) > 0) {
            if (SpelledMobsConfig.isDebugLoggingEnabled() && entity.level().getGameTime() % 100 == 0) {
                SpelledMobs.LOGGER.debug("[SpelledMobs] [{}:{}] 法术 {} 正在冷却中: {} tick",
                        entityType, entityName,
                        spellEntry.getSpellId(),
                        entityCooldowns.get(spellEntry.getSpellId()));
            }
            return;
        }

        // 查找目标
        LivingEntity target = TargetFinder.findTarget(entity, DEFAULT_SEARCH_RADIUS);
        if (target == null) {
            if (SpelledMobsConfig.isDebugLoggingEnabled() && entity.level().getGameTime() % 100 == 0) {
                SpelledMobs.LOGGER.debug("[SpelledMobs] [{}:{}] 未找到目标，无法施法", entityType, entityName);
            }
            return;
        }

        String targetName = target.getName().getString();
        String targetType = target.getType().toString();

        SpelledMobs.LOGGER.info("[SpelledMobs] [{}:{}] 找到目标: [{}:{}]，准备施放法术: {}",
                entityType, entityName,
                targetType, targetName,
                spellEntry.getSpellId());

        // 确定法术等级
        int level = spellEntry.getMinLevel();
        if (spellEntry.getMaxLevel() > spellEntry.getMinLevel()) {
            level += RANDOM.nextInt(spellEntry.getMaxLevel() - spellEntry.getMinLevel() + 1);
        }

        // 施放法术
        boolean success = IronsSpellsCompat.castSpell(entity, target, entity.level(), spellEntry.getSpellId(), level);

        if (success) {
            // 设置冷却时间 - 减少测试时的冷却时间
            int originalCooldown = spellEntry.getMinCastTime();
            if (spellEntry.getMaxCastTime() > spellEntry.getMinCastTime()) {
                originalCooldown += RANDOM.nextInt(spellEntry.getMaxCastTime() - spellEntry.getMinCastTime() + 1);
            }

            // 测试模式下冷却时间减半
            int cooldown = originalCooldown / 2;
            entityCooldowns.put(spellEntry.getSpellId(), cooldown);

            SpelledMobs.LOGGER.info("[SpelledMobs] [{}:{}] 成功施放法术 {} (等级 {}) 目标: [{}:{}]，冷却时间: {} tick",
                    entityType, entityName,
                    spellEntry.getSpellId(),
                    level,
                    targetType, targetName,
                    cooldown);
        } else {
            SpelledMobs.LOGGER.warn("[SpelledMobs] [{}:{}] 施放法术 {} 失败，请查看详细日志以了解原因",
                    entityType, entityName,
                    spellEntry.getSpellId());
        }
    }

    /**
     * 强制实体施放指定法术
     * 
     * @param entity  施法实体
     * @param target  目标实体
     * @param spellId 法术ID
     * @param level   法术等级
     * @return 是否成功施放
     */
    public boolean forceCastSpell(LivingEntity entity, LivingEntity target, String spellId, int level) {
        // 检查是否已加载Iron's Spells
        if (!IronsSpellsCompat.isIronsSpellsLoaded() || !IronsSpellsCompat.isInitialized()) {
            SpelledMobs.LOGGER.warn("[SpelledMobs] 尝试施放法术，但Iron's Spells模组未加载或未初始化");
            return false;
        }

        if (entity == null || target == null || !entity.isAlive() || !target.isAlive()) {
            return false;
        }

        boolean success = IronsSpellsCompat.castSpell(entity, target, entity.level(), spellId, level);

        if (success && SpelledMobsConfig.isDebugLoggingEnabled()) {
            SpelledMobs.LOGGER.info("[SpelledMobs] {} 成功施放法术 {} (等级 {}) 目标: {}",
                    entity.getName().getString(),
                    spellId,
                    level,
                    target.getName().getString());
        }

        return success;
    }

    /**
     * 清除实体的所有冷却时间
     * 
     * @param entity 实体
     */
    public void clearCooldowns(LivingEntity entity) {
        cooldowns.remove(entity);
    }

    /**
     * 获取法术的当前冷却时间
     * 
     * @param entity  实体
     * @param spellId 法术ID
     * @return 冷却时间（刻）
     */
    public int getSpellCooldown(LivingEntity entity, String spellId) {
        return cooldowns.getOrDefault(entity, new HashMap<>()).getOrDefault(spellId, 0);
    }

    /**
     * 设置法术的冷却时间
     * 
     * @param entity   实体
     * @param spellId  法术ID
     * @param cooldown 冷却时间（刻）
     */
    public void setSpellCooldown(LivingEntity entity, String spellId, int cooldown) {
        Map<String, Integer> entityCooldowns = cooldowns.computeIfAbsent(entity, k -> new HashMap<>());
        entityCooldowns.put(spellId, Math.max(0, cooldown));
    }
}