# HongsCORE 开发文档

- 文档版本: 20260425
- 编写作者: 黄弘(Hongs)
- 技术支持: kevin.hongs@gmail.com

## 文件体系

### 目录结构

- bin               运维脚本
- lib               运行包库(启动时可指定 -classpath)
- etc               配置资源(启动时可指定 --confpath)
- var               数据文件(启动时可指定 --datapath)
    - log           运行日志(启动时可指定 -Dlogs.dir)
    - tmp           临时文件(启动时可指定 -Dtmps.dir)
    - serial        序列缓存数据文件目录
    - server        Jetty JSP 编译及会话数据存放目录
    - sqlite        Sqlite本地数据库目录
    - lucene        Lucene本地索引库目录
- web               前端文件(启动时可指定 --basepath,--basehref)
    - centra        系统管理区域
    - centre        应用处理区域
    - common        通用配置数据
    - public        公共资源
    - static        静态资源
        - assets    前端常用组件
        - addons    前端扩展组件
        - upload    默认上传目录

这是 hongs-web 和 hongs-wms 的目录结构, 由 pom.xml 中的 maven-antrun-plugin 构建. 如不做转移, 则是典型的 java webapp 结构，bin/lib/etc/var 都在 WEB-INF 下面.

### 类库结构

- `io.github.ihongs`            核心
- `io.github.ihongs.action`     动作支持
- `io.github.ihongs.combat`     命令支持
- `io.github.ihongs.db`         关系数据模型
- `io.github.ihongs.dh`         数据仓库组件
- `io.github.ihongs.jsp`        JSP 工具
- `io.github.ihongs.util`       辅助工具
- `io.github.ihongs.serv`       服务组件

以上仅列举了主要的包, 更多框架信息请参考源码文档. 另外在类的命名上遵循少许规则, 如 IXxx 为接口, JXxx 为接口默认的实现或抽象. 自行扩展定制的无需遵守这些规则, 我的对齐强迫症而已.

### 路径映射

- `foo.bar.xxx`         对应标记 @Combat("fop.bar") 的类下 @Combat("xxx") 的方法
- `foo.bar`             对应标记 @Combat("foo.bar") 的类下 @Combat("__main__") 的方法
- `foo/bar/xxx.act`     对应标记 @Action("foo/bar") 的类下 @Action("xxx") 的方法
- `foo/bar.act`         对应标记 @Action("foo/bar") 的类下 @Action("__main__") 的方法
- `api/foo/bar`         对应标记 @Action("foo/bar") 的类下 @Action("search,create,update,delete") 的方法, 这四种动作分别对应 HTTP METHOD: GET,POST,PUT,DELETE
- `common/auth/name.js` 读取 WBE-INF/etc/name.menu.xml 中 actions+session 的组合
- `common/conf/name.js` 读取 WEB-INF/etc/name.properties 中 fore.xxxx. 开头的配置
- `common/lang/name.js` 读取 WEB-INF/etc/name_xx_XX.properties 中 fore.xxxx. 开头的配置

action 和 combat 使用 @Action 和 @Combat 注解来设置访问路径, 如果不指定则用类,方法名作为路径; 请在 etc/defines.properties 中设置 mount.serv 为 Action,Combat 类, 或 xxx.foo.* 告知该包下存在 Action,Combat 类, 多个类/包用";"分隔.
common 下那几个 `.js` 路径, 将扩展名 `.js` 换成 `.json` 即可得到 JSON 格式的数据; 语言配置可在 name 后加语言区域标识, 如 example_zh_CN.js 为获取 example 的中文大陆简体的 js 格式的语言配置.

### 请求规则

支持 Content-Type 为 `application/x-www-form-urlencoded`, `multipart/form-data` 和 `application/json` 的请求, 组成结构为:
```
f1=1&f2.eq=2&f3.in.=30&f3.in.=31&t1.f4.at=18,55&ob=f5!+f6&wd=Hello+world
```

在 URL 中 + 表示空格. 可兼容 PHP 的格式:
```
f1=1&f2[eq]=2&f3[in][]=30&f3[in][]=31&t1[f4][at]=18,55&ob=f5!+f6&wd=Hello+world
```

此 URL 查询串将转为类似下面 JSON 的结构:
```json
{
    "f1": 1,
    "f2": {
        "eq": 2
    },
    "f3": {
        "in": [
            30,
            31
        ]
    },
    "t1": {
        "f4": {
            "at": "18,55"
        }
    },
    "ob": "f5! f6",
    "wd": "Hello world"
}
```

其中 eq 这样的标识为过滤操作符, 其含义为:

- `eq`  等于
- `ne`  不等于
- `lt`  小于
- `le`  小于或等于
- `gt`  大于
- `ge`  大于或等于
- `at`  区间
- `is`  空或非空
- `in`  包含
- `no`  不包含
- `on`  全包含
- `se`  搜索
- `ns`  搜索排除
- `co`  匹配
- `nc`  匹配排除
- `up`  加权

其中 wd 这样的参数有特定的意义, 列举如下:

- `id`  主键值
- `wd`  关键词
- `pn`  当前页码(page num)
- `rn`  额定行数(rows num)
- `cb`  响应回调(callback)
- `ob`  排序字段(order by)
- `rb`  响应字段(reply with)
- `ab`  应用处理(apply with)
- `or`  或查询  (or )
- `ar`  多组    (and)
- `nr`  否定    (not)

其中 ob,rb,ab 的值还支持逗号和空格分隔，如 `ob=abc,def` 等同于 `ob.=abc&ob.=def` 等同于 `{"ob":["abc","def"]}`. 请避免将这些参数作为您的字段名, 除 id 外字段名长度大于 2 即可.

### 响应结构

默认返回 JSON 数据结构:
```json
{
    "ok": true, // true成功 false失败,
    "ern": "错误代号",
    "err": "错误信息",
    "msg": "响应消息",
    // ...
}
```

ern 为 Er301,Er302,Er401,Er402,Er403,Er404 时, err 如有值为"Goto URL"则跳转到 URL. 其他数据通常有:
```json
{
    // 分页信息, 在 search 动作返回
    "page": {
        "state": 1, // 状态: 1 正常, 0 缺失,
        "count": 100, // 总行数,
        // ...
    },

    // 列表信息, 在 search 动作返回
    "list": [
        {
            "字段": "取值",
            // ...
        },
        // ...
    ],

    // 单元信息, 在 search 动作返回
    "info": {
        "字段": "取值",
        // ...
    },

    // 枚举信息, 在 search,select 动作返回
    "enfo": {
        "字段": [
            ["取值", "名称"],
            // ...
        ],
        // ...
    },

    // 新建编号, 在 create 动作返回
    "id": "新的编号",

    // 影响数量, 在 update,delete 动作返回; 验证结果, 在 unique,exists 动作返回
    "rn": 10, // 影响行数, 1 真, 0 假
}
```

在调用 .api 时, 可将所有请求数据采用 JSON 或 URLEncode 编码放入 `__data__` 参数传递; 加请求参数 `__mode__=wrap` 可将全部返回数据放入 data 键下; 加请求参数 `__mode__=scok` 则即使发生异常也返回 200 状态; 加请求参数 `__mode__=RULES` 启用数据转换规则. 多个可用逗号分隔, 另附 RULES 参数:

- `all2str`     全部转为字串
- `num2str`     数字转为字串
- `null2str`    空转为空字串
- `bool2str`    true转为字串1, false转为空串
- `bool2num`    true转为数字1, false转为数字0
- `date2sec`    转为时间戳(秒)
- `date2mic`    转为时间戳(毫秒)
- `flat.map`    拉平 map 层级, 连接符为 `.`
- `flat_map`    拉平 map 层级, 连接符为 `_`

`dete2mic` 或 `date2sec` 搭配 `all2str` 则将转换后的时间戳数字再转为字符串; 如果仅指定 `all2str` 则时间/日期会转为"年-月-日"格式的字符串; `flat.map` 或 `flat_map` 可将数据层级拉平, 如 `{user: {name: "Kevin"}}` 可拉平为 `{"user.name": "Kevin"}` 或 `{"user_name": "Kevin"}`. 多个规则可以用逗号、分号、空格或加号分隔.

另有参数 `cb=回调名` 可用来封装为 JSONP 方式以供跨域调用, 为遵守普遍规则, 此参数默认叫 callback, 如果 callback 对应字段或有其他用途, 则总是能用且优先可用 cb 参数.

## 表单配置

定义数据结构, 用于存储和检索数据. 结构类似于 Protobuf: form 类似 Protobuf 的 message; field 有 required/repeated 对应 Protobuf message 下条目的 required/optional/repeated; type 对应条目的类型, 贴近 HTML 控件和数据库字段类型.

每个表单(form)可以有一个 name="@" 的字段, 其名称即表单的名称, 其配置即表单的配置, 默认不设置 name 视为此字段, 同样也有一些控制视图的参数:
- `xxxxable`: 如同 field 中的同名参数, 但值为字段名称列表, * 表全部字段, ? 按类型判别
- `callable`: 告知 XxxAction 脚手架动作类, 限定可执行方法, 缺省全部允许, 为空全部禁止

另, 枚举(enum)可以有一个 code="-" 的条目, 该条目用作表示未知, 当出现未登记的值时, 可视作其他.

字段名推荐:

- `id`      主键, CHAR(16)
- `pid`     父键, CHAR(16)
- `xx_id`   外键, CHAR(16), xx为关联表缩写
- `mtime`   修改时间, DATETIME,TIMESTAMP,BIGINT,INTEGER
- `ctime`   创建时间, DATETIME,TIMESTAMP,BIGINT,INTEGER
- `etime`   截止时间, DATETIME,TIMESTAMP,BIGINT,INTEGER, 用于仓库层记录数据的失效时间, 避免用作应用层的业务到期时间
- `state`   状态标识, SMALLINT,TINYINT, 1 正常, 0 删除, 可增设其他状态值; 如果删除后需要恢复之前的状态, 请新增其他状态字段, 或使用负值标识删除.

因字段名可用于 URL 中作为过滤参数, 而部分参数已有特殊含义, 字段取名时请尽量避开这些名称: pn,qn,rn,wd,ob,rb,ab,or,ar, 比较简单的办法是避免取 2 个字母的字段名. 另, 在 Cnst 类和配置文件中可以重新定义这些名称, 但并不建议修改(我信奉少量的约定胜于过多的配置).

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

#### 页面类型
- `legend`: 分栏标题，用于分隔标识
- `figure`: 附加板块，用于附加内容

由 info-text 或 form-text 提供内容，格式为 html

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

#### enum 类型参数
- `enum`：枚举名称, 默认同字段名称
- `conf`：配置名称, 默认为当前配置

#### form 类型参数
- `form`: 表单名称, 默认同字段名称
- `conf`：配置名称, 默认为当前配置

#### fork 类型参数
- `data-at`：关联动作名, 内部 action 路径，不带 .act, 可加参数 ?xxx=xxx
- `data-al`: 关联选择页
- `data-rl`: 关联信息页
- `data-vk`：关联取值键
- `data-tk`：关联标题键
- `data-ln`: 关联返回名, 缺省情况, 字段名后缀 '_id' 的去掉 '_id'，否则为 '字段名_fork'
- `pass-id`：跳过像 ID 的值

#### repeated 参数
- `diverse`：是否排重, 取值:
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
- `unstored`：是否不保存（yes/no，用于虚拟字段）
- `inviable`: 物理不可见（yes/no，可查询或排序, 但不可以读取, 暂为 Lucene 特有）
- `invisble`: 逻辑不可见（yes/no，可查询或排序, 但不可以读取, 暂为 Lucene 特有）

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
            <param name="data-at">centra/data/test1/test2/search</param>
            <param name="data-al">centra/data/test1/test2/pick.html</param>
            <param name="data-rl">centra/data/test1/test2/info.html</param>
            <param name="data-ln">test2</param>
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

- `menu` 标签属性:
    - `text`: 菜单标签
    - `href`: 菜单链接
    - `hrel`: 资源链接
    - `role` 标签属性:
        - `name`: 角色或分组名称
        - `action` 标签: 动作路径, 带扩展名
        - `depend` 标签: 依赖定义, role名称
- `rsname` 标签: 标示权限会话的名称或单例类
- `import` 标签: 引入下级导航配置, 格式 xxx/xxx, 不含 `.navi.xml` 扩展, 相对 etc 目录.

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

## 数据同步

此项目仅用于小型信息系统（数据量较小），但也许出于性能或安全的考虑需要水平扩展。
文件系统可通过 sync、NFS、FastDFS 等实现，而 Lucene 因 Hadoop 碎片写入效率低、内存索引无法同步等不得不放弃。
故采用消息队列来实现同步，各个服务器均维护完整数据。通过配置 JMS 实现实现与具体消息队列解耦。

使用方式：
1. 根据选择的 JMS 实现，需要提供相应的依赖：
```xml
<!-- Jakarta JMS API (必要) -->
<dependency>
    <groupId>jakarta.jms</groupId>
    <artifactId>jakarta.jms-api</artifactId>
    <version>3.1.0</version>
</dependency>
<!-- ActiveMQ (可选) -->
<dependency>
    <groupId>org.apache.activemq</groupId>
    <artifactId>activemq-jms-pool</artifactId>
    <version>5.18.4</version>
</dependency>
<dependency>
    <groupId>org.apache.activemq</groupId>
    <artifactId>activemq-client</artifactId>
    <version>5.18.4</version>
</dependency>
<!-- RabbitMQ (可选) -->
<dependency>
    <groupId>com.rabbitmq</groupId>
    <artifactId>rabbitmq-jms</artifactId>
    <version>1.14.0</version>
</dependency>
<dependency>
    <groupId>com.rabbitmq</groupId>
    <artifactId>amqp-client</artifactId>
    <version>5.20.0</version>
</dependency>
```

2. 在 defines.properties 中加配 serve.init:
```
# 初始任务
#serve.init=\
io.github.ihongs.serv.matrix.sync.SyncConsumer;
```

2. 在 matrix.properties 中配置 (ActiveMQ)：
```
# 同步配置
matrix.data.diffuser=io.github.ihongs.serv.matrix.sync.SyncProducer
matrix.sync.connection=org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory
matrix.sync.broker.url=tcp://localhost:61616
matrix.sync.topic.name=matrix.sync
```

## 定制开发

### 前端开发

增/删/改/查/统计/权限在配置好表单后就有了, 接着只需:

1. 登录系统，点击右上角菜单，进入"模型矩阵"
2. 点击左侧单元区的添加，输入名称等，点击下方提交即创建了一个单元
3. 单击选中刚创建的单元，点击右边列表上方的创建按钮，输入表单名称
4. 点击右侧列表的"+"可添添加字段，点击字段右上的"i"可以设置字段
5. 完成后点击保存按钮，一个表单就设置好了
6. 刷新页面，左侧菜单就能看到刚添加的单元
7. 展开单元，点击进入对应模块，尝试操作吧

现在，你可以在 centra/data 或 centre/data 目录下添加一个以表单 ID 为名的目录，在其下添加以下文件即可重建页面体系：

- default.html     引导页面
- defines.css      样式干预定制脚本（默认是空的）
- defines.js       过程干预定制脚本（默认是空的）
- form_init.html   创建表单
- form.html        编辑表单
- info.html        查看详情
- list.html        列表区块
- pick.html        选择区块

一个简单的方法是通过浏览器控制台的网络获取相应页面，复制并存到 URL 对应位置后，在这个基础上继续修改.  如不想使用原页面体系可在构建的 default.html 中按自定规则组织子功能页面体系。您可以使用内置的 JS 框架和组件继续开发，这很简单(见 [**HongsCORE4JS**](hongs-web/wms/static/assets/src/) 和 [**HongsCORE4JS Demo**](hongs-web/wms/static/assets/test/))，通过 defines.js 可微调大部分的细节。但也可以覆盖 default.html 完全定制整个页面，这样您可以随意选择自己习惯的 JS 和 CSS 库。

如果觉得上述定制还不够随心所欲，可以在 /public 或 /static 目录下建立自己的前端子项目，用 Vue 或 React 等进行开发。需要注意，/public 下会追溯 index.html，这对那些用到 Route 的前端项目非常有利，而 /static 下默认并不支持。可以修改 /index.jsp 将网站根路径跳转到定制的子项目路径。

### 后端开发

后端与前端一样简单，如果只是小改、补充，可用 jsp 充当动作脚本，即写即用，无需编译和重启；亦可编译特定的 java 动作程序、模型程序对下层过程进行完全定制。

表单资源默认有 search,create,update,delete 四个接口，具体输入输出方式参考下方 [运行规则](#运行规则) 章节。

如果表单 ID 目录下存在 `__main__.jsp` 文件，则对应资源的所有更新改查等操作均会转到此处，但仍然可以通过 ActionRunner 调用原始的动作程序，这时 `__main__.jsp` 充当过滤器的角色，可以通过 ActionHelper 对输入输出数据进行改写。

想通过 java 进行更深入的定制开发也很简单，在包 io.github.ihongs.serv.centra (后台的叫 centra 前台的叫 centre) 下新建一个 Action 类，如 XxxAction.java，此类至少要提供一个公共的无参构造方法（不写就是默认有），通过类的注解 @Action("centra/xxx") 定义表单资源路径，通过方法注解 @Action("search") 定义动作路径名称。事实上这个类所在的包并非必须要是 io.github.ihongs.serv.centra 和 io.github.ihongs.serv.centre，只是默认会扫描这两个包而已，也可以通过 etc/defines.properties 中在 mount.serv 下增加包和类名，甚至可用加上 your.package.** 扫你自建的全部包和类，而其对应的访问路径是通过 @Action 注解来定义的。

### 新建项目

如果需要编写 java 代码来控制更多细节及补充更多功能，可建立新的项目.

项目 pom.xml 举例:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>my-project</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>war</packaging>
    <dependencies>
        <dependency>
            <groupId>io.github.ihongs</groupId>
            <artifactId>hongs-wms</artifactId>
            <version>1.1-SNAPSHOT</version>
            <type>war</type>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.8.1</version>
                <configuration>
                    <source>1.8</source>
                    <target>1.8</target>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-war-plugin</artifactId>
                <version>3.2.3</version>
                <configuration>
                    <warSourceDirectory>web</warSourceDirectory>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

项目目录结构:
- src
  - main
    - java
    - resources
  - test
    - java
    - resources
- web
  - WEB-INF
    - bin
    - etc
  - centra
  - centre
- target
  - my-project-1.0-SNAPSHOT
    - WEB-INF
      - bin
      - etc
      - lib
      - var
      web.xml
    - centra
    - centre
