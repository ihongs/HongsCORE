# HongsCORE 构建示例

本指南说明如何基于 HongsCORE 框架构建一套学生档案管理系统。

## 前置条件

- JDK 1.8+
- Maven 3.6+
- Git (可选)

## 项目结构

```
current-project/    # 当前项目
├── pom.xml         # Maven 项目配置
├── web/
│   └── WEB-INF/
│       └── etc/
│           ├── centra/
│           │   ├── cust.navi.xml       # 后台自定义导航
│           │   └── data/
│           │       ├── class.form.xml  # 后台班级、学生表单配置
│           │       └── class.navi.xml  # 后台班级、学生导航与权限配置
│           └── centre/
│               ├── cust.navi.xml       # 前台自定义导航
│               └── data/
│                   ├── class.form.xml  # 前台班级、学生表单配置
│                   └── class.navi.xml  # 前台班级、学生导航与权限配置
```

## 1. 创建 Maven 项目

### pom.xml 配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>my-test</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>war</packaging>

    <name>学生档案管理系统</name>
    <description>基于 HongsCORE 框架的学生档案管理系统</description>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.source>1.8</maven.compiler.source>
        <maven.compiler.target>1.8</maven.compiler.target>
    </properties>

    <dependencies>
        <dependency>
            <groupId>io.github.ihongs</groupId>
            <artifactId>hongs-wms</artifactId>
            <version>1.2-SNAPSHOT</version>
            <type>war</type>
        </dependency>
    </dependencies>

    <build>
        <finalName>my-test</finalName>
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
                    <overlays>
                        <overlay>
                            <groupId>io.github.ihongs</groupId>
                            <artifactId>hongs-wms</artifactId>
                        </overlay>
                    </overlays>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

## 2. 配置自定义导航

### centra/cust.navi.xml（后台自定义导航）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<root>
    <rsname>#centra</rsname>
    <import>centra/data/class</import>
</root>
```

### centre/cust.navi.xml（前台自定义导航）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<root>
    <rsname>#centre</rsname>
    <import>centre/data/class</import>
</root>
```

## 3. 配置业务导航和表单

### centra/data/class.navi.xml（后台业务导航）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<root>
    <rsname>#centra</rsname>
    <menu text="学生档案" href="centra/data/class/">
        <menu text="班级管理" href="centra/data/class/">
            <role text="查询" name="centra/data/class/search">
                <action>centra/data/class/search.act</action>
                <action>centra/data/class/recite.act</action>
                <action>centra/data/class/recipe.act</action>
                <action>centra/data/class/acount.act</action>
                <action>centra/data/class/assort.act</action>
                <depend>centra</depend>
            </role>
            <role text="创建" name="centra/data/class/create">
                <action>centra/data/class/create.act</action>
                <depend>centra/data/class/search</depend>
            </role>
            <role text="更新" name="centra/data/class/update">
                <action>centra/data/class/update.act</action>
                <depend>centra/data/class/search</depend>
            </role>
            <role text="删除" name="centra/data/class/delete">
                <action>centra/data/class/delete.act</action>
                <depend>centra/data/class/search</depend>
            </role>
            <role text="回看" name="centra/data/class/reveal">
                <action>centra/data/class/reveal.act</action>
                <depend>centra/data/class/search</depend>
            </role>
            <role text="恢复" name="centra/data/class/revert">
                <action>centra/data/class/revert.act</action>
                <depend>centra/data/class/update</depend>
            </role>
        </menu>
        <menu text="学生管理" href="centra/data/class/student/">
            <role text="查询" name="centra/data/class/student/search">
                <action>centra/data/class/student/search.act</action>
                <action>centra/data/class/student/recite.act</action>
                <action>centra/data/class/student/recipe.act</action>
                <action>centra/data/class/student/acount.act</action>
                <action>centra/data/class/student/assort.act</action>
                <depend>centra</depend>
            </role>
            <role text="创建" name="centra/data/class/student/create">
                <action>centra/data/class/student/create.act</action>
                <depend>centra/data/class/student/search</depend>
            </role>
            <role text="更新" name="centra/data/class/student/update">
                <action>centra/data/class/student/update.act</action>
                <depend>centra/data/class/student/search</depend>
            </role>
            <role text="删除" name="centra/data/class/student/delete">
                <action>centra/data/class/student/delete.act</action>
                <depend>centra/data/class/student/search</depend>
            </role>
            <role text="回看" name="centra/data/class/student/reveal">
                <action>centra/data/class/student/reveal.act</action>
                <depend>centra/data/class/student/search</depend>
            </role>
            <role text="恢复" name="centra/data/class/student/revert">
                <action>centra/data/class/student/revert.act</action>
                <depend>centra/data/class/student/update</depend>
            </role>
        </menu>
    </menu>
</root>
```

### centre/data/class.navi.xml（前台业务导航）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<root>
    <rsname>#centre</rsname>
    <menu text="学生档案" href="centre/data/class/student/">
        <menu text="班级" href="centre/data/class/">
            <role name="centre">
                <action>centre/data/class/search.act</action>
                <action>centre/data/class/recite.act</action>
                <action>centre/data/class/recipe.act</action>
                <action>centre/data/class/acount.act</action>
                <action>centre/data/class/assort.act</action>
            </role>
            <!-- 前台只读 -->
            <role name="_deny_">
                <action>centre/data/class/create.act</action>
                <action>centre/data/class/update.act</action>
                <action>centre/data/class/delete.act</action>
                <action>centre/data/class/delete.act</action>
            </role>
        </menu>
        <menu text="学生" href="centre/data/class/student/">
            <role name="centre">
                <action>centre/data/class/student/search.act</action>
                <action>centre/data/class/student/recite.act</action>
                <action>centre/data/class/student/recipe.act</action>
                <action>centre/data/class/student/assort.act</action>
            </role>
            <!-- 前台只读 -->
            <role name="_deny_">
                <action>centre/data/class/student/create.act</action>
                <action>centre/data/class/student/update.act</action>
                <action>centre/data/class/student/delete.act</action>
            </role>
        </menu>
    </menu>
</root>
```

## 4. 配置表单

### centra/data/class.form.xml（表单配置）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<root>
    <!-- 班级表单 -->
    <form name="class">
        <field name="id" text="ID" type="hidden">
            <param name="default">@id</param>
            <param name="deforce">create</param>
        </field>
        <field name="name" text="班级名称" type="text" required="yes">
            <param name="listable">yes</param>
            <param name="filtable">yes</param>
            <param name="minlength">2</param>
            <param name="maxlength">50</param>
        </field>
        <field name="grade" text="年级" type="select" required="yes">
            <param name="listable">yes</param>
            <param name="filtable">yes</param>
            <param name="enum">grade</param>
        </field>
        <field name="major" text="专业" type="text">
            <param name="listable">yes</param>
            <param name="filtable">yes</param>
            <param name="maxlength">100</param>
        </field>
        <field name="head_teacher" text="班主任" type="text">
            <param name="listable">yes</param>
            <param name="maxlength">50</param>
        </field>
        <field name="state" text="状态" type="select">
            <param name="listable">yes</param>
            <param name="filtable">yes</param>
            <param name="statable">yes</param>
            <param name="enum">state</param>
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

    <!-- 学生表单 -->
    <form name="student">
        <field name="id" text="ID" type="hidden">
            <param name="default">@id</param>
            <param name="deforce">create</param>
        </field>
        <field name="student_no" text="学号" type="text" required="yes">
            <param name="listable">yes</param>
            <param name="filtable">yes</param>
            <param name="minlength">2</param>
            <param name="maxlength">20</param>
        </field>
        <field name="name" text="姓名" type="text" required="yes">
            <param name="listable">yes</param>
            <param name="filtable">yes</param>
            <param name="srchable">yes</param>
            <param name="minlength">2</param>
            <param name="maxlength">50</param>
        </field>
        <field name="gender" text="性别" type="select" required="yes">
            <param name="listable">yes</param>
            <param name="filtable">yes</param>
            <param name="statable">yes</param>
            <param name="enum">gender</param>
        </field>
        <field name="birthday" text="出生日期" type="date">
            <param name="listable">yes</param>
            <param name="filtable">yes</param>
        </field>
        <field name="class_id" text="班级" type="fork" required="yes">
            <param name="listable">yes</param>
            <param name="filtable">yes</param>
            <param name="data-at">centra/data/class/search</param>
            <param name="data-al">centra/data/class/pick.html</param>
            <param name="data-rl">centra/data/class/info.html</param>
            <param name="data-ln">class</param>
            <param name="data-vk">id</param>
            <param name="data-tk">name</param>
        </field>
        <field name="phone" text="联系电话" type="text">
            <param name="listable">yes</param>
            <param name="maxlength">20</param>
        </field>
        <field name="email" text="电子邮箱" type="email">
            <param name="listable">yes</param>
            <param name="maxlength">100</param>
        </field>
        <field name="address" text="家庭住址" type="textarea">
            <param name="maxlength">200</param>
        </field>
        <field name="enrollment_date" text="入学日期" type="date">
            <param name="listable">yes</param>
            <param name="filtable">yes</param>
        </field>
        <field name="photo" text="照片" type="image" rule="Thumb">
            <param name="path">static/upload/class/student/photo</param>
            <param name="href">static/upload/class/student/photo</param>
            <param name="accept">image/*</param>
            <param name="thumb-kind">jpg</param>
            <param name="thumb-size">200x200</param>
            <param name="thumb-mode">pick</param>
        </field>
        <field name="remark" text="备注" type="textarea">
            <param name="maxlength">500</param>
        </field>
        <field name="state" text="状态" type="select">
            <param name="listable">yes</param>
            <param name="filtable">yes</param>
            <param name="statable">yes</param>
            <param name="enum">student_state</param>
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

    <!-- 枚举定义 -->
    <enum name="state">
        <value code="1">启用</value>
        <value code="0">禁用</value>
    </enum>

    <enum name="gender">
        <value code="1">男</value>
        <value code="2">女</value>
    </enum>

    <enum name="grade">
        <value code="1">一年级</value>
        <value code="2">二年级</value>
        <value code="3">三年级</value>
        <value code="4">四年级</value>
        <value code="5">五年级</value>
        <value code="6">六年级</value>
        <value code="7">初一</value>
        <value code="8">初二</value>
        <value code="9">初三</value>
        <value code="10">高一</value>
        <value code="11">高二</value>
        <value code="12">高三</value>
    </enum>

    <enum name="student_state">
        <value code="1">在读</value>
        <value code="2">休学</value>
        <value code="3">毕业</value>
        <value code="0">退学</value>
    </enum>
</root>
```

### centre/data/class.form.xml（前台表单）

前台表单可以通过 `extend` 继承自后台表单：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<root>
    <!-- 前台班级表单继承自后台 -->
    <form name="class" extend="centra/data/class:class">
    </form>

    <!-- 前台学生表单继承自后台 -->
    <form name="student" extend="centra/data/class:student">
    </form>
</root>
```

## 5. 构建 HongsCORE 框架

首先需要构建并安装 HongsCORE 框架到本地 Maven 仓库：

```bash
cd /path/to/HongsCORE
mvn install
```

## 6. 构建应用项目

```bash
cd /path/to/current-project
mvn package
```

## 7. 初始化数据

```bash
cd /path/to/current-project/target/release-name/WEB-INF
bin/hdo source setup
```

AI 自动测试可导入密钥, 执行 `bin/hdo source test/00.ai.sql`，之后测试可带上请求头 `Authorization: Bearer I-AM-THE-KING`。

## 8. 启动服务器

```bash
cd /path/to/current-project/target/release-name/WEB-INF
bin/hdo server start --DEBUG 6
```

参数 `--DEBUG 6` 为输出标准控制台(0)+记录INFO/WARN(2)+记录TRACE/DEBUG(4)，如需增加记录到日志(1)，换成 `--DEBUG 7` 会记录到日志 `var/run.log`。

## 9. 关闭服务器

```bash
cd /path/to/current-project/target/release-name/WEB-INF
bin/hdo server stop
```

也可以用 `kill PID` 命令关闭，`/path/to/current-project/target/release-name/WEB-INF/var/server/ppid` 存放着 '进程ID 端口号'。尽量不要用 `kill -9` 杀进程，以免导致内存数据未及时存盘等问题。如确认进程已意外中止，可删除 ppid 文件后重启程序。

## 10. 访问应用

- 后台管理：http://localhost:8080/centra/
- 前台应用：http://localhost:8080/centre/

## 表单配置要点

### 字段类型

- `text` - 文本输入
- `textarea` - 多行文本
- `select` - 下拉选择
- `hidden` - 隐藏字段
- `date`/`datetime` - 日期/时间
- `image` - 图片上传
- `email` - 邮箱
- `fork` - 关联选择

### 常用参数

- `listable` - 是否在列表显示
- `filtable` - 是否可筛选
- `srchable` - 是否可搜索
- `statable` - 是否可统计
- `default` - 默认取值
- `deforce` - 强制方式（create/always）
- `min`/`max` - 最小/最大数值
- `minlength`/`maxlength` - 最小/最大长度
- `minrepeat`/`maxrepeat` - 最少/最多数量, 针对开启 repeated
- `pattern` - 正则验证
- `enum` - 枚举名称

### 权限配置

- `role` - 角色定义
- `action` - 动作权限
- `depend` - 依赖其他 role
- `name="_deny_"` - 特殊角色，用于禁止某些操作

## 导航配置要点

- `import` - 导入其他导航配置（注意不要加 `.navi.xml` 后缀）
- `rsname` - 权限会话名称（用其他配置的会话）
- `href` - 菜单链接
- `hrel` - 内部链接
- `text` - 菜单显示文本

## 配置继承说明

以下配置文件可以从 hongs-wms 默认继承，如无变更不必在项目中创建：

- `defines.properties` - 启动配置
- `default.properties` - 默认配置
- `centra.navi.xml` - 后台导航
- `centre.navi.xml` - 前台导航

只需在 `centra/cust.navi.xml` 和 `centre/cust.navi.xml` 中添加自定义导航导入即可。

## 注意事项

1. **表单与导航路径**：如果表单名称与导航最后一级目录相同，导航路径要省略最后一级（如表单是 class，导航用 `centra/data/class/` 而不是 `centra/data/class/class/`）
2. **回看和恢复权限**：回看（reveal）依赖查询（search），恢复（revert）依赖更新（update）。
3. **权限控制**：未在 `.navi.xml` 中配置的动作不受限制，若动作明确谁也不让用，将其放入 `_deny_` 角色下面。
4. **接口权限**: 初始化数据时导入密钥，请求头带上 `Authorization: Bearer I-AM-THE-KING` 和 `X-Requested-With: AJAX`。

## 扩展开发

### 添加自定义页面

可以在以下位置添加自定义 HTML 页面：

- `web/centra/data/class/student/` - 后台学生模块页面
- `web/centre/data/class/student/` - 前台学生模块页面

常用页面：
- `default.html` - 引导页面
- `form_init.html` - 创建表单
- `form.html` - 编辑表单
- `info.html` - 查看详情
- `list.html` - 列表页面
- `pick.html` - 选择页面

### 添加自定义 Java 代码

如需添加自定义 Java 代码：

1. 创建 `src/main/java` 目录
2. 编写 Java 类，加上 `@Action("path/to/entity")` 注解
3. 编写 `public void actionName(ActionHelper helper)` 方法，加上 `@Action` 注解，若注解不给 value 则同方法名
4. 拷贝 `hongs-wms/web/WEB-INF/defines.properties` 到当前项目的 `web/WEB-INF` 目录并编辑它，在 `mount.serv` 添加相应的包路径

### 自定义数据库

演示使用 SQLite，实际使用请务必更换。 

1. 添加相应的 JDBC 驱动依赖到 pom.xml
2. 创建自定义 `default.db.xml` 配置数据库，结构如下：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<db xmlns="http://hongs-core"
xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
xsi:schemaLocation="http://ihongs.github.io/hongs-core db.xsd">
  <source jdbc="org.sqlite.JDBC"
          name="jdbc:sqlite:default.db">
      <param name="connectionProperties">
          autoReconnect=true;useUnicode=true;characterEncoding=utf8;zeroDateTimeBehavior=convertToNull;useCursorFetch=true
      </param>
  </source>
</db>
```
