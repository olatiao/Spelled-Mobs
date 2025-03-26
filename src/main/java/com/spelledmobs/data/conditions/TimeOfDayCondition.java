package com.spelledmobs.data.conditions;

import com.google.gson.JsonObject;
import com.spelledmobs.data.BaseSpellCondition;
import com.spelledmobs.data.SpellCondition;
import com.spelledmobs.data.SpellConditionContext;

/**
 * 时间条件
 */
public class TimeOfDayCondition extends BaseSpellCondition {

    /**
     * 创建时间条件
     * 
     * @param operator     比较操作符
     * @param numericValue 时间值（0-24000，Minecraft一天的刻数）
     * @param invert       是否反转结果
     * @param extraData    额外数据
     */
    public TimeOfDayCondition(ComparisonOperator operator, double numericValue, boolean invert, JsonObject extraData) {
        super(ConditionType.TIME_OF_DAY, operator, null, numericValue, invert, extraData);
    }

    @Override
    public boolean check(SpellConditionContext context) {
        if (context.getLevel() == null) {
            return false;
        }

        // 获取当前世界时间
        long worldTime = context.getLevel().getDayTime() % 24000L;

        // 比较时间
        boolean result = getOperator().compareNumeric(worldTime, getNumericValue());
        return applyInvert(result);
    }
}