package com.spelledmobs.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.spelledmobs.SpelledMobs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 管理实体的法术施放数据
 */
public class SpellCastingData {
    private static final Random RANDOM = new Random();

    // 存储每种实体的法术配置
    private final Map<ResourceLocation, List<SpellEntry>> entitySpells = new HashMap<>();
    // 存储每种实体的检查间隔
    private final Map<ResourceLocation, Integer> entityCheckIntervals = new HashMap<>();

    /**
     * 加载所有实体法术配置
     * 
     * @param resourceRoot 资源根路径
     */
    public void loadEntitySpells(String resourceRoot) {
        // 清空现有配置
        entitySpells.clear();
        entityCheckIntervals.clear();

        SpelledMobs.LOGGER.info("开始加载实体法术配置...");

        try {
            // 加载数据包中的所有实体法术配置文件
            // 在实际实现中，这里需要使用Minecraft的资源系统来加载文件
            // 以下代码仅作示例
            List<String> configFiles = findEntitySpellConfigs(resourceRoot);

            for (String configFile : configFiles) {
                try {
                    loadEntitySpellConfig(configFile);
                } catch (Exception e) {
                    SpelledMobs.LOGGER.error("加载实体法术配置文件失败: {}", configFile, e);
                }
            }

            SpelledMobs.LOGGER.info("实体法术配置加载完成，共加载 {} 个实体的配置", entitySpells.size());
        } catch (Exception e) {
            SpelledMobs.LOGGER.error("加载实体法术配置时发生错误", e);
        }
    }

    /**
     * 查找所有实体法术配置文件
     * 
     * @param resourceRoot 资源根路径
     * @return 配置文件路径列表
     */
    private List<String> findEntitySpellConfigs(String resourceRoot) {
        // 在实际实现中，需要使用Minecraft的资源系统来查找文件
        // 以下代码仅作示例
        List<String> configFiles = new ArrayList<>();
        // TODO: 实现实际资源查找
        return configFiles;
    }

    /**
     * 加载单个实体法术配置文件
     * 
     * @param configFile 配置文件路径
     * @throws IOException 如果加载失败
     */
    private void loadEntitySpellConfig(String configFile) throws IOException {
        // 在实际实现中，需要使用Minecraft的资源系统来加载文件
        // 以下代码仅作示例
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(configFile)) {
            if (inputStream == null) {
                throw new IOException("无法找到配置文件: " + configFile);
            }

            JsonObject jsonObject = JsonParser.parseReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            parseEntitySpellConfig(jsonObject);
        }
    }

    /**
     * 解析实体法术配置
     * 
     * @param jsonObject 配置JSON对象
     */
    private void parseEntitySpellConfig(JsonObject jsonObject) {
        // 解析实体ID
        String entityId = jsonObject.get("entityId").getAsString();
        ResourceLocation entityKey = new ResourceLocation(entityId);

        // 解析检查间隔（默认为20刻）
        int checkInterval = 20;
        if (jsonObject.has("checkInterval")) {
            checkInterval = jsonObject.get("checkInterval").getAsInt();
        }

        // 存储检查间隔
        entityCheckIntervals.put(entityKey, Math.max(1, checkInterval));

        // 解析法术列表
        if (jsonObject.has("spells") && jsonObject.get("spells").isJsonArray()) {
            List<SpellEntry> spells = new ArrayList<>();

            for (JsonElement spellElement : jsonObject.get("spells").getAsJsonArray()) {
                if (spellElement.isJsonObject()) {
                    JsonObject spellObject = spellElement.getAsJsonObject();
                    SpellEntry spellEntry = parseSpellEntry(spellObject);
                    if (spellEntry != null) {
                        spells.add(spellEntry);
                    }
                }
            }

            if (!spells.isEmpty()) {
                entitySpells.put(entityKey, spells);
                SpelledMobs.LOGGER.info("加载实体 {} 的 {} 个法术配置", entityId, spells.size());
            }
        }
    }

    /**
     * 解析法术条目
     * 
     * @param spellObject 法术JSON对象
     * @return 法术条目
     */
    private SpellEntry parseSpellEntry(JsonObject spellObject) {
        try {
            // 解析法术ID（必须字段）
            if (!spellObject.has("spellId")) {
                SpelledMobs.LOGGER.error("法术条目缺少必要的spellId字段");
                return null;
            }

            String spellId = spellObject.get("spellId").getAsString();

            // 解析法术等级
            int minLevel = 1;
            int maxLevel = 1;
            if (spellObject.has("minLevel")) {
                minLevel = Math.max(1, spellObject.get("minLevel").getAsInt());
            }
            if (spellObject.has("maxLevel")) {
                maxLevel = Math.max(minLevel, spellObject.get("maxLevel").getAsInt());
            }

            // 解析施法冷却时间
            int minCastTime = 60;
            int maxCastTime = 200;
            if (spellObject.has("minCastTime")) {
                minCastTime = Math.max(1, spellObject.get("minCastTime").getAsInt());
            }
            if (spellObject.has("maxCastTime")) {
                maxCastTime = Math.max(minCastTime, spellObject.get("maxCastTime").getAsInt());
            }

            // 解析权重和几率
            int weight = 1;
            float chance = 1.0f;
            if (spellObject.has("weight")) {
                weight = Math.max(1, spellObject.get("weight").getAsInt());
            }
            if (spellObject.has("chance")) {
                chance = Math.min(1.0f, Math.max(0.0f, spellObject.get("chance").getAsFloat()));
            }

            // 创建法术条目
            SpellEntry spellEntry = new SpellEntry(spellId, minLevel, maxLevel, minCastTime, maxCastTime, weight,
                    chance);

            // 解析条件
            if (spellObject.has("conditions") && spellObject.get("conditions").isJsonArray()) {
                for (JsonElement conditionElement : spellObject.get("conditions").getAsJsonArray()) {
                    if (conditionElement.isJsonObject()) {
                        // JsonObject conditionObject = conditionElement.getAsJsonObject();
                        // TODO: 解析条件并添加到法术条目
                        // 这里需要根据实际的条件类型来实现
                    }
                }
            }

            return spellEntry;
        } catch (Exception e) {
            SpelledMobs.LOGGER.error("解析法术条目时发生错误", e);
            return null;
        }
    }

    /**
     * 判断实体是否有法术可用
     * 
     * @param entity 实体
     * @return 是否有可用法术
     */
    public boolean hasSpells(LivingEntity entity) {
        if (entity == null) {
            return false;
        }

        ResourceLocation entityKey = EntityType.getKey(entity.getType());
        return entitySpells.containsKey(entityKey) && !entitySpells.get(entityKey).isEmpty();
    }

    /**
     * 获取实体的下一个可施放的法术
     * 
     * @param entity 实体
     * @return 法术条目，如果没有可用法术则返回null
     */
    public SpellEntry getNextSpellToCast(LivingEntity entity) {
        if (!hasSpells(entity)) {
            return null;
        }

        ResourceLocation entityKey = EntityType.getKey(entity.getType());
        List<SpellEntry> spells = entitySpells.get(entityKey);

        // 获取实体检查间隔
        int checkInterval = entityCheckIntervals.getOrDefault(entityKey, 20);

        // 只在特定tick检查，减少性能开销
        if (entity.tickCount % checkInterval != 0) {
            return null;
        }

        // 根据权重和几率选择法术
        List<SpellEntry> availableSpells = new ArrayList<>();
        for (SpellEntry spell : spells) {
            // 检查施法几率
            if (RANDOM.nextFloat() > spell.getChance()) {
                continue;
            }

            // TODO: 检查法术条件是否满足
            // 这里需要创建条件上下文并检查所有条件

            // 暂时简单地添加所有几率通过的法术
            availableSpells.add(spell);
        }

        if (availableSpells.isEmpty()) {
            return null;
        }

        // 根据权重随机选择一个法术
        int totalWeight = availableSpells.stream().mapToInt(SpellEntry::getWeight).sum();
        int randomWeight = RANDOM.nextInt(totalWeight) + 1;

        int currentWeight = 0;
        for (SpellEntry spell : availableSpells) {
            currentWeight += spell.getWeight();
            if (randomWeight <= currentWeight) {
                return spell;
            }
        }

        // 默认返回第一个（理论上不会执行到这里）
        return availableSpells.get(0);
    }

    /**
     * 获取实体的所有法术
     * 
     * @param entity 实体
     * @return 法术列表，如果没有则返回空列表
     */
    public List<SpellEntry> getEntitySpells(LivingEntity entity) {
        if (entity == null) {
            return new ArrayList<>();
        }

        ResourceLocation entityKey = EntityType.getKey(entity.getType());
        return entitySpells.getOrDefault(entityKey, new ArrayList<>());
    }

    /**
     * 获取实体的检查间隔
     * 
     * @param entity 实体
     * @return 检查间隔（刻）
     */
    public int getEntityCheckInterval(LivingEntity entity) {
        if (entity == null) {
            return 20;
        }

        ResourceLocation entityKey = EntityType.getKey(entity.getType());
        return entityCheckIntervals.getOrDefault(entityKey, 20);
    }
}