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
        if (!SpelledMobs.isIronsSpellsLoaded()) {
            return;
        }

        // 遍历所有实体
        level.getAllEntities().forEach(entity -> {
            if (entity instanceof LivingEntity livingEntity) {
                updateEntitySpellCasting(livingEntity);
            }
        });

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
        if (!SpelledMobs.isIronsSpellsLoaded()) {
            return;
        }

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
            return;
        }

        // 检查法术冷却时间
        if (entityCooldowns.getOrDefault(spellEntry.getSpellId(), 0) > 0) {
            return;
        }

        // 查找目标
        LivingEntity target = TargetFinder.findTarget(entity, DEFAULT_SEARCH_RADIUS);
        if (target == null) {
            return;
        }

        // 确定法术等级
        int level = spellEntry.getMinLevel();
        if (spellEntry.getMaxLevel() > spellEntry.getMinLevel()) {
            level += RANDOM.nextInt(spellEntry.getMaxLevel() - spellEntry.getMinLevel() + 1);
        }

        // 施放法术
        boolean success = IronsSpellsCompat.castSpell(entity, target, entity.level(), spellEntry.getSpellId(), level);

        if (success) {
            // 设置冷却时间
            int cooldown = spellEntry.getMinCastTime();
            if (spellEntry.getMaxCastTime() > spellEntry.getMinCastTime()) {
                cooldown += RANDOM.nextInt(spellEntry.getMaxCastTime() - spellEntry.getMinCastTime() + 1);
            }
            entityCooldowns.put(spellEntry.getSpellId(), cooldown);

            if (SpelledMobsConfig.isDebugLoggingEnabled()) {
                SpelledMobs.LOGGER.info("{} 成功施放法术 {} (等级 {}) 目标: {}",
                        entity.getName().getString(),
                        spellEntry.getSpellId(),
                        level,
                        target.getName().getString());
            }
        } else if (SpelledMobsConfig.isDebugLoggingEnabled()) {
            SpelledMobs.LOGGER.warn("{} 施放法术 {} 失败",
                    entity.getName().getString(),
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
        if (!SpelledMobs.isIronsSpellsLoaded()) {
            SpelledMobs.LOGGER.warn("尝试施放法术，但Iron's Spells模组未加载");
            return false;
        }

        if (entity == null || target == null || !entity.isAlive() || !target.isAlive()) {
            return false;
        }

        boolean success = IronsSpellsCompat.castSpell(entity, target, entity.level(), spellId, level);

        if (success && SpelledMobsConfig.isDebugLoggingEnabled()) {
            SpelledMobs.LOGGER.info("{} 成功施放法术 {} (等级 {}) 目标: {}",
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