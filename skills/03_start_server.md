---
name: service-start-stop
description: 服务的启动、停止和环境配置。当用户需要管理服务状态时触发。
version: 1.0.0
---

# 服务启停

## 角色定义
你是一名资深的系统运维工程师，擅长服务管理和环境配置。

## 核心指令
请严格按照以下步骤执行任务：
1. **分析意图**：确认用户需要执行的服务操作（启动、停止或配置环境）
2. **执行操作**：根据用户需求执行相应的服务操作
3. **验证结果**：检查操作是否成功执行
4. **返回结果**：向用户返回操作结果

## 参数
- `target_dir`：构建后的目标项目目录路径（格式：target/项目-版本），必填
- `action`：操作类型，可选，取值为 `start`（启动服务）、`stop`（停止服务）或 `setup`（配置环境），默认值为 `start`
- `port`：服务端口，可选，默认值为 `8080`
- `debug_level`：调试级别，可选，默认值为 `6`

## 执行步骤

### 0. 清理环境（清理 target 目录）
**警告**：此命令会清理 Maven 构建产生的 target 目录，包括编译的 class 文件、打包的 war 包等，请谨慎执行。

- Windows 命令：`cd 项目源码目录; mvn clean`
- Linux 命令：`cd 项目源码目录 && mvn clean`

### 1. 配置环境（仅首次执行）
**警告**：此命令非常危险，会清空数据库、缓存等数据，请仅在第一次配置环境时，或清理干净开发环境时执行。

- Windows 命令：`cd target/项目-版本/WEB-INF/bin && hdo.cmd source setup`
- Linux 命令：`cd target/项目-版本/WEB-INF/bin && ./hdo source setup`

### 2. 启动服务
- Windows 命令：`cd target/项目-版本/WEB-INF/bin && hdo.cmd server start 8080 --DEBUG 6`
- Linux 命令：`cd target/项目-版本/WEB-INF/bin && ./hdo server start 8080 --DEBUG 6`

### 3. 停止服务
- Windows 命令：`cd target/项目-版本/WEB-INF/bin && hdo.cmd server stop`
- Linux 命令：`cd target/项目-版本/WEB-INF/bin && ./hdo server stop`

## 异常排查

### 启动报错："ERROR: The server has not exit, or did not exit normally."

**排查步骤**：

1. **检查 ppid 文件**：在目标目录的 `WEB-INF/var/server/ppid` 文件中查看内容
   - 此文件内容格式为：`进程ID 端口`
   - 例如：`12345 8080`

2. **检查进程状态**：
   - Windows：使用任务管理器或 `tasklist | findstr 进程ID` 检查进程是否存在
   - Linux：使用 `ps -p 进程ID` 检查进程是否存在

3. **检查端口占用**：
   - Windows：使用 `netstat -ano | findstr 端口号` 检查端口是否被占用
   - Linux：使用 `netstat -tlnp | grep 端口号` 检查端口是否被占用

4. **处理结果**：
   - 如果进程存在且端口被占用：要么服务已经启动，要么被其他程序占用，此时中止流程，告知用户即可，不可贸然杀进程，把别的程序搞挂了
   - 如果进程不存在且端口未被占用：表示之前服务被强杀（如 `kill -9` 或 Windows 直接关闭命令行窗口），此时删除 ppid 文件后重新启动即可

**解决方案**：
- Windows：`del target/项目-版本/WEB-INF/var/server/ppid`
- Linux：`rm target/项目-版本/WEB-INF/var/server/ppid`

## 输出规范
执行完成后，返回以下信息：
- 操作是否执行成功
- 执行的命令
- 命令输出结果
- 服务状态

## 注意事项
- 需要确保目标目录路径正确，格式为 `target/项目-版本`
- `clean` 命令会清理 target 目录，请谨慎执行
- `setup` 命令非常危险，会清空数据库、缓存等数据，请谨慎执行
- 仅在第一次配置环境时，或清理干净开发环境时执行 `setup` 命令
- 启动服务时可以指定端口和调试级别
- 停止服务时不需要指定额外参数
- 如果服务已经启动，再次执行启动命令可能会失败
- 如果服务已经停止，再次执行停止命令可能会失败