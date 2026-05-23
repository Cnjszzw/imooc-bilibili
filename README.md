# 仿B站弹幕视频网站

> **个人全栈项目** | 2024/05 - 2024/11 | [GitHub](https://github.com/Cnjszzw/imooc-bilibili)

SpringBoot + Vue 仿 Bilibili 弹幕视频网站，实现用户体系、视频管理、弹幕推送、站内搜索、动态发布等核心功能。

---

## 技术栈

| 层次 | 技术 |
|------|------|
| 后端框架 | SpringBoot 2.5、MyBatis |
| 数据库与缓存 | MySQL 8.0、Redis |
| 消息队列 | RocketMQ 4.9 |
| 搜索引擎 | Elasticsearch 7.x + Kibana |
| 文件存储 | FastDFS（分布式文件系统） + Nginx |
| 实时通信 | WebSocket（JSR 356） |
| 前端 | Vue 2、Axios、Video.js |
| 安全 | JWT（access + refresh）、RSA + MD5 |

---

## 架构总览

```
┌──────────┐    ┌──────────────┐    ┌─────────┐    ┌───────────┐
│  Vue 前端 │───▶│ SpringBoot API│───▶│  MySQL  │    │  FastDFS  │
│          │    │              │    │  Redis  │    │  + Nginx  │
│  弹幕推送◀────│  WebSocket  ◀─┼──▶│ RocketMQ│    │           │
│  动态刷新◀────│  Elasticsearch│   └─────────┘    └───────────┘
└──────────┘    └──────────────┘
```

---

## 核心亮点

### 1. 用户体系：工厂+策略模式 + JWT 双Token

- 登录采用**工厂 + 策略模式**：`UserGranter` 策略接口定义登录契约，`UserLoginFactory` 通过配置驱动注册具体策略（PhoneGranter/EmailGranter）
- 新增登录方式只需**实现接口 + 配置文件注册**，无需修改调用方代码（开闭原则）
- JWT **双 Token 无感登录**：access token 过期后用 refresh token 自动续期
- RSA 加密传输密码 + MD5 加盐存储，RBAC 模型 + AOP 切面实现接口级/数据级权限控制

### 2. 动态发布：三轮迭代 → 推拉结合（最大亮点）

动态系统经历**三次迭代**，逐步解决高并发写入瓶颈：

| 版本 | 方案 | 10万粉丝耗时 | 瓶颈 |
|------|------|------------|------|
| V1 | 同步推：遍历粉丝直接写 Redis | 用户等待 **7.3s** | 写入 O(粉丝数)，大V不可用 |
| V2 | MQ 异步推：发消息即返回 | 用户等待 **40ms** | 消费者仍需遍历写入 7s |
| V3 | **推拉结合** | 推 ~1ms / 拉 ~1ms | 解决 |

**V3 推拉结合逻辑**：
- 粉丝 **< 10w**：走推模式（遍历写入粉丝收件箱）
- 粉丝 **≥ 10w**：走拉模式（大 V 只写自己的发件箱 `outbox-{userId}`，粉丝刷动态时主动拉取）
- 临界点 10w 来源于压测数据：push 成本 = 粉丝数 × Redis SET，pull 成本 = 1 次 LPUSH，交叉点在 10w

**压测结果**（本地单机，MacBook Air M4/24GB）：

| 粉丝数 | V2 纯推 | V3 推拉结合 |
|--------|--------|------------|
| 1,000 | ~268ms | ~268ms |
| 10,000 | ~1,031ms | ~1,031ms |
| 100,000 | 7,307ms | **~250ms** |
| 1,000,000 | 72s+ | **~2ms** |

> 详细测试方法与数据见 [`doc/`](doc/) 目录。

### 3. 弹幕系统：WebSocket + MQ 异步削峰

- 基于 WebSocket（JSR 356 `@ServerEndpoint`）构建弹幕实时推送
- 弹幕高并发写入场景：消息先入 RocketMQ → 消费者异步消费落库，避免瞬间大量写入压垮 DB
- 连接管理：`ConcurrentHashMap` 管理所有在线连接的注册/注销，每 5s 推送在线人数

### 4. 视频存储：FastDFS + 断点续传 + 秒传

- 基于 FastDFS 实现视频分布式存储，支持**分片上传与断点续传**
- 秒传功能：前端计算文件 MD5 → 后端查库，命中则跳过上传直接返回（降低服务器带宽）
- Nginx 反向代理提供外部 HTTP 访问

### 5. 站内搜索：Elasticsearch Multi-Match

- 基于 Multi-Match 跨字段全文检索（title / nick / description）
- 关键词高亮 + 多维度排序（按时间/弹幕数/播放量）+ 分页查询
- 用户注册与视频发布时同步写入 ES 索引，保证搜索实时性

### 6. 线程池性能优化

- 自定义 `ThreadPoolExecutor`（5核心/6最大/60s超时/LinkedBlockingQueue/AbortPolicy）
- 利用线程池并行聚合多源数据，数据汇总接口响应从 **150ms → 50ms**，提升 67%

---

## 项目结构

```
server/imooc-bilibili/
├── imooc-bilibili-api        # API 层（Controller + AOP 切面）
├── imooc-bilibili-service    # 业务层（服务 + 策略模式 + WebSocket + 配置）
└── imooc-bilibili-dao        # 数据层（MyBatis Mapper + ES Repository）

frontEnd/imooc-bilibili-vue/  # Vue 前端
doc/                          # 压测文档（V1/V2/V3）
```

---

## 快速启动

### 环境要求

- JDK 8+、MySQL 8.0、Redis、RocketMQ 4.9、Elasticsearch 7.x、FastDFS、Node.js

### 后端

```bash
cd server/imooc-bilibili
# 初始化数据库（执行 SQL 脚本）
# 配置 application.properties 中的数据库/Redis/MQ/ES/FastDFS 连接信息
mvn clean package
java -jar imooc-bilibili-api/target/imooc-bilibili-api.jar
```

### 前端

```bash
cd frontEnd/imooc-bilibili-vue
npm install
npm run serve
```

---

## 历史文档

- [旧版 README（含 WebSocket 实现细节 Q&A）](doc/README-history.md)
- [动态发布 V1：同步推送性能测试](doc/动态发布-V1同步推送性能测试.md)
- [动态发布 V2：MQ 异步推送性能测试](doc/动态发布-V2MQ异步推送性能测试.md)
- [动态发布 V3：推拉结合](doc/动态发布-V3推拉结合.md)
