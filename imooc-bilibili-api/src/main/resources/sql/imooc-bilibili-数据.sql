-- 初始化 t_following_group 默认分组
INSERT INTO `t_following_group` (`id`, `userId`, `name`, `type`, `createTime`, `updateTime`)
VALUES
    (1, NULL, '特别关注', '0', NOW(), NOW()),
    (2, NULL, '悄悄关注', '1', NOW(), NOW()),
    (3, NULL, '默认分组', '2', NOW(), NOW());

-- 初始化 t_auth_role 角色数据
INSERT INTO `t_auth_role` (`id`, `name`, `code`, `createTime`, `updateTime`)
VALUES
    (1, '等级0', 'Lv0', NOW(), NOW()),
    (2, '等级1', 'Lv1', NOW(), NOW()),
    (3, '等级2', 'Lv2', NOW(), NOW());