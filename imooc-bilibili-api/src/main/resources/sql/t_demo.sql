SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_demo
-- ----------------------------
DROP TABLE IF EXISTS `t_demo`;
CREATE TABLE `t_demo`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '姓名',
  `createTime` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '测试表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Insert mock data into t_demo
-- ----------------------------
INSERT INTO `t_demo` (`name`, `createTime`) VALUES
('张三', '2025-01-15 10:30:00'),
('李四', '2025-02-20 14:20:00'),
('王五', '2025-03-10 09:15:00'),
('赵六', '2025-03-25 16:45:00'),
('孙七', '2025-04-08 11:00:00'),
('周八', '2025-04-18 08:30:00'),
('吴九', '2025-05-01 13:10:00'),
('郑十', '2025-05-05 17:55:00'),
('冯十一', '2025-06-12 10:05:00'),
('陈十二', '2025-07-22 15:40:00'),
('褚十三', '2025-08-03 12:25:00'),
('卫十四', '2025-08-19 09:50:00'),
('蒋十五', '2025-09-07 14:15:00'),
('沈十六', '2025-10-11 11:35:00'),
('韩十七', '2025-11-29 16:00:00'),
('杨十八', '2025-12-08 10:10:00'),
('朱十九', '2026-01-16 08:55:00'),
('秦二十', '2026-02-26 13:40:00'),
('许二一', '2026-03-14 17:20:00'),
('何二二', '2026-04-02 11:05:00');

SET FOREIGN_KEY_CHECKS = 1;
