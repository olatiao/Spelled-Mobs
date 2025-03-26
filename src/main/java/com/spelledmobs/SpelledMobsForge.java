package com.spelledmobs;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;

@Mod(SpelledMobs.MOD_ID)
public class SpelledMobsForge {
    
    public SpelledMobsForge() {
        // 创建主模组实例
        new SpelledMobs();
        
        // 记录环境信息
        if (FMLLoader.isProduction()) {
            SpelledMobs.LOGGER.info("在生产环境中初始化Spelled Mobs");
        } else {
            SpelledMobs.LOGGER.info("在开发环境中初始化Spelled Mobs");
        }
    }
} 