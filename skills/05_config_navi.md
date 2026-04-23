---
name: config-navi
description: 配置导航菜单、权限等，创建 .navi.xml 文件并定义导航结构。当用户需要配置导航时触发。
version: 1.0.0
---

# 配置导航

## 角色定义
你是一名资深的软件开发工程师，擅长导航配置和权限管理。

## 核心指令
请严格按照以下步骤执行任务：
1. **分析意图**：确认用户需要配置的导航菜单和权限
2. **创建目录**：在项目的配置目录中创建导航配置子目录
3. **配置导航**：创建 .navi.xml 文件，定义导航结构和权限
4. **更新引用**：在 cust.navi.xml 中添加 import 引用
5. **验证配置**：检查配置文件格式是否正确
6. **测试验证**：在项目中测试导航配置是否生效

## 参数
- `project_dir`：项目目录路径，必填
- `navi_name`：导航配置名称，必填
- `config_location`：配置文件位置，可选，取值为 `web-inf`（默认）、`resources` 或 `global`
- `scope`：导航作用域，可选，取值为 `centra`（后台，默认）或 `centre`（前台）

## 配置文件位置
导航配置有两个位置，优先级从高到低：

1. **WEB-INF/etc 目录**（推荐）：
   - `src/main/webapp/WEB-INF/etc/centra/data`：针对后台，不打包，适合一般性配置和可能修改的配置
   - `src/main/webapp/WEB-INF/etc/centre/data`：针对前台，不打包，适合一般性配置和可能修改的配置

2. **resources 目录**：
   - `src/main/resources/centra/data`：针对后台，会打包进 jar，适合不变的配置
   - `src/main/resources/centre/data`：针对前台，会打包进 jar，适合不变的配置

**优先使用 WEB-INF/etc 目录**，因为这些配置不打包，方便后期修改。

## 执行步骤
1. **创建目录**：根据配置位置和作用域创建相应的目录结构
   - 后台目录：`New-Item -ItemType Directory -Path "项目目录/src/main/webapp/WEB-INF/etc/centra/data" -Force`
   - 前台目录：`New-Item -ItemType Directory -Path "项目目录/src/main/webapp/WEB-INF/etc/centre/data" -Force`

2. **配置导航**：
   - 后台举例：`centra/data/test1.navi.xml`
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

   - 前台举例：`centre/data/test1.navi.xml`
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
       </menu>
   </root>
   ```

3. **更新引用**：在 cust.navi.xml 中添加 import 引用
   - 后台：在 `src/main/webapp/WEB-INF/etc/centra/cust.navi.xml` 中添加 `<import>centra/data/test1</import>`
   - 前台：在 `src/main/webapp/WEB-INF/etc/centre/cust.navi.xml` 中添加 `<import>centre/data/test1</import>`

4. **验证配置**：检查配置文件格式是否正确
   - 确保 XML 格式正确
   - 确保菜单和角色名称符合规范
   - 确保权限设置正确

5. **测试验证**：在项目中测试导航配置是否生效
   - 构建项目：`mvn clean package`
   - 部署项目到 Web 容器
   - 访问后台管理页面，验证导航菜单是否正确显示

## 输出规范
执行完成后，返回以下信息：
- 导航配置是否创建成功
- 导航配置文件路径
- 导航配置文件内容
- 验证结果

## 注意事项
- 需要确保项目目录路径正确
- 导航名称应该与业务逻辑对应
- 菜单和角色设置应该根据业务需求进行调整
- 权限设置应该合理，遵循最小权限原则
- 优先使用 `src/main/webapp/WEB-INF/etc` 目录，因为这些配置不打包，方便后期修改
- 导航配置文件放在 `centra/data/` 或 `centre/data/` 子目录中，通过 import 引用
- 不需要创建 `centra.navi.xml` 和 `centre.navi.xml`，它们会自动从 hongs-wms 继承
- `cust.navi.xml` 是专门用于引入自定义导航的配置文件，内容简洁，易于维护
- action 应该使用完整的路径格式，如 `centra/data/test1/search.act`
- role name 应该使用清晰的命名格式，如 `centra/data/test1/search`、`centra/data/test1/create` 等，增删改查分开定义
- 后台导航可以根据业务需求添加更多功能，如统计、恢复等
- 前台导航配置规则：
  - 没配置权限的默认为许可
  - 配置了 role centre 的操作（如增改删）需要登录
  - 没配置的操作（如查询、统计）不用登录
  - 前台通常没有恢复等功能