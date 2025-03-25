package com.spelledmobs.config;

import com.spelledmobs.SpelledMobs;

/**
 * 模组配置类
 */
public class SpelledMobsConfig {
    // 是否启用调试日志
    private static boolean debugLogging = false;
    
    // 是否显示施法特效
    private static boolean showCastingEffects = true;
    
    // 是否启用命令反馈
    private static boolean enableCommandFeedback = true;
    
    // 最大检查距离
    private static int maxCheckDistance = 128;
    
    /**
     * 初始化默认配置
     */
    public static void init() {
        // TODO: 实现配置文件加载
        SpelledMobs.LOGGER.info("加载配置...");
    }
    
    /**
     * 检查是否启用调试日志
     * @return 是否启用调试日志
     */
    public static boolean isDebugLoggingEnabled() {
        return debugLogging;
    }
    
    /**
     * 设置是否启用调试日志
     * @param enabled 是否启用
     */
    public static void setDebugLogging(boolean enabled) {
        debugLogging = enabled;
    }
    
    /**
     * 检查是否显示施法特效
     * @return 是否显示施法特效
     */
    public static boolean shouldShowCastingEffects() {
        return showCastingEffects;
    }
    
    /**
     * 设置是否显示施法特效
     * @param show 是否显示
     */
    public static void setShowCastingEffects(boolean show) {
        showCastingEffects = show;
    }
    
    /**
     * 检查是否启用命令反馈
     * @return 是否启用命令反馈
     */
    public static boolean isCommandFeedbackEnabled() {
        return enableCommandFeedback;
    }
    
    /**
     * 设置是否启用命令反馈
     * @param enabled 是否启用
     */
    public static void setCommandFeedback(boolean enabled) {
        enableCommandFeedback = enabled;
    }
    
    /**
     * 获取最大检查距离
     * @return 最大检查距离
     */
    public static int getMaxCheckDistance() {
        return maxCheckDistance;
    }
    
    /**
     * 设置最大检查距离
     * @param distance 距离
     */
    public static void setMaxCheckDistance(int distance) {
        if (distance > 0) {
            maxCheckDistance = distance;
        }
    }
} 