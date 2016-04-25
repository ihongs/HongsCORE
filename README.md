# HongsCORE framework for Java

* 文档版本: 15.09.20
* 软件版本: 0.3.8-20160416
* 设计作者: 黄弘(Kevin Hongs)
* 技术支持: kevin.hongs@gmail.com

**HongsCORE** 即 **Hong's Common Object Requesting Engine**, 通用对象请求引擎, 拼凑的有些生硬. 在设计第一个原型框架时(PHP 版 2006 年), 我买了一台 Intel Core CPU 的笔记本电脑, 当时随意的给她取了个名字叫 Core, 后来觉得名字应该更有意义才扩展成了以上缩写.
另一个原因是: 从最初的 PHP 版一直到现在的 Java 版, 我都有设计一个核心工厂类, 主要作用就是用于请求和管理唯一对象, 实现  Singleton (单例模式), 在需要某个对象时只管请求, 使对象的使用效率更高. 具体到这个 Java 版本中, 利用了 ThreadLocal, 和 Tomcat,Jetty 等 Servlet 容器的单实例多线程特性来实现行之有效的单例模式.

原 PHP 版框架在 **上海科捷信息技术有限公司** 的 **AFP8 广告系统** *(已被其他公司收购)* 中使用, 其部分模块是在科捷的工作期间开发的, 本人已于 2011 年离开科捷, 故不再对其更新亦不再提供源码. 原科捷公司和其 AFP8 系统及其衍生品拥有原 PHP 版框架代码的全部处置权.

感谢 **林叶,杨林,袁杰,赵征** 等朋友过去对我的支持和理解, 谢谢你们!

## 特性概叙

1. 支持库表配置, 支持多数据库;
2. 支持 JOIN,IN 的自动关联查询, 可自动处理关联插入;
3. 支持隶属/一对多/多对多等关联模式并能自动处理关系;
4. 有简单的存取模型基类, 对常规增删改查无需编写代码;
5. 统一的服务端维护程序, 可与前端应用共享配置及模型;
6. 简单有效的动作权限解决方案;
7. 与对应的 HongsCORE4JS(for Javascript) 配合实现高效 WEB 应用开发方案;
8. 默认嵌入 jetty,sqlite,lucene 等库, 除 JDK 外无需安装其他软件即可运行;
9. 内建有数据管理功能, 无需编程即可构建信息管理系统.

另见 [**更新日志**](UPDATE.md), 及 [**HongsCORE framework for Javascript**](hongs-web/web/static/assets/source/).

## 许可说明

本软件及源码以 [**MIT License**](LICENSE.md) 协议发布，源码开放使用和修改，依赖的库请参阅其对应的许可声明。请在源码中保留作者的署名（**黄弘, Huang Hong, Hongs**）等信息。

> 被授权人权利：
> 被授权人有权利使用、复制、修改、合并、出版发行、散布、再授权及贩售软件及软件的副本。
> 被授权人可根据程序的需要修改授权条款为适当的内容。

> 被授权人义务：
> 在软件和软件的副本中都必须包含版权声明和许可声明。

## 使用方法

下载 Hongs-CORE-x.x.x.tar.gz 后解压到任意目录(别下 release 里的、很久没更新了), 打开命令行(Linux,Mac的终端)并切换到该目录下, 先执行 `bin/run system:setup` 设置数据库, 再执行 `bin/run server:start` 启动服务器, 然后打开浏览器在地址栏输入 http://localhost:8080/ 即可进入; 登录账号 `admin@xxx.com` 口令 `123456`; 如需停止服务, 关闭命令窗口或按 Ctrl+C 即可; Linux,Mac 系统需要检查 run 是否有执行权限(`chmod +x etc/*`).

同时为 windows 用户提供了 setup.bat 和 start.bat 两个快捷命令来执行上面的两条命令, windows 用户只需双击即可设置和启动.

注意: 需要 JDK 而非 JRE(Java) 才能运行, 使用前请确保 JDK 已安装并加入 PATH 环境变量, 或设置 JAVA_HOME 环境变量为 jdk 的安装目录(Windows 必须设置). Windows,Linux,Mac 系统在官网下载并使用安装程序安装的通常已自动设置好了.

> JDK 下载地址: http://www.oracle.com/technetwork/java/javase/downloads/index.html

## 前端开发

如果你是一位前端开发人员，别去想什么 Node.js,MongoDB 等 javascript 一统前后端的方案了, 你应该做的就是把体验做好, 神马增/删/改/查/接口/权限就不用费劲考虑了, 你只需:

1. 登录系统，点击右上角菜单，进入"模块管理"
2. 点击左侧单元区的添加，输入名称等，点击下方提交即创建了一个单元
3. 点击选中刚创建的单元，点击右边列表上方的创建按钮，输入表单名称
4. 点击右侧列表的"+"可添添加字段，点击字段右上的"i"可以设置字段
5. 完成后点击保存按钮，一个表单就设置好了
6. 刷新页面，右上菜单就能看到刚添加的单元
7. 点击单元，顶部菜单有表单项，尝试操作吧

现在，你可以在 manage/data 目录下添加一个以表单 ID 为名的目录，在其下添加以下文件即可重建页面体系：

    default.html     引导页面
    form.html        创建表单
    form4edit.html   编辑表单
    form4view.html   查看表单
    list.html        列表区块
    list4pick.html   选择区块

一个简单的方法是通过 http://localhost:8080/mytest/form.html 这样的 url 来获取 html 文件, 然后存下来后在这个基础上改. 以后会增加一个按钮来比较方便的固化这个 html. 如不想使用原页面体系可在构建的 default.html 中按自定规则编码。

## 后端开发

> 与前端类似，在设置好模块后，可根据实际情况对特定请求动作覆盖开发，后续详细补充

## 类库依赖

    jQuery      [JS]
    Respond     [JS]
    Bootstrap   [JS, CSS]
    Glyphicons  [Bootstrap]

适用 Java 版本 JDK 1.5 及以上, 推荐使用 1.7; Java 库依赖情况请参见各个 module 中 pom.xml 的 dependencies 部分.

## 文件体系

### 目录结构:

    - bin               运维脚本
    - lib               运行包库(启动时可指定 -classpath)
    - etc               配置资源(启动时可指定 --confpath)
    - var               数据文件(启动时可指定 --datapath)
        - log           运行日志(启动时可指定 -Dlogs.dir)
        - tmp           临时文件(启动时可指定 -Dtmps.dir)
        - upload        文件上传临时存放目录
        - serial        序列缓存数据文件目录
        - sesion        Jetty 及 API 会话数据存放目录
        - sqlite        Sqlite本地数据库目录
        - lucene        Lucene本地索引库目录
    - web               前端文件(启动时可指定 --basepath,--basehref)
        - common        前端通用库
            - css       前端样式
            - fonts     前端字体
            - img       前端图片
            - pages     通用页面
            - src       前端源码
        - compon        其他可选前端组件
        - manage        内置信息管理系统

### 类库结构:

    app.hongs           核心
    app.hongs.action    动作支持
    app.hongs.cmdlet    命令支持
    app.hongs.db        关系数据模型
    app.hongs.dh        文档处理组件
    app.hongs.tags      JSP标签
    app.hongs.util      工具

以上仅列举了主要的包, 更多框架信息请参考 API 文档.

## 运行规则

### 路径映射

    xxx.foo:bar         对应标记 @Cmdlet("xxx.foo") 的类下 @Cmdlet("bar") 的方法
    xxx.foo             对应标记 @Cmdlet("xxx.foo") 的类下 @Cmdlet("__main__") 的方法
    xxx/foo/bar.act     对应标记 @Action("xxx/foo") 的类下 @Action("bar") 的方法
    xxx/foo.act         对应标记 @Action("xxx/foo") 的类下 @Action("__main__") 的方法
    xxx/foo.api         对应标记 @Action("xxx/foo") 的类下 @Action("retrieve,create,update,delete") 的方法(这四种动作分别对应 HTTP METHOD: GET,POST,PUT,DELETE)
    common/auth/name.js 读取 WBE-INF/conf/name.as.xml 中 actions+session 的组合
    common/conf/name.js 读取 WEB-INF/conf/name.properties 中 fore.xxxx. 开头的配置
    common/lang/name.js 读取 WEB-INF/conf/name.xx-XX.properties 中 fore.xxxx. 开头的配置

action 和 cmdlet 使用 @Action 和 @Cmdlet 注解来设置访问路径, 如果不指定则用类,方法名作为路径; 请在 etc/\_begin\_.properties 中设置 core.load.serv 为 Action,Cmdlet 类, 或 xxx.foo.* 告知该包下存在 Action,Cmdlet 类, 多个类/包用";"分隔.
最后3个路径, 将扩展名 .js 换成 .json 即可得到 JSON 格式的数据; 语言配置可在 name 后加语言区域标识, 如 example_zh_CN.js 为获取 example 的中文大陆简体的 js 格式的语言配置.

### 请求规则

支持 Content-Type 为 application/x-www-form-urlencoded, multipart/form-data 和 application/json 的请求, 组成结构为("+" 在 URL 中表示空格):

    f1=1&f2.!eq=2&f3.!in.=30&f3.!in.=31&t1.f4.!gt=abc&ob=-f5+f6&wd=Hello+world

或兼容 PHP 的方式:

    f1=1&f2[!eq]=2&f3[!in][]=30&f3[!in][]=31&t1[f4][!gt]=abc&ob=-f5+f6&wd=Hello+world

会转成 JSON 结构:

    {
        "f1": 1,
        "f2": {
            "!eq": 2
        },
        "f3": {
            "!in": [
                30,
                31
            ]
        },
        "t1": {
            "f4": {
                "!gt": "abc"
            }
        },
        "ob": "-f5 f6",
        "wd": "Hello world"
    }

其中 !eq 这样的标识为过滤操作符, 其含义为:

    !eq     等于
    !ne     不等于
    !gt     大于
    !ge     大于或等于
    !lt     小于
    !le     小于或等于
    !in     包含
    !ni     不包含

有一些参数名具有特定意义, 如:

     pn     当前页码(page num)
     rn     额定行数(rows cnt)
     wd     搜索字词(word)
     ud     内置参数(used)
     ob     排序字段(order by)
     rb     需求字段(reply by)
     or     或查询(or)
     ar     多组或(and)

请避免将这些参数作为字段名.

### 响应结构

默认返回 JSON 格式的数据:

    {
        "ok": true成功 false失败,
        "err": "错误代码",
        "msg": "响应消息",
        "ref": "跳转地址",
        其他...
    }

ref 通常没有, 无访问权限而需要跳转时才会设置. 其他数据通常有:

    // 列表信息, 在 retrieve,list 动作返回
    "list": [
        {
            "字段": "取值",
            ...
        },
        ...
    ],

    // 分页信息, 在 retrieve,list 动作返回
    "page": {
        "pagecount": 总的页数,
        "rowscount": 当前行数,
        ...
    }

    // 枚举信息, 在 retrieve,list,info 动作返回
    "enum": {
        "字段": [
            ["取值", "名称"],
            ...
        ],
        ...
    }

    // 单元信息, 在 retrieve,info,create 动作返回
    "info": {
        "字段": "取值",
        ...
    }

    // 错误信息, 在 create,update 动作返回
    "errs": {
        "字段": "错误",
        ...
    }

    // 数量信息, 在 update,delete 动作返回
    "rows": 操作行数

    // 验证信息, 在 unique,exists 动作返回
    "sure": 验证真假

在调用 API(REST) 时, 可将所有请求数据采用 JSON 或 URLEncode 编码放入 !data 参数传递; 如加请求参数 !wrap=1 可将全部返回数据放入 data 键下; 如加请求参数 !scok=1 则无论是否异常总是返回 200 状态; 可加请求参数 !conv=RULES 启用数据转换规则, RULES 取值可以为:

    all2str     全部转为字串
    num2str     数字转为字串
    null2str    空转为空字串
    bool2str    true转为字串1, false转为空串
    bool2num    true转为数字1, false转为数字0
    date2sec    转为时间戳(秒)
    date2mic    转为时间戳(毫秒)

dete2mic 或 date2sec 搭配 all2str 则将转换后的时间戳数字再转为字符串; 如果仅指定 all2str 则时间/日期会转为"年-月-日"格式的字符串. 多个规则可以用逗号或加号分隔

## 运行设置

### 导航配置

一个 xxx.navi.xml 配置文件由 menu,role,depend,action 和 rsname,import 这些节点组成. menu 为菜单项; role 为权限角色或称权限分组; depend 为依赖的权限; action 登记权限具体的动作, 权限过滤器据此判断某个动作是否可被调用; rsname 标示权限会话的名称或单例类; import 用于引入其他导航配置.

### 表单配置

一个 xxx.form.xml 配置文件由 form,field,param,enum,value 这些节点组成, 结构类似于 Protobuf, form 类似 Protobuf 的 message. field 有 required/repeated 对应 Protobuf message 下条目的 required/optional/repeated; type 对应条目的类型, 只是更贴近HTML控件和数据库字段类型.

其中 field 的 param 设置中, 可用于控制视图布局的参数有:

    sortable    字段可排序(枚举等类型的字段实为分组类聚)
    findable    字段可搜索(且搜索框查询会查询此字段的值)
    listable    字段可显示在列表中
    inedible    字段不在编辑页出现
    invisble    字段不在查看页出现
    unstored    不保存原文(针对 Lucene 特有, 可查询或排序却不能读取)

每个表单(form)可以有一个 name="@" 的字段, 该字段的名称即为此表单的名称, 其配置即表单的配置, 同样也有一些控制视图的参数:

    dont.auto.bind.listable 告知 Mview 不要自动将字段设为可列举
    dont.auto.bind.sortable 告知 Mview 不要自动将字段设为可排序
    dont.auto.bind.findable 告知 Mview 不要自动将字段设为可搜索
    dont.auto.append.fields 告知 Mview 不要自动追加表内字段
    dont.auto.append.assocs 告知 Mview 不要自动追加关联字段
    dont.show.create.button 告知视图不要显示创建按钮
    dont.show.update.button 告知视图不要显示修改按钮
    dont.show.delete.button 告知视图不要显示删除按钮
    dont.show.checks.column 告知视图不要显示选择列
    cant.call.ACTION_HANDLE 告知 DBAction 等"脚手架"处理器拒绝处理某些操作, 也可以通过权限控制

另, 每个枚举(enum)可以有一个 code="\*" 的取值, 该取值用作"其他"选项, 当出现枚举中没有记录的值时, 将显示为"其他".

### 数据配置

一个 xxx.db.xml 配置文件由

### 模型规范

推荐在实体关系模型(ERM)设计上遵循规范: 表名由 "分区\_模块\_主题\_子主题" 组成, 主题可以有多级, 但推荐最多两级, 模块关系设计成类似树形拓扑的结构.

分区分别为:

    a       应用区, 存放应用数据
    b       仓库区, 存放历史数据
    m       市场区, 存放结果数据
    s       缓冲区, 存放缓冲数据

字段名推荐:

    id      主键, CHAR(20)
    pid     父键, CHAR(20)
    xx_id   外键, CHAR(20), xx为关联表缩写
    ctime   创建时间, DATETIME,TIMESTAMP,BIGINT,INTEGER
    mtime   修改时间, DATETIME,TIMESTAMP,BIGINT,INTEGER
    etime   结束时间, DATETIME,TIMESTAMP,BIGINT,INTEGER
    state   状态标识, TINYINT, 1正常, 0删除, 可增设其他状态值; 如果删除后需要恢复之前的状态, 请新增其他状态字段

因字段名可用于 URL 中作为过滤参数, 而部分参数已有特殊含义, 字段取名时请尽量避开这些名称: pn,gn,rn,wd,md,ob,rb,or,ar, 比较简单的办法是避免取 2 个字母的字段名. 另, 在 Cnst 类和配置文件中可以重新定义这些名称, 但并不建议修改(我信奉少量的约定胜于过多的配置).

# KEEP IT SIMPLE, STUPID!

> 编写只做一件事情，并且要做好的程序；编写可以在一起工作的程序，编写处理文本流的程序，因为这是通用的接口。这就是UNIX哲学。所有的哲学真正的浓缩为一个铁一样的定律，高明的工程师的神圣的“KISS 原则”无处不在。大部分隐式的UNIX哲学不是这些前辈所说的，而是他们所做的和UNIX自身建立的例子。从整体上看，我们能够抽象出下面这些观点：

> 1.  模块原则：写简单的，通过干净的接口可以被连接的部件。
> 2.  清楚原则：清楚要比小聪明好。
> 3.  合并原则：设计能被其它程序连接的程序。
> 4.  分离原则：从机制分离从策略，从实现分离出接口。
> 5.  简单原则：设计要简单，仅当你需要的时候才增加复杂性。
> 6.  节俭原则：只有当被证实是清晰，其它什么也不做的时候，才写大的程序。
> 7.  透明原则：为使检查和调试更容易而设计。
> 8.  健壮原则：健壮性是透明和简单的追随者。
> 9.  表现原则：把知识整理成资料，于是程序逻辑能变得易理解和精力充沛的。
> 10. 意外原则：在接口设计中，总是做最小意外的事情。
> 11. 沉默原则：一个程序令人吃惊的什么也不说的时候，他应该就什么也不说。
> 12. 补救原则：当你必须失败的时候，尽可能快的吵闹地失败。
> 13. 经济原则：程序员的时间很宝贵，优先机器时间来节约它。
> 14. 产生原则：避免手工堆砌，当你可能的时候，编写可以写程序的程序。
> 15. 优化原则：在雕琢之前先有原型；在你优化它之前，先让他可以运行。
> 16. 差异原则：怀疑所有声称的“唯一真理”。
> 17. 扩展原则：为将来设计，因为它可能比你认为的来得要快。
