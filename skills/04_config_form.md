---
name: config-form
description: 配置表单(form)，创建 .form.xml 文件并定义表单结构。当用户需要配置表单时触发。
version: 1.0.0
---

# 配置表单

## 角色定义
你是一名资深的软件开发工程师，擅长表单配置和数据管理。

## 核心指令
请严格按照以下步骤执行任务：
1. **分析意图**：确认用户需要配置的表单名称和字段
2. **创建目录**：在项目的配置目录中创建表单配置文件
3. **配置表单**：创建 .form.xml 文件，定义表单结构和字段属性
4. **验证配置**：检查配置文件格式是否正确
5. **测试验证**：在项目中测试表单配置是否生效

## 参数
- `project_dir`：项目目录路径，必填
- `form_name`：表单名称，必填
- `form_file`：表单配置文件路径，默认值为 `src/main/webapp/WEB-INF/etc/centra/data/${form_name}.form.xml`
- `fields`：表单字段配置，可选
- `config_location`：配置文件位置，可选，取值为 `web-inf`、`resources` 或 `global`，默认值为 `web-inf`
- `scope`：表单作用域，可选，取值为 `centra`（后台）或 `centre`（前台），默认值为 `centra`
- `extend_from`：继承的表单名称，可选，用于前台表单继承后台表单

## 配置文件位置
配置文件有三个位置，优先级从高到低：

1. **WEB-INF/etc 目录**：
   - `src/main/webapp/WEB-INF/etc/centra/data`：针对后台，不打包，适合一般性配置和可能修改的配置
   - `src/main/webapp/WEB-INF/etc/centre/data`：针对前台，不打包，适合一般性配置和可能修改的配置

2. **resources 目录**：
   - `src/main/resources/centra/data`：针对后台，会打包进 jar，适合不变的配置
   - `src/main/resources/centre/data`：针对前台，会打包进 jar，适合不变的配置

3. **resources/io/github/ihongs/config 目录**：
   - 不再建立子目录，这是放全局配置的地方，对应 WEB-INF/etc

**优先放在 WEB-INF/etc 目录**，因为这些配置不打包，方便后期修改。

**注意**：`centra/data` 和 `centre/data` 会自动暴露对应的接口，如 `centra/data/xxx/search.act`。

## 前台表单配置
如果表单需要暴露在前台，则在 `src/main/webapp/WEB-INF/etc/centre/data` 下创建对应的表单配置，并通过 `extend` 属性从对应后台表单继承。

### 前台表单配置示例
```xml
<?xml version="1.0" encoding="UTF-8"?>
<root>
    <!-- 前台表单定义，继承后台表单 -->
    <form name="${form_name}" extend="${extend_from}">
        <!-- 可以覆盖后台表单的字段定义 -->
        <field name="status" text="状态" type="select">
            <param name="enum">${form_name}-status</param>
            <param name="default">1</param>
        </field>
        <!-- 可以添加前台特有的字段 -->
        <field name="front_field" text="前台特有字段" type="text">
            <param name="default">默认值</param>
        </field>
    </form>
</root>
```

## 执行步骤
1. **创建目录**：根据配置位置和作用域创建相应的目录结构
   - **WEB-INF/etc 目录**（推荐）：
     - 后台：`New-Item -ItemType Directory -Path "项目目录/src/main/webapp/WEB-INF/etc/centra/data" -Force`
     - 前台：`New-Item -ItemType Directory -Path "项目目录/src/main/webapp/WEB-INF/etc/centre/data" -Force`
   - **resources 目录**：
     - 后台：`New-Item -ItemType Directory -Path "项目目录/src/main/resources/centra/data" -Force`
     - 前台：`New-Item -ItemType Directory -Path "项目目录/src/main/resources/centre/data" -Force`
   - **global 目录**：
     - `New-Item -ItemType Directory -Path "项目目录/src/main/resources/io/github/ihongs/config" -Force`

2. **配置表单**：根据配置位置和作用域创建 .form.xml 文件，定义表单结构和字段属性
   - **后台表单**（centra/data）：
     ```xml
     <?xml version="1.0" encoding="UTF-8"?>
     <root>
         <!-- 枚举定义 -->
         <enum name="${form_name}-status">
             <value code="1">启用</value>
             <value code="0">禁用</value>
         </enum>
         
         <!-- 表单定义 -->
         <form name="${form_name}">
             <field name="id" text="ID" type="hidden">
                 <param name="default">@id</param>
             </field>
             <field name="name" text="名称" type="text" required="yes">
                 <param name="minlength">2</param>
                 <param name="maxlength">50</param>
             </field>
             <field name="email" text="邮箱" type="email" required="yes">
                 <param name="pattern">^\w+([-.]\w+)*@\w+([-.]\w+)*$</param>
             </field>
             <field name="phone" text="电话" type="tel">
                 <param name="pattern">^\+?[0-9][\d\-]+[0-9]$</param>
             </field>
             <field name="status" text="状态" type="select">
                 <param name="enum">${form_name}-status</param>
                 <param name="default">1</param>
             </field>
             <field name="create_time" text="创建时间" type="datetime">
                 <param name="default">@now</param>
                 <param name="deforce">create</param>
             </field>
             <field name="update_time" text="更新时间" type="datetime">
                 <param name="default">@now</param>
                 <param name="deforce">always</param>
             </field>
         </form>
     </root>
     ```
   - **前台表单**（centre/data）：
     ```xml
     <?xml version="1.0" encoding="UTF-8"?>
     <root>
         <!-- 前台表单定义，继承后台表单 -->
         <form name="${form_name}" extend="${form_name}">
             <!-- 可以覆盖后台表单的字段定义 -->
             <field name="status" text="状态" type="select">
                 <param name="enum">${form_name}-status</param>
                 <param name="default">1</param>
             </field>
             <!-- 可以添加前台特有的字段 -->
             <field name="front_field" text="前台特有字段" type="text">
                 <param name="default">默认值</param>
             </field>
         </form>
     </root>
     ```

3. **验证配置**：检查配置文件格式是否正确
   - 确保 XML 格式正确
   - 确保表单和字段名称符合规范
   - 确保字段类型和参数设置正确
   - 确保前台表单正确继承后台表单

4. **测试验证**：在项目中测试表单配置是否生效
   - 构建项目：`mvn clean package`
   - 部署项目到 Web 容器
   - 访问表单页面，验证表单是否正确显示

## 输出规范
执行完成后，返回以下信息：
- 表单配置是否创建成功
- 表单配置文件路径
- 表单配置文件内容
- 验证结果

## 示例
```bash
# 在 WEB-INF/etc/centra/data 目录创建名为 user 的后台表单配置（推荐）
config_form --project_dir=test/my-project --form_name=user --scope=centra

# 在 WEB-INF/etc/centre/data 目录创建名为 user 的前台表单配置，继承后台表单
config_form --project_dir=test/my-project --form_name=user --scope=centre --extend_from=user

# 在 resources/centra/data 目录创建名为 user 的后台表单配置
config_form --project_dir=test/my-project --form_name=user --config_location=resources --scope=centra --form_file=src/main/resources/centra/data/user.form.xml

# 在 resources/centre/data 目录创建名为 user 的前台表单配置，继承后台表单
config_form --project_dir=test/my-project --form_name=user --config_location=resources --scope=centre --extend_from=user --form_file=src/main/resources/centre/data/user.form.xml
```

## 注意事项
- 需要确保项目目录路径正确
- 表单名称应该与业务逻辑对应
- 字段类型和参数设置应该根据业务需求进行调整
- 枚举定义应该与字段使用对应
- 默认值和强制规则应该根据业务逻辑设置
- 优先使用 `src/main/webapp/WEB-INF/etc` 目录，因为这些配置不打包，方便后期修改
- `centra/data` 和 `centre/data` 会自动暴露对应的接口，如 `centra/data/xxx/search.act`
- 前台表单可以通过 `extend` 属性继承后台表单，减少重复配置
- 前台表单可以覆盖后台表单的字段定义，也可以添加前台特有的字段