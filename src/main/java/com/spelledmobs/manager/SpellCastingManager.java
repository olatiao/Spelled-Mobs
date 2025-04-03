package com.spelledmobs.manager;

import com.spelledmobs.SpelledMobs;
import com.spelledmobs.compatibility.IronsSpellsCompat;
import com.spelledmobs.config.SpelledMobsConfig;
import com.spelledmobs.data.SpellCastingData;
import com.spelledmobs.data.SpellEntry;
import com.spelledmobs.util.TargetFinder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理实体的法术施放
 */
public class SpellCastingManager {
    private static final double DEFAULT_SEARCH_RADIUS = 16.0;
    private static final Random RANDOM = new Random();
    
    // 上次状态检查时间
    private long lastStatusCheckTime = 0;
    // 状态检查间隔（毫秒）
    private static final long STATUS_CHECK_INTERVAL = 5000; // 5秒检查一次
    
    // 持续性法术的默认持续时间（刻）
    private static final Map<String, Integer> SPELL_DURATIONS = new HashMap<>();
    
    static {
        // 初始化已知持续性法术的持续时间 - 显著增加持续时间避免过快结束
        // 持续时间调整为更长，不少于5秒
        SPELL_DURATIONS.put("irons_spellbooks:fire_breath", 120);      // 6秒 (增加)
        SPELL_DURATIONS.put("irons_spellbooks:frost_breath", 120);     // 6秒 (增加)
        SPELL_DURATIONS.put("irons_spellbooks:electrocute", 100);      // 5秒 (增加)
        SPELL_DURATIONS.put("irons_spellbooks:gust", 100);             // 5秒 (增加)
        SPELL_DURATIONS.put("irons_spellbooks:tornado", 160);          // 8秒 (增加)
        SPELL_DURATIONS.put("irons_spellbooks:ascension", 160);        // 8秒 (增加)
        SPELL_DURATIONS.put("irons_spellbooks:black_hole", 200);       // 10秒 (增加)
        SPELL_DURATIONS.put("irons_spellbooks:holy_ray", 120);         // 6秒 (增加)
    }

    // 记录实体的冷却时间
    private final Map<LivingEntity, Map<String, Integer>> cooldowns = new ConcurrentHashMap<>();

    // 实体施法数据
    private final SpellCastingData spellCastingData;

    /**
     * 创建法术施放管理器
     * 
     * @param spellCastingData 实体施法数据
     */
    public SpellCastingManager(SpellCastingData spellCastingData) {
        this.spellCastingData = spellCastingData;
        // 注册事件监听器
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    /**
     * 监听实体死亡事件，清理施法状态
     */
    @SubscribeEvent
    public void onEntityDeath(LivingDeathEvent event) {
        try {
            LivingEntity entity = event.getEntity();
            if (entity != null) {
                // 清理冷却时间
                cooldowns.remove(entity);
                // 清理施法状态
                spellCastingData.cleanupEntityState(entity);
                
                if (SpelledMobsConfig.isDebugLoggingEnabled()) {
                    SpelledMobs.LOGGER.debug("[SpelledMobs] 实体 {} 死亡，清理施法状态", 
                        entity.getName().getString());
                }
            }
        } catch (Exception e) {
            SpelledMobs.LOGGER.error("[SpelledMobs] 处理实体死亡事件时出错", e);
        }
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

        // 定期检查施法状态
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastStatusCheckTime > STATUS_CHECK_INTERVAL) {
            lastStatusCheckTime = currentTime;
            checkAllEntityCastingStatus(level);
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
     * 检查所有实体的施法状态，用于调试
     */
    private void checkAllEntityCastingStatus(ServerLevel level) {
        if (!SpelledMobsConfig.isDebugLoggingEnabled()) {
            return;
        }
        
        SpelledMobs.LOGGER.debug("[SpelledMobs] ==== 开始检查所有实体施法状态 ====");
        int castingCount = 0;
        
        for (var entity : level.getAllEntities()) {
            if (entity instanceof LivingEntity livingEntity) {
                if (spellCastingData.isCasting(livingEntity)) {
                    castingCount++;
                    String spellId = spellCastingData.getCurrentSpellId(livingEntity);
                    if (spellId != null) {
                        SpelledMobs.LOGGER.debug("[SpelledMobs] 实体 {} 正在施放法术: {}", 
                            livingEntity.getName().getString(), spellId);
                    }
                }
            }
        }
        
        SpelledMobs.LOGGER.debug("[SpelledMobs] ==== 施法状态检查完成，当前正在施法实体: {} ====", castingCount);
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

        // 检查实体是否正在施放法术，正在施放时不允许施放新法术
        boolean casting = spellCastingData.isCasting(entity);
        
        // 添加调试日志确认施法状态
        if (casting && SpelledMobsConfig.isDebugLoggingEnabled()) {
            String currentSpell = spellCastingData.getCurrentSpellId(entity);
            SpelledMobs.LOGGER.debug("[SpelledMobs] [{}:{}] 正在施放法术: {}，不能施放新法术",
                    entityType, entityName, currentSpell != null ? currentSpell : "未知");
        }
        
        if (casting) {
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

        // 检查是否是持续性法术
        String spellId = spellEntry.getSpellId();
        boolean isContinuousSpell = SPELL_DURATIONS.containsKey(spellId);
        int duration = isContinuousSpell ? SPELL_DURATIONS.get(spellId) : 0;
        
        // 打印调试信息
        if (SpelledMobsConfig.isDebugLoggingEnabled()) {
            SpelledMobs.LOGGER.debug("[SpelledMobs] 准备施放法术: {}, 是否持续性: {}, 持续时间: {}",
                spellId, isContinuousSpell, duration);
        }

        // 施放法术
        boolean success = IronsSpellsCompat.castSpell(entity, target, entity.level(), spellEntry.getSpellId(), level);

        if (success) {
            // 设置冷却时间
            int originalCooldown = spellEntry.getMinCastTime();
            if (spellEntry.getMaxCastTime() > spellEntry.getMinCastTime()) {
                originalCooldown += RANDOM.nextInt(spellEntry.getMaxCastTime() - spellEntry.getMinCastTime() + 1);
            }

            int cooldown = originalCooldown / 2;
            entityCooldowns.put(spellEntry.getSpellId(), cooldown);
            
            // 如果是持续性法术，记录施法状态
            if (isContinuousSpell) {
                // 记录施法状态
                spellCastingData.startCasting(entity, spellId, level, duration);
                
                // 打印确认开始施法的日志
                SpelledMobs.LOGGER.info("[SpelledMobs] [{}:{}] 成功施放持续性法术 {} (等级 {}) 目标: [{}:{}]，持续时间: {} tick，冷却时间: {} tick",
                        entityType, entityName,
                        spellEntry.getSpellId(),
                        level,
                        targetType, targetName,
                        duration,
                        cooldown);
                
                // 立即检查施法状态是否设置成功
                if (SpelledMobsConfig.isDebugLoggingEnabled()) {
                    boolean castingState = spellCastingData.isCasting(entity);
                    SpelledMobs.LOGGER.debug("[SpelledMobs] 施法状态检查 - 实体: {}, 是否正在施法: {}",
                            entityName, castingState);
                }
            } else {
                SpelledMobs.LOGGER.info("[SpelledMobs] [{}:{}] 成功施放法术 {} (等级 {}) 目标: [{}:{}]，冷却时间: {} tick",
                        entityType, entityName,
                        spellEntry.getSpellId(),
                        level,
                        targetType, targetName,
                        cooldown);
            }
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
        
        // 检查实体是否正在施放法术
        if (spellCastingData.isCasting(entity)) {
            // 先停止当前法术再施放新法术
            spellCastingData.stopCasting(entity);
            SpelledMobs.LOGGER.debug("[SpelledMobs] 实体 {} 正在施放法术，强制停止当前法术，施放新法术", 
                entity.getName().getString());
        }

        boolean success = IronsSpellsCompat.castSpell(entity, target, entity.level(), spellId, level);

        if (success) {
            // 检查是否是持续性法术
            if (SPELL_DURATIONS.containsKey(spellId)) {
                // 获取法术持续时间
                int duration = SPELL_DURATIONS.get(spellId);
                // 记录施法状态
                spellCastingData.startCasting(entity, spellId, level, duration);
                
                if (SpelledMobsConfig.isDebugLoggingEnabled()) {
                    SpelledMobs.LOGGER.info("[SpelledMobs] {} 成功施放持续性法术 {} (等级 {}) 目标: {}，持续时间: {} tick",
                            entity.getName().getString(),
                            spellId,
                            level,
                            target.getName().getString(),
                            duration);
                }
            } else if (SpelledMobsConfig.isDebugLoggingEnabled()) {
                SpelledMobs.LOGGER.info("[SpelledMobs] {} 成功施放法术 {} (等级 {}) 目标: {}",
                        entity.getName().getString(),
                        spellId,
                        level,
                        target.getName().getString());
            }
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
        // 同时清理施法状态
        spellCastingData.cleanupEntityState(entity);
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

    /**
     * 检查指定法术是否是持续性法术
     *
     * @param spellId 法术ID
     * @return 是否是持续性法术
     */
    public static boolean isContinuousSpell(String spellId) {
        return SPELL_DURATIONS.containsKey(spellId);
    }

    /**
     * 获取指定法术的持续时间
     *
     * @param spellId 法术ID
     * @return 持续时间（刻），如果不是持续性法术则返回0
     */
    public static int getSpellDuration(String spellId) {
        return SPELL_DURATIONS.getOrDefault(spellId, 0);
    }
}