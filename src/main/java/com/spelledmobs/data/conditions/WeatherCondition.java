package com.spelledmobs.data.conditions;

import com.google.gson.JsonObject;
import com.spelledmobs.data.BaseSpellCondition;
import com.spelledmobs.data.SpellCondition;
import com.spelledmobs.data.SpellConditionContext;

/**
 * 天气条件
 */
public class WeatherCondition extends BaseSpellCondition {

    /**
     * 创建天气条件
     * 
     * @param operator    比较操作符
     * @param stringValue 天气类型（"clear"、"rain"、"thunder"）
     * @param invert      是否反转结果
     * @param extraData   额外数据
     */
    public WeatherCondition(ComparisonOperator operator, String stringValue, boolean invert, JsonObject extraData) {
        super(ConditionType.WEATHER, operator, stringValue, 0, invert, extraData);
    }

    @Override
    public boolean check(SpellConditionContext context) {
        if (context.getLevel() == null) {
            return false;
        }

        // 获取当前天气状态
        String currentWeather;
        if (context.getLevel().isThundering()) {
            currentWeather = "thunder";
        } else if (context.getLevel().isRaining()) {
            currentWeather = "rain";
        } else {
            currentWeather = "clear";
        }

        // 比较天气
        boolean result = getOperator().compareString(currentWeather, getStringValue());
        return applyInvert(result);
    }
}