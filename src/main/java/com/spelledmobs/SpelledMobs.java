package com.spelledmobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.spelledmobs.compatibility.IronsSpellsCompat;
import com.spelledmobs.config.SpelledMobsConfig;
import com.spelledmobs.data.SpellCastingData;
import com.spelledmobs.manager.SpellCastingManager;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Files;

/**
 * 主模组类
 */
@Mod(SpelledMobs.MOD_ID)
public class SpelledMobs {
    public static final String MOD_ID = "spelledmobs";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    // 定义日志前缀常量，用于统一所有日志格式
    public static final String LOG_PREFIX = "[SpelledMobs] ";

    private static SpelledMobs instance;
    private SpellCastingData spellCastingData;
    private SpellCastingManager spellCastingManager;

    public SpelledMobs() {
        instance = this;
        LOGGER.info("{}Spelled Mobs 正在初始化...", LOG_PREFIX);

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::setup);

        // 注册Forge总线事件处理器
        MinecraftForge.EVENT_BUS.register(this);

        // 初始化数据
        spellCastingData = new SpellCastingData();
        spellCastingManager = new SpellCastingManager(spellCastingData);

        // 立即创建配置目录，不等待服务器启动
        createConfigDirectories();

        LOGGER.info("{}SpelledMobs模组已加载，调试日志{}启用。可以给生物赋予法术能力啦！",
                LOG_PREFIX, SpelledMobsConfig.isDebugLoggingEnabled() ? "已" : "未");

        // 初始化兼容层
        IronsSpellsCompat.init();
    }

    /**
     * 获取模组实例
     */
    public static SpelledMobs getInstance() {
        return instance;
    }

    /**
     * 获取法术施放管理器
     */
    public SpellCastingManager getSpellCastingManager() {
        return spellCastingManager;
    }

    /**
     * 获取法术数据
     */
    public SpellCastingData getSpellCastingData() {
        return spellCastingData;
    }

    /**
     * 通用设置，加载兼容性和配置
     */
    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("{}Spelled Mobs 正在完成设置...", LOG_PREFIX);

        // 在这个阶段再次尝试创建配置目录
        event.enqueueWork(() -> {
            createConfigDirectories();
        });
    }

    /**
     * 服务器启动时加载数据包和配置
     */
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        LOGGER.info("{}服务器启动事件已触发，开始加载实体法术配置...", LOG_PREFIX);

        try {
            // 确保配置目录存在
            Path configDir = FMLPaths.CONFIGDIR.get();
            Path spelledMobsConfigDir = configDir.resolve("spelledmobs");
            Path entitySpellsDir = spelledMobsConfigDir.resolve("entity_spells");
            
            // 检查目录是否存在，并尝试创建
            if (!Files.exists(spelledMobsConfigDir)) {
                Files.createDirectories(spelledMobsConfigDir);
                LOGGER.info("{}模组配置目录创建成功", LOG_PREFIX);
            }
            
            if (!Files.exists(entitySpellsDir)) {
                Files.createDirectories(entitySpellsDir);
                LOGGER.info("{}法术配置目录创建成功", LOG_PREFIX);
            }

            // 加载法术数据
            spellCastingData.loadEntitySpells("data/" + MOD_ID + "/entity_spells");
            LOGGER.info("{}实体法术配置加载成功！", LOG_PREFIX);
        } catch (Exception e) {
            LOGGER.error("{}加载实体法术配置时发生错误", LOG_PREFIX, e);
        }
    }

    /**
     * 服务器tick事件，处理实体法术施放
     */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        // 如果Iron's Spells未加载或未初始化，跳过处理
        if (!IronsSpellsCompat.isIronsSpellsLoaded() || !IronsSpellsCompat.isInitialized()) {
            return;
        }

        if (event.phase == TickEvent.Phase.END) {
            try {
                // 处理所有已加载世界中的实体法术施放
                event.getServer().getAllLevels().forEach(level -> {
                    if (level instanceof ServerLevel) {
                        ServerLevel serverLevel = (ServerLevel) level;
                        spellCastingManager.onServerTick(serverLevel);
                    }
                });
            } catch (Exception e) {
                if (SpelledMobsConfig.isDebugLoggingEnabled()) {
                    LOGGER.error("{}处理实体法术施放时发生错误", LOG_PREFIX, e);
                }
            }
        }
    }

    /**
     * 重新加载所有配置和数据
     */
    public void reloadData() {
        LOGGER.info("{}正在重新加载实体法术配置...", LOG_PREFIX);
        try {
            spellCastingData.loadEntitySpells("data/" + MOD_ID + "/entity_spells");
            LOGGER.info("{}实体法术配置重新加载成功！", LOG_PREFIX);
        } catch (Exception e) {
            LOGGER.error("{}重新加载实体法术配置时发生错误", LOG_PREFIX, e);
        }
    }

    /**
     * 注册命令
     */
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("{}注册SpelledMobs命令...", LOG_PREFIX);

        event.getDispatcher().register(
                Commands.literal("spelledmobs")
                        .then(Commands.literal("debug")
                                .then(Commands.literal("enable")
                                        .executes(context -> {
                                            SpelledMobsConfig.setDebugLogging(true);
                                            context.getSource().sendSuccess(() -> Component.literal("调试日志已启用"), true);
                                            return 1;
                                        }))
                                .then(Commands.literal("disable")
                                        .executes(context -> {
                                            SpelledMobsConfig.setDebugLogging(false);
                                            context.getSource().sendSuccess(() -> Component.literal("调试日志已禁用"), true);
                                            return 1;
                                        })))
                        .then(Commands.literal("reload")
                                .executes(context -> {
                                    reloadData();
                                    context.getSource().sendSuccess(() -> Component.literal("SpelledMobs配置已重新加载"), true);
                                    return 1;
                                }))
                        .then(Commands.literal("cast")
                                .then(Commands.argument("target", EntityArgument.entity())
                                        .then(Commands.argument("spellid", StringArgumentType.string())
                                                .then(Commands.argument("level", IntegerArgumentType.integer(1, 10))
                                                        .executes(context -> {
                                                            Entity targetEntity = EntityArgument.getEntity(context,
                                                                    "target");
                                                            if (!(targetEntity instanceof LivingEntity target)) {
                                                                context.getSource()
                                                                        .sendFailure(Component.literal("目标必须是生物"));
                                                                return 0;
                                                            }

                                                            Entity sourceEntity = context.getSource().getEntity();
                                                            if (!(sourceEntity instanceof LivingEntity source)) {
                                                                context.getSource()
                                                                        .sendFailure(Component.literal("只有生物可以施法"));
                                                                return 0;
                                                            }

                                                            String spellId = StringArgumentType.getString(context,
                                                                    "spellid");
                                                            int level = IntegerArgumentType.getInteger(context,
                                                                    "level");

                                                            boolean success = spellCastingManager.forceCastSpell(source,
                                                                    target, spellId, level);

                                                            if (success) {
                                                                context.getSource()
                                                                        .sendSuccess(
                                                                                () -> Component.literal(
                                                                                        "成功施放法术 " + spellId + " (等级 "
                                                                                                + level + ") 目标: "
                                                                                                + target.getName()
                                                                                                        .getString()),
                                                                                true);
                                                            } else {
                                                                context.getSource().sendFailure(
                                                                        Component.literal(
                                                                                "施放法术 " + spellId + " 失败，请检查法术ID是否正确"));
                                                            }

                                                            return success ? 1 : 0;
                                                        }))))));

        LOGGER.info("{}SpelledMobs命令注册完成", LOG_PREFIX);
    }

    /**
     * 创建配置目录
     */
    private void createConfigDirectories() {
        LOGGER.info("{}创建配置目录...", LOG_PREFIX);
        
        try {
            // 确保配置目录存在
            Path configDir = FMLPaths.CONFIGDIR.get();
            
            Path spelledMobsConfigDir = configDir.resolve("spelledmobs");
            Path entitySpellsDir = spelledMobsConfigDir.resolve("entity_spells");
            
            // 检查目录是否存在，并尝试创建
            if (!Files.exists(spelledMobsConfigDir)) {
                Files.createDirectories(spelledMobsConfigDir);
                LOGGER.info("{}模组配置目录创建成功", LOG_PREFIX);
            }
            
            if (!Files.exists(entitySpellsDir)) {
                Files.createDirectories(entitySpellsDir);
                LOGGER.info("{}法术配置目录创建成功", LOG_PREFIX);
                
                // 创建默认配置文件
                spellCastingData.createDefaultConfigFiles(entitySpellsDir.toFile());
                LOGGER.info("{}默认配置文件创建成功", LOG_PREFIX);
            }
            
            // 在模组加载阶段预先加载一次配置
            spellCastingData.loadEntitySpells("data/" + MOD_ID + "/entity_spells");
            
        } catch (Exception e) {
            LOGGER.error("{}创建配置目录时发生错误", LOG_PREFIX, e);
        }
    }
}
