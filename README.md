# HongsCORE framework for Java

[![zread](https://img.shields.io/badge/Ask_Zread-_.svg?style=flat&color=00b0aa&labelColor=000000&logo=data%3Aimage%2Fsvg%2Bxml%3Bbase64%2CPHN2ZyB3aWR0aD0iMTYiIGhlaWdodD0iMTYiIHZpZXdCb3g9IjAgMCAxNiAxNiIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHBhdGggZD0iTTQuOTYxNTYgMS42MDAxSDIuMjQxNTZDMS44ODgxIDEuNjAwMSAxLjYwMTU2IDEuODg2NjQgMS42MDE1NiAyLjI0MDFWNC45NjAxQzEuNjAxNTYgNS4zMTM1NiAxLjg4ODEgNS42MDAxIDIuMjQxNTYgNS42MDAxSDQuOTYxNTZDNS4zMTUwMiA1LjYwMDEgNS42MDE1NiA1LjMxMzU2IDUuNjAxNTYgNC45NjAxVjIuMjQwMUM1LjYwMTU2IDEuODg2NjQgNS4zMTUwMiAxLjYwMDEgNC45NjE1NiAxLjYwMDFaIiBmaWxsPSIjZmZmIi8%2BCjxwYXRoIGQ9Ik00Ljk2MTU2IDEwLjM5OTlIMi4yNDE1NkMxLjg4ODEgMTAuMzk5OSAxLjYwMTU2IDEwLjY4NjQgMS42MDE1NiAxMS4wMzk5VjEzLjc1OTlDMS42MDE1NiAxNC4xMTM0IDEuODg4MSAxNC4zOTk5IDIuMjQxNTYgMTQuMzk5OUg0Ljk2MTU2QzUuMzE1MDIgMTQuMzk5OSA1LjYwMTU2IDE0LjExMzQgNS42MDE1NiAxMy43NTk5VjExLjAzOTlDNS42MDE1NiAxMC42ODY0IDUuMzE1MDIgMTAuMzk5OSA0Ljk2MTU2IDEwLjM5OTlaIiBmaWxsPSIjZmZmIi8%2BCjxwYXRoIGQ9Ik0xMy43NTg0IDEuNjAwMUgxMS4wMzg0QzEwLjY4NSAxLjYwMDEgMTAuMzk4NCAxLjg4NjY0IDEwLjM5ODQgMi4yNDAxVjQuOTYwMUMxMC4zOTg0IDUuMzEzNTYgMTAuNjg1IDUuNjAwMSAxMS4wMzg0IDUuNjAwMUgxMy43NTg0QzE0LjExMTkgNS42MDAxIDE0LjM5ODQgNS4zMTM1NiAxNC4zOTg0IDQuOTYwMVYyLjI0MDFDMTQuMzk4NCAxLjg4NjY0IDE0LjExMTkgMS42MDAxIDEzLjc1ODQgMS42MDAxWiIgZmlsbD0iI2ZmZiIvPgo8cGF0aCBkPSJNNCAxMkwxMiA0TDQgMTJaIiBmaWxsPSIjZmZmIi8%2BCjxwYXRoIGQ9Ik00IDEyTDEyIDQiIHN0cm9rZT0iI2ZmZiIgc3Ryb2tlLXdpZHRoPSIxLjUiIHN0cm9rZS1saW5lY2FwPSJyb3VuZCIvPgo8L3N2Zz4K&logoColor=ffffff)](https://zread.ai/ihongs/HongsCORE)

- 软件版本: 1.2.0
- 文档版本: 20260425
- 开发作者: 黄弘(Hongs)
- 技术支持: kevin.hongs@gmail.com

**HongsCORE** 即 **Hong's Common Object Requesting Engine**, 通用对象请求引擎, 拼凑的有些生硬. 在设计第一个原型框架时(PHP 版 2006 年), 我买了一台 Intel Core CPU 的笔记本电脑, 当时随意的给她取了个名字叫 Core, 后来觉得名字应该更有意义才扩展成了以上缩写.
另一个原因是: 从最初的 PHP 版一直到现在的 Java 版, 都有设立一个核心工厂类, 用于请求和管理唯一对象, 以实现  Singleton (单例模式), 需要某对象时只管请求, 使对象的使用效率更高. 具体到这个 Java 版本中, 利用 ThreadLocal 以及 Tomcat,Jetty 等 Servlet 容器的单实例多线程特性来实现行之有效的单例模式 (根据实际情况分为全局单例和线程单例).

## 许可说明

本软件及源码以 [**MIT License**](LICENSE) 协议发布，源码开放使用和修改，依赖的库请参阅其对应的许可声明。请在源码中保留作者的署名（**黄弘, Huang Hong, Hongs**）等信息。

> 被授权人权利：
> 被授权人有权利使用、复制、修改、合并、出版发行、散布、再授权及贩售软件及软件的副本。
> 被授权人可根据程序的需要修改授权条款为适当的内容。

> 被授权人义务：
> 在软件和软件的副本中都必须包含版权声明和许可声明。

## 特性概叙

 1. 支持库表配置, 支持多数据库;
 2. 支持 JOIN 或 IN 的关联查询, 并可自动处理关联插入;
 3. 支持隶属/一对多/多对多等关联模式并能自动处理关系;
 4. 有简单的存取模型基类, 对常规增删改查无需编写代码;
 5. 统一的服务端维护程序, 可与前端应用共享配置及模型;
 6. 简单有效的动作权限解决方案;
 7. 与对应的 HongsCORE4JS(for Javascript) WEB 组件配合实现高效的开发方案;
 8. 默认嵌入 jetty,sqlite,lucene 等库, 除 JDK 外无需安装其他软件即可运行;
 9. 内含自助模块, 无需编程即可构建简单的信息管理系统;
10. 内含统计模块, 可实现简单分类、区间及分组聚合统计.

另见 [**开发手册**](MANUAL/MANUAL.md) 及 [**HongsCORE framework for Javascript**](hongs-wms/web/static/assets/src/).

## 使用方法

请先安装 JDK 和 Maven, 然后进入您的工作目录, 按以下流程执行命令:
```bash
    # 获取并构建系统
    git clone --depth 1 https://github.com/ihongs/HongsCORE.git
    cd  HongsCORE
    mvn clean package

    # 设置和启动系统
    cd  hongs-wms/target/HongsXXX
    bin/app source setup
    bin/app server start --DEBUG 6
    # 按 ctrl+c 停止运行
```

- `hongs-serv-web/target/HongsWeb` 为基础系统, 仅包含基本的应用服务和前端组件等;
- `hongs-serv-wms/target/HongsWMS` 为管理系统, 拥有全功能的用户管理和自助模块等.
- 以上两个包内都放有 Dockerfile 可轻构建容器镜像, 搭建虚拟系统.
- 加 --DEBUG 6 可开启调试及警告输出模式, 可在控制台(命令行)显示执行过程(调试信息), server start 命令可跟数字表示启动端口, 默认 8080 端口. 同时为 windows 用户提供了 setup.bat 和 start.bat 两个快捷命令来执行以上同等任务, windows 用户只需双击即可.
- 注意[1]: 退出运行应该用 Ctrl+C 而不应当直接关闭命令窗口, 后者可能导致直接死掉而无法执行退出清理, 再次启动需要删除 var/server 下端口对应的 `.pid` 文件才行. 也可用通过 `bin/hdo server stop` 命令来停止.
- 注意[2]: 需要 JDK 而非 JRE(Java) 才能运行, 使用前请确保 JDK 已安装并加入 PATH 环境变量, 或设置 JAVA_HOME 环境变量为 jdk 的安装目录(Windows 必须设置). 在官网下载并使用安装程序安装的通常已自动设置好了.

## 类库依赖

适用 Java 版本 JDK 1.8 及以上, 推荐使用 17; Java 库依赖情况请参见各个 module 中 pom.xml 的 dependencies 部分; Javascript 及 CSS 部分默认依赖 jQuery,jQueryUI,Bootstrap 等, 更多参见 [**Powered By**](hongs-web/web/power.html).

- JDK 下载地址: http://www.oracle.com/technetwork/java/javase/downloads/index.html
- MVN 下载地址: http://maven.apache.org/download.cgi
- OpenJDK 地址: http://jdk.java.net/archive/

## 开发理念

首先必须承认，此项目核心思想并非独创，参考了 Django/MongoDB/CouchDB 等。

迄今为止(2016年)，我做了上十年的 OLTP/OLAP 开发，大部分时候就是面对数据结构来回的倒腾，就想为什么要重复的写这些增删改查呢？这么多的业务逻辑有什么底层共性吗？万变不离其宗的宇宙终极规律没发现，但只要把应用范围缩小到一个特定的范围，**一般情况自动处理，特殊情况可以干预**，那么就能从占工作量大半的转换逻辑中抽身出来，从而可以去处理更多特别的事情。

想象流程是从一个数据结构转换成另一个数据结构。比如从 URL `x?a=123&b[]=456&b[]=789` 转换成数据结构 `{a:123, b: [456,789]}` 再转成查询语句 `SELECT * FROM x WHERE a = 123 AND b IN (456, 789)`。把这个道理再扩大一点，如果在服务器这边拥有一个资源 x 的描述文件(scheme)，据此进行结构翻译（转换）。对这个 scheme 进一步丰富，还可以处理资源的关联、输入的校验、输出的处理等等。这个概念还可以扩展到 HTML 的表单、列表、详情展示等，从一个数据描述结构转换成另一种数据呈现结构。如果需要进行限制，中间环节改变数据流或进行一个深度合并即可。

在这套系统里，这个 scheme 文件有两种：一是早期围绕关系数据库做的 [.db.xml](hongs-core/src/main/resources/io/github/ihongs/config/default.db.xml)，结合数据库的表结构，来描述资源模型；另一个是后定义的 [.form.xml](hongs-core/src/main/resources/io/github/ihongs/config/default.form.xml) 配置，旨在完整的描述数据结构、枚举数据、校验规则，这主要是受到 Protobuf 的启发。后者衍生出了针对 lucene 的模型, 亦可用于 neo4j,mongodb,couchdb 等。

正确的解释并查询是一方面，但也需要在正确的存储后才能保障，故校验规则是这套体系里非常重要的部分。与别的校验框架理念并不相同，别人可能只在意数据能不能被许可往下传递，而这个系统更关注如何向下传递需要的数据。比如：传递过来一个文件，在这套系统并不仅仅关心这个文件的格式、尺寸对不对，更要处理存到哪里、如何组织 URL，如果是图片，可能还要按自定规则处理成缩略图。[Verify](hongs-core/src/main/java/io/github/ihongs/util/verify/Verify.java) 是校验入口，通过同包下的其他 Rule 类进行校验，也支持函数式的方式快速自定规则；另外利用 [VerifyHelper](hongs-core/src/main/java/io/github/ihongs/action/VerifyHelper.java) 可以将 .form.xml 中的设置"翻译"成实际的校验规则。

更多请参阅 [**开发手册**](MANUAL/MANUAL.md)

## 开发准则

> 以下为 K.I.S.S 原则的中文翻译, 这是此项目的核心信仰, 然而我并不会完全遵守。

**[KEEP IT SIMPLE, STUPID!](K.I.S.S.md)**

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
