package com.spelledmobs.data;

import com.google.gson.JsonObject;
import com.spelledmobs.SpelledMobs;
import com.spelledmobs.data.SpellCondition.ComparisonOperator;
import com.spelledmobs.data.SpellCondition.ConditionType;
import com.spelledmobs.data.conditions.*;

/**
 * 法术条件工厂，用于创建不同类型的条件
 */
public class SpellConditionFactory {

    /**
     * 创建法术条件
     * 
     * @param type         条件类型
     * @param operator     比较操作符
     * @param stringValue  字符串值
     * @param numericValue 数值
     * @param invert       是否反转结果
     * @param extraData    额外数据
     * @return 条件实例，如果类型不支持则返回null
     */
    public static SpellCondition createCondition(ConditionType type, ComparisonOperator operator, String stringValue,
            double numericValue, boolean invert, JsonObject extraData) {
        try {
            return switch (type) {
                case HEALTH_PERCENTAGE -> new HealthPercentageCondition(operator, numericValue, invert, extraData);
                case TARGET_DISTANCE -> new TargetDistanceCondition(operator, numericValue, invert, extraData);
                case TARGET_TYPE -> new TargetTypeCondition(operator, stringValue, invert, extraData);
                case WEATHER -> new WeatherCondition(operator, stringValue, invert, extraData);
                case TIME_OF_DAY -> new TimeOfDayCondition(operator, numericValue, invert, extraData);
                case RANDOM_CHANCE -> new RandomChanceCondition(operator, numericValue, invert, extraData);
                default -> {
                    SpelledMobs.LOGGER.warn("[SpelledMobs] 不支持的条件类型: {}", type);
                    yield null;
                }
            };
        } catch (Exception e) {
            SpelledMobs.LOGGER.error("[SpelledMobs] 创建条件时发生错误: {} ({})", type, e.getMessage());
            return null;
        }
    }

    /**
     * 从JSON对象创建条件
     * 
     * @param jsonObject 条件JSON对象
     * @return 条件实例，如果解析失败则返回null
     */
    public static SpellCondition fromJson(JsonObject jsonObject) {
        try {
            // 条件类型（必须）
            if (!jsonObject.has("type")) {
                SpelledMobs.LOGGER.error("[SpelledMobs] 条件缺少type字段");
                return null;
            }

            String typeStr = jsonObject.get("type").getAsString();
            ConditionType type = ConditionType.byId(typeStr);

            // 比较操作符（可选，默认为EQUALS）
            ComparisonOperator operator = ComparisonOperator.EQUALS;
            if (jsonObject.has("operator")) {
                String opStr = jsonObject.get("operator").getAsString();
                operator = ComparisonOperator.byId(opStr);
            }

            // 字符串值（可选）
            String stringValue = null;
            if (jsonObject.has("value")) {
                stringValue = jsonObject.get("value").getAsString();
            }

            // 数值（可选）
            double numericValue = 0;
            if (jsonObject.has("numeric_value")) {
                numericValue = jsonObject.get("numeric_value").getAsDouble();
            }

            // 反转（可选，默认为false）
            boolean invert = false;
            if (jsonObject.has("invert")) {
                invert = jsonObject.get("invert").getAsBoolean();
            }

            // 额外数据（可选）
            JsonObject extraData = null;
            if (jsonObject.has("extra_data") && jsonObject.get("extra_data").isJsonObject()) {
                extraData = jsonObject.get("extra_data").getAsJsonObject();
            }

            return createCondition(type, operator, stringValue, numericValue, invert, extraData);
        } catch (Exception e) {
            SpelledMobs.LOGGER.error("[SpelledMobs] 解析条件JSON时发生错误", e);
            return null;
        }
    }
}