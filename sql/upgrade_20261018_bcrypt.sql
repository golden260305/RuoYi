-- ----------------------------
-- 密码摘要算法升级为 BCrypt 所需的结构调整
-- 执行时机：升级代码前，对已有数据库执行一次
-- 说明：BCrypt 摘要固定 60 字符，原 varchar(50) 会被截断导致校验失败
-- ----------------------------

alter table sys_user modify column password varchar(100) default '' comment '密码';
