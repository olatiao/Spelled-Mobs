package com.spelledmobs.data.conditions;

import com.google.gson.JsonObject;
import com.spelledmobs.data.BaseSpellCondition;
import com.spelledmobs.data.SpellCondition;
import com.spelledmobs.data.SpellConditionContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

/**
 * 目标类型条件
 */
public class TargetTypeCondition extends BaseSpellCondition {

    /**
     * 创建目标类型条件
     * 
     * @param operator    比较操作符
     * @param stringValue 实体类型ID，例如 "minecraft:zombie"
     * @param invert      是否反转结果
     * @param extraData   额外数据
     */
    public TargetTypeCondition(ComparisonOperator operator, String stringValue, boolean invert, JsonObject extraData) {
        super(ConditionType.TARGET_TYPE, operator, stringValue, 0, invert, extraData);
    }

    @Override
    public boolean check(SpellConditionContext context) {
        if (context.getTarget() == null) {
            return false;
        }

        // 获取目标实体的类型ID
        ResourceLocation targetTypeId = EntityType.getKey(context.getTarget().getType());
        String targetTypeString = targetTypeId.toString();

        // 比较类型
        boolean result = getOperator().compareString(targetTypeString, getStringValue());
        return applyInvert(result);
    }
}