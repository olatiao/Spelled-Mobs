package com.spelledmobs.manager;

import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.Iterator;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.MinecraftServer;
import com.spelledmobs.SpelledMobs;
import com.spelledmobs.config.SpelledMobsConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

/**
 * 管理实体施法冷却时间的类
 */
public class CooldownManager {
    private final Map<UUID, Integer> entityCastCooldowns = new HashMap<>();
    private static final int CLEANUP_INTERVAL = 6000; // 每5分钟清理一次
    private int cleanupCounter = 0;
    private MinecraftServer server;

    public CooldownManager() {
        // 注册服务器启动事件监听器
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            this.server = server;
        });

        // 注册服务器停止事件监听器
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            this.server = null;
        });
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
     * 验证实体是否有效
     */
    private boolean isValidEntity(Entity entity) {
        return entity != null && !entity.isRemoved();
    }

    /**
     * 获取实体的UUID
     */
    private UUID getEntityId(Entity entity) {
        return entity != null ? entity.getUuid() : null;
    }

    /**
     * 检查实体是否存在于任何世界中
     */
    private boolean doesEntityExist(UUID entityId) {
        if (server == null || entityId == null) return false;

        for (ServerWorld world : server.getWorlds()) {
            Entity entity = world.getEntity(entityId);
            if (isValidEntity(entity)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 更新所有实体的冷却时间
     */
    public void updateCooldowns() {
        // 更新所有冷却时间
        entityCastCooldowns.entrySet().removeIf(entry -> {
            int newCooldown = entry.getValue() - 1;
            if (newCooldown <= 0) {
                logDebug("实体 {} 的冷却时间结束", entry.getKey());
                return true;
            }
            entry.setValue(newCooldown);
            return false;
        });

        // 定期清理无效的冷却时间
        if (++cleanupCounter >= CLEANUP_INTERVAL) {
            cleanupCounter = 0;
            cleanupInvalidCooldowns();
        }
    }
    
    /**
     * 清理无效的冷却时间
     */
    private void cleanupInvalidCooldowns() {
        if (server == null) return;

        Iterator<Map.Entry<UUID, Integer>> iterator = entityCastCooldowns.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            UUID entityId = entry.getKey();
            
            if (!doesEntityExist(entityId)) {
                logDebug("清理无效的冷却时间: {}", entityId);
                iterator.remove();
            }
        }
    }
    
    /**
     * 检查实体是否在冷却中
     */
    public boolean isOnCooldown(Entity entity) {
        UUID entityId = getEntityId(entity);
        return entityId != null && entityCastCooldowns.containsKey(entityId);
    }
    
    /**
     * 设置实体的冷却时间
     */
    public void setCooldown(Entity entity, int cooldown) {
        UUID entityId = getEntityId(entity);
        if (entityId == null || cooldown <= 0) return;
        
        entityCastCooldowns.put(entityId, cooldown);
        logDebug("设置实体 {} 的冷却时间为 {} ticks", entityId, cooldown);
    }
    
    /**
     * 清除实体的冷却时间
     */
    public void clearCooldown(Entity entity) {
        UUID entityId = getEntityId(entity);
        if (entityId != null && entityCastCooldowns.remove(entityId) != null) {
            logDebug("清除实体 {} 的冷却时间", entityId);
        }
    }
    
    /**
     * 清除所有冷却时间
     */
    public void clearAllCooldowns() {
        int count = entityCastCooldowns.size();
        entityCastCooldowns.clear();
        logDebug("清除所有冷却时间，共 {} 个", count);
    }
    
    /**
     * 获取实体的剩余冷却时间
     */
    public int getRemainingCooldown(Entity entity) {
        UUID entityId = getEntityId(entity);
        return entityId != null ? entityCastCooldowns.getOrDefault(entityId, 0) : 0;
    }

    /**
     * 获取当前正在冷却的实体数量
     */
    public int getActiveCooldownCount() {
        return entityCastCooldowns.size();
    }

    /**
     * 获取实体的冷却时间百分比
     */
    public float getCooldownPercentage(Entity entity, int totalCooldown) {
        if (totalCooldown <= 0) return 0.0f;
        int remaining = getRemainingCooldown(entity);
        return remaining > 0 ? (float)remaining / totalCooldown : 0.0f;
    }
} 