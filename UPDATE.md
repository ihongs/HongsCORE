# 更新日志

* 2020/12/19 错误码改为十进制表示
* 2020/12/12 动态模板中总是采用 xxx.xxx 映射到 xxx.xxx.jsp 的方式, 不再对 .html 做特殊处理, 从而规避一个文件可能存在多种路径, 规避可能导致的访问限制疏忽的问题
* 2020/12/04 增加聚合分组统计, 重写整个统计模块
* 2020/10/18 请求参数及响应数据中的 enum 更名为 enfo
* 2020/09/20 统一导航配置的 href,hrel 属性的忽略规则，均采用 ! 开头表示特别配置
* 2020/09/06 创建从返回完整信息改为仅返回编号(nid); 返回数据 size 更名为 cnt, enum 更名为 enfo; ab 参数 .enum 更名为 .enfo
* 2019/08/10 不再遵循 REST/CRUD 的资源4个方法规则, 取消统计和历史的虚拟子实体层级, 方法直接命名在主实体上; 并增加 select 接口为创建时提供默认值和枚举等, 使创建不再依赖查询权限
* 2019/08/01 升级 lucene 到 7.7.2, 升级 jetty 到 9.4.19, 升级其他组件到最新的稳定版
* 2019/07/28 去掉查询条件的关系代号中的前缀":", 关系符均为两个字母，不再使用前缀
* 2019/03/23 搜索模块中精确模糊匹配不再二选一, 利用冗余的隐藏字段使得 Matrix 模块中一般结构变更不再需要重建索引
* 2019/01/09 移除 Mview 类, 改写 auto 模板, 不再依赖 Mview 和 Data
* 2018/10/28 增加 WEB 服务启动任务配置, 用于执行一些初始化或设置计划任务等
* 2018/08/12 分离全局和线程内 Core 的实现, 全局 Core 内增加统一读写锁, 取消外部分散的全局同步和读写锁
* 2018/07/12 将包名 app.hongs 更名为 io.github.ihongs, 启动脚本名更名为 hdo 和 hdb
* 2018/04/14 彻底移除 ueditor js 和 jsp,java 组件, 改用 summernote 提供富文本编辑的功能
* 2018/03/23 hongs-serv-member 模块更名为 hongs-serv-master,
* 2018/03/22 hongs-serv-socket,hongs-serv-graphs,hongs-serv-medium 等模块从基础版中移除
* 2018/03/03 增加图数据支持(Neo4j)
* 2017/12/30 去掉查询条件的关系代号中的前缀"!"
* 2017/11/12 将全部 manage 更名为 centra, handle 更名为 centre, 增加小型外卖系统示例
* 2017/08/04 表单配置中, 字段的 xxxxable 仅控制模板, 表单的 xxxxable 才控制查询
* 2017/05/27 升级 lucene 5.5 到 6.5
* 2017/03/20 重写缩略图工具, 增加背景颜色设置、拼贴位置计算等, 删减文件读写方法, 使其仅作为 Thumbnails 的简单补充
* 2017/03/12 增加会话过滤、存储工具, 彻底解决用 API 用参数传递 sesion token 的问题
* 2016/10/21 去掉 commons-fileupload 依赖, 改用 Servlet 3 所带 Part 类进行上传处理
* 2016/10/20 废弃 Core.Destroy 改用 AutoCloseable 接口, destroy 方法均改名为 close
* 2016/09/19 彻底去除 FetchCase 中当前表字段表示方法; 模型默认为严格模式, 简单拼接速度较快; 启用聪明模式后将自动为关联的各用例中的字段、条件补全表名和别名; 未指明模式时, 当有关联时自动开启聪明模式
* 2016/09/04 重写 Model 的 filter 相关方法, 将全部过滤逻辑在 AssocCase 中重写, 改写 Synt.filter(原名foreach) 以适应 Java8 的函数式形式
* 2016/08/06 将 db 包中的 FetchCase,FetchMore 等类迁移到 app.hongs.db.util 下
* 2016/05/20 增加 sessionId 通过参数传递的方式, 使用 sessionId 作为 API Token, 简化接口的会话类数据操作 (2016/05/22 通过研究 Tomcat 和 Jetty 的源码, 无法完美解决此问题, 故只能废弃)
* 2016/05/18 在 hongs-web 的 pom.xml 中编写 ant 代码以替代原来的 pack.sh 打包发布操作
* 2016/05/14 增加 HongsUnchecked 异常类, 用于表示 unchecked exception, 此前的部分 HongsError 使用有误
* 2016/05/05 数据库连接池从 C3P0 更换为 DBCP, app.hongs.db.DB.Roll 更名为 app.hongs.db.link.Loop
* 2016/05/03 增加 Lucene 查询迭代, 增加公共注册动作
* 2016/04/16 修改 FetchCase/FetchMore 等, 增加级联操作, 解决关联查询中的别名、层级问题
* 2016/01/05 增加 medium,market 两个模块, 分别用于媒体和销售; 拆分基础管理服务为 member,module 作为独立模块
* 2016/01/03 Web,Action 结构更新, 分成 manage,handle,static 三大块, 分别表示: 管理区,应用区,静态区
* 2015/11/24 FetchCase 中组织 SQL 语句不再需要加 "." 前缀即可在关联查询中自动为字段添加当前表别名
* 2015/10/30 取消了 object.config 来初始化组件的方式, 改为 data-* 的配置方式
* 2015/10/13 建立 PackAction 用于一次请求多个 Action; 修复 SQL Like 生成语句问题; 将 FetchNext 更名为 DB.Roll, DBAssist 改回 Mview
* 2015/09/23 修复关联查询子表数据未获取的问题
* 2015/09/22 将常用的参数等集中放到 app.hongs.Cnst 类中, 这样就不用总去读配置了，此部分参数本就属于约定胜于配置的，并无配置的意义
* 2015/09/22 添加了 app.hongs.action.PickedHelper 助手类, 可以更方便的通过 ActionRunner 本地调用, 为 form 关联查询数据了
* 2015/09/20 完成模块管理功能; 更新、优化、修复其他 NullPointException, JS 等问题
* 2015/08/20 更新 create 方法的返回结构，同 retrieve[info] 避免前端需要识别不同数据结构
* 2015/05/28 强化并首选 jetty 作为嵌入式 web 容器; 因其他容器的 jsp 版本可能与 jetty 使用的冲突, 需要去掉 jsp 相关的包方可在 tomcat 等容器下正常打开页面
* 2015/04/24 完善 Async 并添加 Batch 异步操作类, 为异步任务和消息处理进行准备
* 2015/03/03 重构, 完善权限模块; 重写 Dict.get,Dict.put 及 JS 中的对应方法, 使程序逻辑更简单, 更接近 PHP 的关系数组操作方式
* 2015/02/18 将 ActionWarder 的 Filter 模式改回 0.3.1 的独立继承模式, 新类为 ActionDriver, 兼容 ActionWarder 的 Filter 初始化模式; 今天是我的公历生日, 祝自己生日快乐, 万事如意!
* 2015/02/14 增加 jetty,sqlite, 默认使用 jetty 运行, 数据库默认使用 sqlite
* 2015/01/18 大规模重构, 重新规划 Maven 模块结构
* 2014/11/20 实现 REST 风格的 API, 重新规划了 Action 的组织结构
* 2014/11/10 切换到 Maven
* 2014/08/10 全面倒向 bootstrap, 大规模的使用 tab, 放弃使用浮层作为选择框
* 2014/08/09 为更好的实现自己的规则, 重写了前端验证程序
* 2014/05/17 增加后端验证助手类 app.hongs.action.VerifyHelper, 配合 Verify 注解可以更方便的对传递过来的表单数据进行验证
* 2014/04/04 使用 c3p0 作为数据源组件, 去掉原来的用 jdbc 直接连接的方式, 但保留了使用外部容器的数据源的方式
* 2014/04/03 引入动作注解链，对常规数据的注入、验证以及存储事务提供更方便的操作
* 2014/01/27 去掉全部模型属性的外部名称(id,pid等), 统一使用实际的属性名, 避免因名称转换(同一属性不同场合不同名称)导致理解困难
* 2014/01/25 重写 CmdletHelper 的 args 解析程序, 保留原来类似 Perl 的 Getopt::Longs 的解析规则, 去掉对短参的支持, 不再转换 args 的数组类型. cmdlet 函数也与 main 函数参数一致
* 2013/12/26 使用 InitFilter 处理框架的初始化, 返回数据在 InitFilter 结束前不写入 response 对象, 方便动作注解和其他过滤器对返回数据进行处理
* 2013/10/20 在样式上统一使用 bootstrap(不采取其 JS, JS 仍然使用 jquery.tools)
* 2013/05/26 将项目托管到 GitHub 上进行管理
* 2013/03/08 完成 hongs-core-js 的 jquery-tools 的迁移, 前端组件支撑由原来的 jquery-ui 改为 jquery-tools
* 2012/04/26 将 app.hongs.util.Text 里的数字操作部分拿出来放到 app.hongs.util.Num 里
* 2011/09/25 基本完成 hongs-core-js 的jquery重写实现, 大部分组件已可以使用(不支持拖拽、树搜索)
* 2011/03/23 新增 UploadHelper 上传助手类(使用 apache 的 commons-fileupload)
* 2011/01/25 树模型支持搜索逐个定位(Javascript)
* 2011/01/01 表格列支持拖拽改变尺寸(Javascript)
* 2010/11/21 支持多表串联(JOIN模式)
* 2010/11/12 支持多表串联(IN模式)
* 2010/11/11 更改配置及消息体系(Javascript)
* 2010/09/20 增加日期选择功能(Javascript)
* 2010/08/15 增加浮动块功能(Javascript)
* 2010/06/30 增加 JSP 扩展标记库, 与 Servlet 共享配置和语言对象等
* 2010/05/01 增加动作权限过滤
