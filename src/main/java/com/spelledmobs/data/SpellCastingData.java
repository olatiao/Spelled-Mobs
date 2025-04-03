package com.spelledmobs.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.spelledmobs.SpelledMobs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import net.minecraftforge.fml.loading.FMLPaths;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理实体的法术施放数据
 */
public class SpellCastingData {
    private static final Random RANDOM = new Random();

    // 存储每种实体的法术配置
    private final Map<ResourceLocation, List<SpellEntry>> entitySpells = new HashMap<>();
    // 存储每种实体的检查间隔
    private final Map<ResourceLocation, Integer> entityCheckIntervals = new HashMap<>();
    
    // 存储实体当前施法状态
    private final Map<UUID, SpellCastingState> entityCastingStates = new ConcurrentHashMap<>();

    /**
     * 实体施法状态类
     */
    private static class SpellCastingState {
        private final String spellId;
        private final int level;
        private final long startTime;
        private final int duration;  // 持续时间(刻)
        private boolean isCasting;

        public SpellCastingState(String spellId, int level, int duration) {
            this.spellId = spellId;
            this.level = level;
            this.startTime = System.currentTimeMillis();
            this.duration = duration > 0 ? duration : 60; // 默认至少3秒
            this.isCasting = true;
            
            SpelledMobs.LOGGER.debug("[SpelledMobs] 创建施法状态 - 法术: {}, 持续时间: {} 刻, 开始时间: {}", 
                spellId, this.duration, this.startTime);
        }

        public boolean isFinished() {
            // 如果手动停止施法
            if (!isCasting) {
                return true;
            }
            
            // 计算已经经过的时间（毫秒）
            long currentTime = System.currentTimeMillis();
            long elapsed = currentTime - startTime;
            
            // 将持续时间从刻转换为毫秒 (1刻 = 50毫秒)
            long durationMillis = duration * 50L;
            
            // 判断是否超过持续时间
            boolean finished = elapsed >= durationMillis;
            
            if (finished) {
                SpelledMobs.LOGGER.debug("[SpelledMobs] 法术 {} 已结束 - 持续时间: {} 毫秒, 实际持续: {} 毫秒", 
                    spellId, durationMillis, elapsed);
            }
            
            return finished;
        }

        public void stopCasting() {
            this.isCasting = false;
            SpelledMobs.LOGGER.debug("[SpelledMobs] 法术 {} 被手动停止", spellId);
        }

        public String getSpellId() {
            return spellId;
        }

        public int getLevel() {
            return level;
        }
        
        public long getRemainingTime() {
            if (!isCasting) {
                return 0;
            }
            
            long elapsed = System.currentTimeMillis() - startTime;
            long durationMillis = duration * 50L;
            return Math.max(0, durationMillis - elapsed);
        }
    }

    /**
     * 加载所有实体法术配置
     * 
     * @param resourceRoot 资源根路径 (已忽略，改为直接使用Forge配置目录)
     */
    public void loadEntitySpells(String resourceRoot) {
        // 清空现有配置
        entitySpells.clear();
        entityCheckIntervals.clear();

        SpelledMobs.LOGGER.info("[SpelledMobs] 开始加载实体法术配置...");

        try {
            // 使用Forge API获取配置目录的路径
            Path configDir = FMLPaths.CONFIGDIR.get();
            Path spelledMobsConfigDir = configDir.resolve("spelledmobs");
            Path entitySpellsDir = spelledMobsConfigDir.resolve("entity_spells");

            // 确保目录存在
            if (!Files.exists(spelledMobsConfigDir)) {
                Files.createDirectories(spelledMobsConfigDir);
                SpelledMobs.LOGGER.info("[SpelledMobs] 创建主配置目录: {}", spelledMobsConfigDir);
            }

            if (!Files.exists(entitySpellsDir)) {
                Files.createDirectories(entitySpellsDir);
                SpelledMobs.LOGGER.info("[SpelledMobs] 创建实体法术配置目录: {}", entitySpellsDir);
                // 创建默认配置示例
                createDefaultConfigFiles(entitySpellsDir.toFile());
            }

            // 检查目录是否为空
            File[] files = entitySpellsDir.toFile().listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
            if (files == null || files.length == 0) {
                SpelledMobs.LOGGER.info("[SpelledMobs] 法术配置目录为空，创建默认配置文件");
                createDefaultConfigFiles(entitySpellsDir.toFile());
            }

            // 从文件夹加载配置
            boolean foundConfigs = loadConfigsFromFolder(entitySpellsDir.toFile());

            if (entitySpells.isEmpty()) {
                if (foundConfigs) {
                    SpelledMobs.LOGGER.warn("[SpelledMobs] 虽然找到了配置文件，但没有有效的法术配置");
                } else {
                    SpelledMobs.LOGGER.warn("[SpelledMobs] 未从 {} 找到有效的法术配置文件", entitySpellsDir);
                }
                // 如果没有找到配置，添加默认测试配置
                addDefaultTestSpells();
            } else {
                SpelledMobs.LOGGER.info("[SpelledMobs] 实体法术配置加载完成，共加载 {} 个实体的配置", entitySpells.size());
            }
        } catch (Exception e) {
            SpelledMobs.LOGGER.error("[SpelledMobs] 加载实体法术配置时发生错误", e);
            // 出错时添加默认测试配置
            addDefaultTestSpells();
        }
    }

    /**
     * 从文件夹加载配置
     * 
     * @param folder 配置文件夹
     * @return 是否找到任何配置文件
     */
    private boolean loadConfigsFromFolder(File folder) {
        if (!folder.exists() || !folder.isDirectory()) {
            return false;
        }

        boolean foundAny = false;
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));

        if (files != null && files.length > 0) {
            for (File file : files) {
                try {
                    loadEntitySpellConfigFromFile(file);
                    foundAny = true;
                } catch (Exception e) {
                    SpelledMobs.LOGGER.error("[SpelledMobs] 加载配置文件 {} 失败", file.getName(), e);
                }
            }
        }

        return foundAny;
    }

    /**
     * 从文件加载实体法术配置
     * 
     * @param file 配置文件
     * @throws IOException 如果加载失败
     */
    private void loadEntitySpellConfigFromFile(File file) throws IOException {
        try (FileReader reader = new FileReader(file)) {
            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
            parseEntitySpellConfig(jsonObject);
            SpelledMobs.LOGGER.info("[SpelledMobs] 已从文件 {} 加载法术配置", file.getName());
        }
    }

    /**
     * 创建默认配置文件示例
     * 
     * @param folder 配置文件夹
     */
    public void createDefaultConfigFiles(File folder) {
        // 为僵尸创建默认配置
        createExampleConfig(folder, "zombie.json", "minecraft:zombie", 20, new String[][] {
                { "irons_spellbooks:fireball", "1", "3", "60", "120", "1", "1.0" }
        });

        // 为骷髅创建默认配置
        createExampleConfig(folder, "skeleton.json", "minecraft:skeleton", 20, new String[][] {
                { "irons_spellbooks:ice_spike", "1", "2", "40", "80", "1", "1.0" }
        });

        // 为爬行者创建默认配置
        createExampleConfig(folder, "creeper.json", "minecraft:creeper", 30, new String[][] {
                { "irons_spellbooks:lightning_bolt", "1", "1", "100", "200", "1", "0.8" }
        });
    }

    /**
     * 创建示例配置文件
     * 
     * @param folder        文件夹
     * @param fileName      文件名
     * @param entityId      实体ID
     * @param checkInterval 检查间隔
     * @param spells        法术数组 [spellId, minLevel, maxLevel, minCastTime,
     *                      maxCastTime, weight, chance]
     */
    private void createExampleConfig(File folder, String fileName, String entityId, int checkInterval,
            String[][] spells) {
        File configFile = new File(folder, fileName);

        try (FileWriter writer = new FileWriter(configFile)) {
            JsonObject root = new JsonObject();
            root.addProperty("entityId", entityId);
            root.addProperty("checkInterval", checkInterval);

            JsonArray spellsArray = new JsonArray();
            for (String[] spell : spells) {
                JsonObject spellObj = new JsonObject();
                spellObj.addProperty("spellId", spell[0]);
                spellObj.addProperty("minLevel", Integer.parseInt(spell[1]));
                spellObj.addProperty("maxLevel", Integer.parseInt(spell[2]));
                spellObj.addProperty("minCastTime", Integer.parseInt(spell[3]));
                spellObj.addProperty("maxCastTime", Integer.parseInt(spell[4]));
                spellObj.addProperty("weight", Integer.parseInt(spell[5]));
                spellObj.addProperty("chance", Float.parseFloat(spell[6]));
                spellsArray.add(spellObj);
            }

            root.add("spells", spellsArray);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            writer.write(gson.toJson(root));

            SpelledMobs.LOGGER.info("[SpelledMobs] 已创建示例配置文件: {}", configFile.getAbsolutePath());
        } catch (IOException e) {
            SpelledMobs.LOGGER.error("[SpelledMobs] 创建示例配置文件失败: {}", fileName, e);
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
                SpelledMobs.LOGGER.info("[SpelledMobs] 加载实体 {} 的 {} 个法术配置", entityId, spells.size());
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
            // 解析法术ID（必须有）
            if (!spellObject.has("spellId")) {
                SpelledMobs.LOGGER.error("[SpelledMobs] 法术配置缺少spellId字段");
                return null;
            }

            String spellId = spellObject.get("spellId").getAsString();

            // 解析最小和最大法术等级（默认均为1）
            int minLevel = 1;
            if (spellObject.has("minLevel")) {
                minLevel = spellObject.get("minLevel").getAsInt();
            }

            int maxLevel = minLevel;
            if (spellObject.has("maxLevel")) {
                maxLevel = spellObject.get("maxLevel").getAsInt();
            }

            // 确保maxLevel不小于minLevel
            maxLevel = Math.max(minLevel, maxLevel);

            // 解析最小和最大施法冷却时间（默认为60和200刻）
            int minCastTime = 60;
            if (spellObject.has("minCastTime")) {
                minCastTime = spellObject.get("minCastTime").getAsInt();
            }

            int maxCastTime = 200;
            if (spellObject.has("maxCastTime")) {
                maxCastTime = spellObject.get("maxCastTime").getAsInt();
            }

            // 确保maxCastTime不小于minCastTime
            maxCastTime = Math.max(minCastTime, maxCastTime);

            // 解析权重（默认为1）
            int weight = 1;
            if (spellObject.has("weight")) {
                weight = Math.max(1, spellObject.get("weight").getAsInt());
            }

            // 解析施法几率（默认为1.0）
            float chance = 1.0f;
            if (spellObject.has("chance")) {
                chance = Math.min(1.0f, Math.max(0.0f, spellObject.get("chance").getAsFloat()));
            }

            // 创建法术条目
            SpellEntry spellEntry = new SpellEntry(spellId, minLevel, maxLevel, minCastTime, maxCastTime, weight,
                    chance);

            // 解析施法条件（可选）
            if (spellObject.has("conditions") && spellObject.get("conditions").isJsonArray()) {
                JsonArray conditionsArray = spellObject.get("conditions").getAsJsonArray();

                for (JsonElement conditionElement : conditionsArray) {
                    if (conditionElement.isJsonObject()) {
                        JsonObject conditionObject = conditionElement.getAsJsonObject();
                        SpellCondition condition = SpellConditionFactory.fromJson(conditionObject);
                        if (condition != null) {
                            spellEntry.addCondition(condition);
                        }
                    }
                }
            }

            return spellEntry;
        } catch (Exception e) {
            SpelledMobs.LOGGER.error("[SpelledMobs] 解析法术条目时发生错误", e);
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

        // 预设一些测试用的法术配置，方便测试
        if (entitySpells.isEmpty()) {
            addDefaultTestSpells();
        }

        ResourceLocation entityKey = EntityType.getKey(entity.getType());
        return entitySpells.containsKey(entityKey) && !entitySpells.get(entityKey).isEmpty();
    }

    /**
     * 添加默认测试用法术配置
     * 此方法仅用于测试，实际应该从配置文件加载
     */
    private void addDefaultTestSpells() {
        SpelledMobs.LOGGER.info("[SpelledMobs] 未找到法术配置，添加默认测试法术配置...");

        // 使用铁魔法模组中实际存在的法术ID
        // 注意：这些是铁魔法模组中真实的法术ID
        addTestSpellsForEntity("minecraft:zombie", "irons_spellbooks:fireball", 1, 3, 60, 120);
        addTestSpellsForEntity("minecraft:skeleton", "irons_spellbooks:ice_spike", 1, 2, 40, 80);
        addTestSpellsForEntity("minecraft:creeper", "irons_spellbooks:lightning_bolt", 1, 1, 100, 200);
        addTestSpellsForEntity("minecraft:spider", "irons_spellbooks:poison_arrow", 1, 2, 80, 160);
        addTestSpellsForEntity("minecraft:witch", "irons_spellbooks:magic_missile", 1, 3, 20, 60);

        // 测试更多法术
        addTestSpellsForEntity("minecraft:husk", "irons_spellbooks:fire_breath", 1, 2, 100, 200);
        addTestSpellsForEntity("minecraft:stray", "irons_spellbooks:frost_breath", 1, 2, 100, 200);
        addTestSpellsForEntity("minecraft:evoker", "irons_spellbooks:ascension", 1, 3, 60, 120);
        addTestSpellsForEntity("minecraft:pillager", "irons_spellbooks:lesser_heal", 1, 2, 120, 240);

        SpelledMobs.LOGGER.info("[SpelledMobs] 默认测试法术配置已添加，共 {} 个实体类型", entitySpells.size());

        // 打印所有添加的法术，方便调试
        for (Map.Entry<ResourceLocation, List<SpellEntry>> entry : entitySpells.entrySet()) {
            String entityId = entry.getKey().toString();
            List<SpellEntry> spells = entry.getValue();
            for (SpellEntry spell : spells) {
                SpelledMobs.LOGGER.info("[SpelledMobs] 实体 {} 可以施放法术: {} (等级 {}-{})",
                        entityId, spell.getSpellId(), spell.getMinLevel(), spell.getMaxLevel());
            }
        }
    }

    /**
     * 为指定实体添加测试法术
     */
    private void addTestSpellsForEntity(String entityId, String spellId, int minLevel, int maxLevel,
            int minCastTime, int maxCastTime) {
        ResourceLocation entityKey = new ResourceLocation(entityId);

        // 创建法术条目
        SpellEntry spellEntry = new SpellEntry(spellId, minLevel, maxLevel, minCastTime, maxCastTime, 1, 1.0f);

        // 添加到实体法术列表
        List<SpellEntry> spells = entitySpells.computeIfAbsent(entityKey, k -> new ArrayList<>());
        spells.add(spellEntry);

        // 设置检查间隔
        entityCheckIntervals.put(entityKey, 20); // 默认每秒检查一次

        SpelledMobs.LOGGER.info("[SpelledMobs] 为实体 {} 添加测试法术: {}", entityId, spellId);
    }

    /**
     * 手动为实体添加法术
     * 
     * @param entityId    实体ID，如 "minecraft:zombie"
     * @param spellId     法术ID，如 "fireball"
     * @param minLevel    最小法术等级
     * @param maxLevel    最大法术等级
     * @param minCastTime 最小施法冷却时间（刻）
     * @param maxCastTime 最大施法冷却时间（刻）
     */
    public void addSpellForEntity(String entityId, String spellId, int minLevel, int maxLevel,
            int minCastTime, int maxCastTime) {
        addTestSpellsForEntity(entityId, spellId, minLevel, maxLevel, minCastTime, maxCastTime);
    }

    /**
     * 开始实体施放法术
     * @param entity 施法实体
     * @param spellId 要施放的法术
     * @param level 法术等级
     * @param duration 持续时间(刻)，-1表示非持续性法术
     */
    public void startCasting(LivingEntity entity, String spellId, int level, int duration) {
        if (entity == null || spellId == null) {
            SpelledMobs.LOGGER.warn("[SpelledMobs] 尝试开始施法但实体或法术ID为空");
            return;
        }
        
        // 对于非持续性法术，不需要记录状态
        if (duration <= 0) {
            SpelledMobs.LOGGER.debug("[SpelledMobs] 法术 {} 不是持续性法术，不记录状态", spellId);
            return;
        }
        
        UUID entityId = entity.getUUID();
        SpellCastingState state = new SpellCastingState(spellId, level, duration);
        entityCastingStates.put(entityId, state);
        
        SpelledMobs.LOGGER.info("[SpelledMobs] 实体 {} 开始施放法术 {}, 等级 {}, 持续时间 {} 刻", 
                entity.getName().getString(), spellId, level, duration);
    }

    /**
     * 停止实体施放法术
     * @param entity 施法实体
     */
    public void stopCasting(LivingEntity entity) {
        if (entity == null) {
            return;
        }
        
        UUID entityId = entity.getUUID();
        SpellCastingState state = entityCastingStates.get(entityId);
        if (state != null) {
            state.stopCasting();
            SpelledMobs.LOGGER.debug("[SpelledMobs] 实体 {} 停止施放法术 {}", 
                    entity.getName().getString(), state.getSpellId());
        }
    }

    /**
     * 检查实体是否正在施放法术
     * @param entity 施法实体
     * @return 是否正在施放法术
     */
    public boolean isCasting(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        
        UUID entityId = entity.getUUID();
        SpellCastingState state = entityCastingStates.get(entityId);
        if (state == null) {
            return false;
        }
        
        // 检查法术是否已经结束
        if (state.isFinished()) {
            entityCastingStates.remove(entityId);
            SpelledMobs.LOGGER.debug("[SpelledMobs] 实体 {} 的法术 {} 已结束", 
                    entity.getName().getString(), state.getSpellId());
            return false;
        }
        
        return true;
    }

    /**
     * 获取实体当前施放的法术ID
     * @param entity 施法实体
     * @return 法术ID，如果没有则返回null
     */
    public String getCurrentSpellId(LivingEntity entity) {
        if (entity == null) {
            return null;
        }
        
        UUID entityId = entity.getUUID();
        SpellCastingState state = entityCastingStates.get(entityId);
        return state != null ? state.getSpellId() : null;
    }

    /**
     * 清理实体施法状态
     * @param entity 施法实体
     */
    public void cleanupEntityState(LivingEntity entity) {
        if (entity == null) {
            return;
        }
        
        UUID entityId = entity.getUUID();
        SpellCastingState oldState = entityCastingStates.remove(entityId);
        
        if (oldState != null) {
            SpelledMobs.LOGGER.debug("[SpelledMobs] 清理实体 {} 的施法状态: {}", 
                entity.getName().getString(), oldState.getSpellId());
        }
    }

    /**
     * 获取下一个要施放的法术
     * 
     * @param entity 施法实体
     * @return 要施放的法术条目，如果无法施放则返回null
     */
    public SpellEntry getNextSpellToCast(LivingEntity entity) {
        if (!hasSpells(entity)) {
            return null;
        }
        
        // 检查实体是否已经在施放法术
        if (isCasting(entity)) {
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

        // 获取实体的目标
        LivingEntity target = entity.getLastHurtByMob();
        if (target == null || !target.isAlive()) {
            // 如果没有上次攻击者或已死亡，尝试获取当前目标
            if (entity instanceof Mob mobEntity) {
                target = mobEntity.getTarget();
            }
        }

        // 如果没有目标，则不施法
        if (target == null || !target.isAlive()) {
            return null;
        }

        // 创建条件上下文
        Level level = entity.level();
        SpellConditionContext context = new SpellConditionContext(entity, target, level);

        // 首先过滤出满足条件的法术
        List<SpellEntry> eligibleSpells = new ArrayList<>();
        for (SpellEntry spell : spells) {
            // 只检查法术条件是否满足，不进行几率检查
            boolean conditionsMet = spell.checkConditions(context);
            if (conditionsMet) {
                eligibleSpells.add(spell);
            }
        }

        if (eligibleSpells.isEmpty()) {
            return null;
        }

        // 使用轮盘赌选择算法，考虑权重和几率
        List<SpellEntry> weightedSpells = new ArrayList<>();
        for (SpellEntry spell : eligibleSpells) {
            // 将法术按权重和几率添加到加权列表中
            // 例如，权重为2、几率为0.5的法术会有50%的几率被添加两次
            if (RANDOM.nextFloat() <= spell.getChance()) {
                for (int i = 0; i < spell.getWeight(); i++) {
                    weightedSpells.add(spell);
                }
            }
        }

        if (weightedSpells.isEmpty()) {
            return null;
        }

        // 从加权列表中随机选择一个法术
        return weightedSpells.get(RANDOM.nextInt(weightedSpells.size()));
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