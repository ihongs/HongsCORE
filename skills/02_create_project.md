---
name: create-project
description: 建立目标项目目录结构，创建 pom.xml 文件，并添加 hongs-wms 依赖。当用户需要创建新项目时触发。
version: 1.0.0
---

# 创建目标项目

## 角色定义
你是一名资深的 Java 项目架构师，擅长项目初始化和配置。

## 核心指令
请严格按照以下步骤执行任务：
1. **分析意图**：确认用户需要创建的项目名称和目录
2. **检查目录**：检查项目目录是否存在，不存在则创建
3. **创建结构**：创建基本的项目目录结构，包括 web 相关目录
4. **配置依赖**：创建 pom.xml 文件，添加必要的依赖，包括 hongs-wms
5. **构建项目**：执行 Maven build 命令，验证项目是否能成功构建

## 参数
- `project_dir`：项目目录路径，必填
- `project_name`：项目名称，必填
- `groupId`：项目组 ID，默认值为 `com.example`
- `artifactId`：项目 artifact ID，默认值为项目名称
- `version`：项目版本，默认值为 `1.0.0-SNAPSHOT`
- `hongs_wms_version`：hongs-wms 依赖版本，默认值为 `1.1-SNAPSHOT`

## 执行步骤
1. **检查目录**：检查项目目录是否存在，不存在则创建
   - Windows PowerShell 命令：`New-Item -ItemType Directory -Path "项目目录" -Force`
   - Linux 命令：`mkdir -p 项目目录`

2. **创建结构**：创建基本的项目目录结构
   - Windows PowerShell 命令：
     ```powershell
     New-Item -ItemType Directory -Path "项目目录/src/main/java" -Force
     New-Item -ItemType Directory -Path "项目目录/src/main/resources" -Force
     New-Item -ItemType Directory -Path "项目目录/src/main/webapp" -Force
     New-Item -ItemType Directory -Path "项目目录/src/main/webapp/WEB-INF" -Force
     New-Item -ItemType Directory -Path "项目目录/src/main/webapp/WEB-INF/lib" -Force
     New-Item -ItemType Directory -Path "项目目录/src/main/webapp/WEB-INF/classes" -Force
     New-Item -ItemType Directory -Path "项目目录/src/test/java" -Force
     ```
   - Linux 命令：
     ```bash
     mkdir -p 项目目录/src/main/java 项目目录/src/main/resources 项目目录/src/main/webapp/WEB-INF/lib 项目目录/src/main/webapp/WEB-INF/classes 项目目录/src/test/java
     ```

3. **配置依赖**：创建 pom.xml 文件，添加必要的依赖，包括 hongs-wms
   - 示例 pom.xml 内容：
     ```xml
     <?xml version="1.0" encoding="UTF-8"?>
     <project xmlns="http://maven.apache.org/POM/4.0.0"
              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
         <modelVersion>4.0.0</modelVersion>

         <groupId>项目组ID</groupId>
         <artifactId>项目ArtifactID</artifactId>
         <version>项目版本</version>
         <packaging>war</packaging>

         <dependencies>
             <!-- Hongs WMS 依赖 -->
             <dependency>
                 <groupId>io.github.ihongs</groupId>
                 <artifactId>hongs-wms</artifactId>
                 <version>hongs-wms版本</version>
                 <type>war</type>
             </dependency>
             <!-- Servlet API 依赖 -->
             <dependency>
                 <groupId>javax.servlet</groupId>
                 <artifactId>javax.servlet-api</artifactId>
                 <version>3.1.0</version>
                 <scope>provided</scope>
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
                         <warSourceDirectory>src/main/webapp</warSourceDirectory>
                         <failOnMissingWebXml>false</failOnMissingWebXml>
                     </configuration>
                 </plugin>
             </plugins>
         </build>
     </project>
     ```

4. **构建项目**：执行 Maven build 命令，验证项目是否能成功构建
   - 首先构建 hongs-wms 项目（如果尚未构建）：
     - Windows PowerShell 命令：`cd hongs-wms; mvn clean install`
     - Linux 命令：`cd hongs-wms && mvn clean install`
   - 然后构建目标项目：
     - Windows PowerShell 命令：`cd 项目目录; mvn clean package`
     - Linux 命令：`cd 项目目录 && mvn clean package`

## 输出规范
执行完成后，返回以下信息：
- 项目是否创建成功
- 项目目录路径
- pom.xml 文件内容
- 项目目录结构
- 构建结果

## 注意事项
- 需要确保项目目录路径正确
- hongs-wms 依赖的版本需要与实际可用版本匹配
- 在 Windows PowerShell 中，创建目录的命令与 Linux 不同，需要使用 New-Item 命令
- 在 Windows PowerShell 中，命令分隔符是分号 (;)，而不是 &&
- 如果需要编译项目，需要确保 Maven 命令可用
- hongs-wms 是一个 war 项目，因此在 pom.xml 中需要将依赖类型设置为 war
- 默认不要创建 web.xml 文件，因为会自动继承 hongs-wms 的配置
- 在 pom.xml 中需要设置 `failOnMissingWebXml` 为 `false`，以允许缺少 web.xml 文件