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



#### BUG清单:
(1):快速点击 页面的收藏视频，出现了收藏数量异常增加和减少的情况，原因是
快速点击的时候，有时候会连续多次请求这个收藏或者取消收藏的接口，后端这边
不会进行报错提示，前端以为都是正常的，导致进行了数量的加减，最终体现在
页面上了，但是重新刷新页面，点赞数量是正常的，后端接口正常。准确来讲是前端
的bug
