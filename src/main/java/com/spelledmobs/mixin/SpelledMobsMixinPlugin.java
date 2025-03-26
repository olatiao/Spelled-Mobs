package com.spelledmobs.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Mixin插件，用于禁用Iron's Spellbooks中有问题的Mixin
 */
public class SpelledMobsMixinPlugin implements IMixinConfigPlugin {
    // Iron's Spellbooks中有问题的Mixin类名列表
    private static final Set<String> INCOMPATIBLE_MIXINS = new HashSet<>(Arrays.asList(
            "io.redspace.ironsspellbooks.mixin.MinecraftMixin", // 禁用有问题的MinecraftMixin
            "mixins.irons_spellbooks.json:MinecraftMixin" // 另一种可能的引用方式
    ));

    @Override
    public void onLoad(String mixinPackage) {
        // 加载时没有需要做的事情
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // 检查这个mixin是否在我们需要禁用的列表中
        if (INCOMPATIBLE_MIXINS.contains(mixinClassName)) {
            System.out.println("[SpelledMobs] 禁用兼容性问题的Mixin: " + mixinClassName);
            return false;
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
        // 没有需要接受的额外目标
    }

    @Override
    public List<String> getMixins() {
        // 我们没有自己的mixin要添加
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // 在应用前不需要做任何事情
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // 在应用后不需要做任何事情
    }
}