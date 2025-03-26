package com.spelledmobs.data;

import java.util.HashMap;
import java.util.Map;

/**
 * 法术条件接口，用于检查是否满足施放条件
 */
public interface SpellCondition {
    /**
     * 检查条件是否满足
     * 
     * @param context 条件上下文
     * @return 是否满足条件
     */
    boolean check(SpellConditionContext context);
    
    /**
     * 获取条件类型
     * 
     * @return 条件类型
     */
    ConditionType getType();
    
    /**
     * 获取条件比较操作符
     * 
     * @return 比较操作符
     */
    ComparisonOperator getOperator();
    
    /**
     * 条件类型枚举
     */
    enum ConditionType {
        // 基础条件
        HEALTH_PERCENTAGE("HEALTH_PERCENTAGE"),
        HEALTH_ABSOLUTE("HEALTH_ABSOLUTE"),
        TARGET_DISTANCE("TARGET_DISTANCE"),
        TARGET_HEALTH("TARGET_HEALTH"),
        TARGET_TYPE("TARGET_TYPE"),
        ENTITY_NAME("ENTITY_NAME"),
        HELD_ITEM("HELD_ITEM"),
        
        // 环境条件
        TIME_OF_DAY("TIME_OF_DAY"),
        WEATHER("WEATHER"),
        MOON_PHASE("MOON_PHASE"),
        BIOME("BIOME"),
        LIGHT_LEVEL("LIGHT_LEVEL"),
        HEIGHT("HEIGHT"),
        
        // 状态条件
        IS_IN_WATER("IS_IN_WATER"),
        IS_ON_FIRE("IS_ON_FIRE"),
        IS_SNEAKING("IS_SNEAKING"),
        IS_SPRINTING("IS_SPRINTING"),
        STATUS_EFFECT("STATUS_EFFECT"),
        ARMOR_VALUE("ARMOR_VALUE"),
        LAST_DAMAGE_SOURCE("LAST_DAMAGE_SOURCE"),
        
        // 高级条件
        TARGET_COUNT("TARGET_COUNT"),
        RANDOM_CHANCE("RANDOM_CHANCE");
        
        private final String id;
        private static final Map<String, ConditionType> BY_ID = new HashMap<>();
        
        static {
            for (ConditionType type : values()) {
                BY_ID.put(type.getId(), type);
            }
        }
        
        ConditionType(String id) {
            this.id = id;
        }
        
        public String getId() {
            return id;
        }
        
        public static ConditionType byId(String id) {
            return BY_ID.getOrDefault(id, RANDOM_CHANCE);
        }
    }
    
    /**
     * 比较操作符枚举
     */
    enum ComparisonOperator {
        EQUALS("EQUALS"),
        NOT_EQUALS("NOT_EQUALS"),
        GREATER_THAN("GREATER_THAN"),
        LESS_THAN("LESS_THAN"),
        GREATER_THAN_OR_EQUALS("GREATER_THAN_OR_EQUALS"),
        LESS_THAN_OR_EQUALS("LESS_THAN_OR_EQUALS"),
        CONTAINS("CONTAINS"),
        STARTS_WITH("STARTS_WITH"),
        ENDS_WITH("ENDS_WITH");
        
        private final String id;
        private static final Map<String, ComparisonOperator> BY_ID = new HashMap<>();
        
        static {
            for (ComparisonOperator op : values()) {
                BY_ID.put(op.getId(), op);
            }
        }
        
        ComparisonOperator(String id) {
            this.id = id;
        }
        
        public String getId() {
            return id;
        }
        
        public static ComparisonOperator byId(String id) {
            return BY_ID.getOrDefault(id, EQUALS);
        }
        
        /**
         * 比较数值
         * 
         * @param value1 第一个值
         * @param value2 第二个值
         * @return 比较结果
         */
        public boolean compareNumeric(double value1, double value2) {
            return switch (this) {
                case EQUALS -> value1 == value2;
                case NOT_EQUALS -> value1 != value2;
                case GREATER_THAN -> value1 > value2;
                case LESS_THAN -> value1 < value2;
                case GREATER_THAN_OR_EQUALS -> value1 >= value2;
                case LESS_THAN_OR_EQUALS -> value1 <= value2;
                default -> false;
            };
        }
        
        /**
         * 比较字符串
         * 
         * @param value1 第一个字符串
         * @param value2 第二个字符串
         * @return 比较结果
         */
        public boolean compareString(String value1, String value2) {
            if (value1 == null || value2 == null) {
                return false;
            }
            
            return switch (this) {
                case EQUALS -> value1.equals(value2);
                case NOT_EQUALS -> !value1.equals(value2);
                case CONTAINS -> value1.contains(value2);
                case STARTS_WITH -> value1.startsWith(value2);
                case ENDS_WITH -> value1.endsWith(value2);
                default -> false;
            };
        }
    }
} 