# AppointmentScheduler 最终改造方案

## 1. 文档说明

本文档用于固化 AppointmentScheduler 本轮的最终改造方案，并作为当前分支改造工作的交付基线。方案内容以仓库中已经展开的改造方向为准，重点解决性能、可维护性、安全性和可观测性四类问题，确保系统从“可运行”提升到“可持续维护、可监控、可扩展”的状态。

本次改造不以大规模重写业务为目标，而是优先处理当前系统中最影响稳定性和线上运行质量的薄弱点，在尽量不破坏现有业务流程的前提下完成架构加固。

## 2. 改造背景

现有系统具备完整的预约、通知、发票、工作计划和用户角色管理能力，但在工程层面存在以下典型问题：

| 问题类别 | 当前表现 | 直接影响 |
| --- | --- | --- |
| 异步与定时任务 | 邮件发送和定时任务缺少独立、可调优的线程池治理 | 高峰期可能相互影响，异常时不易定位 |
| 通知查询 | 未读通知数量通过拉全量列表再计数；全部已读逐条更新 | 数据库与应用层无谓开销偏大 |
| 日历数据接口 | 首页日历接口按用户返回全量预约 | 页面越用越慢，接口无法按时间窗口裁剪 |
| 前端静态资源 | 全局模板无差别加载 DataTables、FullCalendar、外部 CDN 资源 | 首页负载偏重，缓存策略不清晰 |
| 轮询机制 | 通知轮询频率过高且不感知页面可见性 | 增加无效请求，对服务端持续施压 |
| 安全配置 | 注册页通过 `WebSecurity` 忽略过滤链 | 安全策略绕行，后续扩展困难 |
| 可观测性 | 缺少 Prometheus 指标接入与基础 Actuator 暴露 | 线上运行状态难以量化监控 |
| 数据库访问 | 关键查询缺少针对性的组合索引 | 日历和通知查询性能受限 |
| 回归保护 | 本轮改造点缺少足够的集成测试覆盖 | 后续回归风险较高 |

## 3. 改造目标

本次改造的最终目标如下：

1. 将邮件发送和定时任务从默认执行模型中解耦，建立可配置、可观测、可优雅关闭的异步基础设施。
2. 将通知中心和首页日历的核心查询从“全量拉取”改造为“按用途、按窗口、按聚合”访问。
3. 将前端资源从“全局加载”改造为“按页面按需加载”，同时降低无意义轮询带来的服务压力。
4. 将安全配置从“绕过过滤链”改造为“在过滤链内显式放行”，为后续安全治理留出空间。
5. 引入基础监控指标暴露、压缩和静态资源缓存配置，改善生产环境运行质量。
6. 为本轮关键改造补齐集成测试和数据库索引，保证方案可验收、可回归。

## 4. 最终方案总览

### 4.1 异步执行与定时任务治理

在系统中新增统一的异步与调度配置类 `AsyncExecutionConfig`，作为本轮基础设施改造的核心入口。

方案要点：

- 为邮件发送显式提供 `mailExecutor` 线程池，核心参数全部落到 `application.properties`。
- 为定时任务显式提供 `taskScheduler`，避免与默认调度线程模型耦合。
- 为异步和调度任务增加统一异常日志处理，确保失败可定位。
- 开启优雅停机能力，保证服务关闭时等待已提交任务完成，减少半途终止造成的数据与通知不一致。
- 邮件异步任务统一使用 `@Async("mailExecutor")`，避免继续落到默认执行器。

预期收益：

- 邮件发送不再阻塞主业务流程。
- 定时任务与邮件线程互不争抢执行资源。
- 线程池参数可以按部署环境独立调优。
- 异步失败与定时任务失败可以直接在日志中追踪。

涉及模块：

- `src/main/java/com/example/slabiak/appointmentscheduler/config/AsyncExecutionConfig.java`
- `src/main/java/com/example/slabiak/appointmentscheduler/service/impl/EmailServiceImpl.java`
- `src/main/java/com/example/slabiak/appointmentscheduler/service/impl/ScheduledTasksServiceImpl.java`
- `src/main/resources/application.properties`

### 4.2 预约与通知查询改造

本轮查询优化围绕两个高频场景展开：日历预约展示和通知中心。

#### 4.2.1 日历接口改造

原有 `/api/user/{userId}/appointments` 更适合列表页，而不适合周历或月历场景。最终方案是在保留原有兼容行为的前提下，为该接口增加 `start` 和 `end` 参数，当参数存在时按时间窗口返回数据。

方案要点：

- 在 `AjaxController` 中兼容解析 `start/end` 参数，支持 ISO 日期时间格式。
- 按角色分别调用客户、服务提供者、管理员的日历查询方法。
- 在 `AppointmentService` 和 `AppointmentRepository` 中新增面向日历窗口的查询接口。
- 查询层使用 `join fetch a.work`，减少序列化时的懒加载问题。

预期收益：

- 首页 FullCalendar 不再请求全量预约数据。
- 数据库只返回当前周或当前月真正需要的记录。
- 后续扩展分页、过滤或多视图能力时有稳定接口基础。

#### 4.2.2 通知中心改造

通知中心由“对象列表驱动”调整为“聚合与批量写驱动”。

方案要点：

- 未读数量直接通过 `count` 查询返回，替代“查列表再 `size()`”。
- “全部标记已读”改为单条批量更新 SQL，而不是逐条读取后循环 `save`。
- 通知列表查询改为按用户和创建时间倒序直接访问仓库层，不再从 `User` 关联对象反查。

预期收益：

- 未读数轮询开销显著下降。
- 通知清空已读场景对数据库和 JPA Session 的压力明显减小。
- 控制器和服务边界更清晰，避免无意义加载整个 `User` 聚合。

涉及模块：

- `src/main/java/com/example/slabiak/appointmentscheduler/controller/AjaxController.java`
- `src/main/java/com/example/slabiak/appointmentscheduler/controller/NotificationController.java`
- `src/main/java/com/example/slabiak/appointmentscheduler/service/AppointmentService.java`
- `src/main/java/com/example/slabiak/appointmentscheduler/service/NotificationService.java`
- `src/main/java/com/example/slabiak/appointmentscheduler/service/impl/AppointmentServiceImpl.java`
- `src/main/java/com/example/slabiak/appointmentscheduler/service/impl/NotificationServiceImpl.java`
- `src/main/java/com/example/slabiak/appointmentscheduler/dao/AppointmentRepository.java`
- `src/main/java/com/example/slabiak/appointmentscheduler/dao/NotificationRepository.java`

### 4.3 前端资源与轮询策略优化

前端层面的最终方案不是全面重写页面，而是先将最明显的负担点收敛掉。

方案要点：

- 从全局布局模板中移除 DataTables、FullCalendar、Google Analytics、外部字体图标等非所有页面必需资源。
- 将 DataTables 资源仅加载到预约列表页和通知列表页。
- 将 FullCalendar 资源仅加载到日历相关页面。
- 统一使用本地 `webjars` 和项目静态资源路径，降低对外部 CDN 的硬依赖。
- 通知轮询由 5 秒一次调整为 30 秒一次。
- 轮询逻辑基于 `fetch` 实现，并在页面不可见时暂停，在页面重新可见时恢复。
- 通知角标仅基于数量状态切换样式，避免依赖外部图标库。

预期收益：

- 公共布局首屏资源体积下降。
- 非日历页面不再承担 FullCalendar 的加载成本。
- 非表格页面不再承担 DataTables 的加载成本。
- 页面切到后台时不再持续发送无效通知请求。

涉及模块：

- `src/main/resources/templates/fragments/layout.html`
- `src/main/resources/templates/home.html`
- `src/main/resources/templates/appointments/selectDate.html`
- `src/main/resources/templates/appointments/listAppointments.html`
- `src/main/resources/templates/notifications/listNotifications.html`
- `src/main/resources/static/css/style.css`

### 4.4 安全、监控与运行参数加固

该部分改造目标是让系统具备最基本的生产化运行条件。

方案要点：

- 在 Spring Security 过滤链内显式放行静态资源、注册页和 Actuator 基础端点，不再通过 `WebSecurity#ignoring` 绕开过滤链。
- Web 层仅公开 `health` 端点；`info`、`metrics` 和 `prometheus` 不对公网暴露，避免运行信息泄露。
- 增加 Micrometer Prometheus 依赖。
- 启用响应压缩，降低 HTML、CSS、JS、JSON 传输成本。
- 为静态资源启用长期缓存和内容哈希策略，改善浏览器缓存命中率。
- 升级 Lombok 版本，避免旧版本在新 JDK 或新构建链上的兼容性问题。

预期收益：

- 安全配置更加一致，可继续叠加鉴权、审计和 CSRF 策略。
- 监控系统可以直接抓取应用指标。
- 前端静态资源重复访问成本下降。
- 构建兼容性和依赖基线更稳。

涉及模块：

- `src/main/java/com/example/slabiak/appointmentscheduler/security/WebSecurityConfig.java`
- `src/main/resources/application.properties`
- `pom.xml`

### 4.5 数据库索引与测试兜底

任何性能优化如果没有索引与测试支撑，最终都很难稳定落地，因此本轮将数据库和测试视为正式交付的一部分。

方案要点：

- 为预约表补充 `(id_provider, start)`、`(id_customer, start)` 组合索引，支撑按用户和时间窗口查询。
- 为通知表补充 `(id_user, is_read, created_at)` 组合索引，支撑未读数统计、未读列表和时间排序。
- 新增集成测试覆盖以下关键行为：
  - 日历接口只返回请求时间窗口内的预约。
  - 未读通知计数接口直接返回聚合结果。
  - 批量“全部已读”逻辑生效。
  - 注册页和 Actuator 基础端点可按预期访问。
  - 异步执行器和调度器 Bean 已成功注册。

涉及模块：

- `src/main/resources/appointmentscheduler.sql`
- `src/test/resources/appointmentscheduler.sql`
- `src/test/java/com/example/slabiak/appointmentscheduler/controller/AjaxControllerIT.java`
- `src/test/java/com/example/slabiak/appointmentscheduler/security/SecurityAndActuatorIT.java`
- `src/test/java/com/example/slabiak/appointmentscheduler/service/notification/NotificationServiceIT.java`

## 5. 建议实施顺序

虽然当前分支已经包含了本轮主要改造，但从交付和复盘角度，建议将最终实施顺序定义为下面四步：

1. 基础设施先行。
   先落异步执行器、调度器、配置项和异常处理，保证后续邮件与定时任务改造有统一承载层。

2. 高开销查询收口。
   先改日历窗口查询和通知聚合查询，因为这部分最直接影响数据库压力和页面响应时间。

3. 页面资源与轮询收口。
   再调整布局模板、页面脚本和轮询策略，确保接口优化能真正转化为页面侧收益。

4. 运维与回归兜底。
   最后补监控暴露、缓存压缩、索引和集成测试，形成可发布闭环。

## 6. 验收标准

本次改造完成后，至少应满足以下验收条件：

- `GET /api/user/{userId}/appointments?start=...&end=...` 仅返回窗口内预约。
- `GET /api/user/notifications` 返回未读数量，不依赖全量通知列表加载。
- “全部已读”操作通过单条批量更新完成。
- 邮件发送方法统一绑定到 `mailExecutor`。
- 定时任务通过独立 `taskScheduler` 执行，且异常可记录到日志。
- `/customers/new/**` 在安全过滤链内可访问。
- `/actuator/health` 可匿名访问，`/actuator/prometheus` 不再默认暴露到 Web 层。
- 静态资源具备缓存与压缩配置。
- 关键查询具备对应组合索引。
- 集成测试可以覆盖上述关键路径。

## 7. 风险与控制措施

| 风险 | 说明 | 控制措施 |
| --- | --- | --- |
| 线程池参数过小 | 邮件堆积导致发送延迟 | 参数配置化，按环境压测后调整 |
| 批量更新绕过实体回调 | 与逐条 `save` 的行为存在差异 | 将影响范围限定在简单状态位更新，并用集成测试校验 |
| 日历接口兼容性 | 老页面和新页面可能共用同一接口 | 保留无 `start/end` 参数时的原行为 |
| Actuator 暴露面扩大 | 需要控制暴露范围 | Web 层仅开放 `health`，指标端点放到受控网络或认证通道 |
| 静态缓存过长 | 旧资源可能被浏览器缓存 | 启用内容哈希策略，避免缓存脏读 |

## 8. 本轮改造后的状态定义

完成本方案后，AppointmentScheduler 应达到以下状态：

- 核心高频页面具备基本性能优化，而不是继续依赖全量查询。
- 邮件和调度任务具备最小可用的生产级执行治理能力。
- 安全配置、监控暴露和静态资源策略具备统一入口。
- 数据库层、服务层、控制器层和页面层之间的职责边界更清楚。
- 本轮改造具备文档、索引、配置和测试四个维度的完整交付件。

## 9. 后续建议

本轮改造完成后，下一阶段建议优先考虑以下事项，但不纳入本次最终方案的交付范围：

1. 将通知轮询进一步升级为 WebSocket 或 Server-Sent Events。
2. 为预约列表和通知列表补充分页与更细粒度过滤。
3. 将 `WebSecurityConfigurerAdapter` 升级为新版 Spring Security 配置方式。
4. 为 Actuator 指标补充 Grafana 面板和告警规则。
5. 补充针对首页和日历接口的压测基线。

---

如无额外范围变更，以上内容即作为 AppointmentScheduler 本轮最终改造方案的正式版本。
