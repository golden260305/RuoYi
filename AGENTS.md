# 仓库指南

## 项目结构与模块组织

本项目是基于 Java 17 的 Maven 多模块工程。`ruoyi-admin` 是 Spring Boot 启动模块，包含控制器、配置，以及 `src/main/resources/templates` 和 `static` 下的 Thymeleaf 页面与静态资源。`ruoyi-common` 存放通用领域对象和工具类；`ruoyi-framework` 负责安全、Web 与框架配置；`ruoyi-system` 包含核心业务服务和 MyBatis 映射；`ruoyi-quartz` 管理定时任务；`ruoyi-generator` 提供代码生成逻辑与模板。数据库脚本位于 `sql/`，运维脚本和补充文档位于 `bin/`、`doc/`。

## 构建、测试与本地开发

- `mvn clean package -DskipTests`：构建全部模块，并生成 `ruoyi-admin/target/ruoyi-admin.jar`。
- `mvn test`：运行 Maven Reactor 中所有模块的测试。
- `mvn -pl ruoyi-admin -am spring-boot:run`：构建依赖模块并在本地启动应用。
- `java -jar ruoyi-admin/target/ruoyi-admin.jar`：运行已打包的应用。

首次启动前，从 `sql/` 导入所需数据库脚本，并在 `ruoyi-admin/src/main/resources/application-druid.yml` 中配置数据源。通用应用配置位于 `application.yml`。

## 编码风格与命名约定

遵循现有 Java 风格：使用 UTF-8、四空格缩进、大括号单独换行；导入按 `java.*`、第三方框架、项目包分组。包名使用 `com.ruoyi` 下的小写名称；类名使用 PascalCase，成员使用 camelCase。服务接口采用 `I...Service`，实现类采用 `...ServiceImpl`，控制器采用 `...Controller`。Mapper XML 文件名应与对应 Java Mapper 接口一致。项目未配置统一格式化或静态检查工具，请参照相邻代码，避免无关的格式调整。

## 工具类使用约定

业务代码优先复用 `com.ruoyi.common.utils` 下已有的工具类，不要重复实现字符串判空、日期格式化、文件处理等通用逻辑。

**同名工具类必须使用项目版本。** `StringUtils`、`DateUtils`、`BeanUtils` 是在 Apache Commons、Spring 同名类基础上扩展而来的，业务代码应始终导入 `com.ruoyi.common.utils.*` 下的版本，而不是 `org.apache.commons.lang3.*` 或 `org.springframework.beans.BeanUtils`，否则会丢失项目自定义的方法。

常用场景对应关系：

- 字符串判空、截取：`com.ruoyi.common.utils.StringUtils`（`isEmpty`、`isNotEmpty`、`substring`、`substringBetweenLast`）
- 日期格式化与解析：`com.ruoyi.common.utils.DateUtils`（`parseDate`、`dateTime`、`getDate`）
- 对象拷贝与属性填充：`com.ruoyi.common.utils.bean.BeanUtils`、`BeanValidators`
- 请求参数获取：`com.ruoyi.common.utils.ServletUtils`
- 登录用户信息：`com.ruoyi.common.utils.ShiroUtils`
- 字典数据读取：`com.ruoyi.common.utils.DictUtils`
- 缓存读写：`com.ruoyi.common.utils.CacheUtils`
- 文件与图片处理：`com.ruoyi.common.utils.file.FileUtils`、`FileUploadUtils`、`FileTypeUtils`、`ImageUtils`
- Excel 导入导出：`com.ruoyi.common.utils.poi.ExcelUtil`
- 唯一标识生成：`com.ruoyi.common.utils.uuid.IdUtils`
- 权限校验：`com.ruoyi.common.utils.security.PermissionUtils`、`Md5Utils`、`CipherUtils`
- 反射调用：`com.ruoyi.common.utils.reflect.ReflectUtils`
- HTML 转义与过滤：`com.ruoyi.common.utils.html.EscapeUtil`、`HTMLFilter`
- 金额计算：`com.ruoyi.common.utils.Arith`

项目未引入 Hutool、Guava，引入新的第三方工具库需要先确认确有必要。新出现的通用逻辑应沉淀到 `ruoyi-common` 的工具类中，不要散落在各业务模块内。

## 测试规范

仓库目前没有已提交的 `src/test` 测试目录，也未设置覆盖率门槛。新增测试应放在对应模块的 `src/test/java` 中，并镜像生产代码包结构。单元测试命名为 `*Test`，集成测试命名为 `*IT`。优先编写聚焦的服务或工具类测试；仅在验证依赖注入或持久化行为时加载 Spring 上下文。提交前运行 `mvn test`。

## 提交与合并请求规范

近期提交使用简短、直接的中文说明，例如 `优化代码`、`升级poi到最新版本5.5.1`。每个提交应只处理一个主题，并说明具体改动；必要时添加模块前缀，例如 `ruoyi-system: 修复用户查询条件`。合并请求需说明动机和影响，列出配置或 SQL 变更，关联相关 Issue；页面或样式修改应附截图，并注明验证命令。禁止提交凭据、`target/` 产物、日志或本地上传数据。
