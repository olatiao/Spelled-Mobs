package com.spelledmobs.data;

import java.util.ArrayList;
import java.util.List;

/**
 * 法术条目，包含法术的基本信息
 */
public class SpellEntry {
    private final String spellId;
    private final int minLevel;
    private final int maxLevel;
    private final int minCastTime;
    private final int maxCastTime;
    private final int weight;
    private final float chance;
    private final List<SpellCondition> conditions = new ArrayList<>();

    /**
     * 创建法术条目
     * 
     * @param spellId     法术ID
     * @param minLevel    最小施法等级
     * @param maxLevel    最大施法等级
     * @param minCastTime 最小施法冷却时间
     * @param maxCastTime 最大施法冷却时间
     * @param weight      权重
     * @param chance      施放几率
     */
    public SpellEntry(String spellId, int minLevel, int maxLevel, int minCastTime, int maxCastTime, int weight,
            float chance) {
        this.spellId = spellId;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.minCastTime = minCastTime;
        this.maxCastTime = maxCastTime;
        this.weight = weight;
        this.chance = chance;
    }

    /**
     * 获取法术ID
     */
    public String getSpellId() {
        return spellId;
    }

    /**
     * 获取最小施法等级
     */
    public int getMinLevel() {
        return minLevel;
    }

    /**
     * 获取最大施法等级
     */
    public int getMaxLevel() {
        return maxLevel;
    }

    /**
     * 获取最小施法冷却时间
     */
    public int getMinCastTime() {
        return minCastTime;
    }

    /**
     * 获取最大施法冷却时间
     */
    public int getMaxCastTime() {
        return maxCastTime;
    }

    /**
     * 获取权重
     */
    public int getWeight() {
        return weight;
    }

    /**
     * 获取施放几率
     */
    public float getChance() {
        return chance;
    }

    /**
     * 添加施法条件
     * 
     * @param condition 条件
     */
    public void addCondition(SpellCondition condition) {
        if (condition != null) {
            conditions.add(condition);
        }
    }

    /**
     * 获取所有施法条件
     */
    public List<SpellCondition> getConditions() {
        return conditions;
    }

    /**
     * 判断是否满足所有施法条件
     * 
     * @param context 条件上下文
     * @return 是否满足条件
     */
    public boolean checkConditions(SpellConditionContext context) {
        if (conditions.isEmpty()) {
            return true;
        }

        for (SpellCondition condition : conditions) {
            boolean result = condition.check(context);
            if (!result) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return "SpellEntry{" +
                "spellId='" + spellId + '\'' +
                ", minLevel=" + minLevel +
                ", maxLevel=" + maxLevel +
                ", minCastTime=" + minCastTime +
                ", maxCastTime=" + maxCastTime +
                ", conditions=" + conditions.size() +
                ", weight=" + weight +
                ", chance=" + chance +
                '}';
    }
}