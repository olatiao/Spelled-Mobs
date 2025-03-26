package com.spelledmobs.data.conditions;

import com.google.gson.JsonObject;
import com.spelledmobs.data.BaseSpellCondition;
import com.spelledmobs.data.SpellCondition;
import com.spelledmobs.data.SpellConditionContext;

/**
 * 生命值百分比条件
 */
public class HealthPercentageCondition extends BaseSpellCondition {

    /**
     * 创建生命值百分比条件
     * 
     * @param operator     比较操作符
     * @param numericValue 百分比值 (0-100)
     * @param invert       是否反转结果
     * @param extraData    额外数据
     */
    public HealthPercentageCondition(ComparisonOperator operator, double numericValue, boolean invert,
            JsonObject extraData) {
        super(ConditionType.HEALTH_PERCENTAGE, operator, null, numericValue, invert, extraData);
    }

    @Override
    public boolean check(SpellConditionContext context) {
        if (context.getCaster() == null) {
            return false;
        }

        // 计算生命值百分比
        float maxHealth = context.getCaster().getMaxHealth();
        if (maxHealth <= 0) {
            return false;
        }

        float currentHealth = context.getCaster().getHealth();
        float percentage = (currentHealth / maxHealth) * 100.0f;

        // 比较百分比
        boolean result = getOperator().compareNumeric(percentage, getNumericValue());
        return applyInvert(result);
    }
}