package com.spelledmobs.compatibility;

import com.spelledmobs.SpelledMobs;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * 简化版Iron's Spells 'n Spellbooks模组兼容类
 * 使用反射调用Iron's Spells的API，避免直接依赖
 */
public class IronsSpellsCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpelledMobs.MOD_ID);
    private static boolean initialized = false;
    private static boolean isLoaded = false;
    private static boolean hasLoggedAvailableSpells = false;

    private static Method getSpellMethod;
    private static Method onCastMethod;
    private static Method setAdditionalCastDataMethod;
    private static Constructor<?> targetEntityCastDataConstructor;
    private static Constructor<?> createMagicDataMethod;
    private static Field mobCastSourceField;

    private static Class<?> spellRegistryClass;
    private static Class<?> abstractSpellClass;
    private static Class<?> castSourceClass;
    private static Class<?> magicDataClass;
    private static Class<?> iMagicEntityClass;
    private static Class<?> targetEntityCastDataClass;

    /**
     * 初始化兼容性代码
     */
    public static void init() {
        if (initialized) {
            LOGGER.info("[SpelledMobs] 铁魔法兼容层已初始化，跳过重复初始化");
            return;
        }

        try {
            // 检查模组是否已加载
            isLoaded = ModList.get().isLoaded("irons_spellbooks");
            if (!isLoaded) {
                LOGGER.warn("[SpelledMobs] 未检测到Iron's Spells 'n Spellbooks模组，施法功能将不可用");
                return;
            }

            LOGGER.info("[SpelledMobs] 检测到Iron's Spells 'n Spellbooks模组，初始化兼容层...");

            // 获取必要的类和方法
            LOGGER.debug("[SpelledMobs] 加载SpellRegistry类...");
            spellRegistryClass = Class.forName("io.redspace.ironsspellbooks.api.registry.SpellRegistry");
            LOGGER.debug("[SpelledMobs] 加载AbstractSpell类...");
            abstractSpellClass = Class.forName("io.redspace.ironsspellbooks.api.spells.AbstractSpell");
            LOGGER.debug("[SpelledMobs] 加载CastSource类...");
            castSourceClass = Class.forName("io.redspace.ironsspellbooks.api.spells.CastSource");
            LOGGER.debug("[SpelledMobs] 加载MagicData类...");
            magicDataClass = Class.forName("io.redspace.ironsspellbooks.api.magic.MagicData");
            LOGGER.debug("[SpelledMobs] 加载IMagicEntity接口...");
            iMagicEntityClass = Class.forName("io.redspace.ironsspellbooks.api.entity.IMagicEntity");
            LOGGER.debug("[SpelledMobs] 加载TargetEntityCastData类...");
            targetEntityCastDataClass = Class
                    .forName("io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData");

            // 获取MOB枚举值
            LOGGER.debug("[SpelledMobs] 获取MOB施法源...");
            mobCastSourceField = castSourceClass.getDeclaredField("MOB");

            // 获取getSpell方法
            LOGGER.debug("[SpelledMobs] 获取getSpell方法...");
            getSpellMethod = spellRegistryClass.getDeclaredMethod("getSpell", String.class);

            // 获取castSpell方法
            LOGGER.debug("[SpelledMobs] 获取onCast方法...");
            onCastMethod = abstractSpellClass.getDeclaredMethod(
                    "onCast",
                    Level.class,
                    int.class,
                    LivingEntity.class,
                    castSourceClass,
                    magicDataClass);

            // 获取创建MagicData的构造函数
            LOGGER.debug("[SpelledMobs] 获取MagicData构造函数...");
            createMagicDataMethod = magicDataClass.getDeclaredConstructor(boolean.class);
            createMagicDataMethod.setAccessible(true);

            // 获取设置目标数据的方法
            LOGGER.debug("[SpelledMobs] 获取setAdditionalCastData方法...");
            setAdditionalCastDataMethod = magicDataClass.getDeclaredMethod("setAdditionalCastData",
                    Class.forName("io.redspace.ironsspellbooks.api.spells.ICastData"));

            // 获取TargetEntityCastData构造函数
            LOGGER.debug("[SpelledMobs] 获取TargetEntityCastData构造函数...");
            targetEntityCastDataConstructor = targetEntityCastDataClass.getDeclaredConstructor(LivingEntity.class);

            initialized = true;
            LOGGER.info("[SpelledMobs] Iron's Spells 'n Spellbooks兼容层初始化成功！现在可以使用施法功能");

            // 输出可用的法术列表
            logAvailableSpells();
        } catch (Exception e) {
            initialized = false;
            isLoaded = false;
            LOGGER.error("[SpelledMobs] 初始化Iron's Spells 'n Spellbooks兼容层时出错:", e);
            LOGGER.error("[SpelledMobs] 由于上述错误，施法功能将不可用");
        }
    }

    /**
     * 记录所有可用的法术ID到日志中，便于用户选择
     */
    private static void logAvailableSpells() {
        if (hasLoggedAvailableSpells || !initialized) {
            return;
        }

        try {
            LOGGER.info("[SpelledMobs] 正在获取铁魔法中可用的法术列表...");

            // 使用反射获取所有法术ID
            Class<?> spellsClass = Class.forName("io.redspace.ironsspellbooks.api.spells.SpellId");
            Field[] fields = spellsClass.getDeclaredFields();

            List<String> spellIds = new ArrayList<>();
            for (Field field : fields) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    try {
                        Object value = field.get(null);
                        if (value instanceof String) {
                            spellIds.add((String) value);
                        }
                    } catch (Exception e) {
                        // 忽略访问错误
                    }
                }
            }

            if (!spellIds.isEmpty()) {
                LOGGER.info("[SpelledMobs] 发现以下可用的法术ID (共 {} 个):", spellIds.size());
                for (String spellId : spellIds) {
                    LOGGER.info("[SpelledMobs]  - {}", spellId);
                }
            } else {
                // 如果反射方法失败，使用硬编码的已知法术列表
                LOGGER.info("[SpelledMobs] 无法通过反射获取法术ID，使用已知法术列表:");
                List<String> knownSpells = getKnownSpells();
                for (String spellId : knownSpells) {
                    LOGGER.info("[SpelledMobs]  - {}", spellId);
                }
            }

            hasLoggedAvailableSpells = true;
        } catch (Exception e) {
            LOGGER.warn("[SpelledMobs] 获取铁魔法法术列表时出错:", e);

            // 如果反射方法失败，使用硬编码的已知法术列表
            LOGGER.info("[SpelledMobs] 使用已知法术列表:");
            List<String> knownSpells = getKnownSpells();
            for (String spellId : knownSpells) {
                LOGGER.info("[SpelledMobs]  - {}", spellId);
            }

            hasLoggedAvailableSpells = true;
        }
    }

    /**
     * 获取已知的铁魔法法术ID列表
     * 这些是从铁魔法模组中提取的真实法术ID
     */
    private static List<String> getKnownSpells() {
        List<String> spells = new ArrayList<>();
        // 火系法术
        spells.add("irons_spellbooks:fireball");
        spells.add("irons_spellbooks:fire_breath");
        spells.add("irons_spellbooks:flamestrike");
        spells.add("irons_spellbooks:meteor");
        spells.add("irons_spellbooks:firebolt");
        // 冰系法术
        spells.add("irons_spellbooks:ice_spike");
        spells.add("irons_spellbooks:frost_breath");
        spells.add("irons_spellbooks:frost_step");
        spells.add("irons_spellbooks:raise_dead");
        // 雷系法术
        spells.add("irons_spellbooks:lightning_bolt");
        spells.add("irons_spellbooks:electrocute");
        spells.add("irons_spellbooks:chain_lightning");
        // 圣系法术
        spells.add("irons_spellbooks:holy_ray");
        spells.add("irons_spellbooks:divine_smite");
        spells.add("irons_spellbooks:lesser_heal");
        spells.add("irons_spellbooks:greater_heal");
        // 血系法术
        spells.add("irons_spellbooks:bleed");
        spells.add("irons_spellbooks:heartstop");
        spells.add("irons_spellbooks:blood_step");
        // 奥术法术
        spells.add("irons_spellbooks:magic_missile");
        spells.add("irons_spellbooks:teleport");
        spells.add("irons_spellbooks:counterspell");
        spells.add("irons_spellbooks:disintegrate");
        // 自然法术
        spells.add("irons_spellbooks:poison_arrow");
        spells.add("irons_spellbooks:gust");
        spells.add("irons_spellbooks:tornado");
        // 召唤法术
        spells.add("irons_spellbooks:summon_vex");
        spells.add("irons_spellbooks:summon_zombie");
        spells.add("irons_spellbooks:summon_skeleton");
        // 以旋效果
        spells.add("irons_spellbooks:ascension");
        spells.add("irons_spellbooks:black_hole");

        return spells;
    }

    /**
     * 让普通实体施放法术
     *
     * @param caster     施法者实体
     * @param target     目标实体
     * @param level      世界
     * @param spellId    法术ID (如 "irons_spellbooks:fireball" 或简写 "fireball")
     * @param spellLevel 法术等级
     * @return 是否成功施放
     */
    public static boolean castSpell(LivingEntity entity, LivingEntity target, Level level, String spellId,
            int spellLevel) {
        if (!isLoaded || !initialized) {
            LOGGER.warn("[SpelledMobs] 尝试施放法术 {} 但铁魔法兼容层未初始化", spellId);
            return false;
        }

        LOGGER.debug("[SpelledMobs] 实体 {} 尝试向 {} 施放法术 {} (等级 {})",
                entity.getName().getString(),
                target.getName().getString(),
                spellId,
                spellLevel);

        try {
            // 处理法术ID，确保格式正确（添加模组前缀如果需要）
            if (!spellId.contains(":")) {
                String originalId = spellId;
                spellId = "irons_spellbooks:" + spellId;
                LOGGER.debug("[SpelledMobs] 法术ID转换: {} -> {}", originalId, spellId);
            }

            // 获取法术对象前验证法术ID是否可能有效
            if (!isValidSpellId(spellId)) {
                LOGGER.warn("[SpelledMobs] 法术ID {} 可能无效，不在已知法术列表中", spellId);
            }

            // 获取法术
            LOGGER.debug("[SpelledMobs] 尝试获取法术对象: {}", spellId);
            Object spell = getSpellMethod.invoke(null, spellId);
            if (spell == null) {
                LOGGER.warn("[SpelledMobs] 未找到法术: {}，请检查法术ID是否正确", spellId);
                return false;
            }
            LOGGER.debug("[SpelledMobs] 成功获取法术对象: {}", spellId);

            // 获取MOB施法源
            Object mobCastSource = mobCastSourceField.get(null);

            // 创建临时MagicData
            LOGGER.debug("[SpelledMobs] 创建MagicData对象...");
            Object magicData = createMagicDataMethod.newInstance(true); // true表示是生物

            // 创建目标数据
            LOGGER.debug("[SpelledMobs] 创建目标数据对象，目标: {}", target.getName().getString());
            Object targetData = targetEntityCastDataConstructor.newInstance(target);

            // 设置目标数据
            LOGGER.debug("[SpelledMobs] 设置目标数据...");
            setAdditionalCastDataMethod.invoke(magicData, targetData);

            // 调用onCast方法实现法术效果
            LOGGER.debug("[SpelledMobs] 调用onCast方法，实际施放法术...");
            onCastMethod.invoke(spell, level, spellLevel, entity, mobCastSource, magicData);
            LOGGER.info("[SpelledMobs] 法术施放成功: {} -> {} ({})",
                    entity.getName().getString(),
                    target.getName().getString(),
                    spellId);

            // 播放法术音效和粒子效果
            if (!level.isClientSide) {
                try {
                    LOGGER.debug("[SpelledMobs] 尝试播放法术音效...");
                    Method getCastFinishSoundMethod = abstractSpellClass.getDeclaredMethod("getCastFinishSound");
                    Object soundOptional = getCastFinishSoundMethod.invoke(spell);

                    if (soundOptional != null) {
                        Class<?> optionalClass = Class.forName("java.util.Optional");
                        Method isPresent = optionalClass.getDeclaredMethod("isPresent");
                        Method get = optionalClass.getDeclaredMethod("get");

                        if ((Boolean) isPresent.invoke(soundOptional)) {
                            Object sound = get.invoke(soundOptional);
                            // 播放声音，使用反射而不是直接强制转换
                            try {
                                // 找到playSound方法
                                Class<?> levelClass = Level.class;
                                Method playSoundMethod = null;

                                for (Method method : levelClass.getMethods()) {
                                    if (method.getName().equals("playSound") && method.getParameterCount() == 7) {
                                        playSoundMethod = method;
                                        break;
                                    }
                                }

                                if (playSoundMethod != null) {
                                    playSoundMethod.invoke(level, null, entity.getX(), entity.getY(), entity.getZ(),
                                            sound, SoundSource.HOSTILE, 1.0f, 1.0f);
                                    LOGGER.debug("[SpelledMobs] 播放法术音效成功");
                                }
                            } catch (Exception ex) {
                                LOGGER.warn("[SpelledMobs] 播放法术音效失败", ex);
                            }
                        }
                    }
                } catch (Exception ex) {
                    LOGGER.warn("[SpelledMobs] 处理法术音效时出错", ex);
                }
            }

            return true;
        } catch (Exception e) {
            LOGGER.error("[SpelledMobs] 施放法术 {} 过程中发生错误", spellId, e);
            return false;
        }
    }

    /**
     * 检查法术ID是否在已知法术列表中
     * 
     * @param spellId 法术ID
     * @return 是否是已知的法术ID
     */
    private static boolean isValidSpellId(String spellId) {
        return getKnownSpells().contains(spellId);
    }

    /**
     * 检查Iron's Spells是否已加载并初始化
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * 检查Iron's Spells是否已加载
     */
    public static boolean isIronsSpellsLoaded() {
        return isLoaded;
    }
}