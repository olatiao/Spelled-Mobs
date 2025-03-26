# Spelled Mobs

一个基于 Forge 的 Minecraft 模组，可以通过数据包为任何生物添加施法能力，作为 [Iron's Spells 'n Spellbooks](https://www.curseforge.com/minecraft/mc-mods/irons-spells-n-spellbooks) 的附属模组。

## 简介

Spelled Mobs 允许你通过简单的 JSON 配置文件，给游戏中的任何生物（无论是原版的还是其他模组添加的）赋予施法能力。生物可以根据各种条件（如血量、目标距离、手持物品等）来决定何时施放什么法术。

## 特性

- 为任何生物添加施法能力
- 通过数据包配置，无需编程即可修改
- 丰富的条件系统可以精确控制施法时机：
  - 基于实体和目标的属性（血量、距离、类型等）
  - 基于环境条件（时间、天气、生物群系等）
  - 基于状态条件（是否在水中、着火、潜行等）
  - 基于随机几率
- 可设置法术等级范围
- 可设置施法冷却时间范围
- 支持多个法术配置，并可设置优先级
- 命令系统用于管理和调试

## 数据包配置

### 基本结构

```json
{
  "entity_id": "minecraft:zombie",
  "check_interval": 40,
  "spells": [
    {
      "spell_id": "irons_spellbooks.fire_bolt",
      "min_level": 1,
      "max_level": 3,
      "min_cast_time": 100,
      "max_cast_time": 200,
      "conditions": [
        {
          "type": "TARGET_DISTANCE",
          "operator": "LESS_THAN_OR_EQUALS",
          "numeric_value": 12.0
        },
        {
          "type": "HEALTH_PERCENTAGE",
          "operator": "GREATER_THAN",
          "numeric_value": 25.0
        }
      ]
    }
  ]
}
```

### 配置项说明

#### 顶级配置

| 字段 | 类型 | 描述 | 默认值 |
|------|------|------|--------|
| entity_id | String | 实体的标识符，例如"minecraft:zombie" | 必填 |
| check_interval | Integer | 检查施法条件的间隔（以游戏刻为单位） | 20 |
| spells | Array | 法术配置列表 | 必填 |

#### 法术配置

| 字段 | 类型 | 描述 | 默认值 |
|------|------|------|--------|
| spell_id | String | Iron's Spells 'n Spellbooks中的法术ID，必须包含模组前缀（如"irons_spellbooks.fire_bolt"） | 必填 |
| min_level | Integer | 最小法术等级 | 1 |
| max_level | Integer | 最大法术等级 | 1 |
| min_cast_time | Integer | 最小施法冷却时间（以游戏刻为单位） | 60 |
| max_cast_time | Integer | 最大施法冷却时间（以游戏刻为单位） | 200 |
| conditions | Array | 施法条件列表 | [] |

> **注意**: 法术ID必须使用完整格式，包含"irons_spellbooks."前缀，例如"irons_spellbooks.fire_bolt"而不仅仅是"fire_bolt"。

#### 条件配置

| 字段 | 类型 | 描述 | 默认值 |
|------|------|------|--------|
| type | String | 条件类型，见下表 | 必填 |
| operator | String | 比较运算符，见下表 | "EQUALS" |
| value | String | 字符串比较值（用于某些条件类型） | "" |
| numeric_value | Float | 数值比较值（用于某些条件类型） | 0 |
| invert | Boolean | 是否反转条件结果 | false |
| extra_data | Object | 额外数据（用于某些条件类型） | {} |

#### 条件类型

**基础条件**：
- `HEALTH_PERCENTAGE` - 实体当前血量百分比
- `HEALTH_ABSOLUTE` - 实体当前血量绝对值
- `TARGET_DISTANCE` - 与目标的距离
- `TARGET_HEALTH` - 目标血量
- `TARGET_TYPE` - 目标类型
- `ENTITY_NAME` - 实体名称
- `HELD_ITEM` - 手持物品

**环境条件**：
- `TIME_OF_DAY` - 世界时间（0-24000）
- `WEATHER` - 天气状态 ("clear", "rain", "thunder"/"storm")
- `MOON_PHASE` - 月相（0-7）
- `BIOME` - 生物群系
- `LIGHT_LEVEL` - 光照等级（0-15）
- `HEIGHT` - 高度（Y坐标）

**状态条件**：
- `IS_IN_WATER` - 是否在水中
- `IS_ON_FIRE` - 是否着火
- `IS_SNEAKING` - 是否潜行
- `IS_SPRINTING` - 是否疾跑
- `STATUS_EFFECT` - 状态效果
- `ARMOR_VALUE` - 护甲值
- `LAST_DAMAGE_SOURCE` - 最后受到的伤害源

**高级条件**：
- `TARGET_COUNT` - 附近目标数量（支持额外数据：radius, target_type）
- `RANDOM_CHANCE` - 随机几率（百分比，0-100）

#### 比较运算符

- `EQUALS` - 等于
- `NOT_EQUALS` - 不等于
- `GREATER_THAN` - 大于
- `LESS_THAN` - 小于
- `GREATER_THAN_OR_EQUALS` - 大于等于
- `LESS_THAN_OR_EQUALS` - 小于等于
- `CONTAINS` - 包含（仅适用于字符串）
- `STARTS_WITH` - 以...开头（仅适用于字符串）
- `ENDS_WITH` - 以...结尾（仅适用于字符串）

## 命令系统

模组提供了一系列命令，用于管理和调试模组功能：

- `/spelledmobs help` - 显示帮助信息
- `/spelledmobs reload` - 重新加载所有施法配置
- `/spelledmobs check <spell_id>` - 检查法术ID是否有效
- `/spelledmobs cast <entity> <spell_id> <level> [<target>]` - 让实体施放法术
- `/spelledmobs config debugLogging <true|false>` - 设置调试日志
- `/spelledmobs config commandFeedback <true|false>` - 设置命令反馈
- `/spelledmobs config maxCheckDistance <value>` - 设置最大检查距离

## 法术ID参考

Iron's Spells 'n Spellbooks中的法术必须使用简易ID，不能包含模组前缀。以下是一些常用法术的示例：

- `fire_bolt` - 火焰弹
- `ice_block` - 冰块
- `lightning_bolt` - 闪电
- `teleport` - 传送术
- `fireball` - 火球
- `heal` - 治疗术

有关法术ID的完整列表，请参考Iron's Spells 'n Spellbooks的官方文档或游戏内法术书。
如果是其他附属模组新增的法术则需要增加模组前缀，具体在游戏中执行/cast 命令即可知道该怎么使用

## 示例配置

### 僵尸法师

当目标不是僵尸且距离小于等于12格，并且自身血量大于25%时，施放1-3级火球术。当自身血量低于等于25%时，施放1-2级冰块术进行自保。

```json
{
  "entity_id": "minecraft:zombie",
  "check_interval": 40,
  "spells": [
    {
      "spell_id": "fire_bolt",
      "min_level": 1,
      "max_level": 3,
      "min_cast_time": 100,
      "max_cast_time": 200,
      "conditions": [
        {
          "type": "TARGET_DISTANCE",
          "operator": "LESS_THAN_OR_EQUALS",
          "numeric_value": 12.0
        },
        {
          "type": "TARGET_TYPE",
          "operator": "NOT_EQUALS",
          "value": "minecraft:zombie"
        },
        {
          "type": "HEALTH_PERCENTAGE",
          "operator": "GREATER_THAN",
          "numeric_value": 25.0
        }
      ]
    },
    {
      "spell_id": "ice_block",
      "min_level": 1,
      "max_level": 2,
      "min_cast_time": 300,
      "max_cast_time": 400,
      "conditions": [
        {
          "type": "HEALTH_PERCENTAGE",
          "operator": "LESS_THAN_OR_EQUALS",
          "numeric_value": 25.0
        }
      ]
    }
  ]
}
```

### 骷髅法师

对玩家施放闪电术，在血量低且目标靠近时施放传送术逃离，并在合适距离召唤骷髅支援。

```json
{
  "entity_id": "minecraft:skeleton",
  "check_interval": 30,
  "spells": [
    {
      "spell_id": "lightning_bolt",
      "min_level": 1,
      "max_level": 4,
      "min_cast_time": 80,
      "max_cast_time": 160,
      "conditions": [
        {
          "type": "TARGET_DISTANCE",
          "operator": "LESS_THAN_OR_EQUALS",
          "numeric_value": 16.0
        },
        {
          "type": "TARGET_TYPE",
          "operator": "EQUALS",
          "value": "minecraft:player"
        }
      ]
    },
    {
      "spell_id": "teleport",
      "min_level": 1,
      "max_level": 1,
      "min_cast_time": 200,
      "max_cast_time": 300,
      "conditions": [
        {
          "type": "HEALTH_PERCENTAGE",
          "operator": "LESS_THAN",
          "numeric_value": 30.0
        },
        {
          "type": "TARGET_DISTANCE",
          "operator": "LESS_THAN",
          "numeric_value": 5.0
        }
      ]
    },
    {
      "spell_id": "summon_skeleton",
      "min_level": 1,
      "max_level": 2,
      "min_cast_time": 600,
      "max_cast_time": 800,
      "conditions": [
        {
          "type": "TARGET_DISTANCE",
          "operator": "GREATER_THAN",
          "numeric_value": 5.0
        },
        {
          "type": "TARGET_DISTANCE",
          "operator": "LESS_THAN_OR_EQUALS",
          "numeric_value": 16.0
        }
      ]
    }
  ]
}
```

### 溺尸三叉戟大师

拿着三叉戟的溺尸会释放水系法术，在雨天时还会使用冰霜步，在满月晚上有几率召唤同伴，并为其他溺尸提供水下呼吸法术。

```json
{
  "entity_id": "minecraft:drowned",
  "check_interval": 20,
  "spells": [
    {
      "spell_id": "water_jet",
      "min_level": 2,
      "max_level": 4,
      "min_cast_time": 100,
      "max_cast_time": 200,
      "conditions": [
        {
          "type": "TARGET_DISTANCE",
          "operator": "LESS_THAN_OR_EQUALS",
          "numeric_value": 15.0
        },
        {
          "type": "IS_IN_WATER",
          "operator": "EQUALS",
          "value": "true"
        },
        {
          "type": "HELD_ITEM",
          "operator": "EQUALS",
          "value": "minecraft:trident"
        }
      ]
    },
    {
      "spell_id": "frost_step",
      "min_level": 1,
      "max_level": 3,
      "min_cast_time": 200,
      "max_cast_time": 300,
      "conditions": [
        {
          "type": "HEALTH_PERCENTAGE",
          "operator": "LESS_THAN",
          "numeric_value": 50.0
        },
        {
          "type": "TARGET_DISTANCE",
          "operator": "LESS_THAN",
          "numeric_value": 8.0
        },
        {
          "type": "WEATHER",
          "operator": "EQUALS",
          "value": "rain"
        }
      ]
    },
    {
      "spell_id": "summon_drowned",
      "min_level": 1,
      "max_level": 2,
      "min_cast_time": 400,
      "max_cast_time": 600,
      "conditions": [
        {
          "type": "TARGET_COUNT",
          "operator": "LESS_THAN",
          "numeric_value": 3.0,
          "extra_data": {
            "radius": "24.0",
            "target_type": "minecraft:drowned"
          }
        },
        {
          "type": "MOON_PHASE",
          "operator": "EQUALS",
          "numeric_value": 0.0
        },
        {
          "type": "TIME_OF_DAY",
          "operator": "GREATER_THAN",
          "numeric_value": 13000.0
        },
        {
          "type": "RANDOM_CHANCE",
          "operator": "LESS_THAN",
          "numeric_value": 30.0
        }
      ]
    },
    {
      "spell_id": "water_breathing",
      "min_level": 1,
      "max_level": 1,
      "min_cast_time": 300,
      "max_cast_time": 300,
      "conditions": [
        {
          "type": "TARGET_TYPE",
          "operator": "EQUALS",
          "value": "minecraft:drowned"
        },
        {
          "type": "TARGET_DISTANCE",
          "operator": "LESS_THAN_OR_EQUALS",
          "numeric_value": 12.0
        },
        {
          "type": "STATUS_EFFECT",
          "operator": "NOT_EQUALS",
          "value": "minecraft:water_breathing"
        }
      ]
    }
  ]
}
```

## 贡献

欢迎提交 Issue 和 Pull Request！在提交之前，请确保：

1. 代码符合项目的编码规范
2. 添加了适当的注释和文档
3. 测试了所有新功能
4. 更新了相关文档

## 许可证

本项目采用 MIT 许可证。详见 [LICENSE](LICENSE) 文件。 