-- DB: master

--
-- 部门
--

DROP TABLE IF EXISTS `a_master_dept`;
CREATE TABLE `a_master_dept` (
  `id` CHAR(16) NOT NULL,
  `pid` CHAR(16) DEFAULT NULL,
  `name` VARCHAR(200) NOT NULL,
  `note` TEXT,
  `ctime` INTEGER(10) DEFAULT NULL,
  `mtime` INTEGER(10) DEFAULT NULL,
  `rtime` INTEGER(10) DEFAULT NULL,
  `state` TINYINT DEFAULT '1',
  PRIMARY KEY (`id`),
  FOREIGN KEY (`pid`) REFERENCES `a_master_dept` (`id`) ON DELETE CASCADE
);

CREATE INDEX `IK_a_master_dept_dept` ON `a_master_dept` (`pid`);
CREATE INDEX `IK_a_master_dept_state` ON `a_master_dept` (`state`);
CREATE INDEX `IK_a_master_dept_ctime` ON `a_master_dept` (`ctime`);
CREATE INDEX `IK_a_master_dept_mtime` ON `a_master_dept` (`mtime`);
CREATE UNIQUE INDEX `UK_a_master_dept_name` ON `a_master_dept` (`name`,`pid`);

INSERT INTO `a_master_dept` (`id`,`pid`,`name`,`note`,`ctime`,`mtime`,`rtime`,`state`) VALUES ('0',NULL,'ROOT','ROOT','1424075622','1424075622','0',1);
INSERT INTO `a_master_dept` (`id`,`pid`,`name`,`note`,`ctime`,`mtime`,`rtime`,`state`) VALUES ('CENTRA','0','管理区','默认内部管理区域','1424075622','1424075622','0',1);
INSERT INTO `a_master_dept` (`id`,`pid`,`name`,`note`,`ctime`,`mtime`,`rtime`,`state`) VALUES ('CENTRE','0','公共区','默认注册到此区域','1424075622','1424075622','0',1);

--
-- 部门拥有角色
--

DROP TABLE IF EXISTS `a_master_dept_role`;
CREATE TABLE `a_master_dept_role` (
  `dept_id` CHAR(16) NOT NULL,
  `role` VARCHAR(80) NOT NULL,
  PRIMARY KEY (`dept_id`,`role`),
  FOREIGN KEY (`dept_id`) REFERENCES `a_master_dept` (`id`)
);

CREATE INDEX `IK_a_master_dept_role_dept` ON `a_master_dept_role` (`dept_id`);
CREATE INDEX `IK_a_master_dept_role_role` ON `a_master_dept_role` (`role`);

INSERT INTO `a_master_dept_role` VALUES ('CENTRA','centra');
INSERT INTO `a_master_dept_role` VALUES ('CENTRA','centre');
INSERT INTO `a_master_dept_role` VALUES ('CENTRE','centre');

--
-- 用户
--

DROP TABLE IF EXISTS `a_master_user`;
CREATE TABLE `a_master_user` (
  `id` CHAR(16) NOT NULL,
  `passcode` VARCHAR(128) DEFAULT NULL, /* 密码校验码 */
  `password` VARCHAR(128) DEFAULT NULL,
  `username` VARCHAR(200) DEFAULT NULL,
  `name` VARCHAR(200) DEFAULT NULL,
  `head` VARCHAR(100) DEFAULT NULL,
  `note` TEXT,
  `ctime` INTEGER(10) DEFAULT NULL,
  `mtime` INTEGER(10) DEFAULT NULL,
  `rtime` INTEGER(10) DEFAULT NULL, /* 权限最后更新时间 */
  `state` TINYINT DEFAULT '1', /* 1 正常, 2 系统, 0 删除, -1 锁定
  /* 扩展字段 */
  `organ` VARCHAR(200) DEFAULT NULL,
  `title` VARCHAR(200) DEFAULT NULL,
  `email` VARCHAR(200) DEFAULT NULL,
  `phone` VARCHAR(20 ) DEFAULT NULL,
  `email_checked` TINYINT DEFAULT '0',
  `phone_checked` TINYINT DEFAULT '0',
  PRIMARY KEY (`id`)
);

CREATE INDEX `IK_a_master_user_ctime` ON `a_master_user` (`ctime`);
CREATE INDEX `IK_a_master_user_mtime` ON `a_master_user` (`mtime`);
CREATE INDEX `IK_a_master_user_state` ON `a_master_user` (`state`);
CREATE INDEX `IK_a_master_user_username` ON `a_master_user` (`username`);
CREATE UNIQUE INDEX `UK_a_master_user_username` ON `a_master_user` (`username`);
-- CREATE UNIQUE INDEX `UK_a_master_user_email` ON `a_master_user` (`email`);
-- CREATE UNIQUE INDEX `UK_a_master_user_phone` ON `a_master_user` (`phone`);

INSERT INTO `a_master_user` (`id`,`password`,`username`,`name`,`head`,`note`,`ctime`,`mtime`,`rtime`,`state`) VALUES ('1','9BA587D4E465F45669F19AF20CA033D9','abc@def.cn','超级管理员',NULL,NULL,'1424075622','1424075622','0',1);
INSERT INTO `a_master_user` (`id`,`password`,`username`,`name`,`head`,`note`,`ctime`,`mtime`,`rtime`,`state`) VALUES ('0','9BA587D4E465F45669F19AF20CA033D9','xyz@def.cn','匿名使用者',NULL,NULL,'1424075622','1424075622','0',2);

--
-- 用户所属部门
--

DROP TABLE IF EXISTS `a_master_user_dept`;
CREATE TABLE `a_master_user_dept` (
  `user_id` CHAR(16) NOT NULL,
  `dept_id` CHAR(16) NOT NULL,
  PRIMARY KEY (`user_id`,`dept_id`),
  FOREIGN KEY (`user_id`) REFERENCES `a_master_user` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`dept_id`) REFERENCES `a_master_dept` (`id`) ON DELETE CASCADE
);

CREATE INDEX `IK_a_master_user_dept_user` ON `a_master_user_dept` (`user_id`);
CREATE INDEX `IK_a_master_user_dept_dept` ON `a_master_user_dept` (`dept_id`);

INSERT INTO `a_master_user_dept` VALUES ('1','0');

--
-- 用户所属角色
--

DROP TABLE IF EXISTS `a_master_user_role`;
CREATE TABLE `a_master_user_role` (
  `user_id` CHAR(16) NOT NULL,
  `role` VARCHAR(80) NOT NULL,
  PRIMARY KEY (`user_id`,`role`),
  FOREIGN KEY (`user_id`) REFERENCES `a_master_user` (`id`) ON DELETE CASCADE
);

CREATE INDEX `IK_a_master_user_role_user` ON `a_master_user_role` (`user_id`);
CREATE INDEX `IK_a_master_user_role_role` ON `a_master_user_role` (`role`);

INSERT INTO `a_master_user_role` VALUES ('1','centra');
INSERT INTO `a_master_user_role` VALUES ('1','centre');
INSERT INTO `a_master_user_role` VALUES ('1','centra/manage/info');
INSERT INTO `a_master_user_role` VALUES ('1','centra/manage/file');
INSERT INTO `a_master_user_role` VALUES ('1','centra/manage/fils');
INSERT INTO `a_master_user_role` VALUES ('1','centra/master/user/search');
INSERT INTO `a_master_user_role` VALUES ('1','centra/master/user/create');
INSERT INTO `a_master_user_role` VALUES ('1','centra/master/user/update');
INSERT INTO `a_master_user_role` VALUES ('1','centra/master/user/delete');
INSERT INTO `a_master_user_role` VALUES ('1','centra/master/dept/search');
INSERT INTO `a_master_user_role` VALUES ('1','centra/master/dept/create');
INSERT INTO `a_master_user_role` VALUES ('1','centra/master/dept/update');
INSERT INTO `a_master_user_role` VALUES ('1','centra/master/dept/delete');
INSERT INTO `a_master_user_role` VALUES ('1','centra/matrix/form/search');
INSERT INTO `a_master_user_role` VALUES ('1','centra/matrix/form/create');
INSERT INTO `a_master_user_role` VALUES ('1','centra/matrix/form/update');
INSERT INTO `a_master_user_role` VALUES ('1','centra/matrix/form/delete');
INSERT INTO `a_master_user_role` VALUES ('1','centra/matrix/unit/search');
INSERT INTO `a_master_user_role` VALUES ('1','centra/matrix/unit/create');
INSERT INTO `a_master_user_role` VALUES ('1','centra/matrix/unit/update');
INSERT INTO `a_master_user_role` VALUES ('1','centra/matrix/unit/delete');

--
-- 用户关联登录
--

DROP TABLE IF EXISTS `a_master_user_sign`;
CREATE TABLE `a_master_user_sign` (
  `user_id` CHAR(16) NOT NULL,
  `unit` VARCHAR(32) NOT NULL,
  `code` VARCHAR(64) NOT NULL,
  PRIMARY KEY (`user_id`,`unit`),
  FOREIGN KEY (`user_id`) REFERENCES `a_master_user` (`id`) ON DELETE CASCADE
);

CREATE INDEX `IK_a_master_user_sign_user` ON `a_master_user_sign` (`user_id`);
CREATE INDEX `IK_a_master_user_sign_unit` ON `a_master_user_sign` (`unit`);
CREATE INDEX `IK_a_master_user_sign_code` ON `a_master_user_sign` (`code`);
CREATE UNIQUE INDEX `UK_a_master_user_sign_id` ON `a_master_user_sign` (`unit`, `code`);
