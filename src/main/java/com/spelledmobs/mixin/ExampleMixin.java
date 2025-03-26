package com.spelledmobs.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 示例 Mixin 类，仅用于测试 Mixin 系统正常工作
 * 这个 Mixin 不会对游戏进行任何修改
 */
@Mixin(LivingEntity.class)
public class ExampleMixin {
    
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        // 这个方法不做任何事情，仅用于测试 Mixin 系统
    }
} 