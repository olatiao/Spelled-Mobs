package com.spelledmobs.data.conditions;

import com.google.gson.JsonObject;
import com.spelledmobs.data.BaseSpellCondition;
import com.spelledmobs.data.SpellCondition;
import com.spelledmobs.data.SpellConditionContext;

/**
 * 目标距离条件
 */
public class TargetDistanceCondition extends BaseSpellCondition {

    /**
     * 创建目标距离条件
     * 
     * @param operator     比较操作符
     * @param numericValue 距离值（方块数）
     * @param invert       是否反转结果
     * @param extraData    额外数据
     */
    public TargetDistanceCondition(ComparisonOperator operator, double numericValue, boolean invert,
            JsonObject extraData) {
        super(ConditionType.TARGET_DISTANCE, operator, null, numericValue, invert, extraData);
    }

    @Override
    public boolean check(SpellConditionContext context) {
        if (context.getCaster() == null || context.getTarget() == null) {
            return false;
        }

        // 计算与目标的距离
        double distance = context.getCaster().distanceTo(context.getTarget());

        // 比较距离
        boolean result = getOperator().compareNumeric(distance, getNumericValue());
        return applyInvert(result);
    }
}