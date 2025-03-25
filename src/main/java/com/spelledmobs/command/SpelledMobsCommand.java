package com.spelledmobs.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.command.CommandRegistryAccess;
import com.mojang.brigadier.Command;

import com.spelledmobs.SpelledMobs;
import com.spelledmobs.config.SpelledMobsConfig;
import com.spelledmobs.manager.SpellCastingManager;
import com.spelledmobs.util.SpellcastingHelper;

import java.util.List;

/**
 * 模组命令管理类
 */
public class SpelledMobsCommand {

    /**
     * 注册命令
     * 
     * @param dispatcher 命令分发器
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
            CommandRegistryAccess commandRegistryAccess,
            CommandManager.RegistrationEnvironment registrationEnvironment) {
        // 主命令
        LiteralCommandNode<ServerCommandSource> mainNode = CommandManager
                .literal("spelledmobs")
                .requires(source -> source.hasPermissionLevel(2)) // 需要权限等级2（OP）
                .executes(context -> {
                    showHelp(context);
                    return Command.SINGLE_SUCCESS;
                })
                .build();

        // 帮助子命令
        LiteralCommandNode<ServerCommandSource> helpNode = CommandManager
                .literal("help")
                .executes(context -> {
                    showHelp(context);
                    return Command.SINGLE_SUCCESS;
                })
                .build();

        // 重载子命令
        LiteralCommandNode<ServerCommandSource> reloadNode = CommandManager
                .literal("reload")
                .executes(context -> {
                    // 重新加载配置
                    SpellCastingManager.INSTANCE.reload();
                    sendSuccess(context, "已重新加载所有施法配置");
                    return Command.SINGLE_SUCCESS;
                })
                .build();

        // 检查法术ID子命令
        LiteralCommandNode<ServerCommandSource> checkNode = CommandManager
                .literal("check")
                .then(CommandManager.argument("spell_id", StringArgumentType.string())
                        .executes(context -> {
                            String spellId = StringArgumentType.getString(context, "spell_id");
                            boolean isValid = SpellcastingHelper.isValidSpell(spellId);
                            if (isValid) {
                                sendSuccess(context, String.format("法术ID '%s' 有效", spellId));
                            } else {
                                sendError(context, String.format("法术ID '%s' 无效", spellId));
                            }
                            return Command.SINGLE_SUCCESS;
                        }))
                .build();

        // 强制施法子命令
        LiteralCommandNode<ServerCommandSource> forcecastNode = CommandManager
                .literal("forcecast")
                .then(CommandManager.argument("entity", EntityArgumentType.entity())
                        .then(CommandManager.argument("spell_id", StringArgumentType.string())
                                .then(CommandManager.argument("level", IntegerArgumentType.integer(1, 10))
                                        .executes(context -> {
                                            return executeForcecastCommand(context, null);
                                        })
                                        .then(CommandManager.argument("target", EntityArgumentType.entity())
                                                .executes(context -> {
                                                    Entity targetEntity = EntityArgumentType.getEntity(context,
                                                            "target");
                                                    return executeForcecastCommand(context, targetEntity);
                                                })))))
                .build();

        // 施法子命令
        LiteralCommandNode<ServerCommandSource> castNode = CommandManager
                .literal("cast")
                .then(CommandManager.argument("entity", EntityArgumentType.entity())
                        .then(CommandManager.argument("spell_id", StringArgumentType.string())
                                .then(CommandManager.argument("level", IntegerArgumentType.integer(1, 10))
                                        .executes(context -> {
                                            return executeCastCommand(context, null);
                                        })
                                        .then(CommandManager.argument("target", EntityArgumentType.entity())
                                                .executes(context -> {
                                                    Entity targetEntity = EntityArgumentType.getEntity(context,
                                                            "target");
                                                    return executeCastCommand(context, targetEntity);
                                                })))))
                .build();

        // 配置子命令
        LiteralCommandNode<ServerCommandSource> configNode = CommandManager
                .literal("config")
                .then(CommandManager.literal("debugLogging")
                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                                .executes(context -> {
                                    boolean value = BoolArgumentType.getBool(context, "value");
                                    SpelledMobsConfig.setDebugLogging(value);
                                    sendSuccess(context, String.format("调试日志已%s", value ? "启用" : "禁用"));
                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(CommandManager.literal("showEffects")
                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                                .executes(context -> {
                                    boolean value = BoolArgumentType.getBool(context, "value");
                                    SpelledMobsConfig.setShowCastingEffects(value);
                                    sendSuccess(context, String.format("施法特效已%s", value ? "启用" : "禁用"));
                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(CommandManager.literal("maxCheckDistance")
                        .then(CommandManager.argument("value", IntegerArgumentType.integer(1, 256))
                                .executes(context -> {
                                    int value = IntegerArgumentType.getInteger(context, "value");
                                    SpelledMobsConfig.setMaxCheckDistance(value);
                                    sendSuccess(context, String.format("最大检查距离已设置为 %d", value));
                                    return Command.SINGLE_SUCCESS;
                                })))
                .build();

        // 添加一个列出可用法术的命令
        LiteralCommandNode<ServerCommandSource> listSpellsNode = CommandManager
                .literal("listspells")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(context -> {
                    sendInfo(context, "用法: /spelledmobs listspells <实体>");
                    return Command.SINGLE_SUCCESS;
                })
                .then(CommandManager.argument("entity", EntityArgumentType.entity())
                        .executes(context -> {
                            return executeListSpellsCommand(context);
                        }))
                .build();

        // 添加所有子命令到主命令
        mainNode.addChild(helpNode);
        mainNode.addChild(reloadNode);
        mainNode.addChild(checkNode);
        mainNode.addChild(castNode);
        mainNode.addChild(forcecastNode);
        mainNode.addChild(configNode);
        mainNode.addChild(listSpellsNode);

        // 原生格式测试命令（直接使用Iron's Spells的命令格式）
        LiteralCommandNode<ServerCommandSource> nativeTestNode = CommandManager
                .literal("nativetest")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(context -> {
                    sendInfo(context,
                            "这个命令用于直接测试Iron's Spells的原生命令格式，语法：/spelledmobs nativetest <实体> <法术ID> <等级> [目标]");
                    return Command.SINGLE_SUCCESS;
                })
                .then(CommandManager.argument("entity", EntityArgumentType.entity())
                        .then(CommandManager.argument("spell_id", StringArgumentType.string())
                                .then(CommandManager.argument("level", IntegerArgumentType.integer(1, 10))
                                        .executes(context -> {
                                            return executeNativeTestCommand(context, null);
                                        })
                                        .then(CommandManager.argument("target", EntityArgumentType.entity())
                                                .executes(context -> {
                                                    Entity targetEntity = EntityArgumentType.getEntity(context,
                                                            "target");
                                                    return executeNativeTestCommand(context, targetEntity);
                                                })))))
                .build();

        mainNode.addChild(nativeTestNode);

        // 注册命令
        dispatcher.getRoot().addChild(mainNode);
    }

    /**
     * 执行施放法术命令
     * 
     * @param context      命令上下文
     * @param targetEntity 目标实体，可为null
     * @return 命令执行结果
     */
    private static int executeCastCommand(CommandContext<ServerCommandSource> context, Entity targetEntity) {
        try {
            // 获取命令参数
            Entity entity = EntityArgumentType.getEntity(context, "entity");
            String spellId = StringArgumentType.getString(context, "spell_id");
            int level = IntegerArgumentType.getInteger(context, "level");

            if (!(entity instanceof LivingEntity livingEntity)) {
                sendError(context, "指定的实体不是生物实体");
                return 0;
            }

            LivingEntity targetLiving = null;
            if (targetEntity != null && targetEntity instanceof LivingEntity) {
                targetLiving = (LivingEntity) targetEntity;
            }

            boolean success = SpellcastingHelper.castSpell(livingEntity, targetLiving, spellId, level);

            if (success) {
                sendSuccess(context, String.format("成功让实体 %s 施放法术 %s (等级 %d)",
                        entity.getEntityName(), spellId, level));
                return Command.SINGLE_SUCCESS;
            } else {
                sendError(context, String.format("让实体 %s 施放法术 %s 失败",
                        entity.getEntityName(), spellId));
                return 0;
            }
        } catch (Exception e) {
            sendError(context, "执行施法命令时出错: " + e.getMessage());
            SpelledMobs.LOGGER.error("施法命令执行错误", e);
            return 0;
        }
    }

    /**
     * 显示帮助信息
     * 
     * @param context 命令上下文
     */
    private static void showHelp(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        source.sendFeedback(() -> Text.literal("=== Spelled Mobs 命令帮助 ===").formatted(Formatting.GOLD), false);
        source.sendFeedback(() -> Text.literal("/spelledmobs help - 显示此帮助信息").formatted(Formatting.YELLOW), false);
        source.sendFeedback(() -> Text.literal("/spelledmobs reload - 重新加载所有施法配置").formatted(Formatting.YELLOW), false);
        source.sendFeedback(
                () -> Text.literal("/spelledmobs check <spell_id> - 检查法术ID是否有效").formatted(Formatting.YELLOW), false);
        source.sendFeedback(() -> Text.literal("/spelledmobs cast <entity> <spell_id> <level> [<target>] - 让实体施放法术")
                .formatted(Formatting.YELLOW), false);
        source.sendFeedback(
                () -> Text.literal("/spelledmobs forcecast <entity> <spell_id> <level> [<target>] - 强制实体施放法术（调试用）")
                        .formatted(Formatting.YELLOW),
                false);
        source.sendFeedback(() -> Text
                .literal("/spelledmobs nativetest <entity> <spell_id> <level> [<target>] - 使用Iron's Spells原生命令格式测试")
                .formatted(Formatting.YELLOW), false);
        source.sendFeedback(() -> Text.literal("/spelledmobs config debugLogging <true|false> - 设置调试日志")
                .formatted(Formatting.YELLOW), false);
        source.sendFeedback(() -> Text.literal("/spelledmobs config showEffects <true|false> - 设置施法特效")
                .formatted(Formatting.YELLOW), false);
        source.sendFeedback(() -> Text.literal("/spelledmobs config maxCheckDistance <value> - 设置最大检查距离")
                .formatted(Formatting.YELLOW), false);
        source.sendFeedback(
                () -> Text.literal("/spelledmobs listspells <entity> - 列出实体可用法术").formatted(Formatting.YELLOW), false);

        source.sendFeedback(
                () -> Text.literal("提示：直接使用完整法术ID，如'firebolt'或'irons_spellbooks:firebolt'").formatted(Formatting.AQUA),
                false);
        source.sendFeedback(() -> Text.literal("原生命令格式为：/cast <实体> <法术ID> <等级>").formatted(Formatting.AQUA), false);
    }

    /**
     * 发送成功消息
     * 
     * @param context 命令上下文
     * @param message 消息内容
     */
    private static void sendSuccess(CommandContext<ServerCommandSource> context, String message) {
        if (SpelledMobsConfig.isCommandFeedbackEnabled()) {
            context.getSource().sendFeedback(() -> Text.literal("[SpelledMobs] " + message).formatted(Formatting.GREEN),
                    false);
        }
    }

    /**
     * 发送错误消息
     * 
     * @param context 命令上下文
     * @param message 消息内容
     */
    private static void sendError(CommandContext<ServerCommandSource> context, String message) {
        context.getSource().sendError(
                Text.literal("[SpelledMobs] " + message).formatted(Formatting.RED));
    }

    /**
     * 执行强制施法命令
     * 
     * @param context      命令上下文
     * @param targetEntity 目标实体
     * @return 命令执行结果
     */
    private static int executeForcecastCommand(CommandContext<ServerCommandSource> context, Entity targetEntity) {
        try {
            // 获取命令参数
            Entity entity = EntityArgumentType.getEntity(context, "entity");
            String spellId = StringArgumentType.getString(context, "spell_id");
            int level = IntegerArgumentType.getInteger(context, "level");

            if (!(entity instanceof LivingEntity livingEntity)) {
                sendError(context, "指定的实体不是生物实体");
                return 0;
            }

            LivingEntity targetLiving = null;
            if (targetEntity != null && targetEntity instanceof LivingEntity) {
                targetLiving = (LivingEntity) targetEntity;
            }

            // 记录详细执行信息
            ServerCommandSource source = context.getSource();
            source.sendFeedback(
                    () -> Text.literal(String.format("[SpelledMobs] 开始强制施法尝试 - 法术: %s, 等级: %d", spellId, level))
                            .formatted(Formatting.YELLOW),
                    true);

            // 执行强制施法，绕过所有条件检查
            boolean success = SpellCastingManager.INSTANCE.forceCastSpell(livingEntity, spellId, level, targetLiving);

            if (success) {
                sendSuccess(context, String.format("成功让实体 %s 强制施放法术 %s (等级 %d)",
                        entity.getEntityName(), spellId, level));
                return Command.SINGLE_SUCCESS;
            } else {
                sendError(context, String.format("让实体 %s 施放法术 %s 失败",
                        entity.getEntityName(), spellId));
                return 0;
            }
        } catch (Exception e) {
            sendError(context, "执行命令时出错: " + e.getMessage());
            SpelledMobs.LOGGER.error("强制施法命令执行错误", e);
            return 0;
        }
    }

    /**
     * 发送信息消息（黄色）
     * 
     * @param context 命令上下文
     * @param message 消息内容
     */
    private static void sendInfo(CommandContext<ServerCommandSource> context, String message) {
        context.getSource().sendFeedback(() -> Text.literal("[SpelledMobs] " + message).formatted(Formatting.YELLOW),
                false);
    }

    /**
     * 执行原生格式测试命令
     * 
     * @param context      命令上下文
     * @param targetEntity 目标实体
     * @return 命令执行结果
     */
    private static int executeNativeTestCommand(CommandContext<ServerCommandSource> context, Entity targetEntity) {
        try {
            // 获取命令参数
            Entity entity = EntityArgumentType.getEntity(context, "entity");
            String spellId = StringArgumentType.getString(context, "spell_id");
            int level = IntegerArgumentType.getInteger(context, "level");

            if (!(entity instanceof LivingEntity)) {
                sendError(context, "指定的实体不是生物实体");
                return 0;
            }

            // 准备命令格式
            String nativeCmd = "";
            if (targetEntity != null) {
                nativeCmd = String.format("cast %s %s %d %s",
                        entity.getUuidAsString(), spellId, level, targetEntity.getUuidAsString());
            } else {
                nativeCmd = String.format("cast %s %s %d",
                        entity.getUuidAsString(), spellId, level);
            }

            // 记录和显示命令
            SpelledMobs.LOGGER.info("尝试使用原生命令格式: /{}", nativeCmd);
            sendInfo(context, "正在执行: /" + nativeCmd);

            // 执行命令
            int result = context.getSource().getServer().getCommandManager()
                    .executeWithPrefix(context.getSource(), nativeCmd);

            if (result > 0) {
                sendSuccess(context, String.format(
                        "原生命令执行成功，结果码: %d", result));
                return Command.SINGLE_SUCCESS;
            } else {
                sendError(context, String.format(
                        "原生命令执行失败，结果码: %d", result));
                return 0;
            }
        } catch (Exception e) {
            sendError(context, "执行原生测试命令时出错: " + e.getMessage());
            SpelledMobs.LOGGER.error("原生测试命令执行错误", e);
            return 0;
        }
    }

    /**
     * 执行列出实体可用法术命令
     * 
     * @param context 命令上下文
     * @return 命令执行结果
     */
    private static int executeListSpellsCommand(CommandContext<ServerCommandSource> context) {
        try {
            // 获取命令参数
            Entity entity = EntityArgumentType.getEntity(context, "entity");

            if (!(entity instanceof LivingEntity livingEntity)) {
                sendError(context, "指定的实体不是生物实体");
                return 0;
            }

            // 获取实体ID
            String entityId = entity.getType().toString();
            sendInfo(context, String.format("实体类型: %s，ID: %s", entity.getEntityName(), entityId));

            // 获取实体配置的法术
            List<String> spells = SpellCastingManager.INSTANCE.getEntitySpells(livingEntity);

            if (spells.isEmpty()) {
                sendInfo(context, "该实体没有配置法术");
            } else {
                sendInfo(context, String.format("找到 %d 个配置的法术:", spells.size()));
                for (int i = 0; i < spells.size(); i++) {
                    final int index = i;
                    context.getSource().sendFeedback(
                            () -> Text.literal(String.format("  • %s", spells.get(index))).formatted(Formatting.GREEN),
                            false);
                }
            }

            // 尝试获取Iron's Spells的可用法术
            sendInfo(context, "正在尝试列出Iron's Spells模组的可用法术...");
            List<String> availableSpells = SpellcastingHelper.getAvailableSpells();

            if (availableSpells.isEmpty()) {
                sendInfo(context, "无法获取Iron's Spells的法术列表");
            } else {
                sendInfo(context, String.format("找到 %d 个可用法术 (只显示前10个):", availableSpells.size()));
                int displayCount = Math.min(10, availableSpells.size());
                for (int i = 0; i < displayCount; i++) {
                    final int index = i;
                    context.getSource().sendFeedback(
                            () -> Text.literal(String.format("  • %s", availableSpells.get(index)))
                                    .formatted(Formatting.AQUA),
                            false);
                }
                if (availableSpells.size() > 10) {
                    sendInfo(context, String.format("...以及其他 %d 个法术", availableSpells.size() - 10));
                }
            }

            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            sendError(context, "执行列出法术命令时出错: " + e.getMessage());
            SpelledMobs.LOGGER.error("列出法术命令执行错误", e);
            return 0;
        }
    }
}