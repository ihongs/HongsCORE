-- DB: master

--
-- 部门
--

DROP TABLE IF EXISTS `a_master_unit`;
CREATE TABLE `a_master_unit` (
  `id` CHAR(16) NOT NULL,
  `pid` CHAR(16) DEFAULT NULL,
  `name` VARCHAR(200) NOT NULL,
  `note` TEXT,
  `ctime` INTEGER(10) DEFAULT NULL,
  `mtime` INTEGER(10) DEFAULT NULL,
  `rtime` INTEGER(10) DEFAULT NULL,
  `boost` INTEGER DEFAULT '0',
  `state` TINYINT DEFAULT '1',
  PRIMARY KEY (`id`),
  FOREIGN KEY (`pid`) REFERENCES `a_master_unit` (`id`) ON DELETE CASCADE
);

CREATE INDEX `IK_a_master_unit_unit` ON `a_master_unit` (`pid`);
CREATE INDEX `IK_a_master_unit_boost` ON `a_master_unit` (`boost`);
CREATE INDEX `IK_a_master_unit_state` ON `a_master_unit` (`state`);
CREATE INDEX `IK_a_master_unit_ctime` ON `a_master_unit` (`ctime`);
CREATE INDEX `IK_a_master_unit_mtime` ON `a_master_unit` (`mtime`);
CREATE UNIQUE INDEX `UK_a_master_unit_name` ON `a_master_unit` (`name`,`pid`);

INSERT INTO `a_master_unit` (`id`,`pid`,`name`,`note`,`ctime`,`mtime`,`rtime`,`state`) VALUES ('0',NULL,'ROOT','ROOT','1424075622','1424075622','0',1);
INSERT INTO `a_master_unit` (`id`,`pid`,`name`,`note`,`ctime`,`mtime`,`rtime`,`state`) VALUES ('CENTRA','0','管理区','默认内部管理区域','1424075622','1424075622','0',1);
INSERT INTO `a_master_unit` (`id`,`pid`,`name`,`note`,`ctime`,`mtime`,`rtime`,`state`) VALUES ('CENTRE','0','公共区','默认注册到此区域','1424075622','1424075622','0',1);

--
-- 部门拥有角色
--

DROP TABLE IF EXISTS `a_master_unit_role`;
CREATE TABLE `a_master_unit_role` (
  `unit_id` CHAR(16) NOT NULL,
  `role` VARCHAR(80) NOT NULL,
  PRIMARY KEY (`unit_id`,`role`),
  FOREIGN KEY (`unit_id`) REFERENCES `a_master_unit` (`id`)
);

CREATE INDEX `IK_a_master_unit_role_unit` ON `a_master_unit_role` (`unit_id`);
CREATE INDEX `IK_a_master_unit_role_role` ON `a_master_unit_role` (`role`);

INSERT INTO `a_master_unit_role` VALUES ('CENTRA','centra');
INSERT INTO `a_master_unit_role` VALUES ('CENTRA','centre');
INSERT INTO `a_master_unit_role` VALUES ('CENTRE','centre');

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
  `ptime` INTEGER(10) DEFAULT NULL, /* 密码最后更新时间 */
  `state` TINYINT DEFAULT '1', /* 1 正常, 2 系统, 0 删除, -1 锁定
  /* 扩展字段 */
  `title` VARCHAR(200) DEFAULT NULL,
  `email` VARCHAR(200) DEFAULT NULL,
  `phone` VARCHAR(20 ) DEFAULT NULL,
  `title_checked` TINYINT DEFAULT '0',
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

INSERT INTO `a_master_user` (`id`,`password`,`username`,`name`,`head`,`note`,`ctime`,`mtime`,`rtime`,`ptime`,`state`) VALUES ('1','9BA587D4E465F45669F19AF20CA033D9','abc@def.cn','超管',NULL,NULL,'1424075622','1424075622','0','0', 1);
INSERT INTO `a_master_user` (`id`,`password`,`username`,`name`,`head`,`note`,`ctime`,`mtime`,`rtime`,`ptime`,`state`) VALUES ('0','9BA587D4E465F45669F19AF20CA033D9','xyz@def.cn','佚名',NULL,NULL,'1424075622','1424075622','0','0',-2);

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
INSERT INTO `a_master_user_role` VALUES ('1','centra/master/unit/search');
INSERT INTO `a_master_user_role` VALUES ('1','centra/master/unit/create');
INSERT INTO `a_master_user_role` VALUES ('1','centra/master/unit/update');
INSERT INTO `a_master_user_role` VALUES ('1','centra/master/unit/delete');
INSERT INTO `a_master_user_role` VALUES ('1','centra/matrix/form/search');
INSERT INTO `a_master_user_role` VALUES ('1','centra/matrix/form/create');
INSERT INTO `a_master_user_role` VALUES ('1','centra/matrix/form/update');
INSERT INTO `a_master_user_role` VALUES ('1','centra/matrix/form/delete');
INSERT INTO `a_master_user_role` VALUES ('1','centra/matrix/furl/search');
INSERT INTO `a_master_user_role` VALUES ('1','centra/matrix/furl/create');
INSERT INTO `a_master_user_role` VALUES ('1','centra/matrix/furl/update');
INSERT INTO `a_master_user_role` VALUES ('1','centra/matrix/furl/delete');

--
-- 用户所属部门
--

DROP TABLE IF EXISTS `a_master_unit_user`;
CREATE TABLE `a_master_unit_user` (
  `user_id` CHAR(16) NOT NULL,
  `unit_id` CHAR(16) NOT NULL,
  `type` TINYINT DEFAULT '0', /* 0 所属部门, 1 管理部分 */
  PRIMARY KEY (`user_id`,`unit_id`,`type`),
  FOREIGN KEY (`user_id`) REFERENCES `a_master_user` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`unit_id`) REFERENCES `a_master_unit` (`id`) ON DELETE CASCADE
);

CREATE INDEX `IK_a_master_unit_user_user` ON `a_master_unit_user` (`user_id`);
CREATE INDEX `IK_a_master_unit_user_unit` ON `a_master_unit_user` (`unit_id`);
CREATE INDEX `IK_a_master_unit_user_type` ON `a_master_unit_user` (`type`);

INSERT INTO `a_master_unit_user` VALUES ('1','0','0');

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
