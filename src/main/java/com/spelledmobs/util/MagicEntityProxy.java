package com.spelledmobs.util;

import net.minecraft.world.entity.LivingEntity;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 魔法实体代理类，用于动态实现Iron's Spellbooks的IMagicEntity接口
 * 这个类使用Java的动态代理机制，避免直接依赖Iron's Spellbooks模组
 */
public class MagicEntityProxy {
    private final LivingEntity owner;
    private final Object proxyInstance;

    // 用于缓存魔法属性
    private float maxMana = 100.0f;
    private float currentMana = 100.0f;
    private float manaRegenRate = 1.0f;

    /**
     * 创建一个新的魔法实体代理
     * 
     * @param owner 拥有此代理的实体
     */
    public MagicEntityProxy(LivingEntity owner) {
        this.owner = owner;

        // 创建动态代理
        try {
            Class<?> iMagicEntityClass = Class.forName("io.redspace.ironsspellbooks.api.entity.IMagicEntity");
            this.proxyInstance = Proxy.newProxyInstance(
                    this.getClass().getClassLoader(),
                    new Class<?>[] { iMagicEntityClass },
                    new MagicEntityInvocationHandler());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create magic entity proxy", e);
        }
    }

    /**
     * 获取代理实例，可以转换为IMagicEntity接口
     */
    public Object getProxyInstance() {
        return proxyInstance;
    }

    /**
     * 处理接口方法调用的内部类
     */
    private class MagicEntityInvocationHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();

            // 实现IMagicEntity接口的方法
            switch (methodName) {
                case "getEntity":
                    return owner;

                case "consumeMana":
                    if (args != null && args.length > 0 && args[0] instanceof Float) {
                        float amount = (Float) args[0];
                        currentMana = Math.max(0, currentMana - amount);
                        return true;
                    }
                    return false;

                case "getMana":
                    return currentMana;

                case "setMana":
                    if (args != null && args.length > 0 && args[0] instanceof Float) {
                        currentMana = Math.min(maxMana, (Float) args[0]);
                    }
                    return null;

                case "getMaxMana":
                    return maxMana;

                case "setMaxMana":
                    if (args != null && args.length > 0 && args[0] instanceof Float) {
                        maxMana = (Float) args[0];
                        currentMana = Math.min(currentMana, maxMana);
                    }
                    return null;

                case "getManaRegenRate":
                    return manaRegenRate;

                case "setManaRegenRate":
                    if (args != null && args.length > 0 && args[0] instanceof Float) {
                        manaRegenRate = (Float) args[0];
                    }
                    return null;

                // 由于我们的生物不需要法术列表和冷却，提供默认实现
                case "getKnownSpells":
                    return java.util.Collections.emptyList();

                case "getSpellCooldown":
                    return 0.0f;

                case "setSpellCooldown":
                    return null;

                case "getCooldownModifier":
                    return 1.0f;

                // 默认返回
                default:
                    System.out.println("Unhandled IMagicEntity method call: " + methodName);

                    // 尝试获取返回类型的默认值
                    Class<?> returnType = method.getReturnType();
                    if (returnType.equals(boolean.class))
                        return false;
                    if (returnType.equals(int.class))
                        return 0;
                    if (returnType.equals(float.class))
                        return 0.0f;
                    if (returnType.equals(double.class))
                        return 0.0d;
                    return null;
            }
        }
    }
}