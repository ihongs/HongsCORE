# HongsCORE framework for Java

* 文档版本: 17.06.17
* 软件版本: 0.4.0-20170617
* 设计作者: 黄弘(Kevin Hongs)
* 技术支持: kevin.hongs@gmail.com

**HongsCORE** 即 **Hong's Common Object Requesting Engine**, 通用对象请求引擎, 拼凑的有些生硬. 在设计第一个原型框架时(PHP 版 2006 年), 我买了一台 Intel Core CPU 的笔记本电脑, 当时随意的给她取了个名字叫 Core, 后来觉得名字应该更有意义才扩展成了以上缩写.
另一个原因是: 从最初的 PHP 版一直到现在的 Java 版, 我都有设计一个核心工厂类, 主要作用就是用于请求和管理唯一对象, 实现  Singleton (单例模式), 在需要某个对象时只管请求, 使对象的使用效率更高. 具体到这个 Java 版本中, 利用了 ThreadLocal, 和 Tomcat,Jetty 等 Servlet 容器的单实例多线程特性来实现行之有效的单例模式.

## 特性概叙

1. 支持库表配置, 支持多数据库;
2. 支持 JOIN 或 IN 的关联查询, 并可自动处理关联插入;
3. 支持隶属/一对多/多对多等关联模式并能自动处理关系;
4. 有简单的存取模型基类, 对常规增删改查无需编写代码;
5. 统一的服务端维护程序, 可与前端应用共享配置及模型;
6. 简单有效的动作权限解决方案;
7. 与对应的 HongsCORE4JS(for Javascript) WEB 组件配合实现高效的开发方案;
8. 默认嵌入 jetty,sqlite,lucene 等库, 除 JDK 外无需安装其他软件即可运行;
9. 内含自助模块, 无需编程即可构建简单的信息管理系统.

另见 [**更新日志**](UPDATE.md), 及 [**HongsCORE framework for Javascript**](hongs-web/web/static/assets/src/).

## 许可说明

本软件及源码以 [**MIT License**](LICENSE) 协议发布，源码开放使用和修改，依赖的库请参阅其对应的许可声明。请在源码中保留作者的署名（**黄弘, Huang Hong, Hongs**）等信息。

> 被授权人权利：
> 被授权人有权利使用、复制、修改、合并、出版发行、散布、再授权及贩售软件及软件的副本。
> 被授权人可根据程序的需要修改授权条款为适当的内容。

> 被授权人义务：
> 在软件和软件的副本中都必须包含版权声明和许可声明。

## 使用方法

请先安装 JDK 和 Maven, 然后进入您的工作目录, 按以下流程执行命令:

    # 获取并构建系统
    git clone https://github.com/ihongs/HongsCORE.git
    cd  HongsCORE
    mvn clean package

    # 设置和启动系统
    cd  hongs-serv-xxx/target/HongsXXX
    bin/app system.setup
    bin/app server.start

    # hongs-serv-web/target/HongsWeb 为基础系统, 仅包含基本的应用服务和前端组件等;
    # hongs-serv-wms/target/HongsWMS 为管理系统, 拥有全功能的用户管理和自助模块等.
    # 以上两个包内都放有 Dockerfile 可轻松构建容器镜像, 搭建虚拟系统.

加 --DEBUG 1 可开启调试输出模式, 可在控制台(命令行)显示执行过程(调试信息), server.start 命令可跟数字表示启动端口, 默认 8080 端口. 同时为 windows 用户提供了 setup.bat 和 start.bat 两个快捷命令来执行以上同等任务, windows 用户只需双击即可.

注意[1]: 退出运行应该用 Ctrl+C 而不应当直接关闭命令窗口, 后者可能导致直接死掉而无法执行退出清理, 再次启动需要删除 var/server 下端口对应的 .pid 文件才行.

注意[2]: 需要 JDK 而非 JRE(Java) 才能运行, 使用前请确保 JDK 已安装并加入 PATH 环境变量, 或设置 JAVA_HOME 环境变量为 jdk 的安装目录(Windows 必须设置). 在官网下载并使用安装程序安装的通常已自动设置好了.

    JDK 下载地址: http://www.oracle.com/technetwork/java/javase/downloads/index.html
    MVN 下载地址: http://maven.apache.org/download.cgi

## 前端开发

如果你是一位前端开发人员，别去想什么 Node.js,MongoDB 等 javascript 一统前后端的方案了, 你应该做的就是把体验做好, 神马增/删/改/查/接口/权限就不用费劲考虑了, 你只需:

1. 登录系统，点击右上角菜单，进入"模块管理"
2. 点击左侧单元区的添加，输入名称等，点击下方提交即创建了一个单元
3. 点击选中刚创建的单元，点击右边列表上方的创建按钮，输入表单名称
4. 点击右侧列表的"+"可添添加字段，点击字段右上的"i"可以设置字段
5. 完成后点击保存按钮，一个表单就设置好了
6. 刷新页面，右上菜单就能看到刚添加的单元
7. 点击单元，顶部菜单有表单项，尝试操作吧

现在，你可以在 centra/data 或 centre/data 目录下添加一个以表单 ID 为名的目录，在其下添加以下文件即可重建页面体系：

    default.html     引导页面
    form.html        编辑表单
    form_adds.html   创建表单
    list.html        列表区块
    list_fork.html   选择区块

一个简单的方法是通过浏览器控制台的网络获取相应页面，复制并存到 URL 对应位置后，在这个基础上继续修改.  如不想使用原页面体系可在构建的 default.html 中按自定规则组织子功能页面体系。

## 后端开发

> 与前端类似，在设置好模块后，可根据实际情况对特定请求动作覆盖开发，后续详细补充

## 类库依赖

    jQuery      [JS]
    Respond     [JS]
    Bootstrap   [JS, CSS]
    Glyphicons  [Bootstrap]

适用 Java 版本 JDK 1.6 及以上, 推荐使用 1.7; Java 库依赖情况请参见各个 module 中 pom.xml 的 dependencies 部分.

## 文件体系

### 目录结构:

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

### 类库结构:

    io.github.ihongs            核心
    io.github.ihongs.action     动作支持
    io.github.ihongs.cmdlet     命令支持
    io.github.ihongs.db         关系数据模型
    io.github.ihongs.dh         数据仓库组件
    io.github.ihongs.jsp        JSP 工具
    io.github.ihongs.util       辅助工具
    io.github.ihongs.serv       服务组件

以上仅列举了主要的包, 更多框架信息请参考 API 文档.

## 运行规则

### 路径映射

    foo.bar.xxx         对应标记 @Cmdlet("fop.bar") 的类下 @Cmdlet("xxx") 的方法
    foo.bar             对应标记 @Cmdlet("foo.bar") 的类下 @Cmdlet("__main__") 的方法
    foo/bar/xxx.act     对应标记 @Action("foo/bar") 的类下 @Action("xxx") 的方法
    foo/bar.act         对应标记 @Action("foo/bar") 的类下 @Action("__main__") 的方法
    api/foo/bar         对应标记 @Action("foo/bar") 的类下 @Action("search,create,update,delete") 的方法
                        这四种动作分别对应 HTTP METHOD: GET,POST,PUT,DELETE
    common/auth/name.js 读取 WBE-INF/conf/name.menu.xml 中 actions+session 的组合
    common/conf/name.js 读取 WEB-INF/conf/name.properties 中 fore.xxxx. 开头的配置
    common/lang/name.js 读取 WEB-INF/conf/name_xx_XX.properties 中 fore.xxxx. 开头的配置

action 和 cmdlet 使用 @Action 和 @Cmdlet 注解来设置访问路径, 如果不指定则用类,方法名作为路径; 请在 etc/defines.properties 中设置 mount.serv 为 Action,Cmdlet 类, 或 xxx.foo.* 告知该包下存在 Action,Cmdlet 类, 多个类/包用";"分隔.
最后3个路径, 将扩展名 .js 换成 .json 即可得到 JSON 格式的数据; 语言配置可在 name 后加语言区域标识, 如 example_zh_CN.js 为获取 example 的中文大陆简体的 js 格式的语言配置.

### 请求规则

支持 Content-Type 为 application/x-www-form-urlencoded, multipart/form-data 和 application/json 的请求, 组成结构为:

    f1=1&f2:eq=2&f3:in.=30&f3:in.=31&t1.f4:rg=18,55&ob=-f5+f6&wd=Hello+world

在 URL 中 + 表示空格. 可兼容 PHP 的格式:

    f1=1&f2[:eq]=2&f3[:in][]=30&f3[:in][]=31&t1[f4][:rg]=18,55&ob=-f5+f6&wd=Hello+world

此 URL 查询串将转为类似下面 JSON 的结构:

    {
        "f1": 1,
        "f2": {
            ":eq": 2
        },
        "f3": {
            ":in": [
                30,
                31
            ]
        },
        "t1": {
            "f4": {
                ":rg": "18,55"
            }
        },
        "ob": "-f5 f6",
        "wd": "Hello world"
    }

其中 eq 这样的标识为过滤操作符, 其含义为:

     eq     等于
     ne     不等于
     gt     大于
     ge     大于或等于
     lt     小于
     le     小于或等于
     rn     区间
     on     区间集
     in     包含
     ni     不包含

还有一些参数名称具有特定的意义, 列举如下:

     id     主键值
     wd     关键词
     pn     当前页码(page num)
     gn     分组数量(pags num)
     rn     额定行数(rows num)
     cb     响应回调(callback)
     ob     排序字段(order by)
     rb     响应字段(reply with)
     ab     应用处理(apply with)
     or     或查询  (or )
     ar     多组或  (and)

请避免将这些参数作为您的字段名.

### 响应结构

默认返回 JSON 数据结构:

    {
        "ok": true成功 false失败,
        "ern": "错误代号",
        "err": "错误信息",
        "msg": "响应消息",
        其他...
    }

ern 为 Er301,Er302,Er401,Er402,Er403,Er404 时, err 如有值为"Goto URL"则跳转到 URL. 其他数据通常有:

    // 列表信息, 在 search,list 动作返回
    "list": [
        {
            "字段": "取值",
            ...
        },
        ...
    ],

    // 分页信息, 在 search,list 动作返回
    "page": {
        "pagecount": 总的页数,
        "rowscount": 当前行数,
        ...
    }

    // 枚举信息, 在 search,list,info 动作返回
    "enum": {
        "字段": [
            ["取值", "名称"],
            ...
        ],
        ...
    }

    // 单元信息, 在 search,info,create 动作返回
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
    "size": 操作行数

    // 验证信息, 在 unique,exists 动作返回
    "size": 1真, 0假

在调用 API(REST) 时, 可将所有请求数据采用 JSON 或 URLEncode 编码放入 .data 参数传递; 如加请求参数 .wrap=1 可将全部返回数据放入 data 键下; 如加请求参数 .scok=1 则无论是否异常总是返回 200 状态; 可加请求参数 .conv=RULES 启用数据转换规则, RULES 取值可以为:

    all2str     全部转为字串
    num2str     数字转为字串
    null2str    空转为空字串
    bool2str    true转为字串1, false转为空串
    bool2num    true转为数字1, false转为数字0
    date2sec    转为时间戳(秒)
    date2mic    转为时间戳(毫秒)
    flat.map    拉平 map 层级, 连接符为 .
    flat_map    拉平 map 层级, 连接符为 _

dete2mic 或 date2sec 搭配 all2str 则将转换后的时间戳数字再转为字符串; 如果仅指定 all2str 则时间/日期会转为"年-月-日"格式的字符串; flat.map 或 flat_map 可将数据层级拉平, 如 user: {name: "Kevin"} 可拉平为 "user.name": "Kevin" 或 "user_name": "Kevin". 多个规则可以用逗号、分号、空格或加号分隔.

另有参数 cb=回调名 可用来封装为 JSONP 方式以供跨域调用, 为遵守普遍规则, 此参数默认叫 callback, 如果 callback 对应字段或有其他用途, 则总是能用且优先可用 cb 参数.

## 运行设置

### 导航配置

一个 xxx.navi.xml 配置文件由 menu,role,depend,action 和 rsname,import 这些节点组成. menu 为菜单项; role 为权限角色或称权限分组; depend 为依赖的权限; action 登记权限具体的动作, 权限过滤器据此判断某个动作是否可被调用; rsname 标示权限会话的名称或单例类; import 用于引入其他导航配置.

### 表单配置

一个 xxx.form.xml 配置文件由 form,field,param,enum,value 这些节点组成, 结构类似于 Protobuf, form 类似 Protobuf 的 message. field 有 required/repeated 对应 Protobuf message 下条目的 required/optional/repeated; type 对应条目的类型, 只是更贴近HTML控件和数据库字段类型.

其中 field 的 param 设置中, 可用于控制布局和查询的参数有:

    listable           字段可获取(可通过rb参数控制)
    sortable           字段可排序(可通过ob参数控制, 枚举等类型的字段实为分组类聚)
    srchable           字段可搜索(可通过wd参数限制)
    findable           字段可过滤(可以用于查询, 用过滤标识时, 以层级数据结构给出)
    unstored           不保存原文(可查询或排序, 但不可以读取, 仅针对 Lucene 特有)

每个表单(form)可以有一个 name="@" 的字段, 该字段的名称即为此表单的名称, 其配置即表单的配置, 同样也有一些控制视图的参数:

    xxxxable           如同 field 中的同名参数, 但值为字段名称列表, * 表全部字段, ? 按类型判别
    callable           告知 XxxAction 脚手架动作类, 限定可执行方法, 缺省全部允许, 为空全部禁止
    dont.append.fields 告知 Mview 自动追加表内字段
    dont.append.assocs 告知 Mview 自动追加关联字段

另, 每个枚举(enum)可以有一个 code="\*" 的取值, 该取值用作"其他"选项, 当出现枚举中没有记录的值时, 将显示为"其他".

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
    mtime   修改时间, DATETIME,TIMESTAMP,BIGINT,INTEGER
    ctime   创建时间, DATETIME,TIMESTAMP,BIGINT,INTEGER
    etime   截止时间, DATETIME,TIMESTAMP,BIGINT,INTEGER, 用于仓库层记录数据的失效时间, 避免用作应用层的业务到期时间
    state   状态标识, TINYINT, 1正常, 0删除, 可增设其他状态值; 如果删除后需要恢复之前的状态, 请新增其他状态字段, 或使用负值标识删除.

因字段名可用于 URL 中作为过滤参数, 而部分参数已有特殊含义, 字段取名时请尽量避开这些名称: pn,gn,rn,wd,ob,rb,ab,or,ar, 比较简单的办法是避免取 2 个字母的字段名. 另, 在 Cnst 类和配置文件中可以重新定义这些名称, 但并不建议修改(我信奉少量的约定胜于过多的配置).

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
