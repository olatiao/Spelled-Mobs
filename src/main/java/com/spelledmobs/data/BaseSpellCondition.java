package com.spelledmobs.data;

import com.google.gson.JsonObject;

/**
 * 基础法术条件类，用于实现通用条件逻辑
 */
public abstract class BaseSpellCondition implements SpellCondition {
    private final ConditionType type;
    private final ComparisonOperator operator;
    private final String stringValue;
    private final double numericValue;
    private final boolean invert;
    private final JsonObject extraData;

    /**
     * 创建基础法术条件
     * 
     * @param type         条件类型
     * @param operator     比较操作符
     * @param stringValue  字符串值
     * @param numericValue 数值
     * @param invert       是否反转结果
     * @param extraData    额外数据
     */
    protected BaseSpellCondition(ConditionType type, ComparisonOperator operator, String stringValue,
            double numericValue, boolean invert, JsonObject extraData) {
        this.type = type;
        this.operator = operator;
        this.stringValue = stringValue != null ? stringValue : "";
        this.numericValue = numericValue;
        this.invert = invert;
        this.extraData = extraData != null ? extraData : new JsonObject();
    }

    @Override
    public ConditionType getType() {
        return type;
    }

    @Override
    public ComparisonOperator getOperator() {
        return operator;
    }

    /**
     * 获取字符串值
     */
    protected String getStringValue() {
        return stringValue;
    }

    /**
     * 获取数值
     */
    protected double getNumericValue() {
        return numericValue;
    }

    /**
     * 获取额外数据
     */
    protected JsonObject getExtraData() {
        return extraData;
    }

    /**
     * 是否反转结果
     */
    protected boolean isInvert() {
        return invert;
    }

    /**
     * 应用反转逻辑
     */
    protected boolean applyInvert(boolean result) {
        return invert ? !result : result;
    }

    /**
     * 从额外数据中获取数值
     */
    protected double getExtraDataAsDouble(String key, double defaultValue) {
        if (extraData.has(key)) {
            try {
                return extraData.get(key).getAsDouble();
            } catch (Exception e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * 从额外数据中获取字符串
     */
    protected String getExtraDataAsString(String key, String defaultValue) {
        if (extraData.has(key)) {
            try {
                return extraData.get(key).getAsString();
            } catch (Exception e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * 从额外数据中获取布尔值
     */
    protected boolean getExtraDataAsBoolean(String key, boolean defaultValue) {
        if (extraData.has(key)) {
            try {
                return extraData.get(key).getAsBoolean();
            } catch (Exception e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}