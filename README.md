#### 项目背景
这是一个完全仿照着BiliBili网站（B站）开发的项目,项目购买自慕课网。选择的原因如下：
（1）网上认识的同学有推荐过这个项目，当时靠着这个项目进了美团，所以我觉得不会差。
（2）这个项目确实有一些难点，就比如高并发（向千万用户发布动态，如何处理？），涉及到了很多主流的额常用技术栈，比如SpringBoot、Redis、RocketMQ、WebSocket、FastDFS等等。
（3）确实个人也比较喜欢这个B站，觉得做出来有成就感。
#### 项目的技术架构
下方是整个项目的架构图，其中这个springcloud暂时我还没来得及搞，包括这个Jenkins自动化部署也还没来得及搞
![image](https://github.com/user-attachments/assets/318e348f-6ed7-4fa5-87f0-3c268da85de8)
#### 模块划分
下方是整个项目的功能模块，同样我负责了其中的（1）通用功能模块（2）用户服务模块（3）核心功能模块（4）部分前端的编写（主要是抄和修改）
![image](https://github.com/user-attachments/assets/5f78bdf2-7ca3-46f5-bbd5-df8f2b2a6b30)
#### 项目的个人优化
##### （1）设计模式
通过自己学到的策略工厂模式，对这个登录的功能进行了重构
https://github.com/Cnjszzw/imooc-bilibili/commit/d990cc714535dd9e14504746fe35106a431bb56f
##### （2）线程池
原来的项目没用到这个线程池技术，我通过对这个项目的分析，找到了适合利用线程池的地方，并进行了改造，最后提升了接口响应速度
https://github.com/Cnjszzw/imooc-bilibili/commit/f10361b3b12b5334c1b0eef5dfd4018f3ad30025#diff-89bb0681bc7b37fcb05ba70f78bd28ff8c7da4b91eff65f1fd27f1f05413cd08
##### （3）跨域问题的优化
通过修改后端代码的方式，来解决了这种跨域问题
https://github.com/Cnjszzw/imooc-bilibili/commit/61e3ca309324317f08ee327360956907a01b7ed1
#### BUG清单:
(1):快速点击 页面的收藏视频，出现了收藏数量异常增加和减少的情况，原因是
快速点击的时候，有时候会连续多次请求这个收藏或者取消收藏的接口，后端这边
不会进行报错提示，前端以为都是正常的，导致进行了数量的加减，最终体现在
页面上了，但是重新刷新页面，点赞数量是正常的，后端接口正常。准确来讲是前端
的bug

#### 架构疑问：弹幕推送为什么要“相乘”？

**场景**：10万用户，5台服务器负载均衡，每台持有2万条WebSocket长连接。此时10万用户同时发弹幕，每台服务器收到约2万条弹幕。

**疑问**：为什么不直接“一口气”把2万条弹幕广播给所有用户？为什么要算乘法（2万 × 2万 = 4亿）？

**答案**：关键在于**每条弹幕是独立消息，无法天然打包**。

以本项目实际代码（`WebSocketService.onMessage` + RocketMQ）为例：

1. 用户 A 在 Server-1 发一条弹幕 "666"
2. `@OnMessage` 触发 → 遍历 Server-1 的 `WEBSOCKET_MAP`（2万个 session）
3. 为每个 session 生成一条 RocketMQ 消息 `{sessionId, message: "666"}`
4. 各服务器的 `danmusConsumer` 消费到消息后，按 `sessionId` 找到对应 WebSocket 连接，push 弹幕

**所以 1 条弹幕 → 2万条 RocketMQ 推送**。2万条弹幕同时到达 → 2万 × 2万 = 4亿次推送。这就是“相乘”的来源。

**为什么不能“一口气推送”？**
- 每台服务器只持有 2万 条连接，无法直接 push 到其余 8万 用户（必须跨服务器协调）
- 弹幕是实时逐条产生的，不存在一个天然打包好的“2万条弹幕包裹”
- 要做批量优化（如 100ms 收集窗口聚合），需要自己设计一个缓冲层，那是优化手段而不是默认行为

**一句话**：push 次数 = 弹幕数 × 每台服务器连接数，是 O(N×M) 的拓扑问题，不是一次群发。

#### 面试 Q&A：WebSocket 是如何实现的？

##### Q1：`@ServerEndpoint("/imserver/{token}")` 是什么？

这是 Java JSR 356 标准的 WebSocket 注解，声明该类为一个 WebSocket 服务端点。由 `WebSocketConfig` 中的 `ServerEndpointExporter` 扫描注册到 Tomcat 容器。

**核心特性**：`@ServerEndpoint` 是多例模型——每个客户端发起连接，Tomcat 的 WebSocket 容器会 new 一个全新的 `WebSocketService` 实例。`@Component` 注解只负责让 Spring 扫描到这个类，不改变实例化方式。

```
客户端A连接 → Tomcat new WebSocketService() → 实例① { session=A, userId=1001 }
客户端B连接 → Tomcat new WebSocketService() → 实例② { session=B, userId=1002 }
客户端C连接 → Tomcat new WebSocketService() → 实例③ { session=C, userId=null }
```

##### Q2：`ConcurrentHashMap<String, WebSocketService>` 为什么 value 是它自己？

```java
public static final ConcurrentHashMap<String, WebSocketService> WEBSOCKET_MAP = new ConcurrentHashMap<>();
```

- `static` — 类级别共享，所有实例可见（实例变量隔离，全局状态共享）
- `ConcurrentHashMap` — 多用户并发连接/断开，线程安全
- Key = `session.getId()`（WebSocket 会话唯一标识）
- Value = 当前实例 `this`

**为什么 value 是 WebSocketService？** 每个连接一个实例，实例内持有该连接的 `Session`、`userId`。遍历 Map 就是遍历所有在线连接，拿到 `session.getBasicRemote().sendText()` 即可向该用户推送消息。

生命周期：
```
@OnOpen   → WEBSOCKET_MAP.put(sessionId, this)   // 自注册
@OnClose  → WEBSOCKET_MAP.remove(sessionId)       // 自清理
```

##### Q3：`ApplicationContext` 是干什么的？

```java
private static ApplicationContext APPLICATION_CONTEXT;
```

**原因**：`@ServerEndpoint` 实例由 Tomcat 的 WebSocket 容器创建，不在 Spring 容器管理范围内，所以 `@Autowired` 无效。

**解决**：应用启动时手动注入：

```java
// ImoocBilibiliApp.main()
ApplicationContext app = SpringApplication.run(...);
WebSocketService.setApplicationContext(app);
```

运行时通过 `APPLICATION_CONTEXT.getBean("beanName")` 手动获取 Spring 管理的 Bean。

##### Q4：四个生命周期方法分别做什么？

| 注解 | 触发时机 | 做了什么 |
|------|---------|---------|
| `@OnOpen` | 客户端连接建立 | 解析 token→userId；将自己存入 `WEBSOCKET_MAP`；在线人数+1；回复 "0" 表示连接成功 |
| `@OnMessage` | 收到客户端消息 | 处理消息（业务逻辑通过 `APPLICATION_CONTEXT.getBean()` 获取 Spring Bean） |
| `@OnClose` | 客户端断开 | 从 `WEBSOCKET_MAP` 移除自己；在线人数-1 |
| `@OnError` | 连接出错 | （空实现，仅占位） |
| `@Scheduled(fixedRate=5000)` | 每5秒定时 | 遍历 `WEBSOCKET_MAP` 向所有在线连接推送当前在线人数 |

##### 一句话总结

> JSR 356 规定每个连接一个端点实例（多例），实例持有该连接的私有上下文；两个 static 变量解决跨实例协调：`WEBSOCKET_MAP` 管理所有连接的注册/注销，`APPLICATION_CONTEXT` 解决 Spring Bean 无法 `@Autowired` 的问题。
