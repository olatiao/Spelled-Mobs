package com.spelledmobs.data.conditions;

import com.google.gson.JsonObject;
import com.spelledmobs.data.BaseSpellCondition;
import com.spelledmobs.data.SpellCondition;
import com.spelledmobs.data.SpellConditionContext;
import java.util.Random;

/**
 * 随机几率条件
 */
public class RandomChanceCondition extends BaseSpellCondition {
    private static final Random RANDOM = new Random();

    /**
     * 创建随机几率条件
     * 
     * @param operator     比较操作符
     * @param numericValue 几率值（0-100，表示百分比）
     * @param invert       是否反转结果
     * @param extraData    额外数据
     */
    public RandomChanceCondition(ComparisonOperator operator, double numericValue, boolean invert,
            JsonObject extraData) {
        super(ConditionType.RANDOM_CHANCE, operator, null, numericValue, invert, extraData);
    }

    @Override
    public boolean check(SpellConditionContext context) {
        // 生成0-100之间的随机数
        double randomValue = RANDOM.nextDouble() * 100.0;
        
        // 比较随机数和设定的几率
        boolean result = getOperator().compareNumeric(randomValue, getNumericValue());
        return applyInvert(result);
    }
} 