---
name: config-form
description: 配置表单(form)，创建 .form.xml 文件并定义表单结构。当用户需要配置表单时触发。
version: 1.0.0
---

# 配置表单

## 角色定义
你是一名资深的软件开发工程师，擅长表单配置和数据管理。

## 核心指令
请严格按照以下步骤执行任务：
1. **分析意图**：确认用户需要配置的表单名称和字段
2. **创建目录**：在项目的配置目录中创建表单配置文件
3. **配置表单**：创建 .form.xml 文件，定义表单结构和字段属性
4. **验证配置**：检查配置文件格式是否正确
5. **测试验证**：在项目中测试表单配置是否生效

## 配置文件位置
配置文件有三个位置，优先级从高到低：

1. **WEB-INF/etc 目录**：
   - `src/main/webapp/WEB-INF/etc/centra/data`：针对后台，不打包，适合一般性配置和可能修改的配置
   - `src/main/webapp/WEB-INF/etc/centre/data`：针对前台，不打包，适合一般性配置和可能修改的配置

2. **resources 目录**：
   - `src/main/resources/centra/data`：针对后台，会打包进 jar，适合不变的配置
   - `src/main/resources/centre/data`：针对前台，会打包进 jar，适合不变的配置

3. **resources/io/github/ihongs/config 目录**：
   - 不再建立子目录，这是放全局配置的地方，对应 WEB-INF/etc

**优先放在 WEB-INF/etc 目录**，因为这些配置不打包，方便后期修改。

**注意**：`centra/data` 和 `centre/data` 会自动暴露对应的接口，如 `centra/data/xxx/search.act`。

## 表单配置示例

### 后台表单（WEB-INF/etc/centra/data/test1.form.xml）
```xml
<?xml version="1.0" encoding="UTF-8"?>
<root>
    <!-- 表单定义 -->
    <form name="test1">
        <field name="id" text="ID" type="hidden">
            <param name="default">@id</param>
            <param name="deforce">create</param>
        </field>
        <field name="name" text="名称" type="text" required="yes">
            <param name="listable">yes</param>
            <param name="filtable">yes</param>
            <param name="minlength">2</param>
            <param name="maxlength">50</param>
        </field>
        <field name="file" text="附件" type="file">
            <param name="listable">yes</param>
            <param name="path">static/upload/data/test1/file</param>
            <param name="href">static/upload/data/test1/file</param>
        </field>
        <field name="icon" text="图标" type="image" rule="Thumb">
            <param name="listable">yes</param>
            <param name="path">static/upload/data/test1/icon</param>
            <param name="href">static/upload/data/test1/icon</param>
            <param name="accept">image/*</param>
            <param name="thumb-kind">png</param>
            <param name="thumb-size">300*300</param>
            <param name="thumb-mode">pick</param>
        </field>
        <field name="test2_id" text="关联表单" type="fork">
            <param name="data-at">centra/data/test1/test2/search</param> <!-- 关联接口（内部 action 路径，不带 .act, 可加参数 ?xxx=xxx） -->
            <param name="data-al">centra/data/test1/test2/pick.html</param> <!-- 选择页面链接 -->
            <param name="data-rl">centra/data/test1/test2/info.html</param> <!-- 详情页面链接 -->
            <param name="data-ln">test2</param> <!-- 缺省情况，字段名后缀 '_id' 的去掉 '_id'，否则为 '字段名_fork' -->
            <param name="data-vk">id</param>
            <param name="data-tk">name</param>
        </field>
        <field name="state" text="状态" type="select">
            <param name="listable">yes</param>
            <param name="filtable">yes</param>
            <param name="statable">yes</param>
            <param name="enum">test1-state</param>
            <param name="default">1</param>
        </field>
        <field name="ctime" text="创建时间" type="datetime">
            <param name="listable">yes</param>
            <param name="filtable">yes</param>
            <param name="type">timestamp</param>
            <param name="default">@now</param>
            <param name="deforce">create</param>
        </field>
        <field name="mtime" text="更新时间" type="datetime">
            <param name="listable">yes</param>
            <param name="filtable">yes</param>
            <param name="type">timestamp</param>
            <param name="default">@now</param>
            <param name="deforce">always</param>
        </field>
    </form>
    <!-- 子表单 -->
    <form name="test2">
        <field name="id" text="ID" type="hidden">
            <param name="default">@id</param>
        </field>
        <field name="name" text="名称" type="text" required="yes">
            <param name="listable">yes</param>
            <param name="filtable">yes</param>
        </field>
    </form>
    <!-- 枚举定义 -->
    <enum name="test1-state">
        <value code="1">启用</value>
        <value code="0">禁用</value>
    </enum>
</root>
```

### 前台表单（WEB-INF/etc/centre/data/test1.form.xml）
```xml
<?xml version="1.0" encoding="UTF-8"?>
<root>
    <!-- 前台表单继承自后台表单 -->
    <form name="test1" extend="centra/data/test1:test1">
    </form>
</root>
```

## 字段属性

### 基本属性
- `name`：字段名称
- `text`：字段标签
- `type`：字段类型
- `required`：是否必填（yes/no）
- `repeated`：是否可重复（yes/no）

## 字段类型

### string 类型
- `string`：普通字符串
- `text`：文本输入框
- `textarea`：多行文本框
- `textview`：文本显示
- `email`：邮箱输入
- `url`：网址输入
- `tel`：电话输入
- `sms`：短信输入
- `search`：搜索输入（Lucene 分词查询）
- `stored`：存储字段（Lucene 仅存不查）
- `hidden`：隐藏字段

### number 类型
- `number`：数字输入
- `range`：范围输入
- `color`：颜色选择
- `sorted`：排序字段（Lucene 仅能排序）

### date 类型
- `date`：日期选择
- `time`：时间选择
- `datetime`：日期时间选择

### enum 类型
- `enum`：枚举选择
- `type`：enum 的别名
- `select`：下拉选择
- `switch`：开关选择
- `check`：复选框
- `radio`：单选框

### file 类型
- `file`：文件上传
- `path`：文件路径
- `image`：图片上传
- `video`：视频上传
- `audio`：音频上传

### fork 类型
- `fork`：关联选择
- `pick`：fork 的别名

### form 类型
- `form`：子表单
- `part`：form 的别名

## 字段参数

### 通用参数
- `default`：默认值
  - `@id`：新取 ID
  - `@uid`：用户 ID
  - `@now`：当前时间
  - `@now+偏移`：当前时间加偏移毫秒
  - `@session.属性`：会话属性
  - `@context.属性`：应用属性
  - `@merge:${字段1} ${字段2}`：合并字段值
- `deforce`：强制写时机
  - `create`：仅创建时
  - `update`：仅更新时
  - `always`：任何时候
  - `blanks`：空串存 null

### 文本参数
- `info-text`：字段标签（详情页）
- `info-hint`：字段说明（详情页）
- `form-text`：字段标签（表单页）
- `form-hint`：字段提示（表单页）
- `form-hold`：字段占位（表单页）

### string 类型参数
- `strip`：文本清理（trim, cros, tags, ends, gaps, unis）
- `substr`：截取长度，格式 `[offset, length]`
- `pattern`：正则表达式校验
- `minlength`：最短长度
- `maxlength`：最长长度

### number 类型参数
- `type`：数字类型（byte, short, int, long, float, double）
- `min`：最小值
- `max`：最大值
- `scale`：小数位数

### date 类型参数
- `type`：日期类型（date, time, datestamp, timestamp）
- `format`：日期格式（同 java 的 DateTimeFormatter）
- `offset`：偏移时间（毫秒，可配合精度解决时区问题）
- `min`：最小时间（可用 +- 前缀表示当前时间偏移）
- `max`：最大时间（可用 +- 前缀表示当前时间偏移）

### enum 类型参数
- `enum`：枚举名称
- `conf`：配置名

### file 类型参数
- `path`：上传目录前缀（可用变量 ${BASE_PATH} 等）
- `href`：上传链接前缀（可用变量 ${SERV_PATH} 等）
- `temp`：临时目录
- `size`：大小限制（字节）
- `accept`：类型许可表（逗号分隔，Mime-Type 或 .extension）
- `reject`：类型禁止表（逗号分隔，Mime-Type 或 .extension）
- `naming`：文件命名算法（MD5, SHA-1, SHA-256, keep）
- 图片特有：
  - `thumb-kind`：缩略图格式（如 jpg）
  - `thumb-size`：缩略尺寸（如 80*40:_lg, 60*30:_md）
  - `thumb-mode`：处理模式（pick 截取, keep 保留, test 检查）
  - `thumb-index`：返回索引（默认为 0）
  - `thumb-color`：背景颜色（R,G,B[,A]）
  - `thumb-align`：停靠位置（9 宫格式）

### fork 类型参数
- `form`：关联表单名
- `conf`：配置名
- `data-at`：关联动作名
- `data-vk`：关联取值键
- `pass-id`：跳过像 ID 的值

### form 类型参数
- `form`：子表单名
- `conf`：配置名

### repeated 参数
- `diverse`：是否排重
  - `false/no`：默认，不排重，对应 List
  - `true/yes/set`：排重，对应 LinkedHashSet
  - `hashset`：排重，对应 HashSet
  - `treeset`：排重，对应 TreeSet
  - `descset`：排重，逆序 TreeSet
- `minrepeat`：最小数量
- `maxrepeat`：最大数量

## 控制设置

### 功能控制
- `listable`：是否在列表中显示（yes/no）
- `sortable`：是否可排序（yes/no）
- `filtable`：是否可筛选（yes/no）
- `statable`：是否可统计（yes/no）
- `srchable`：是否可模糊匹配（yes/no）
- `rschable`：是否可全局搜索（yes/no）
- `unstored`：是否不保存（yes/no，常用于虚拟字段）

### 显示控制
- `readonly`：是否只读（yes/no，字段不可编辑）
- `disabled`：是否禁用（yes/no，字段内部操控）
- `unreadable`：是否不出现在详情页（yes/no）
- `unwritable`：是否不出现在表单页（yes/no）

## 执行步骤

### 1. 创建目录结构
**Windows 命令**：
- 后台目录：`New-Item -ItemType Directory -Path "项目目录/src/main/webapp/WEB-INF/etc/centra/data" -Force`
- 前台目录：`New-Item -ItemType Directory -Path "项目目录/src/main/webapp/WEB-INF/etc/centre/data" -Force`

**Linux 命令**：
- 后台目录：`mkdir -p 项目目录/src/main/webapp/WEB-INF/etc/centra/data`
- 前台目录：`mkdir -p 项目目录/src/main/webapp/WEB-INF/etc/centre/data`

### 2. 创建表单配置文件
在对应目录创建 `.form.xml` 文件，根据上述示例配置表单结构。

### 3. 验证配置
- 确保 XML 格式正确
- 确保表单和字段名称符合规范
- 确保字段类型和参数设置正确
- 确保前台表单正确继承后台表单

### 4. 测试验证
- 构建项目：`mvn clean package`
- 启动服务：`hdo.cmd server start 8080 --DEBUG 6`
- 访问表单页面，验证表单是否正确显示

## 注意事项
- 需要确保项目目录路径正确
- 表单名称应该与业务逻辑对应
- 字段类型和参数设置应该根据业务需求进行调整
- 枚举定义应该与字段使用对应
- 默认值和强制规则应该根据业务逻辑设置
- 优先使用 `src/main/webapp/WEB-INF/etc` 目录，因为这些配置不打包，方便后期修改
- `centra/data` 和 `centre/data` 会自动暴露对应的接口，如 `centra/data/xxx/search.act`
- 前台表单可以通过 `extend` 属性继承后台表单，减少重复配置
- 前台表单可以覆盖后台表单的字段定义，也可以添加前台特有的字段