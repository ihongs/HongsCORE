# HongsCORE 开发文档

- 文档版本: 20260425
- 编写作者: 黄弘(Hongs)
- 技术支持: kevin.hongs@gmail.com

## 表单配置

定义数据结构, 用于存储和检索数据。

### 字段属性

#### 基本属性
- `name`：字段名称
- `text`：字段标签
- `type`：字段类型
- `required`：是否必填必选（yes/no）
- `repeated`：是否可多个值（yes/no）

### 字段类型

#### string 类型
- `string`：普通字符串
- `text`：单行文本
- `textarea`：多行文本
- `textview`：富文本
- `email`：邮箱
- `url`：网址
- `tel`：电话号码
- `sms`：手机号码
- `search`：搜索字段（Lucene 分词查询）
- `stored`：存储字段（Lucene 仅存不查）

#### number 类型
- `number`：数字输入
- `range`：范围输入
- `color`：颜色选择
- `sorted`：排序字段（Lucene 仅能排序）

#### hidden 类型
- `hidden`：隐藏字段，内部使用

由 type 参数(非属性)决定具体类型, 默认 string
- `string`: 字符串
- `number`: 数字(同`double`)
- `double`: 双精度
- `float`: 浮点数
- `long`: 长整数
- `int`: 整数

#### enum 类型
- `enum`：枚举选择
- `type`：enum 的别名
- `select`：下拉选择
- `switch`：开关选择
- `check`：复选框
- `radio`：单选框

由 type 参数(非属性)决定具体类型, 默认 string
- `string`: 字符串
- `number`: 数字(同`double`)
- `double`: 双精度
- `float`: 浮点数
- `long`: 长整数
- `int`: 整数

#### date 类型
- `date`：日期选择
- `time`：时间选择
- `datetime`：日期时间选择

由 type 参数(非属性)决定具体类型
- `time`：时间戳
- `timestamp`：时间戳(精确到秒)
- `date`：Date对象
- `datestamp`：Date对象(精确到秒)

#### file 类型
- `file`：文件上传
- `path`：file 的别名
- `image`：图片上传
- `video`：视频上传
- `audio`：音频上传

#### fork 类型
- `fork`：关联选择
- `pick`：fork 的别名

#### form 类型
- `form`：子表单
- `part`：form 的别名

### 字段参数

#### 通用参数
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

#### 文本参数
- `info-text`：字段标签（详情页）
- `info-hint`：字段说明（详情页）
- `form-text`：字段标签（表单页）
- `form-hint`：字段提示（表单页）
- `form-hold`：字段占位（表单页）

#### string 类型参数
- `strip`：文本清理（trim, cros, tags, ends, gaps, unis）
- `substr`：截取长度，格式 `offset,length`
- `pattern`：正则表达式校验
- `minlength`：最短长度
- `maxlength`：最长长度

#### number 类型参数
- `type`：数字类型（int, long, float, double）
- `min`：最小值
- `max`：最大值
- `scale`：小数位数

#### date 类型参数
- `type`：日期类型（time, timestamp, date, datestamp）
- `format`：日期格式（同 java 的 SimpleDateFormat）
- `offset`：偏移时间（毫秒，可配合精度解决时区问题）
- `min`：最小时间（可用 +- 前缀表示当前时间偏移）
- `max`：最大时间（可用 +- 前缀表示当前时间偏移）

#### enum 类型参数
- `enum`：枚举名称, 默认同字段名称
- `conf`：配置名称, 默认为当前配置

#### file 类型参数
- `path`：上传目录前缀（可用变量 ${BASE_PATH} 等）
- `href`：上传链接前缀（可用变量 ${SERV_PATH} 等）
- `temp`：临时目录
- `size`：大小限制（字节）
- `accept`：类型许可表（逗号分隔，Mime-Type 或 .extension）
- `reject`：类型禁止表（逗号分隔，Mime-Type 或 .extension）
- `naming`：文件命名算法（MD5, SHA-1, SHA-256, keep）

图片特有（需加校验规则 rule="Thumb"）：
- `thumb-kind`：缩略图格式（如 jpg）
- `thumb-size`：缩略尺寸（如 80*40:_lg, 60*30:_md）, :_lg 为指定名称后缀 _lg, 单一尺寸可省略
- `thumb-mode`：处理模式（pick 截取, keep 保留, test 检查）
- `thumb-index`：返回索引（默认为 0）
- `thumb-color`：背景颜色（R,G,B[,A]）
- `thumb-align`：裁剪位置, 类似 css background-position 属性, 默认 `center`:
    - `center` 或 `center-center`：居中
    - `top` 或 `top-center`：顶部
    - `left` 或 `center-left`：左侧
    - `right` 或 `center-right`：右侧
    - `bottom` 或 `bottom-center`：底部
    - `top-left`：左上角
    - `top-right`：右上角
    - `bottom-left`：左下角
    - `bottom-right`：右下角

#### fork 类型参数
- `form`：关联表单名
- `conf`：配置名
- `data-at`：关联动作名
- `data-vk`：关联取值键
- `pass-id`：跳过像 ID 的值

#### form 类型参数
- `form`：子表单名, 默认同字段名称
- `conf`：配置名称, 默认为当前配置

#### repeated 参数
- `diverse`：是否排重
  - `false/no`：默认，不排重，对应 List
  - `true/yes/set`：排重，对应 LinkedHashSet
  - `hashset`：排重，对应 HashSet
  - `treeset`：排重，对应 TreeSet
  - `descset`：排重，逆序 TreeSet
- `minrepeat`：最小数量
- `maxrepeat`：最大数量

### 控制设置

#### 功能控制
- `listable`：是否在列表中显示（yes/no）
- `sortable`：是否可排序（yes/no）
- `filtable`：是否可筛选（yes/no）
- `statable`：是否可统计（yes/no）
- `srchable`：是否可搜索（yes/no）
- `unstored`：是否不保存（yes/no，常用于虚拟字段）

#### 显示控制
- `readonly`：是否只读（yes/no，字段不可编辑）
- `disabled`：是否禁用（yes/no，字段内部操控）
- `unreadable`：是否不出现在详情页（yes/no）
- `unwritable`：是否不出现在表单页（yes/no）

### 配置示例

#### 后台表单（WEB-INF/etc/centra/data/test1.form.xml）
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
    <!-- 关联表单 -->
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

#### 前台表单（WEB-INF/etc/centre/data/test1.form.xml）
```xml
<?xml version="1.0" encoding="UTF-8"?>
<root>
    <!-- 前台表单继承自后台表单 -->
    <form name="test1" extend="centra/data/test1:test1">
    </form>
</root>
```

## 导航配置

定义导航菜单及控制权限。

- menu 属性:
    - `text`: 菜单标签
    - `href`: 菜单链接
    - `hrel`: 资源链接
- role 属性:
    - `name`: 角色或分组名称
- action: 动作路径, 带扩展名
- depend: 依赖定义, role名称

menu 的 href 和 hrel 以 '!' 开头表示忽略，不显示在导航菜单中。有几个特殊的 hrel 标识:
- `!MENU`: 表示包含下面的一组子菜单
- `!MARK`: 表示标记后面的一系列菜单
- `!HDIE`: 普通隐藏菜单

#### 后台举例：`centra/data/test1.navi.xml`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<root>
    <rsname>#centra</rsname>
    <menu text="测试1" href="centra/data/test1/">
        <role text="查询" name="centra/data/test1/search">
            <action>centra/data/test1/search.act</action>
            <action>centra/data/test1/recite.act</action><!-- 详情 -->
            <action>centra/data/test1/recipe.act</action><!-- 选项 -->
            <action>centra/data/test1/acount.act</action><!-- 统计 -->
            <action>centra/data/test1/assort.act</action><!-- 分组统计 -->
            <depend>centra</depend>
        </role>
        <role text="创建" name="centra/data/test1/create">
            <action>centra/data/test1/create.act</action>
            <depend>centra/data/test1/search</depend>
        </role>
        <role text="更新" name="centra/data/test1/update">
            <action>centra/data/test1/update.act</action>
            <depend>centra/data/test1/search</depend>
        </role>
        <role text="删除" name="centra/data/test1/delete">
            <action>centra/data/test1/delete.act</action>
            <depend>centra/data/test1/search</depend>
        </role>
        <role text="回看" name="centra/data/test1/reveal">
            <action>centra/data/test1/reveal.act</action>
            <depend>centra/data/test1/search</depend>
        </role>
        <role text="恢复" name="centra/data/test1/revert">
            <action>centra/data/test1/revert.act</action>
            <depend>centra/data/test1/update</depend>
        </role>
    </menu>
</root>
```

#### 前台举例：`centre/data/test1.navi.xml`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<root>
    <rsname>#centre</rsname>
    <menu text="测试1" href="centre/data/test1/">
        <role name="centre">
            <action>centre/data/test1/create.act</action>
            <action>centre/data/test1/update.act</action>
            <action>centre/data/test1/delete.act</action>
        </role>
        <role name="_deny_">
            <action>centre/data/test1/assort.act</action><!-- 分组统计, 资源消耗较大, 非必要不开启 -->
        </role>
    </menu>
</root>
```

#### 汇入上级

与 form 对应, 还需在 `centra/cust.navi.xml` 和 `centre/cust.navi.xml` 中 import 对应的 `.navi.xml` 文件。

```xml
<?xml version="1.0" encoding="UTF-8"?>
  <rsname>#centra</rsname>
  <import name="centra/data/test1.navi.xml"></import>
<root>
```
