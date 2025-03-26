package com.spelledmobs.config;

import com.spelledmobs.SpelledMobs;

/**
 * 模组配置类
 */
public class SpelledMobsConfig {
    // 是否启用调试日志
    private static boolean debugLogging = false;

    // 是否启用命令反馈
    private static boolean commandFeedback = true;

    // 是否显示施法特效
    private static boolean showCastingEffects = true;

    // 最大检查距离
    private static int maxCheckDistance = 64;

    /**
     * 获取是否启用调试日志
     */
    public static boolean isDebugLoggingEnabled() {
        return debugLogging;
    }

    /**
     * 设置是否启用调试日志
     *
     * @param enabled 是否启用
     */
    public static void setDebugLogging(boolean enabled) {
        debugLogging = enabled;
        SpelledMobs.LOGGER.info("调试日志已{}", enabled ? "启用" : "禁用");
    }

    /**
     * 获取是否启用命令反馈
     */
    public static boolean isCommandFeedbackEnabled() {
        return commandFeedback;
    }

    /**
     * 设置是否启用命令反馈
     *
     * @param enabled 是否启用
     */
    public static void setCommandFeedback(boolean enabled) {
        commandFeedback = enabled;
        SpelledMobs.LOGGER.info("命令反馈已{}", enabled ? "启用" : "禁用");
    }

    /**
     * 获取是否显示施法特效
     */
    public static boolean showCastingEffects() {
        return showCastingEffects;
    }

    /**
     * 设置是否显示施法特效
     *
     * @param show 是否显示
     */
    public static void setShowCastingEffects(boolean show) {
        showCastingEffects = show;
    }

    /**
     * 获取最大检查距离
     */
    public static int getMaxCheckDistance() {
        return maxCheckDistance;
    }

    /**
     * 设置最大检查距离
     * 
     * @param distance 距离，1-256之间
     */
    public static void setMaxCheckDistance(int distance) {
        if (distance < 1 || distance > 256) {
            SpelledMobs.LOGGER.warn("尝试设置无效的检查距离: {}，有效范围为1-256", distance);
            return;
        }
        maxCheckDistance = distance;
        SpelledMobs.LOGGER.info("最大检查距离已设置为: {}", distance);
    }
}