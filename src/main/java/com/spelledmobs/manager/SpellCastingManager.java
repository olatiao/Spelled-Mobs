package com.spelledmobs.manager;

import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.Random;
import java.util.HashMap;
import com.spelledmobs.SpelledMobs;
import com.spelledmobs.config.EntitySpellConfig;
import com.spelledmobs.config.EntitySpellConfig.SpellEntry;
import com.spelledmobs.config.SpelledMobsConfig;
import com.spelledmobs.data.SpellCastingData;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;

/**
 * 管理实体施法的主要类
 */
public class SpellCastingManager {
    public static final SpellCastingManager INSTANCE = new SpellCastingManager();

    private static final int MAX_CASTING_ATTEMPTS = 3;
    private static final double DEFAULT_SEARCH_RADIUS = 16.0;
    private static final double MAX_HEIGHT_DIFF = 8.0;

    private final Map<UUID, Integer> castingAttempts = new HashMap<>();
    private final CooldownManager cooldownManager = new CooldownManager();
    private SpellCastingData data = new SpellCastingData();

    private SpellCastingManager() {
    }

    /**
     * 记录调试日志
     */
    private void logDebug(String message, Object... args) {
        if (SpelledMobsConfig.isDebugLoggingEnabled()) {
            SpelledMobs.LOGGER.debug(message, args);
        }
    }

    /**
     * 记录警告日志
     */
    private void logWarn(String message, Object... args) {
        SpelledMobs.LOGGER.warn(message, args);
    }

    /**
     * 记录错误日志
     */
    private void logError(String message, Object... args) {
        SpelledMobs.LOGGER.error(message, args);
    }

    /**
     * 获取实体ID
     */
    private String getEntityId(LivingEntity entity) {
        return entity.getType().toString();
    }

    /**
     * 获取实体的施法配置
     */
    private EntitySpellConfig getEntityConfig(LivingEntity entity) {
        return data.getConfigForEntity(getEntityId(entity));
    }

    /**
     * 检查实体是否有有效的施法配置
     */
    private boolean hasValidSpellConfig(LivingEntity entity) {
        EntitySpellConfig config = getEntityConfig(entity);
        return config != null && !config.getSpells().isEmpty();
    }

    /**
     * 检查实体是否可以施法
     */
    private boolean canCastSpell(LivingEntity entity) {
        return !cooldownManager.isOnCooldown(entity) &&
                getCastingAttempts(entity) < MAX_CASTING_ATTEMPTS;
    }

    /**
     * 检查实体是否有效
     */
    private boolean isValidEntity(LivingEntity entity) {
        return entity != null && !entity.isRemoved() && !entity.isDead();
    }

    /**
     * 创建搜索框
     */
    private Box createSearchBox() {
        return new Box(
                -DEFAULT_SEARCH_RADIUS, -MAX_HEIGHT_DIFF, -DEFAULT_SEARCH_RADIUS,
                DEFAULT_SEARCH_RADIUS, MAX_HEIGHT_DIFF, DEFAULT_SEARCH_RADIUS);
    }

    /**
     * 初始化管理器
     */
    public void init() {
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
    }

    /**
     * 重新加载配置
     */
    public void reload() {
        data.clear();
        cooldownManager.clearAllCooldowns();
        castingAttempts.clear();
        logDebug("重新加载Spelled Mobs配置...");
    }

    /**
     * 设置施法数据
     */
    public void setData(SpellCastingData data) {
        this.data = data;
        cooldownManager.clearAllCooldowns();
        castingAttempts.clear();
    }

    /**
     * 服务器tick事件处理
     */
    private void onServerTick(MinecraftServer server) {
        cooldownManager.updateCooldowns();

        for (ServerWorld world : server.getWorlds()) {
            updateSpellCasting(world);
        }
    }

    /**
     * 更新所有实体的施法状态
     */
    public void updateSpellCasting(ServerWorld world) {
        try {
            List<LivingEntity> entities = getSpellCastingEntities(world);
            for (LivingEntity entity : entities) {
                updateEntitySpellCasting(entity);
            }
            cleanupCastingAttempts();
        } catch (Exception e) {
            logError("更新施法状态时出错: {}", e.getMessage());
            if (SpelledMobsConfig.isDebugLoggingEnabled()) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取需要施法的实体列表
     */
    private List<LivingEntity> getSpellCastingEntities(ServerWorld world) {
        return world.getEntitiesByClass(
                LivingEntity.class,
                createSearchBox(),
                this::isValidSpellCastingEntity);
    }

    /**
     * 检查实体是否可以进行施法
     */
    private boolean isValidSpellCastingEntity(LivingEntity entity) {
        return isValidEntity(entity) &&
                hasValidSpellConfig(entity) &&
                canCastSpell(entity);
    }

    /**
     * 更新单个实体的施法状态
     */
    private void updateEntitySpellCasting(LivingEntity entity) {
        if (!isValidEntity(entity)) {
            logWarn("尝试更新无效实体的施法状态: {}", entity.getUuid());
            return;
        }

        try {
            if (!hasValidSpellConfig(entity)) {
                logDebug("实体 {} 没有有效的施法配置", entity.getUuid());
                return;
            }

            if (!canCastSpell(entity)) {
                logDebug("实体 {} 当前无法施法", entity.getUuid());
                return;
            }

            LivingEntity target = TargetFinder.findTarget(entity, DEFAULT_SEARCH_RADIUS);
            if (target == null) {
                logDebug("实体 {} 未找到合适的目标", entity.getUuid());
                incrementCastingAttempts(entity);
                return;
            }

            if (!isValidEntity(target)) {
                logWarn("实体 {} 找到的目标无效: {}", entity.getUuid(), target.getUuid());
                incrementCastingAttempts(entity);
                return;
            }

            EntitySpellConfig config = getEntityConfig(entity);
            SpellEntry spellEntry = getRandomSpell(config);
            if (spellEntry == null) {
                logWarn("实体 {} 无法获取随机法术", entity.getUuid());
                return;
            }

            int level = SpellExecutor.determineSpellLevel(spellEntry);
            int cooldown = SpellExecutor.determineSpellCooldown(spellEntry);

            if (level <= 0 || cooldown <= 0) {
                logWarn("实体 {} 的法术等级或冷却时间无效: level={}, cooldown={}", 
                    entity.getUuid(), level, cooldown);
                return;
            }

            boolean success = SpellExecutor.castSpell(entity, target, spellEntry.getSpellId(), level);
            
            if (success) {
                cooldownManager.setCooldown(entity, cooldown);
                resetCastingAttempts(entity);
                logDebug("实体 {} 成功施放法术 {} 于目标 {}", 
                    entity.getUuid(), spellEntry.getSpellId(), target.getUuid());
            } else {
                incrementCastingAttempts(entity);
                logDebug("实体 {} 施放法术 {} 失败", 
                    entity.getUuid(), spellEntry.getSpellId());
            }
        } catch (Exception e) {
            logError("更新实体 {} 施法状态时出错: {}", entity.getUuid(), e.getMessage());
            if (SpelledMobsConfig.isDebugLoggingEnabled()) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取随机法术
     */
    private SpellEntry getRandomSpell(EntitySpellConfig config) {
        List<SpellEntry> spells = config.getSpells();
        if (spells.isEmpty()) {
            return null;
        }
        return spells.get(new Random().nextInt(spells.size()));
    }

    /**
     * 获取实体的施法尝试次数
     */
    private int getCastingAttempts(LivingEntity entity) {
        return castingAttempts.getOrDefault(entity.getUuid(), 0);
    }

    /**
     * 增加实体的施法尝试次数
     */
    private void incrementCastingAttempts(LivingEntity entity) {
        int attempts = getCastingAttempts(entity);
        castingAttempts.put(entity.getUuid(), attempts + 1);
        logDebug("实体 {} 施法尝试次数: {}", entity.getUuid(), attempts + 1);
    }

    /**
     * 重置实体的施法尝试次数
     */
    private void resetCastingAttempts(LivingEntity entity) {
        castingAttempts.remove(entity.getUuid());
        logDebug("实体 {} 施法尝试次数已重置", entity.getUuid());
    }

    /**
     * 清理过期的施法尝试记录
     */
    private void cleanupCastingAttempts() {
        castingAttempts.entrySet().removeIf(entry -> {
            boolean shouldRemove = entry.getValue() >= MAX_CASTING_ATTEMPTS;
            if (shouldRemove) {
                logDebug("清理实体 {} 的施法尝试记录", entry.getKey());
            }
            return shouldRemove;
        });
    }

    /**
     * 强制施放法术
     * @param entity 施法者
     * @param spellId 法术ID
     * @param level 法术等级
     * @param target 目标实体
     * @return 施法是否成功
     */
    public boolean forceCastSpell(LivingEntity entity, String spellId, int level, LivingEntity target) {
        if (!isValidEntity(entity)) {
            logWarn("尝试使用无效的施法者强制施法: {}", entity != null ? entity.getUuid() : "null");
            return false;
        }

        if (spellId == null || spellId.trim().isEmpty()) {
            logWarn("尝试使用无效的法术ID强制施法: {}", spellId);
            return false;
        }

        if (level <= 0) {
            logWarn("尝试使用无效的法术等级强制施法: {}", level);
            return false;
        }

        if (!isValidEntity(target)) {
            logWarn("尝试对无效的目标强制施法: {}", target != null ? target.getUuid() : "null");
            return false;
        }

        try {
            boolean success = SpellExecutor.castSpell(entity, target, spellId, level);
            if (success) {
                logDebug("实体 {} 成功强制施放法术 {} 于目标 {}", 
                    entity.getUuid(), spellId, target.getUuid());
            } else {
                logDebug("实体 {} 强制施放法术 {} 失败", 
                    entity.getUuid(), spellId);
            }
            return success;
        } catch (Exception e) {
            logError("强制施法时出错: 施法者={}, 法术={}, 目标={}, 错误={}", 
                entity.getUuid(), spellId, target.getUuid(), e.getMessage());
            if (SpelledMobsConfig.isDebugLoggingEnabled()) {
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * 获取实体的可用法术列表
     */
    public List<String> getEntitySpells(LivingEntity entity) {
        EntitySpellConfig config = getEntityConfig(entity);
        return config != null ? config.getSpells().stream().map(SpellEntry::getSpellId).toList() : List.of();
    }
}