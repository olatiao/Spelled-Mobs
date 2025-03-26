# Spelled Mobs (法术生物)

一个基于 Forge 的 Minecraft 模组，让任何生物都能施放法术。需要作为 [Iron's Spells 'n Spellbooks](https://www.curseforge.com/minecraft/mc-mods/irons-spells-n-spellbooks) 的附属模组使用。

## 简介

Spelled Mobs 通过简单的 JSON 配置文件，使游戏中的任何生物（包括原版和其他模组添加的）获得施放法术的能力。生物会根据配置定期检查条件并施放法术，使战斗更加多样化和有挑战性。

## 功能特点

- 任何生物都可以施放来自 Iron's Spells 模组的法术
- 配置文件简单易懂，无需编程即可设置
- 支持设置法术等级、冷却时间、施放几率等参数
- 内置命令系统用于调试和管理
- 多重配置支持，可以为不同生物设置不同法术组合
- 强大的条件系统，允许精细控制法术施放时机（基于血量、距离、天气等）

## 配置说明

### 配置文件位置

配置文件存放在以下位置：
```
.minecraft/config/spelledmobs/entity_spells/
```

模组首次启动时会自动创建此目录并生成几个示例配置文件（zombie.json、skeleton.json、creeper.json）。

### 配置文件格式

配置文件使用JSON格式，示例如下：

```json
{
  "entityId": "minecraft:zombie",
  "checkInterval": 20,
  "spells": [
    {
      "spellId": "irons_spellbooks:fireball",
      "minLevel": 1,
      "maxLevel": 3,
      "minCastTime": 60,
      "maxCastTime": 120,
      "weight": 1,
      "chance": 1.0,
      "conditions": [
        {
          "type": "HEALTH_PERCENTAGE",
          "operator": "GREATER_THAN",
          "numeric_value": 50.0
        },
        {
          "type": "TARGET_DISTANCE",
          "operator": "LESS_THAN",
          "numeric_value": 10.0
        }
      ]
    }
  ]
}
```

### 配置参数说明

#### 顶级参数

| 参数名 | 类型 | 说明 | 默认值 |
|--------|------|------|--------|
| entityId | String | 实体ID，例如 "minecraft:zombie" | 必填 |
| checkInterval | Integer | 检查施法条件的间隔（游戏刻） | 20 |
| spells | Array | 法术列表 | 必填 |

#### 法术参数

| 参数名 | 类型 | 说明 | 默认值 |
|--------|------|------|--------|
| spellId | String | 法术ID，必须包含模组ID，如 "irons_spellbooks:fireball" | 必填 |
| minLevel | Integer | 最小法术等级 | 1 |
| maxLevel | Integer | 最大法术等级 | 1 |
| minCastTime | Integer | 最小施法冷却时间（游戏刻） | 60 |
| maxCastTime | Integer | 最大施法冷却时间（游戏刻） | 200 |
| weight | Integer | 权重，决定被选中几率 | 1 |
| chance | Float | 施法几率（0.0-1.0） | 1.0 |
| conditions | Array | 施法条件列表 | 可选 |

## 命令系统

模组提供以下命令：

- `/spelledmobs debug enable` - 启用调试日志
- `/spelledmobs debug disable` - 禁用调试日志
- `/spelledmobs reload` - 重新加载所有配置
- `/spelledmobs cast <target> <spellid> <level>` - 强制目标施放指定法术
- `/spelledmobs config` - 显示配置目录信息，用于调试

## 示例配置文件

### 僵尸 (zombie.json)

```json
{
  "entityId": "minecraft:zombie",
  "checkInterval": 20,
  "spells": [
    {
      "spellId": "irons_spellbooks:fireball",
      "minLevel": 1,
      "maxLevel": 3,
      "minCastTime": 60,
      "maxCastTime": 120,
      "weight": 1,
      "chance": 1.0,
      "conditions": [
        {
          "type": "HEALTH_PERCENTAGE",
          "operator": "GREATER_THAN",
          "numeric_value": 50.0
        }
      ]
    }
  ]
}
```

### 骷髅 (skeleton.json)

```json
{
  "entityId": "minecraft:skeleton",
  "checkInterval": 20,
  "spells": [
    {
      "spellId": "irons_spellbooks:ice_spike",
      "minLevel": 1,
      "maxLevel": 2,
      "minCastTime": 40,
      "maxCastTime": 80,
      "weight": 1,
      "chance": 1.0,
      "conditions": [
        {
          "type": "TARGET_DISTANCE",
          "operator": "LESS_THAN",
          "numeric_value": 15.0
        }
      ]
    }
  ]
}
```

### 苦力怕 (creeper.json)

```json
{
  "entityId": "minecraft:creeper",
  "checkInterval": 30,
  "spells": [
    {
      "spellId": "irons_spellbooks:lightning_bolt",
      "minLevel": 1,
      "maxLevel": 1,
      "minCastTime": 100,
      "maxCastTime": 200,
      "weight": 1,
      "chance": 0.8,
      "conditions": [
        {
          "type": "WEATHER",
          "operator": "EQUALS",
          "value": "thunder"
        }
      ]
    }
  ]
}
```

## 条件系统

条件系统允许您精细控制生物何时施放法术。可以为每个法术设置多个条件，只有当所有条件都满足时，生物才会尝试施放该法术。

### 条件参数

| 参数名 | 类型 | 说明 | 默认值 |
|--------|------|------|--------|
| type | String | 条件类型（见下方类型列表） | 必填 |
| operator | String | 比较操作符（见下方操作符列表） | EQUALS |
| value | String | 字符串值（用于字符串比较） | 根据条件类型 |
| numeric_value | Double | 数值（用于数值比较） | 根据条件类型 |
| invert | Boolean | 是否反转结果 | false |
| extra_data | Object | 额外数据，用于特定条件类型 | 可选 |

### 条件类型

#### 基础条件
- `HEALTH_PERCENTAGE` - 检查施法者当前血量百分比（0-100）
- `TARGET_DISTANCE` - 检查与目标的距离（方块数）
- `TARGET_TYPE` - 检查目标实体类型（例如 "minecraft:player"）

#### 环境条件
- `WEATHER` - 检查当前天气状态（"clear"、"rain"、"thunder"）
- `TIME_OF_DAY` - 检查世界时间（0-24000）

#### 高级条件
- `RANDOM_CHANCE` - 检查随机几率（0-100），与操作符结合使用可以创建概率触发条件

### 操作符

- `EQUALS` - 等于
- `NOT_EQUALS` - 不等于
- `GREATER_THAN` - 大于
- `LESS_THAN` - 小于
- `GREATER_THAN_OR_EQUALS` - 大于等于
- `LESS_THAN_OR_EQUALS` - 小于等于
- `CONTAINS` - 包含（仅用于字符串）
- `STARTS_WITH` - 以...开始（仅用于字符串）
- `ENDS_WITH` - 以...结束（仅用于字符串）

### 条件示例

#### 当生命值高于50%时施放火球
```json
{
  "type": "HEALTH_PERCENTAGE",
  "operator": "GREATER_THAN",
  "numeric_value": 50.0
}
```

#### 当目标距离小于10方块时施放法术
```json
{
  "type": "TARGET_DISTANCE",
  "operator": "LESS_THAN",
  "numeric_value": 10.0
}
```

#### 当目标是玩家时施放法术
```json
{
  "type": "TARGET_TYPE",
  "operator": "EQUALS",
  "value": "minecraft:player"
}
```

#### 只在雷暴天气施放闪电法术
```json
{
  "type": "WEATHER",
  "operator": "EQUALS",
  "value": "thunder"
}
```

#### 只在夜晚施放法术（13000-24000为夜晚）
```json
{
  "type": "TIME_OF_DAY",
  "operator": "GREATER_THAN",
  "numeric_value": 13000
}
```

#### 有20%的几率施放法术
```json
{
  "type": "RANDOM_CHANCE",
  "operator": "LESS_THAN",
  "numeric_value": 20.0
}
```

## 计划实现功能

以下功能计划在未来版本中实现：

### 目标优先级系统

未来将添加`targetPriority`字段，允许更精细地控制生物选择施法目标的优先级：

```json
{
  "entityId": "minecraft:zombie",
  "checkInterval": 20,
  "targetPriority": [
    {
      "type": "minecraft:player",
      "priority": 10
    },
    {
      "type": "minecraft:villager",
      "priority": 5
    }
  ],
  "spells": [
    // ... 法术配置 ...
  ]
}
```

这将允许生物根据不同目标类型的优先级选择攻击对象，而不仅仅是默认的目标选择逻辑。

### 计划中的高级条件类型

- `TARGET_COUNT` - 检查附近特定类型目标的数量
- `MOON_PHASE` - 检查当前月相（对狼人类型生物特别有用）
- `BIOME_TYPE` - 检查当前生物群系类型

## 常见问题

### 找不到配置文件夹？

模组应该在启动时自动创建配置文件夹。如果没有自动创建：

1. 确保使用了正确的Forge版本和Iron's Spells模组
2. 尝试启动游戏并进入世界至少一次
3. 如果仍未创建，您可以手动创建目录 `.minecraft/config/spelledmobs/entity_spells/`
4. 在游戏中使用 `/spelledmobs reload` 命令重新加载配置
5. 使用 `/spelledmobs config` 命令查看配置目录信息和状态

### 条件系统不生效？

1. 确保条件格式正确，遵循上述示例
2. 使用 `/spelledmobs debug enable` 启用调试日志查看详细信息
3. 检查条件类型和操作符拼写是否正确
4. 对于数值条件，确保使用 `numeric_value` 字段
5. 对于字符串条件，确保使用 `value` 字段

### 法术ID参考

需要使用完整的法术ID（包含模组前缀）。以下是一些常用法术：

- `irons_spellbooks:fireball` - 火球术
- `irons_spellbooks:ice_spike` - 冰刺术
- `irons_spellbooks:lightning_bolt` - 闪电术
- `irons_spellbooks:magic_missile` - 魔法飞弹
- `irons_spellbooks:fire_breath` - 火焰吐息
- `irons_spellbooks:frost_breath` - 霜冻吐息

完整法术列表请参考Iron's Spells 'n Spellbooks模组文档。

## 许可证

本项目采用 MIT 许可证。详见 [LICENSE](LICENSE) 文件。 