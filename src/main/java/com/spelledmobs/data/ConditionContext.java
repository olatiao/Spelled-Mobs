package com.spelledmobs.data;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import java.util.HashMap;
import java.util.Map;

/**
 * 条件检查上下文，包含检查条件时需要的信息
 */
public class ConditionContext {
    private final LivingEntity entity;
    private final LivingEntity target;
    private final Level level;
    private final Map<String, Object> extraData;

    /**
     * 创建一个条件检查上下文
     * 
     * @param entity 施法实体
     * @param target 目标实体
     * @param level  世界
     */
    public ConditionContext(LivingEntity entity, LivingEntity target, Level level) {
        this.entity = entity;
        this.target = target;
        this.level = level;
        this.extraData = new HashMap<>();
    }

    /**
     * 获取施法实体
     */
    public LivingEntity getEntity() {
        return entity;
    }

    /**
     * 获取目标实体
     */
    public LivingEntity getTarget() {
        return target;
    }

    /**
     * 获取世界
     */
    public Level getLevel() {
        return level;
    }

    /**
     * 添加额外数据
     * 
     * @param key   数据键
     * @param value 数据值
     */
    public void putExtraData(String key, Object value) {
        extraData.put(key, value);
    }

    /**
     * 获取额外数据
     * 
     * @param key 数据键
     * @return 数据值，如果不存在则返回null
     */
    public Object getExtraData(String key) {
        return extraData.get(key);
    }

    /**
     * 获取额外数据，如果不存在则返回默认值
     * 
     * @param key          数据键
     * @param defaultValue 默认值
     * @return 数据值，如果不存在则返回默认值
     */
    public Object getExtraData(String key, Object defaultValue) {
        return extraData.getOrDefault(key, defaultValue);
    }

    /**
     * 获取额外数据，并尝试转换为指定类型
     * 
     * @param <T>   目标类型
     * @param key   数据键
     * @param clazz 目标类型的Class
     * @return 转换后的数据值，如果不存在或无法转换则返回null
     */
    @SuppressWarnings("unchecked")
    public <T> T getExtraDataAs(String key, Class<T> clazz) {
        Object value = extraData.get(key);
        if (value != null && clazz.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * 获取额外数据作为整数
     * 
     * @param key          数据键
     * @param defaultValue 默认值
     * @return 整数值，如果不存在或无法转换则返回默认值
     */
    public int getExtraDataAsInt(String key, int defaultValue) {
        Object value = extraData.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * 获取额外数据作为浮点数
     * 
     * @param key          数据键
     * @param defaultValue 默认值
     * @return 浮点数值，如果不存在或无法转换则返回默认值
     */
    public float getExtraDataAsFloat(String key, float defaultValue) {
        Object value = extraData.get(key);
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        } else if (value instanceof String) {
            try {
                return Float.parseFloat((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * 获取额外数据作为布尔值
     * 
     * @param key          数据键
     * @param defaultValue 默认值
     * @return 布尔值，如果不存在或无法转换则返回默认值
     */
    public boolean getExtraDataAsBoolean(String key, boolean defaultValue) {
        Object value = extraData.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }

    /**
     * 获取额外数据作为字符串
     * 
     * @param key          数据键
     * @param defaultValue 默认值
     * @return 字符串值，如果不存在则返回默认值
     */
    public String getExtraDataAsString(String key, String defaultValue) {
        Object value = extraData.get(key);
        if (value != null) {
            return value.toString();
        }
        return defaultValue;
    }
}