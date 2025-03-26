package com.spelledmobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.spelledmobs.compatibility.IronsSpellsCompat;
import com.spelledmobs.config.SpelledMobsConfig;
import com.spelledmobs.data.SpellCastingData;
import com.spelledmobs.manager.SpellCastingManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * 主模组类
 */
@Mod(SpelledMobs.MOD_ID)
public class SpelledMobs {
    public static final String MOD_ID = "spelledmobs";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    // 是否已加载Iron's Spells 'n Spellbooks模组
    private static boolean ironsSpellsLoaded = false;
    // 是否成功初始化了Iron's Spells 'n Spellbooks兼容性
    private static boolean ironsSpellsCompatInitialized = false;

    private static SpelledMobs instance;
    private SpellCastingData spellCastingData;
    private SpellCastingManager spellCastingManager;

    public SpelledMobs() {
        instance = this;
        LOGGER.info("Spelled Mobs 正在初始化...");

        // 检查Iron's Spells 'n Spellbooks是否已加载
        ironsSpellsLoaded = ModList.get().isLoaded("irons_spellbooks");
        if (!ironsSpellsLoaded) {
            LOGGER.warn("未检测到Iron's Spells 'n Spellbooks模组，部分功能将不可用");
        } else {
            LOGGER.info("已检测到Iron's Spells 'n Spellbooks模组，尝试启用所有功能");
        }

        // 注册Forge事件总线
        @SuppressWarnings("removal")
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::setup);

        // 注册Forge通用事件
        MinecraftForge.EVENT_BUS.register(this);
        
        // 读取配置
        SpelledMobsConfig.setDebugLogging(true); // 默认启用调试日志
        SpelledMobsConfig.setCommandFeedback(true); // 默认启用命令反馈
        SpelledMobsConfig.setMaxCheckDistance(64); // 默认检测距离

        // 初始化数据
        spellCastingData = new SpellCastingData();
        spellCastingManager = new SpellCastingManager(spellCastingData);
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
     * 检查Iron's Spells 'n Spellbooks是否已加载
     */
    public static boolean isIronsSpellsLoaded() {
        return ironsSpellsLoaded;
    }
    
    /**
     * 检查Iron's Spells 'n Spellbooks兼容性是否成功初始化
     */
    public static boolean isIronsSpellsCompatInitialized() {
        return ironsSpellsCompatInitialized;
    }

    /**
     * 通用设置，加载兼容性和配置
     */
    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("Spelled Mobs 正在完成设置...");

        // 初始化法术模组兼容性
        if (ironsSpellsLoaded) {
            event.enqueueWork(() -> {
                try {
                    LOGGER.info("正在初始化 Iron's Spells 'n Spellbooks 兼容性...");
                    IronsSpellsCompat.init();
                    ironsSpellsCompatInitialized = true;
                    LOGGER.info("Iron's Spells 'n Spellbooks 兼容性初始化成功！");
                } catch (Exception e) {
                    LOGGER.error("初始化 Iron's Spells 'n Spellbooks 兼容性时发生错误", e);
                    LOGGER.warn("Iron's Spells 'n Spellbooks 兼容性初始化失败，某些功能将不可用");
                    ironsSpellsCompatInitialized = false;
                }
            });
        }
    }

    /**
     * 服务器启动时加载数据包和配置
     */
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        LOGGER.info("正在加载实体法术配置...");
        
        try {
            // 加载法术数据
            spellCastingData.loadEntitySpells("data/" + MOD_ID + "/entity_spells");
            LOGGER.info("实体法术配置加载成功！");
        } catch (Exception e) {
            LOGGER.error("加载实体法术配置时发生错误", e);
        }
    }

    /**
     * 服务器tick事件，处理实体法术施放
     */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        // 如果Iron's Spells未加载或兼容性初始化失败，跳过处理
        if (!ironsSpellsLoaded || !ironsSpellsCompatInitialized) {
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
                    LOGGER.error("处理实体法术施放时发生错误", e);
                }
            }
        }
    }

    /**
     * 重新加载所有配置和数据
     */
    public void reloadData() {
        LOGGER.info("正在重新加载实体法术配置...");
        try {
            spellCastingData.loadEntitySpells("data/" + MOD_ID + "/entity_spells");
            LOGGER.info("实体法术配置重新加载成功！");
        } catch (Exception e) {
            LOGGER.error("重新加载实体法术配置时发生错误", e);
        }
    }
}
