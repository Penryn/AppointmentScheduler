# AppointmentScheduler

AppointmentScheduler 是一个基于 Spring Boot 的预约管理系统，用于管理服务提供者与客户之间的预约流程。系统覆盖预约创建、服务商工作计划与休息时间、通知、预约取消与异议处理、发票生成以及管理员后台管理等功能。

<a href="https://github.com/slabiak/slabiak.github.io/blob/master/images/appointmentscheduler/calendar.png?raw=true"><img src="https://github.com/slabiak/slabiak.github.io/blob/master/images/appointmentscheduler/calendar.png?raw=true" width="600" alt="AppointmentScheduler 日历视图"></a>

## 当前技术栈

- Java 17
- Spring Boot 3.3.12
- Spring MVC、Thymeleaf、Spring Security 6、Spring Data JPA
- Hibernate 6 + Hypersistence JSON 映射
- MySQL 8 + Flyway
- Bootstrap 5.3.8 + jQuery 3.7.1
- 仓库内置的 FullCalendar 前端资源
- Micrometer + Prometheus
- Testcontainers + Spring Boot Test
- Flying Saucer PDF + JJWT

## 当前运行特性

- 数据库结构和初始化数据由 Flyway 迁移统一管理，迁移目录为 `src/main/resources/db/migration`
- 浏览器侧所有修改状态的请求都启用了 CSRF 防护
- 主要列表页已经改为服务端分页
- 列表页默认分页大小为 20
- 预约列表支持按状态过滤
- 静态资源启用了内容哈希和长缓存配置
- HTTP 压缩已开启
- 对外开放的 actuator 端点：
  - `/actuator/health`
  - `/actuator/info`
  - `/actuator/metrics/**`
  - `/actuator/prometheus`

## 环境要求

- JDK 17
- Maven 3.9+
- Node.js 20
- MySQL 8
- Docker
  - 运行 `mvn verify` 时需要 Docker，因为集成测试依赖 Testcontainers MySQL

## 本地开发

### 1. 克隆仓库

```bash
git clone https://github.com/slabiak/AppointmentScheduler.git
cd AppointmentScheduler
```

### 2. 创建本地数据库

默认本地配置使用名为 `appointmentscheduler` 的 MySQL 数据库，以及 `user/password` 凭据。

如果本机还没有 MySQL，推荐直接用 Docker 启动一个开发用 MySQL：

```bash
docker run --name appointmentscheduler-mysql \
  -e MYSQL_ROOT_PASSWORD=root_pass \
  -e MYSQL_DATABASE=appointmentscheduler \
  -e MYSQL_USER=user \
  -e MYSQL_PASSWORD=password \
  -p 3306:3306 \
  -d mysql:8.0
```

启动后可用下面的命令查看容器状态：

```bash
docker ps --filter name=appointmentscheduler-mysql
```

如果你已经在本机安装并启动了 MySQL，也可以直接用 root 账号进入 MySQL：

```bash
mysql -u root -p
```

然后执行建库和授权 SQL：

```sql
CREATE DATABASE appointmentscheduler;
CREATE USER 'user'@'%' IDENTIFIED BY 'password';
GRANT ALL PRIVILEGES ON appointmentscheduler.* TO 'user'@'%';
FLUSH PRIVILEGES;
```

如果使用上面的 `docker run` 命令启动 MySQL，数据库和用户会由容器环境变量自动创建，通常不需要再手动执行这段 SQL。

### 3. 检查本地配置

默认本地配置文件位于 [src/main/resources/application.properties](src/main/resources/application.properties)。

至少需要检查以下配置项：

- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`
- `app.jwtSecret`
- `base.url`

可选调整：

- 如果你希望本地实际发送邮件，需要配置 `spring.mail.*`
- 如果本地没有 SMTP，可将 `mailing.enabled=false`

### 4. 启动应用

```bash
mvn spring-boot:run
```

应用默认启动在 <http://localhost:8080>。

也可以用 Docker Compose 构建并运行本地源码：

```bash
docker compose up --build
```

该命令会：

- 基于当前工作区源码构建 `appointmentscheduler:local` 镜像
- 启动 MySQL 8 容器
- 等 MySQL 健康检查通过后启动后端应用

后台运行可使用：

```bash
docker compose up --build -d
```

停止并保留数据库数据：

```bash
docker compose down
```

停止并删除数据库卷：

```bash
docker compose down -v
```

### 5. 使用初始化账号登录

首次启动时，Flyway 会自动插入一组演示数据。

| 账号类型 | 用户名 | 密码 |
| --- | --- | --- |
| `admin` | `admin` | `qwerty123` |
| `provider` | `provider` | `qwerty123` |
| `corporate customer` | `customer_c` | `qwerty123` |
| `retail customer` | `customer_r` | `qwerty123` |

## 数据库迁移

- Flyway 是当前唯一的数据库结构管理方式
- JPA 自动建表已关闭：`spring.jpa.hibernate.ddl-auto=none`
- 旧的根目录 SQL 初始化脚本已经移除
- 后续数据库变更请新增到 `src/main/resources/db/migration`

## 测试

当前测试体系覆盖：

- 单元测试
- 集成测试
- API 契约测试
- 安全权限测试
- 并发测试
- Selenium UI 测试
- Playwright 视觉回归测试
- k6 性能测试

仅运行单元测试：

```bash
mvn test
```

运行完整构建，包括集成测试：

```bash
mvn clean verify
```

运行 Selenium UI 测试：

```bash
./mvnw -Pui-tests verify -Dskip.surefire.tests=true
```

运行 Playwright 视觉回归测试：

```bash
npm ci
npm run test:visual
```

首次生成或更新视觉基线截图：

```bash
npm run test:visual:update
```

说明：

- 集成测试通过 Testcontainers 拉起 MySQL，因此需要 Docker
- `src/test/java/**/ui/**` 下的 UI 测试默认不会在 `verify` 流程中执行
- Playwright 测试默认访问 `http://localhost:8080`，可通过 `BASE_URL` 覆盖

## 角色说明

- `admin`
  - 管理服务商和服务项目
  - 可以查看所有预约、服务商、客户和发票
  - 可以手动签发发票
- `provider`
  - 管理自己的工作计划和可提供的服务
  - 只能查看自己的预约
- `customer retail`
  - 可自行注册
  - 可以创建和管理自己的预约
  - 只能看到面向零售客户的服务
- `customer corporate`
  - 与零售客户类似，但需要额外提供 VAT number 和 company name
  - 只能看到面向企业客户的服务

## 预约流程

1. 客户选择服务项目
2. 客户为该服务选择服务商
3. 客户选择一个可预约时间段
4. 客户确认预约

可预约时间的计算依赖以下因素：

- 服务商工作计划
- 配置的休息时间
- 服务商已有预约
- 客户已有预约
- 当前服务项目时长

## 预约生命周期

当前支持的预约状态：

- `scheduled`
- `finished`
- `confirmed`
- `invoiced`
- `canceled`
- `rejection requested`
- `rejection accepted`

典型正常流转如下：

1. `scheduled`
2. `finished`
3. `confirmed`
4. `invoiced`

系统同时支持以下分支流程：

- 客户或服务商取消预约
- 服务完成后的异议申请
- 服务商接受异议申请

## 通知

以下场景会生成通知：

- 新预约创建
- 预约被取消
- 预约已完成
- 客户发起异议申请
- 服务商接受异议申请
- 发票签发

邮件模板位于 `src/main/resources/templates/email`。

## CI/CD（GitHub Actions）

本项目的流水线配置位于 [.github/workflows/ci.yml](.github/workflows/ci.yml)。

### 当前流水线行为

- 触发方式
  - `develop`、`master` 分支的 push 会触发
  - 指向 `develop`、`master` 的 PR 会触发
  - 文档和构建产物目录变更（如 `docs/**`、`target/**`）默认不触发
- CI 阶段（Build and Test）
  - 在 Java 17 环境执行 `./mvnw verify`
  - 使用 Maven 缓存加速依赖下载
  - 发布 Surefire/Failsafe 测试报告产物
  - 生成并发布 JaCoCo 报告产物
  - 在 PR 中评论 JaCoCo 覆盖率报告
  - JaCoCo 产物名采用规范格式：`jacoco-report-分支-rRunNumber-短SHA`
  - JaCoCo 产物默认保留 30 天
- 安全与性能阶段
  - `master` push、手动触发或定时任务会运行 OWASP Dependency-Check
  - `master` push 或手动触发会运行 Selenium UI 测试、Playwright 视觉回归测试与 k6 性能测试
  - UI 测试跳过重复的单元测试，并上传测试报告、截图和测试输出产物
  - Playwright 会上传视觉测试报告、trace、截图和基线产物
  - k6 会上传 JSON、HTML 报告和应用日志产物，并按接口维护延迟阈值


### 如何启用这条 GitHub Actions 流水线

1. 将 [.github/workflows/ci.yml](.github/workflows/ci.yml) 提交到仓库默认分支。
2. 可选：在 GitHub 仓库 Settings -> Secrets and variables -> Actions 中新增 `NVD_API_KEY`，用于提高 OWASP Dependency-Check 的 NVD 同步稳定性。
3. 在 Actions 页面确认工作流已启用。
4. 提交一次 `develop` 或 `master` 变更，或手动触发 workflow_dispatch 验证。

## 本地 breakpoint 压测

本项目使用 k6 做本地 breakpoint 压测。相比 JMeter，k6 可以直接复用当前仓库里的登录、浏览、预约写入和清理流程，更适合这个项目继续扩展。

默认运行方式：

```bash
scripts/run-k6-breakpoint.sh
```

脚本会启动 `docker compose`、等待 `/actuator/health` 变为 `UP`，然后运行 `performance/k6/appointmentscheduler-breakpoint.js`。默认按 RPS 逐级升压：

```text
10,20,40,60,80,100,125,150 requests/s
```

常用参数：

```bash
K6_BREAKPOINT_RATES=20,40,80,120,160 \
K6_STAGE_DURATION=3m \
K6_PRE_ALLOCATED_VUS=80 \
K6_MAX_VUS=300 \
scripts/run-k6-breakpoint.sh
```

如果已有本地服务在运行：

```bash
START_STACK=false BASE_URL=http://localhost:8080 scripts/run-k6-breakpoint.sh
```

结果会保存到 `target/k6-breakpoint/<timestamp>/`，其中包括：

- `summary.json`
- `metrics.json`
- `checks.json`
- `http-status.json`
- `report.html`
- `docker-compose.log`

判断临界值时重点看 P95/P99 延迟、失败率、checks 成功率、RPS 是否进入平台期，以及 `docker-compose.log` 中是否出现数据库连接、超时或异常。

### 部署说明

当前流水线只做 CI、安全扫描与性能验证，不包含自动部署到服务器。

## 说明

- `docker-compose.yml` 会基于本地源码构建镜像，适合验证当前工作区代码的容器化运行效果
- 代码中仍保留少量历史命名，但运行时技术栈、构建流程和基础设施已经对齐到当前的 Spring Boot 3 / Java 17 基线

## 许可证

本项目使用 MIT License，详见 [LICENSE.md](LICENSE.md)。
