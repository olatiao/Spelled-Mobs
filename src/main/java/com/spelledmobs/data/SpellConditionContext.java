package com.spelledmobs.data;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * 法术条件上下文，包含条件检查所需的信息
 */
public class SpellConditionContext {
    private final LivingEntity caster;
    private final LivingEntity target;
    private final Level level;

    /**
     * 创建条件上下文
     *
     * @param caster 施法者
     * @param target 目标
     * @param level  世界
     */
    public SpellConditionContext(LivingEntity caster, LivingEntity target, Level level) {
        this.caster = caster;
        this.target = target;
        this.level = level;
    }

    /**
     * 获取施法者
     */
    public LivingEntity getCaster() {
        return caster;
    }

    /**
     * 获取目标
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
} 