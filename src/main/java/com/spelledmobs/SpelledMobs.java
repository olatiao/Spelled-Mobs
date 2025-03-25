package com.spelledmobs;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.spelledmobs.command.SpelledMobsCommand;
import com.spelledmobs.config.SpelledMobsConfig;
import com.spelledmobs.data.SpellCastingData;
import com.spelledmobs.manager.SpellCastingManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import net.fabricmc.loader.api.FabricLoader;
import com.spelledmobs.util.SpellcastingHelper;

public class SpelledMobs implements ModInitializer {
	public static final String MOD_ID = "spelledmobs";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("初始化 Spelled Mobs 模组...");
		
		// 检查Iron's Spells n Spellbooks是否已安装
		checkDependencies();
		
		// 初始化配置
		SpelledMobsConfig.init();
		
		// 注册命令
		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated, environment) -> {
			SpelledMobsCommand.register(dispatcher, dedicated, environment);
		});
		
		// 为配置和资源重新加载注册监听器
		ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new SimpleResourceReloadListener<SpellCastingData>() {
			private static final Identifier ID = new Identifier(MOD_ID, "spelled_mobs");

			@Override
			public Identifier getFabricId() {
				return ID;
			}

			@Override
			public CompletableFuture<SpellCastingData> load(ResourceManager manager, Profiler profiler, Executor executor) {
				return CompletableFuture.supplyAsync(() -> {
					SpellCastingData data = new SpellCastingData();
					// 加载数据包中的法术配置
					LOGGER.info("开始加载法术配置...");
					
					// 尝试不同的路径
					String[] possibleFolders = {
						"entity_spells",     // 新路径
						"spelled_mobs",      // 原始路径
						"data/spelledmobs/entity_spells",
						"data/spelledmobs/spelled_mobs"
					};
					
					for (String folder : possibleFolders) {
						LOGGER.info("尝试从 {} 文件夹加载配置", folder);
						manager.findResources(folder, id -> id.getPath().endsWith(".json")).forEach((identifier, resource) -> {
							try {
								LOGGER.info("加载配置文件: {}", identifier);
								JsonElement element = GSON.fromJson(resource.getReader(), JsonElement.class);
								data.loadFromJson(identifier, element);
							} catch (Exception e) {
								LOGGER.error("加载法术配置失败 {}: {}", identifier, e.getMessage());
								e.printStackTrace();
							}
						});
					}
					
					if (data.getEntityCount() == 0) {
						LOGGER.warn("未找到任何实体配置! 请检查你的配置文件路径是否正确。");
						LOGGER.warn("配置文件应当放置在: data/spelledmobs/entity_spells/ 目录下");
					} else {
						LOGGER.info("成功加载了 {} 个实体的配置", data.getEntityCount());
					}
					
					return data;
				}, executor);
			}

			@Override
			public CompletableFuture<Void> apply(SpellCastingData data, ResourceManager manager, Profiler profiler, Executor executor) {
				return CompletableFuture.runAsync(() -> {
					// 应用加载的数据到管理器
					SpellCastingManager.INSTANCE.setData(data);
					LOGGER.info("已加载法术配置: {} 个实体", data.getEntityCount());
					
					// 输出已配置的实体列表
					if (data.getEntityCount() > 0) {
						LOGGER.info("已配置实体列表:");
						data.getAllEntityConfigs().keySet().forEach(entityId -> {
							LOGGER.info(" - {}", entityId);
						});
					}
				}, executor);
			}
		});

		// 初始化模组组件
		SpellCastingManager.INSTANCE.init();
		
		LOGGER.info("Spelled Mobs 模组初始化完成！");
		
		// 添加额外的调试信息输出
		LOGGER.info("模组ID: {}", MOD_ID);
		LOGGER.info("配置文件位置: data/spelledmobs/entity_spells/*.json");
	}
	
	/**
	 * 检查依赖模组是否已安装
	 */
	private void checkDependencies() {
		try {
			// 检查Iron's Spells n Spellbooks是否存在
			boolean isIronsSpellsLoaded = FabricLoader.getInstance().isModLoaded("irons_spellbooks");
			if (!isIronsSpellsLoaded) {
				LOGGER.error("未检测到Iron's Spells n Spellbooks模组！Spelled Mobs需要此模组才能正常工作。");
			} else {
				LOGGER.info("已检测到Iron's Spells n Spellbooks模组，版本检查中...");
				
				// 尝试加载Iron's Spells的主类以验证API兼容性
				try {
					Class.forName("io.redspace.ironsspellbooks.api.registry.SpellRegistry");
					LOGGER.info("成功加载Iron's Spells n Spellbooks API，兼容性检查通过");
					
					// 测试是否可以获取法术注册表
					testSpellAccess();
				} catch (ClassNotFoundException e) {
					LOGGER.warn("无法找到Iron's Spells n Spellbooks的API类，将尝试使用备用方法");
					try {
						Class.forName("io.redspace.ironsspellbooks.registry.SpellRegistry");
						LOGGER.info("成功加载Iron's Spells n Spellbooks核心类，但API可能不完全兼容");
					} catch (ClassNotFoundException ex) {
						LOGGER.error("无法找到Iron's Spells n Spellbooks的核心类，模组可能不兼容");
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error("检查依赖时出错: {}", e.getMessage());
		}
	}
	
	/**
	 * 测试法术访问
	 */
	private void testSpellAccess() {
		try {
			String testSpellId = "irons_spellbooks:fireball";
			LOGGER.info("测试法术访问: {}", testSpellId);
			boolean isValid = SpellcastingHelper.isValidSpell(testSpellId);
			if (isValid) {
				LOGGER.info("法术ID有效，可以访问Iron's Spells n Spellbooks的法术");
			} else {
				LOGGER.warn("法术ID无效，可能无法正确访问Iron's Spells n Spellbooks的法术");
			}
			
			// 输出已知命令
			LOGGER.info("检测Iron's Spells的命令结构...");
			try {
				String[] possibleCommands = new String[]{"ispellbook", "cast", "spell"};
				for (String cmd : possibleCommands) {
					LOGGER.info("检测命令: /{}", cmd);
				}
			} catch (Exception e) {
				LOGGER.error("命令检测失败: {}", e.getMessage());
			}
		} catch (Exception e) {
			LOGGER.error("测试法术访问时出错: {}", e.getMessage());
		}
	}
}