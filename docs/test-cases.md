# AppointmentScheduler 测试用例

| 编号 | 模块 | 测试点 | 前置条件 | 步骤 | 预期结果 | 自动化覆盖 |
| --- | --- | --- | --- | --- | --- | --- |
| TC-001 | 用户注册 | 非法零售客户注册 | 未登录 | 提交空用户名、非法邮箱、短密码 | 返回注册表单并显示校验错误 | `SecurityAndActuatorIT` |
| TC-002 | 用户注册 | 合法零售客户注册 | 未登录 | 提交合法用户名、密码、邮箱和地址 | 注册成功并显示登录页 | `SecurityAndActuatorIT` |
| TC-003 | 权限 | 未登录访问预约页 | 未登录 | GET `/appointments/new` | 重定向到登录页 | `SecurityAndActuatorIT` |
| TC-004 | 权限 | 客户访问管理员客户列表 | 以 `customer_r` 登录 | GET `/customers/all` | 返回 403 | `SecurityAndActuatorIT` |
| TC-005 | 权限 | 服务商访问客户预约流程 | 以 `provider` 登录 | GET `/appointments/new` | 返回 403 | `SecurityAndActuatorIT` |
| TC-006 | 权限 | 客户篡改其他客户日历 ID | 以 `customer_r` 登录 | GET `/api/user/1001/appointments` | 返回 403 | `AjaxControllerIT` |
| TC-007 | 权限 | 客户访问他人预约详情 | 创建属于其他客户的预约 | GET `/appointments/{id}` | 返回 403 | `SecurityAndActuatorIT` |
| TC-008 | CSRF | 缺少 CSRF 的状态修改请求 | 以 `customer_r` 登录 | POST `/notifications/markAllAsRead` | 返回 403 | `SecurityAndActuatorIT` |
| TC-009 | 预约时间 | 正常预约 | 服务商有工作时间且无冲突 | 创建工作时间内预约 | 预约保存成功 | `AppointmentServiceTest` |
| TC-010 | 预约时间 | 开始时间早于工作时间 | 服务商 06:00 开始工作 | 预约 05:59 | 拒绝预约 | `AppointmentServiceTest` |
| TC-011 | 预约时间 | 预约刚好占满可用时段 | 可用时段 08:00-10:00，服务 120 分钟 | 计算可用时间 | 返回 08:00-10:00 | `AppointmentAvailabilityTest` |
| TC-012 | 预约时间 | 服务时长超过可用时段 | 可用时段 08:00-09:00，服务 120 分钟 | 计算可用时间 | 返回空列表 | `AppointmentAvailabilityTest` |
| TC-013 | 预约冲突 | 服务商已有同一时间预约 | 同一服务商同一开始时间已有预约 | 再次预约 | 拒绝或数据库约束失败 | `AppointmentServiceTest` |
| TC-014 | 预约冲突 | 客户已有同一时间预约 | 同一客户同一开始时间已有预约 | 再次预约 | 拒绝或数据库约束失败 | `AppointmentServiceTest` |
| TC-015 | 并发 | 两个客户同时抢同一服务商时段 | 两个客户、同一服务商、同一时间 | 并发创建预约 | 最终只保存一条预约 | `AppointmentConcurrentBookingIT` |
| TC-016 | Ajax | 日历接口缺少时间窗口 | 以 `customer_r` 登录 | GET `/api/user/3/appointments` | 返回 400 | `AjaxControllerIT` |
| TC-017 | Ajax | 日历接口非法时间格式 | 以 `customer_r` 登录 | 传入 `not-a-date` | 返回 400 | `AjaxControllerIT` |
| TC-018 | Ajax | 可用时间接口契约 | 以 `customer_r` 登录 | GET `/api/availableHours/2/1/2032-01-20` | 返回 JSON 数组，包含 `workId`、`providerId`、`start`、`end` | `AjaxControllerIT` |
| TC-019 | Ajax | 服务商访问客户可用时间接口 | 以 `provider` 登录 | GET `/api/availableHours/2/1/2032-01-20` | 返回 403 | `AjaxControllerIT` |
| TC-020 | UI | 登录页视觉稳定 | 应用启动 | Playwright 访问 `/login` | 与基线截图一致 | `appointmentscheduler.visual.spec.ts` |
| TC-021 | 性能 | 多角色浏览负载 | 应用和数据库启动 | 执行 k6 load 脚本 | 失败率和 p95 响应时间满足阈值 | `performance/k6` |

## 手工补充测试建议

| 编号 | 模块 | 测试点 | 建议 |
| --- | --- | --- | --- |
| MT-001 | 浏览器兼容 | Safari、Firefox、Chrome 页面差异 | 对登录、注册、预约列表做手工冒烟 |
| MT-002 | PDF | 发票 PDF 内容和排版 | 下载发票后人工核对金额、编号、客户信息 |
| MT-003 | 邮件 | 真实 SMTP 发送 | 在测试邮箱环境验证邮件标题和链接 |
