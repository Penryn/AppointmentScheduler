# AppointmentScheduler 测试计划

## 1. 测试目标

验证 AppointmentScheduler 预约管理系统在核心业务、权限控制、接口契约、并发一致性和页面稳定性方面满足课设质量要求。测试重点放在预约流程、用户角色权限、表单校验、数据库持久化和关键接口响应。

## 2. 测试范围

纳入测试的模块：

- 用户注册、登录和资料维护
- 预约创建、查询、取消、拒绝和状态流转
- 服务商工作计划、休息时间和可用时间计算
- 服务项目和客户类型适配
- 通知和发票相关流程
- 管理员、服务商、客户的访问权限
- Ajax 日历接口、可用时间接口、通知计数接口
- 页面视觉回归和基础性能场景

暂不重点测试的内容：

- 真实 SMTP 邮件投递
- 第三方浏览器兼容性全集
- 生产环境部署可靠性
- PDF 发票的像素级排版

## 3. 测试策略

采用测试金字塔组织测试：

- 单元测试：使用 JUnit、Mockito、AssertJ，验证服务层业务规则和边界值。
- 集成测试：使用 Spring Boot Test、MockMvc、Testcontainers MySQL，验证 Controller、Service、Repository 和数据库协作。
- 安全测试：使用 Spring Security Test，验证未登录、越权访问、CSRF 防护和 ID 篡改。
- 接口契约测试：使用 MockMvc 验证 JSON 结构、字段稳定性、错误状态码。
- 并发测试：使用多线程请求同一预约时段，验证数据库唯一约束防止重复预约。
- UI 回归测试：使用 Selenium 和 Playwright 验证登录页、注册页、预约列表等页面。
- 性能测试：使用 k6 验证多角色浏览、写操作和关键 Ajax 接口的响应时间。

## 4. 测试环境

- JDK 17
- Maven 3.9+
- Docker
- MySQL 8 via Testcontainers
- Node.js 20
- Playwright Chromium
- k6

主要命令：

```bash
mvn test
mvn clean verify
./mvnw -Pui-tests verify -Dskip.surefire.tests=true
npm run test:visual
k6 run performance/k6/appointmentscheduler-load.js
```

## 5. 通过标准

- 单元测试和集成测试全部通过。
- 关键安全用例全部通过，越权访问必须返回 403 或重定向登录。
- 关键接口返回 JSON 结构稳定，非法输入返回 400。
- 并发创建同一服务商同一开始时间的预约时，最终只允许一条记录落库。
- Playwright 视觉回归截图无非预期差异。
- k6 页面/API 请求失败率低于阈值，p95 响应时间满足脚本配置。

## 6. 风险和关注点

- 预约创建存在典型并发风险，需要数据库唯一约束和测试共同保障。
- 传统服务端渲染页面需要重点关注 CSRF 和角色权限。
- 测试依赖 Docker，CI 或本地环境资源不足时可能导致集成测试不稳定。
- Playwright 截图在不同操作系统上会存在字体和渲染差异，需要区分 Linux 与 macOS 基线。
