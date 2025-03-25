package com.spelledmobs.util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import com.spelledmobs.SpelledMobs;
import com.spelledmobs.config.SpelledMobsConfig;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * 用于与Iron's Spells 'n Spellbooks API交互的帮助类
 */
public class SpellcastingHelper {

    /**
     * 尝试让实体施放指定法术
     * 
     * @param caster  施法者
     * @param target  目标实体
     * @param spellId 法术ID
     * @param level   法术等级
     * @return 是否成功施放
     */
    public static boolean castSpell(LivingEntity caster, LivingEntity target, String spellId, int level) {
        try {
            // 首先尝试通过反射直接调用API
            boolean success = false;
            try {
                success = castSpellViaReflection(caster, target, spellId, level);
            } catch (Exception e) {
                SpelledMobs.LOGGER.debug("反射方法失败: {}, 尝试使用命令方法", e.getMessage());
                // 反射失败，尝试使用命令执行法术
                success = castSpellViaCommand(caster, target, spellId, level);
            }

            if (success) {
                SpelledMobs.LOGGER.debug("实体 {} 施放法术 {} 等级 {} 对目标 {}",
                        caster.getUuid(), spellId, level, target != null ? target.getUuid() : "无目标");
            }

            return success;
        } catch (Exception e) {
            SpelledMobs.LOGGER.error("施放法术失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 通过命令执行法术
     * 
     * @param caster  施法者
     * @param target  目标实体
     * @param spellId 法术ID
     * @param level   法术等级
     * @return 是否成功施放
     */
    private static boolean castSpellViaCommand(LivingEntity caster, LivingEntity target, String spellId, int level) {
        try {
            if (!(caster.getWorld() instanceof ServerWorld)) {
                SpelledMobs.LOGGER.error("施法者所在世界不是服务器世界");
                return false;
            }

            ServerWorld world = (ServerWorld) caster.getWorld();
            MinecraftServer server = world.getServer();

            // 确保法术ID格式正确
            if (!spellId.contains(":")) {
                spellId = "irons_spellbooks:" + spellId;
                SpelledMobs.LOGGER.warn("法术ID格式不完整，已添加命名空间前缀: {}", spellId);
            }

            SpelledMobs.LOGGER.info("尝试通过命令施法: 法术={}, 等级={}, UUID={}", spellId, level, caster.getUuidAsString());

            // 记录施法者和目标的位置信息以便调试
            SpelledMobs.LOGGER.debug("施法者位置: {}, 目标位置: {}",
                    caster.getPos(),
                    target != null ? target.getPos() : "无目标");

            boolean anySuccess = false;

            // 尝试不同的命令格式
            // 1. 原版Iron's Spells的命令格式
            // 2. 使用实体选择器的格式
            // 3. 使用execute命令的格式

            // 命令格式列表
            String[] commandFormats = new String[] {
                    // 原版Iron's Spells格式
                    "cast %s %s %d",
                    "cast %s %s %d %s",

                    // 实体选择器格式
                    "execute as %s at @s run cast @s %s %d",
                    "execute as %s at @s run cast @s %s %d %s",

                    // 直接使用Iron's Spells内部命令
                    "ispellbook cast %s %s %d",
                    "ispellbook cast %s %s %d %s",

                    // 尝试另一种可能的命令格式
                    "spell cast %s %s %d",
                    "spell cast %s %s %d %s"
            };

            for (String format : commandFormats) {
                String cmd;

                if (format.contains("%s %s %d %s")) {
                    // 需要目标的命令
                    if (target == null)
                        continue;

                    String targetSelector = "@e[type=" + target.getType().toString().replace("entity.", "")
                            + ",limit=1,sort=nearest,distance=..20]";
                    cmd = String.format(format, caster.getUuidAsString(), spellId, level, targetSelector);
                } else {
                    // 不需要目标的命令
                    cmd = String.format(format, caster.getUuidAsString(), spellId, level);
                }

                SpelledMobs.LOGGER.info("尝试执行命令: /{}", cmd);

                try {
                    // 创建命令源 - 确保有足够的权限
                    ServerCommandSource source = new ServerCommandSource(
                            CommandOutput.DUMMY,
                            caster.getPos(),
                            caster.getRotationClient(),
                            world,
                            4, // 权限级别 (OP)
                            caster.getName().getString(),
                            caster.getDisplayName(),
                            world.getServer(),
                            caster);

                    // 执行命令并检查结果
                    int result = server.getCommandManager().executeWithPrefix(source, cmd);

                    SpelledMobs.LOGGER.info("命令 '{}' 执行完成，结果码: {}", cmd, result);

                    if (result > 0) {
                        anySuccess = true;
                        SpelledMobs.LOGGER.info("命令执行成功! 实体应该已经施放了法术");
                        break;
                    } else {
                        SpelledMobs.LOGGER.warn("命令执行可能失败，将尝试下一种命令格式");
                    }
                } catch (Exception e) {
                    SpelledMobs.LOGGER.warn("命令 '{}' 执行失败: {}", cmd, e.getMessage());
                }
            }

            // 播放特效（如果启用）
            if (SpelledMobsConfig.shouldShowCastingEffects()) {
                playVisualEffects(caster, target, spellId, level);
            }

            return anySuccess;
        } catch (Exception e) {
            SpelledMobs.LOGGER.error("通过命令施放法术失败: {}", e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 通过反射调用Iron's Spells API施放法术
     * 
     * @param caster  施法者
     * @param target  目标实体
     * @param spellId 法术ID
     * @param level   法术等级
     * @return 是否成功施放
     */
    private static boolean castSpellViaReflection(LivingEntity caster, LivingEntity target, String spellId, int level)
            throws Exception {
        // 尝试使用非API包，直接使用核心模组类

        // 先尝试API路径
        String registryPath = "io.redspace.ironsspellbooks.api.registry.SpellRegistry";
        String spellPath = "io.redspace.ironsspellbooks.api.spells.AbstractSpell";

        // 备用非API路径
        String registryPathAlt = "io.redspace.ironsspellbooks.registry.SpellRegistry";
        String spellPathAlt = "io.redspace.ironsspellbooks.spells.AbstractSpell";

        // 尝试加载SpellRegistry类
        Class<?> spellRegistryClass = null;
        try {
            spellRegistryClass = Class.forName(registryPath);
            SpelledMobs.LOGGER.info("使用API路径加载SpellRegistry");
        } catch (ClassNotFoundException e) {
            SpelledMobs.LOGGER.info("API路径未找到，尝试备用路径");
            spellRegistryClass = Class.forName(registryPathAlt);
            spellPath = spellPathAlt;
            SpelledMobs.LOGGER.info("使用核心模组路径");
        }

        // 获取getSpell方法
        var getSpellMethod = spellRegistryClass.getMethod("getSpell", Identifier.class);

        // 使用直接提供的法术ID创建Identifier
        Object spell = getSpellMethod.invoke(null, new Identifier(spellId));

        if (spell == null) {
            SpelledMobs.LOGGER.error("未找到法术: {}", spellId);
            return false;
        }

        SpelledMobs.LOGGER.info("找到法术: {}, 类: {}", spellId, spell.getClass().getName());

        // 获取AbstractSpell类
        Class<?> abstractSpellClass = Class.forName(spellPath);

        // 简化版: 直接调用实际方法
        Method npcCastMethod = findNpcCastMethod(abstractSpellClass);
        if (npcCastMethod != null) {
            Object result = null;

            // 根据参数数量和类型调用方法
            Class<?>[] paramTypes = npcCastMethod.getParameterTypes();

            if (paramTypes.length == 3 && paramTypes[2] == int.class) {
                // 可能是 castFromNpc(World, LivingEntity, int)
                result = npcCastMethod.invoke(spell, caster.getWorld(), caster, level);
            } else if (paramTypes.length == 4 && paramTypes[3] == int.class) {
                // 可能是 castFromNpc(World, LivingEntity, LivingEntity, int)
                result = npcCastMethod.invoke(spell, caster.getWorld(), caster, target, level);
            }

            if (result instanceof Boolean) {
                boolean success = (Boolean) result;
                if (success) {
                    // 播放特效（如果启用）
                    if (SpelledMobsConfig.shouldShowCastingEffects()) {
                        playVisualEffects(caster, target, spellId, level);
                    }

                    SpelledMobs.LOGGER.info("成功使用NPC方法施放法术: {}", npcCastMethod.getName());
                    return true;
                }
            }
        }

        // 如果没有找到合适的NPC方法，抛出异常，将回退到命令法
        throw new NoSuchMethodException("未找到合适的NPC施法方法");
    }

    /**
     * 查找专门用于NPC施法的方法
     */
    private static Method findNpcCastMethod(Class<?> spellClass) {
        // 按优先顺序尝试可能的方法名
        String[] methodNames = {
                "castFromNpc", "npcCast", "castSpellFromNpc", "castByNpc"
        };

        for (String methodName : methodNames) {
            // 先尝试有目标的版本
            try {
                return spellClass.getMethod(methodName,
                        ServerWorld.class, LivingEntity.class, LivingEntity.class, int.class);
            } catch (NoSuchMethodException e) {
                // 忽略并继续尝试
            }

            // 再尝试无目标的版本
            try {
                return spellClass.getMethod(methodName,
                        ServerWorld.class, LivingEntity.class, int.class);
            } catch (NoSuchMethodException e) {
                // 忽略并继续尝试
            }
        }

        return null;
    }

    /**
     * 播放施法视觉特效
     * 
     * @param caster  施法者
     * @param target  目标实体
     * @param spellId 法术ID
     * @param level   法术等级
     */
    private static void playVisualEffects(LivingEntity caster, LivingEntity target, String spellId, int level) {
        // TODO: 实现视觉特效
        // 这可以通过粒子效果、声音等来实现
    }

    /**
     * 检查指定的法术ID是否有效
     * 
     * @param spellId 法术ID
     * @return 法术ID是否有效
     */
    public static boolean isValidSpell(String spellId) {
        try {
            String registryPath = "io.redspace.ironsspellbooks.api.registry.SpellRegistry";
            String registryPathAlt = "io.redspace.ironsspellbooks.registry.SpellRegistry";

            // 尝试加载SpellRegistry类
            Class<?> spellRegistryClass = null;
            try {
                spellRegistryClass = Class.forName(registryPath);
            } catch (ClassNotFoundException e) {
                spellRegistryClass = Class.forName(registryPathAlt);
            }

            var getSpellMethod = spellRegistryClass.getMethod("getSpell", Identifier.class);

            // 直接使用提供的法术ID检查
            Object spell = getSpellMethod.invoke(null, new Identifier(spellId));
            return spell != null;
        } catch (ClassNotFoundException e) {
            SpelledMobs.LOGGER.error("未找到Iron's Spells 'n Spellbooks API类");
            return false;
        } catch (Exception e) {
            SpelledMobs.LOGGER.error("验证法术ID时出错 {}: {}", spellId, e.getMessage());
            return false;
        }
    }

    /**
     * 获取所有可用的法术列表
     * 
     * @return 可用的法术ID列表
     */
    public static List<String> getAvailableSpells() {
        List<String> result = new ArrayList<>();

        try {
            // 尝试加载SpellRegistry类
            String registryPath = "io.redspace.ironsspellbooks.api.registry.SpellRegistry";
            String registryPathAlt = "io.redspace.ironsspellbooks.registry.SpellRegistry";

            Class<?> spellRegistryClass = null;
            try {
                spellRegistryClass = Class.forName(registryPath);
                SpelledMobs.LOGGER.info("使用API路径列出法术");
            } catch (ClassNotFoundException e) {
                SpelledMobs.LOGGER.info("API路径未找到，尝试备用路径");
                spellRegistryClass = Class.forName(registryPathAlt);
            }

            // 尝试获取getSpells或类似方法
            Method getSpellsMethod = null;
            try {
                // 尝试不同可能的方法名
                String[] methodNames = { "getSpells", "getAllSpells", "getRegisteredSpells", "getAll" };
                for (String methodName : methodNames) {
                    try {
                        getSpellsMethod = spellRegistryClass.getMethod(methodName);
                        break;
                    } catch (NoSuchMethodException e) {
                        // 继续尝试下一个
                    }
                }

                if (getSpellsMethod == null) {
                    // 尝试获取Registry实例，然后用stream或迭代器获取所有法术
                    Field registryField = spellRegistryClass.getDeclaredField("REGISTRY");
                    registryField.setAccessible(true);
                    Object registry = registryField.get(null);

                    if (registry != null) {
                        // 尝试调用values()或entrySet()方法
                        Method valuesMethod = registry.getClass().getMethod("values");
                        Object spells = valuesMethod.invoke(registry);

                        if (spells instanceof Collection) {
                            Collection<?> spellCollection = (Collection<?>) spells;
                            for (Object spell : spellCollection) {
                                if (spell != null) {
                                    // 尝试获取法术ID
                                    try {
                                        Method getIdMethod = spell.getClass().getMethod("getId");
                                        Object id = getIdMethod.invoke(spell);
                                        result.add(id.toString());
                                    } catch (Exception ex) {
                                        // 降级为使用toString
                                        result.add(spell.toString());
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // 直接使用getSpells方法
                    Object spells = getSpellsMethod.invoke(null);
                    if (spells instanceof Collection) {
                        Collection<?> spellCollection = (Collection<?>) spells;
                        for (Object spell : spellCollection) {
                            if (spell != null) {
                                // 尝试获取法术ID
                                try {
                                    Method getIdMethod = spell.getClass().getMethod("getId");
                                    Object id = getIdMethod.invoke(spell);
                                    result.add(id.toString());
                                } catch (Exception ex) {
                                    // 降级为使用toString
                                    result.add(spell.toString());
                                }
                            }
                        }
                    } else if (spells instanceof Map) {
                        Map<?, ?> spellMap = (Map<?, ?>) spells;
                        for (Object key : spellMap.keySet()) {
                            result.add(key.toString());
                        }
                    }
                }
            } catch (Exception e) {
                SpelledMobs.LOGGER.error("获取法术列表时出错: {}", e.getMessage());
                if (SpelledMobsConfig.isDebugLoggingEnabled()) {
                    e.printStackTrace();
                }
            }

            // 如果以上方法都失败，尝试反射遍历常量（如果是用enum实现的）
            if (result.isEmpty()) {
                Field[] fields = spellRegistryClass.getDeclaredFields();
                for (Field field : fields) {
                    if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) &&
                            java.lang.reflect.Modifier.isFinal(field.getModifiers())) {
                        try {
                            field.setAccessible(true);
                            Object value = field.get(null);
                            if (value != null) {
                                result.add(field.getName());
                            }
                        } catch (Exception e) {
                            // 忽略异常，继续处理下一个字段
                        }
                    }
                }
            }

            SpelledMobs.LOGGER.info("找到 {} 个可用法术", result.size());
        } catch (Exception e) {
            SpelledMobs.LOGGER.error("列出可用法术时出错: {}", e.getMessage());
            if (SpelledMobsConfig.isDebugLoggingEnabled()) {
                e.printStackTrace();
            }
        }

        return result;
    }

    /**
     * 验证法术ID格式是否正确
     * 
     * @param spellId 要验证的法术ID
     * @return 格式化后的法术ID（如果原始ID不合法则返回null）
     */
    public static String validateSpellId(String spellId) {
        if (spellId == null || spellId.isEmpty()) {
            SpelledMobs.LOGGER.error("法术ID为空");
            return null;
        }

        // 检查是否包含无效字符
        if (!spellId.matches("[a-zA-Z0-9_:.]+")) {
            SpelledMobs.LOGGER.error("法术ID包含无效字符: {}", spellId);
            return null;
        }

        // 检查是否包含命名空间分隔符
        boolean hasNamespace = spellId.contains(":");
        if (!hasNamespace) {
            SpelledMobs.LOGGER.info("法术ID没有命名空间: {}", spellId);

            // 自动添加默认命名空间
            String correctedId = "irons_spellbooks:" + spellId;
            SpelledMobs.LOGGER.info("已自动添加命名空间前缀: {}", correctedId);
            
            // 返回修正后的ID，不再检查有效性（避免循环调用）
            return correctedId;
        }
        
        // 确保命名空间正确
        if (!spellId.startsWith("irons_spellbooks:") && !spellId.startsWith("minecraft:")) {
            SpelledMobs.LOGGER.warn("非标准法术ID命名空间: {}", spellId);
            
            // 提取法术名称部分
            String[] parts = spellId.split(":", 2);
            if (parts.length > 1) {
                String spellName = parts[1];
                // 尝试用正确的命名空间重建
                String correctedId = "irons_spellbooks:" + spellName;
                SpelledMobs.LOGGER.info("尝试修正为标准命名空间: {}", correctedId);
                return correctedId;
            }
        }

        return spellId;
    }

    /**
     * 获取法术的详细信息（如果可用）
     * 
     * @param spellId 法术ID
     * @return 法术详细信息的字符串表示
     */
    public static String getSpellDetails(String spellId) {
        try {
            // 验证ID格式
            String validatedId = validateSpellId(spellId);
            if (validatedId == null) {
                return "无效的法术ID";
            }

            // 检查法术是否存在
            if (!isValidSpell(validatedId)) {
                return "法术不存在";
            }

            // 获取法术
            String registryPath = "io.redspace.ironsspellbooks.api.registry.SpellRegistry";
            String registryPathAlt = "io.redspace.ironsspellbooks.registry.SpellRegistry";

            Class<?> spellRegistryClass = null;
            try {
                spellRegistryClass = Class.forName(registryPath);
            } catch (ClassNotFoundException e) {
                spellRegistryClass = Class.forName(registryPathAlt);
            }

            Method getSpellMethod = spellRegistryClass.getMethod("getSpell", Identifier.class);
            Object spell = getSpellMethod.invoke(null, new Identifier(validatedId));

            if (spell == null) {
                return "找不到法术";
            }

            // 尝试获取法术信息
            StringBuilder details = new StringBuilder();
            details.append("法术: ").append(validatedId).append("\n");

            try {
                // 尝试获取法术名称
                Method getNameMethod = spell.getClass().getMethod("getName");
                Object name = getNameMethod.invoke(spell);
                details.append("名称: ").append(name).append("\n");
            } catch (Exception e) {
                // 忽略异常
            }

            try {
                // 尝试获取法术类型
                Method getTypeMethod = spell.getClass().getMethod("getType");
                Object type = getTypeMethod.invoke(spell);
                details.append("类型: ").append(type).append("\n");
            } catch (Exception e) {
                // 忽略异常
            }

            try {
                // 尝试获取法术描述
                Method getDescriptionMethod = spell.getClass().getMethod("getDescription");
                Object description = getDescriptionMethod.invoke(spell);
                details.append("描述: ").append(description).append("\n");
            } catch (Exception e) {
                // 忽略异常
            }

            return details.toString();
        } catch (Exception e) {
            SpelledMobs.LOGGER.error("获取法术详情时出错: {}", e.getMessage());
            return "获取法术详情时出错: " + e.getMessage();
        }
    }
}