package com.spelledmobs.util;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;
import com.spelledmobs.SpelledMobs;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

/**
 * 施法条件判断类
 */
public class SpellCastCondition {
    private final ConditionType type;
    private final String value;
    private final ComparisonOperator operator;
    private final float numericValue;
    private final boolean invertCondition;
    private final Map<String, String> extraData = new HashMap<>();
    
    public enum ConditionType {
        HEALTH_PERCENTAGE,      // 实体当前血量百分比
        HEALTH_ABSOLUTE,        // 实体当前血量绝对值
        TARGET_DISTANCE,        // 与目标的距离
        TARGET_HEALTH,          // 目标血量
        TARGET_TYPE,            // 目标类型
        ENTITY_NAME,            // 实体名称
        HELD_ITEM,              // 手持物品
        TIME_OF_DAY,            // 世界时间
        WEATHER,                // 天气状态
        MOON_PHASE,             // 月相
        BIOME,                  // 生物群系
        IS_IN_WATER,            // 是否在水中
        IS_ON_FIRE,             // 是否着火
        IS_SNEAKING,            // 是否潜行
        IS_SPRINTING,           // 是否疾跑
        STATUS_EFFECT,          // 状态效果
        LIGHT_LEVEL,            // 光照等级
        HEIGHT,                 // 高度
        ARMOR_VALUE,            // 护甲值
        LAST_DAMAGE_SOURCE,     // 最后受到的伤害源
        TARGET_COUNT,           // 附近目标数量
        RANDOM_CHANCE           // 随机几率
    }
    
    public enum ComparisonOperator {
        EQUALS,
        NOT_EQUALS,
        GREATER_THAN,
        LESS_THAN,
        GREATER_THAN_OR_EQUALS,
        LESS_THAN_OR_EQUALS,
        CONTAINS,
        STARTS_WITH,
        ENDS_WITH
    }
    
    private SpellCastCondition(ConditionType type, String value, ComparisonOperator operator, float numericValue, boolean invertCondition) {
        this.type = type;
        this.value = value;
        this.operator = operator;
        this.numericValue = numericValue;
        this.invertCondition = invertCondition;
    }
    
    /**
     * 从JSON创建施法条件
     * @param json JSON对象
     * @return 施法条件
     */
    public static SpellCastCondition fromJson(JsonObject json) {
        if (!json.has("type")) {
            throw new IllegalArgumentException("Missing required field: type");
        }
        
        String typeStr = json.get("type").getAsString();
        ConditionType type;
        try {
            type = ConditionType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid condition type: " + typeStr);
        }
        
        String value = json.has("value") ? json.get("value").getAsString() : "";
        
        ComparisonOperator operator = ComparisonOperator.EQUALS;
        if (json.has("operator")) {
            try {
                operator = ComparisonOperator.valueOf(json.get("operator").getAsString().toUpperCase());
            } catch (IllegalArgumentException e) {
                SpelledMobs.LOGGER.warn("Invalid operator: {}, using EQUALS", json.get("operator").getAsString());
            }
        }
        
        float numericValue = 0;
        if (json.has("numeric_value")) {
            numericValue = json.get("numeric_value").getAsFloat();
        }
        
        boolean invertCondition = false;
        if (json.has("invert")) {
            invertCondition = json.get("invert").getAsBoolean();
        }
        
        SpellCastCondition condition = new SpellCastCondition(type, value, operator, numericValue, invertCondition);
        
        // 添加额外的数据（针对特定条件类型）
        if (json.has("extra_data") && json.get("extra_data").isJsonObject()) {
            JsonObject extraData = json.getAsJsonObject("extra_data");
            for (var entry : extraData.entrySet()) {
                if (entry.getValue().isJsonPrimitive()) {
                    condition.extraData.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
        }
        
        return condition;
    }
    
    /**
     * 评估施法条件是否满足
     * @param caster 施法者
     * @param target 目标实体（可能为null）
     * @return 条件是否满足
     */
    public boolean evaluate(LivingEntity caster, Entity target) {
        boolean result = switch (type) {
            case HEALTH_PERCENTAGE -> evaluateHealthPercentage(caster);
            case HEALTH_ABSOLUTE -> evaluateHealthAbsolute(caster);
            case TARGET_DISTANCE -> evaluateTargetDistance(caster, target);
            case TARGET_HEALTH -> evaluateTargetHealth(target);
            case TARGET_TYPE -> evaluateTargetType(target);
            case ENTITY_NAME -> evaluateEntityName(caster);
            case HELD_ITEM -> evaluateHeldItem(caster);
            case TIME_OF_DAY -> evaluateTimeOfDay(caster.getWorld());
            case WEATHER -> evaluateWeather(caster.getWorld());
            case MOON_PHASE -> evaluateMoonPhase(caster.getWorld());
            case BIOME -> evaluateBiome(caster);
            case IS_IN_WATER -> caster.isSubmergedInWater();
            case IS_ON_FIRE -> caster.isOnFire();
            case IS_SNEAKING -> caster.isSneaking();
            case IS_SPRINTING -> caster.isSprinting();
            case STATUS_EFFECT -> evaluateStatusEffect(caster);
            case LIGHT_LEVEL -> evaluateLightLevel(caster);
            case HEIGHT -> evaluateHeight(caster);
            case ARMOR_VALUE -> evaluateArmorValue(caster);
            case LAST_DAMAGE_SOURCE -> evaluateLastDamageSource(caster);
            case TARGET_COUNT -> evaluateTargetCount(caster);
            case RANDOM_CHANCE -> Math.random() < numericValue / 100.0f; // 百分比转换为概率
        };
        
        return invertCondition ? !result : result;
    }
    
    private boolean evaluateHealthPercentage(LivingEntity entity) {
        float healthPercentage = entity.getHealth() / entity.getMaxHealth() * 100;
        return compareFloat(healthPercentage, numericValue);
    }
    
    private boolean evaluateHealthAbsolute(LivingEntity entity) {
        return compareFloat(entity.getHealth(), numericValue);
    }
    
    private boolean evaluateTargetDistance(Entity caster, Entity target) {
        if (target == null) return false;
        float distance = caster.distanceTo(target);
        return compareFloat(distance, numericValue);
    }
    
    private boolean evaluateTargetHealth(Entity target) {
        if (target == null || !(target instanceof LivingEntity living)) return false;
        return compareFloat(living.getHealth(), numericValue);
    }
    
    private boolean evaluateTargetType(Entity target) {
        if (target == null) return false;
        String targetId = Registries.ENTITY_TYPE.getId(target.getType()).toString();
        return compareString(targetId, value);
    }
    
    private boolean evaluateEntityName(Entity entity) {
        String name = entity.getName().getString();
        return compareString(name, value);
    }
    
    private boolean evaluateHeldItem(LivingEntity entity) {
        ItemStack mainHand = entity.getMainHandStack();
        if (mainHand.isEmpty()) return false;
        
        String itemId = Registries.ITEM.getId(mainHand.getItem()).toString();
        return compareString(itemId, value);
    }
    
    private boolean evaluateTimeOfDay(World world) {
        // 获取世界时间（0-24000）
        long time = world.getTimeOfDay() % 24000;
        return compareFloat(time, numericValue);
    }
    
    private boolean evaluateWeather(World world) {
        // 根据value值判断天气
        return switch(value.toLowerCase()) {
            case "clear" -> !world.isRaining() && !world.isThundering();
            case "rain" -> world.isRaining() && !world.isThundering();
            case "thunder", "storm" -> world.isThundering();
            default -> false;
        };
    }
    
    private boolean evaluateMoonPhase(World world) {
        // 获取月相（0-7）
        int moonPhase = world.getMoonPhase();
        return compareFloat(moonPhase, numericValue);
    }
    
    private boolean evaluateBiome(LivingEntity entity) {
        try {
            // 获取实体所在的生物群系
            Biome biome = entity.getWorld().getBiome(entity.getBlockPos()).value();
            
            // 获取生物群系ID
            Identifier biomeId = entity.getWorld().getRegistryManager()
                    .get(RegistryKeys.BIOME)
                    .getId(biome);
            
            if (biomeId == null) return false;
            return compareString(biomeId.toString(), value);
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean evaluateStatusEffect(LivingEntity entity) {
        try {
            // 通过ID获取状态效果
            Identifier statusEffectId = new Identifier(value);
            StatusEffect statusEffect = Registries.STATUS_EFFECT.get(statusEffectId);
            
            if (statusEffect == null) return false;
            
            // 检查实体是否有该状态效果
            StatusEffectInstance effectInstance = entity.getStatusEffect(statusEffect);
            if (effectInstance == null) return false;
            
            // 如果有数值比较，则比较效果等级
            if (operator != ComparisonOperator.EQUALS && operator != ComparisonOperator.NOT_EQUALS) {
                return compareFloat(effectInstance.getAmplifier() + 1, numericValue);
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean evaluateLightLevel(LivingEntity entity) {
        int lightLevel = entity.getWorld().getLightLevel(entity.getBlockPos());
        return compareFloat(lightLevel, numericValue);
    }
    
    private boolean evaluateHeight(LivingEntity entity) {
        return compareFloat((float) entity.getY(), numericValue);
    }
    
    private boolean evaluateArmorValue(LivingEntity entity) {
        float armorValue = entity.getArmor();
        return compareFloat(armorValue, numericValue);
    }
    
    private boolean evaluateLastDamageSource(LivingEntity entity) {
        DamageSource lastDamageSource = entity.getRecentDamageSource();
        if (lastDamageSource == null) return false;
        
        return compareString(lastDamageSource.getName(), value);
    }
    
    private boolean evaluateTargetCount(LivingEntity entity) {
        // 获取检测半径（从额外数据中）
        float radius = 16.0f; // 默认值
        if (extraData.containsKey("radius")) {
            try {
                radius = Float.parseFloat(extraData.get("radius"));
            } catch (NumberFormatException e) {
                // 使用默认值
            }
        }
        
        // 获取目标类型（从额外数据中）
        String targetType = extraData.getOrDefault("target_type", "");
        
        // 获取附近实体数量
        int count = entity.getWorld().getEntitiesByClass(
            LivingEntity.class,
            entity.getBoundingBox().expand(radius),
            e -> {
                if (e == entity) return false;
                if (e.isRemoved() || e.isDead()) return false;
                
                // 如果指定了目标类型，则检查实体类型
                if (!targetType.isEmpty()) {
                    String entityId = Registries.ENTITY_TYPE.getId(e.getType()).toString();
                    return entityId.equals(targetType);
                }
                
                return true;
            }
        ).size();
        
        return compareFloat(count, numericValue);
    }
    
    private boolean compareFloat(float actual, float expected) {
        return switch (operator) {
            case EQUALS -> Math.abs(actual - expected) < 0.001f;
            case NOT_EQUALS -> Math.abs(actual - expected) >= 0.001f;
            case GREATER_THAN -> actual > expected;
            case LESS_THAN -> actual < expected;
            case GREATER_THAN_OR_EQUALS -> actual >= expected;
            case LESS_THAN_OR_EQUALS -> actual <= expected;
            default -> false;
        };
    }
    
    private boolean compareString(String actual, String expected) {
        return switch (operator) {
            case EQUALS -> actual.equals(expected);
            case NOT_EQUALS -> !actual.equals(expected);
            case CONTAINS -> actual.contains(expected);
            case STARTS_WITH -> actual.startsWith(expected);
            case ENDS_WITH -> actual.endsWith(expected);
            default -> false;
        };
    }
    
    /**
     * 获取条件类型
     * @return 条件类型
     */
    public ConditionType getType() {
        return type;
    }
    
    /**
     * 获取条件值
     * @return 条件值
     */
    public String getValue() {
        return value;
    }
    
    /**
     * 获取比较运算符
     * @return 比较运算符
     */
    public ComparisonOperator getOperator() {
        return operator;
    }
    
    /**
     * 获取数值条件的值
     * @return 数值条件的值
     */
    public float getNumericValue() {
        return numericValue;
    }
    
    /**
     * 是否反转条件结果
     * @return 是否反转
     */
    public boolean isInverted() {
        return invertCondition;
    }
    
    /**
     * 获取额外数据
     * @param key 键
     * @return 值
     */
    public String getExtraData(String key) {
        return extraData.getOrDefault(key, "");
    }
} 