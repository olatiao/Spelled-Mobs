package com.spelledmobs.config;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.spelledmobs.SpelledMobs;
import com.spelledmobs.util.SpellCastCondition;

/**
 * 实体施法配置类
 */
public class EntitySpellConfig {
    private final List<SpellEntry> spells = new ArrayList<>();
    private int checkInterval = 20; // 默认每秒检查一次（20ticks）
    
    /**
     * 从JSON对象创建实体施法配置
     * @param json JSON对象
     * @return 实体施法配置
     */
    public static EntitySpellConfig fromJson(JsonObject json) {
        EntitySpellConfig config = new EntitySpellConfig();
        
        // 解析检查间隔
        if (json.has("check_interval")) {
            config.checkInterval = json.get("check_interval").getAsInt();
        }
        
        // 解析法术列表
        if (json.has("spells") && json.get("spells").isJsonArray()) {
            JsonArray spellsArray = json.getAsJsonArray("spells");
            for (JsonElement spellElement : spellsArray) {
                if (spellElement.isJsonObject()) {
                    try {
                        SpellEntry spell = SpellEntry.fromJson(spellElement.getAsJsonObject());
                        config.spells.add(spell);
                    } catch (Exception e) {
                        SpelledMobs.LOGGER.error("Error parsing spell entry: {}", e.getMessage());
                    }
                }
            }
        }
        
        return config;
    }
    
    /**
     * 获取所有法术配置
     * @return 法术配置列表
     */
    public List<SpellEntry> getSpells() {
        return spells;
    }
    
    /**
     * 获取检查间隔（以Tick为单位）
     * @return 检查间隔
     */
    public int getCheckInterval() {
        return checkInterval;
    }
    
    /**
     * 法术配置项
     */
    public static class SpellEntry {
        private String spellId;
        private int minLevel = 1;
        private int maxLevel = 1;
        private int minCastTime = 60;  // 默认最小施法间隔3秒（60ticks）
        private int maxCastTime = 200; // 默认最大施法间隔10秒（200ticks）
        private final List<SpellCastCondition> conditions = new ArrayList<>();
        
        /**
         * 从JSON对象创建法术配置项
         * @param json JSON对象
         * @return 法术配置项
         */
        public static SpellEntry fromJson(JsonObject json) {
            SpellEntry entry = new SpellEntry();
            
            // 获取法术ID并确保格式正确
            if (json.has("spell_id")) {
                String rawSpellId = json.get("spell_id").getAsString();
                // 确保法术ID有正确的命名空间
                if (!rawSpellId.contains(":")) {
                    rawSpellId = "irons_spellbooks:" + rawSpellId;
                    SpelledMobs.LOGGER.info("自动添加法术ID命名空间: {}", rawSpellId);
                }
                entry.spellId = rawSpellId;
            }
            
            if (json.has("min_level")) {
                entry.minLevel = json.get("min_level").getAsInt();
            }
            
            if (json.has("max_level")) {
                entry.maxLevel = json.get("max_level").getAsInt();
            }
            
            if (json.has("min_cast_time")) {
                entry.minCastTime = json.get("min_cast_time").getAsInt();
            }
            
            if (json.has("max_cast_time")) {
                entry.maxCastTime = json.get("max_cast_time").getAsInt();
            }
            
            // 解析施法条件
            if (json.has("conditions") && json.get("conditions").isJsonArray()) {
                JsonArray conditionsArray = json.getAsJsonArray("conditions");
                for (JsonElement conditionElement : conditionsArray) {
                    if (conditionElement.isJsonObject()) {
                        try {
                            SpellCastCondition condition = SpellCastCondition.fromJson(conditionElement.getAsJsonObject());
                            entry.conditions.add(condition);
                        } catch (Exception e) {
                            SpelledMobs.LOGGER.error("Error parsing spell condition: {}", e.getMessage());
                        }
                    }
                }
            }
            
            return entry;
        }
        
        /**
         * 获取法术ID
         * @return 法术ID
         */
        public String getSpellId() {
            return spellId;
        }
        
        /**
         * 获取最小法术等级
         * @return 最小法术等级
         */
        public int getMinLevel() {
            return minLevel;
        }
        
        /**
         * 获取最大法术等级
         * @return 最大法术等级
         */
        public int getMaxLevel() {
            return maxLevel;
        }
        
        /**
         * 获取最小施法间隔（以Tick为单位）
         * @return 最小施法间隔
         */
        public int getMinCastTime() {
            return minCastTime;
        }
        
        /**
         * 获取最大施法间隔（以Tick为单位）
         * @return 最大施法间隔
         */
        public int getMaxCastTime() {
            return maxCastTime;
        }
        
        /**
         * 获取施法条件列表
         * @return 施法条件列表
         */
        public List<SpellCastCondition> getConditions() {
            return conditions;
        }
    }
} 