package com.spelledmobs.data;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.spelledmobs.SpelledMobs;
import com.spelledmobs.config.EntitySpellConfig;

import net.minecraft.util.Identifier;

/**
 * 存储所有生物的施法配置数据
 */
public class SpellCastingData {
    private final Map<String, EntitySpellConfig> entityConfigs = new HashMap<>();
    
    /**
     * 从JSON加载生物施法配置
     * @param identifier 资源标识符
     * @param element JSON元素
     */
    public void loadFromJson(Identifier identifier, JsonElement element) {
        if (!element.isJsonObject()) {
            SpelledMobs.LOGGER.error("Invalid spelled mob configuration format in {}: not a JSON object", identifier);
            return;
        }
        
        JsonObject json = element.getAsJsonObject();
        String entityId = json.has("entity_id") ? json.get("entity_id").getAsString() : null;
        
        if (entityId == null || entityId.isEmpty()) {
            SpelledMobs.LOGGER.error("Invalid spelled mob configuration in {}: missing entity_id", identifier);
            return;
        }
        
        try {
            EntitySpellConfig config = EntitySpellConfig.fromJson(json);
            entityConfigs.put(entityId, config);
            SpelledMobs.LOGGER.debug("Loaded spell configuration for entity: {}", entityId);
        } catch (Exception e) {
            SpelledMobs.LOGGER.error("Error parsing spelled mob configuration in {}", identifier, e);
        }
    }
    
    /**
     * 获取指定实体ID的施法配置
     * @param entityId 实体ID
     * @return 施法配置，如果不存在则返回null
     */
    public EntitySpellConfig getConfigForEntity(String entityId) {
        return entityConfigs.get(entityId);
    }
    
    /**
     * 获取配置的实体数量
     * @return 实体数量
     */
    public int getEntityCount() {
        return entityConfigs.size();
    }
    
    /**
     * 获取所有实体的施法配置
     * @return 所有实体的施法配置
     */
    public Map<String, EntitySpellConfig> getAllEntityConfigs() {
        return entityConfigs;
    }
    
    /**
     * 清除所有配置数据
     */
    public void clear() {
        entityConfigs.clear();
    }
} 