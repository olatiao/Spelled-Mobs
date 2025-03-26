package com.spelledmobs.compatibility;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.fml.ModList;
import com.spelledmobs.SpelledMobs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Iron's Spells 'n Spellbooks 模组兼容类
 * 使用反射调用Iron's Spells模组的API，避免直接依赖
 */
public class IronsSpellsCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpelledMobs.MOD_ID);
    private static boolean initialized = false;

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
    public static void init() throws Exception {
        if (initialized) {
            LOGGER.debug("Iron's Spells 'n Spellbooks兼容性已经初始化，跳过");
            return;
        }

        LOGGER.info("正在验证Iron's Spells 'n Spellbooks API可用性...");

        // 确保模组已加载
        if (!ModList.get().isLoaded("irons_spellbooks")) {
            LOGGER.warn("未找到Iron's Spells 'n Spellbooks模组，施法功能将不可用");
            initialized = false;
            return;
        }

        // 检查模组是否已加载
        Class.forName("io.redspace.ironsspellbooks.IronsSpellbooks");
        initialized = true;
        LOGGER.info("检测到Iron's Spells 'n Spellbooks模组，启用兼容功能");

        // 获取必要的类和方法
        spellRegistryClass = Class.forName("io.redspace.ironsspellbooks.api.registry.SpellRegistry");
        abstractSpellClass = Class.forName("io.redspace.ironsspellbooks.api.spells.AbstractSpell");
        castSourceClass = Class.forName("io.redspace.ironsspellbooks.api.spells.CastSource");

        magicDataClass = Class.forName("io.redspace.ironsspellbooks.api.magic.MagicData");
        iMagicEntityClass = Class.forName("io.redspace.ironsspellbooks.api.entity.IMagicEntity");

        targetEntityCastDataClass = Class
                .forName("io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData");

        // 获取MOB枚举值
        mobCastSourceField = castSourceClass.getDeclaredField("MOB");

        // 获取getSpell方法
        getSpellMethod = spellRegistryClass.getDeclaredMethod("getSpell", String.class);

        // 获取castSpell方法
        onCastMethod = abstractSpellClass.getDeclaredMethod(
                "onCast",
                Level.class,
                int.class,
                LivingEntity.class,
                castSourceClass,
                magicDataClass);

        // 获取创建MagicData的构造函数
        createMagicDataMethod = magicDataClass.getDeclaredConstructor(boolean.class);
        createMagicDataMethod.setAccessible(true);

        // 获取设置目标数据的方法
        setAdditionalCastDataMethod = magicDataClass.getDeclaredMethod("setAdditionalCastData",
                Class.forName("io.redspace.ironsspellbooks.api.spells.ICastData"));

        // 获取TargetEntityCastData构造函数
        targetEntityCastDataConstructor = targetEntityCastDataClass.getDeclaredConstructor(LivingEntity.class);

        LOGGER.info("Iron's Spells 'n Spellbooks API验证成功");
    }

    /**
     * 让普通实体施放法术
     *
     * @param caster     施法者实体
     * @param target     目标实体
     * @param level      世界
     * @param spellId    法术ID (如"irons_spellbooks:fireball"或"fireball")
     * @param spellLevel 法术等级
     * @return 是否成功施放
     */
    public static boolean castSpell(LivingEntity entity, LivingEntity target, Level level, String spellId,
            int spellLevel) {
        if (!initialized) {
            LOGGER.debug("无法施放法术，Iron's Spells 'n Spellbooks未加载");
            return false;
        }

        try {
            // 处理法术ID，确保格式正确（添加模组前缀如果需要）
            if (!spellId.contains(":")) {
                spellId = "irons_spellbooks:" + spellId;
            }

            // 获取法术
            Object spell = getSpellMethod.invoke(null, spellId);
            if (spell == null) {
                LOGGER.warn("无法找到法术: {}", spellId);
                return false;
            }

            // 获取MOB施法源
            Object mobCastSource = mobCastSourceField.get(null);

            // 创建临时MagicData
            Object magicData = createMagicDataMethod.newInstance(true); // true表示是生物

            // 创建目标数据
            Object targetData = targetEntityCastDataConstructor.newInstance(target);

            // 设置目标数据
            setAdditionalCastDataMethod.invoke(magicData, targetData);

            // 调用onCast方法实现法术效果
            onCastMethod.invoke(spell, level, spellLevel, entity, mobCastSource, magicData);

            // 播放法术音效和粒子效果
            if (!level.isClientSide) {
                try {
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
                                } else {
                                    LOGGER.warn("无法找到合适的playSound方法");
                                }
                            } catch (Exception ex) {
                                LOGGER.error("播放声音时出错: {}", ex.getMessage());
                            }
                        }
                    }
                } catch (Exception ex) {
                    LOGGER.error("获取或播放法术音效时出错", ex);
                }
            }

            return true;
        } catch (Exception e) {
            LOGGER.error("施法过程中发生错误", e);
            return false;
        }
    }

    /**
     * 尝试将实体转换为IMagicEntity并施放法术
     * 这适用于那些可能已经实现了IMagicEntity接口的实体
     */
    public static boolean tryCastAsMagicEntity(LivingEntity entity, String spellId, int spellLevel) {
        if (!initialized) {
            return false;
        }

        try {
            // 检查实体是否实现了IMagicEntity接口
            if (iMagicEntityClass.isInstance(entity)) {
                // 处理法术ID，确保格式正确
                if (!spellId.contains(":")) {
                    spellId = "irons_spellbooks:" + spellId;
                }

                // 获取法术
                Object spell = getSpellMethod.invoke(null, spellId);
                if (spell == null)
                    return false;

                // 调用initiateCastSpell方法
                Method initiateCastMethod = iMagicEntityClass.getMethod("initiateCastSpell", abstractSpellClass,
                        int.class);
                initiateCastMethod.invoke(entity, spell, spellLevel);
                return true;
            }
            return false;
        } catch (Exception e) {
            LOGGER.error("作为魔法实体施放法术失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查是否已成功初始化
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * 获取支持的法术ID列表
     * 
     * @return 法术ID数组
     */
    public static String[] getSupportedSpells() {
        if (!initialized) {
            LOGGER.warn("尝试获取支持的法术列表，但兼容性未初始化");
            return new String[0];
        }

        // 返回支持的法术列表
        return new String[] {
                "fire_bolt",
                "ice_lance",
                "lightning_bolt",
                "magic_missile",
                "healing_word",
                "fireball",
                "poison_spray"
                // 可根据实际支持添加更多法术
        };
    }

    /**
     * 获取法术名称
     * 
     * @param spellId 法术ID
     * @return 法术名称，如果未找到则返回ID
     */
    public static String getSpellName(String spellId) {
        if (!initialized || spellId == null) {
            return spellId;
        }

        // 简单映射，实际实现时会通过Iron's Spells API获取
        switch (spellId.toLowerCase()) {
            case "fire_bolt":
                return "火焰弹";
            case "ice_lance":
                return "冰矛";
            case "lightning_bolt":
                return "闪电束";
            case "magic_missile":
                return "魔法飞弹";
            case "healing_word":
                return "治疗之言";
            case "fireball":
                return "火球术";
            case "poison_spray":
                return "毒素喷射";
            default:
                return spellId;
        }
    }
}