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

```sql
CREATE DATABASE appointmentscheduler;
CREATE USER 'user'@'%' IDENTIFIED BY 'password';
GRANT ALL PRIVILEGES ON appointmentscheduler.* TO 'user'@'%';
FLUSH PRIVILEGES;
```

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

仅运行单元测试：

```bash
mvn test
```

运行完整构建，包括集成测试：

```bash
mvn clean verify
```

说明：

- 集成测试通过 Testcontainers 拉起 MySQL，因此需要 Docker
- `src/test/java/**/ui/**` 下的 UI 测试默认不会在 `verify` 流程中执行

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

## CI

当前 [azure-pipelines.yml](azure-pipelines.yml) 的流水线行为如下：

- 在 Java 17 环境下执行 `mvn verify`
- 发布 Surefire 和 Failsafe 的测试报告
- 在 `master` 分支通过 Jib 构建并发布镜像

## 说明

- `docker-compose.yml` 当前更适合做已发布镜像的容器化运行验证，不是本地开发源码调试的主流程
- 代码中仍保留少量历史命名，但运行时技术栈、构建流程和基础设施已经对齐到当前的 Spring Boot 3 / Java 17 基线

## 许可证

本项目使用 MIT License，详见 [LICENSE.md](LICENSE.md)。
