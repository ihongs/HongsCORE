-- DB: member

--
-- 部门
--

DROP TABLE IF EXISTS `a_member_dept`;
CREATE TABLE `a_member_dept` (
  `id` CHAR(20) NOT NULL,
  `pid` CHAR(20) DEFAULT NULL,
  `name` VARCHAR(200) NOT NULL,
  `note` TEXT,
  `ctime` UNSIGNED INT(10) DEFAULT NULL,
  `mtime` UNSIGNED INT(10) DEFAULT NULL,
  `rtime` UNSIGNED INT(10) DEFAULT NULL,
  `state` TINYINT(2) DEFAULT '1',
  PRIMARY KEY (`id`),
  FOREIGN KEY (`pid`) REFERENCES `a_member_dept` (`id`) ON DELETE CASCADE
);

CREATE INDEX `IK_a_member_dept_dept` ON `a_member_dept` (`pid`);
CREATE INDEX `IK_a_member_dept_state` ON `a_member_dept` (`state`);
CREATE INDEX `IK_a_member_dept_ctime` ON `a_member_dept` (`ctime`);
CREATE INDEX `IK_a_member_dept_mtime` ON `a_member_dept` (`mtime`);
CREATE UNIQUE INDEX `UK_a_member_dept_name` ON `a_member_dept` (`name`,`pid`);

INSERT INTO `a_member_dept` (`id`,`pid`,`name`,`note`,`ctime`,`mtime`,`rtime`,`state`) VALUES ('0',NULL,'ROOT','ROOT','1424075622230','1424075622230','0',1);
INSERT INTO `a_member_dept` (`id`,`pid`,`name`,`note`,`ctime`,`mtime`,`rtime`,`state`) VALUES ('MANAGE','0','管理区','默认内部管理区域','1424075622230','1424075622230','0',1);
INSERT INTO `a_member_dept` (`id`,`pid`,`name`,`note`,`ctime`,`mtime`,`rtime`,`state`) VALUES ('PUBLIC','0','公共区','默认注册到此区域','1424075622230','1424075622230','0',1);
INSERT INTO `a_member_dept` (`id`,`pid`,`name`,`note`,`ctime`,`mtime`,`rtime`,`state`) VALUES ('HXSDROLE001REB0Q01','MANAGE','技术部','这是技术部','1424075622230','1424075622230','0',1);
INSERT INTO `a_member_dept` (`id`,`pid`,`name`,`note`,`ctime`,`mtime`,`rtime`,`state`) VALUES ('HYPRZ8Q5006II04J01','MANAGE','市场部','这是市场部','1424075622230','1424075622230','0',1);
INSERT INTO `a_member_dept` (`id`,`pid`,`name`,`note`,`ctime`,`mtime`,`rtime`,`state`) VALUES ('HY9XXIS5000T3DD501','HXSDROLE001REB0Q01','研发部','','1424075622230','1424075622230','0',1);
INSERT INTO `a_member_dept` (`id`,`pid`,`name`,`note`,`ctime`,`mtime`,`rtime`,`state`) VALUES ('HYPR7S3N00BWKOZ001','HXSDROLE001REB0Q01','运维部','','1424075622230','1424075622230','0',1);
INSERT INTO `a_member_dept` (`id`,`pid`,`name`,`note`,`ctime`,`mtime`,`rtime`,`state`) VALUES ('HYPS1ROT007T1AG601','HYPRZ8Q5006II04J01','产品部','','1424075622230','1424075622230','0',1);

--
-- 部门所属角色
--

DROP TABLE IF EXISTS `a_member_dept_role`;
CREATE TABLE `a_member_dept_role` (
  `dept_id` CHAR(20) NOT NULL,
  `role` VARCHAR(100) NOT NULL,
  PRIMARY KEY (`dept_id`,`role`),
  FOREIGN KEY (`dept_id`) REFERENCES `a_member_dept` (`id`)
);

CREATE INDEX `IK_a_member_dept_role_dept` ON `a_member_dept_role` (`dept_id`);
CREATE INDEX `IK_a_member_dept_role_role` ON `a_member_dept_role` (`role`);

--
-- 用户
--

DROP TABLE IF EXISTS `a_member_user`;
CREATE TABLE `a_member_user` (
  `id` CHAR(20) NOT NULL,
  `password` VARCHAR(200) DEFAULT NULL,
  `username` VARCHAR(200) DEFAULT NULL,
  `name` VARCHAR(200) DEFAULT NULL,
  `head` VARCHAR(100) DEFAULT NULL,
  `note` TEXT,
  `ctime` UNSIGNED INT(10) DEFAULT NULL,
  `mtime` UNSIGNED INT(10) DEFAULT NULL,
  `rtime` UNSIGNED INT(10) DEFAULT NULL,
  `state` TINYINT(2) DEFAULT '1',
  /* 扩展字段 */
  `organ` VARCHAR(200) DEFAULT NULL,
  `title` VARCHAR(200) DEFAULT NULL,
  `email` VARCHAR(200) DEFAULT NULL,
  `phone` VARCHAR(20 ) DEFAULT NULL,
  `email_checked` TINYINT(1) DEFAULT '0',
  `phone_checked` TINYINT(1) DEFAULT '0',
  PRIMARY KEY (`id`)
);

CREATE INDEX `IK_a_member_user_ctime` ON `a_member_user` (`ctime`);
CREATE INDEX `IK_a_member_user_mtime` ON `a_member_user` (`mtime`);
CREATE INDEX `IK_a_member_user_state` ON `a_member_user` (`state`);
CREATE INDEX `IK_a_member_user_username` ON `a_member_user` (`username`);
CREATE UNIQUE INDEX `UK_a_member_user_username` ON `a_member_user` (`username`);
CREATE UNIQUE INDEX `UK_a_member_user_email` ON `a_member_user` (`email`);
CREATE UNIQUE INDEX `UK_a_member_user_phone` ON `a_member_user` (`phone`);

INSERT INTO `a_member_user` (`id`,`password`,`username`,`name`,`head`,`note`,`ctime`,`mtime`,`rtime`,`state`) VALUES ('1','9BA587D4E465F45669F19AF20CA033D9','abc@def.cn','老大 (管理员)',NULL,NULL,'1424075622230','1424075622230','0',1);
INSERT INTO `a_member_user` (`id`,`password`,`username`,`name`,`head`,`note`,`ctime`,`mtime`,`rtime`,`state`) VALUES ('01I2ODRZHR00KLJOEM','9BA587D4E465F45669F19AF20CA033D9','a@abc.com','张三 (总经理)',NULL,NULL,'1424075622230','1424075622230','0',1);
INSERT INTO `a_member_user` (`id`,`password`,`username`,`name`,`head`,`note`,`ctime`,`mtime`,`rtime`,`state`) VALUES ('01I2ODSOGC00KGZCQK','9BA587D4E465F45669F19AF20CA033D9','b@abc.com','李四 (技术总监)',NULL,NULL,'1424075622230','1424075622230','0',1);
INSERT INTO `a_member_user` (`id`,`password`,`username`,`name`,`head`,`note`,`ctime`,`mtime`,`rtime`,`state`) VALUES ('HXNZ0OLR00297H9H01','9BA587D4E465F45669F19AF20CA033D9','c@abc.com','王五 (市场总监)',NULL,NULL,'1424075622230','1424075622230','0',1);
INSERT INTO `a_member_user` (`id`,`password`,`username`,`name`,`head`,`note`,`ctime`,`mtime`,`rtime`,`state`) VALUES ('HXZGVRBH000XHB0601','9BA587D4E465F45669F19AF20CA033D9','d@abc.com','赵六 (研发主管)',NULL,NULL,'1424075622230','1424075622230','0',1);
INSERT INTO `a_member_user` (`id`,`password`,`username`,`name`,`head`,`note`,`ctime`,`mtime`,`rtime`,`state`) VALUES ('HXZGWPZV002I1J1601','9BA587D4E465F45669F19AF20CA033D9','e@abc.com','钱七 (运维主管)',NULL,NULL,'1424075622230','1424075622230','0',1);
INSERT INTO `a_member_user` (`id`,`password`,`username`,`name`,`head`,`note`,`ctime`,`mtime`,`rtime`,`state`) VALUES ('HY9XQN2L000WGH9Q01','9BA587D4E465F45669F19AF20CA033D9','f@abc.com','孙八 (产品总监)',NULL,NULL,'1424075622230','1424075622230','0',1);

--
-- 用户所属部门
--

DROP TABLE IF EXISTS `a_member_user_dept`;
CREATE TABLE `a_member_user_dept` (
  `user_id` CHAR(20) NOT NULL,
  `dept_id` CHAR(20) NOT NULL,
  PRIMARY KEY (`user_id`,`dept_id`),
  FOREIGN KEY (`user_id`) REFERENCES `a_member_user` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`dept_id`) REFERENCES `a_member_dept` (`id`) ON DELETE CASCADE
);

CREATE INDEX `IK_a_member_user_dept_user` ON `a_member_user_dept` (`user_id`);
CREATE INDEX `IK_a_member_user_dept_dept` ON `a_member_user_dept` (`dept_id`);

INSERT INTO `a_member_user_dept` VALUES ('1','0');
INSERT INTO `a_member_user_dept` VALUES ('01I2ODRZHR00KLJOEM','0');
INSERT INTO `a_member_user_dept` VALUES ('01I2ODSOGC00KGZCQK','HXSDROLE001REB0Q01');
INSERT INTO `a_member_user_dept` VALUES ('HXNZ0OLR00297H9H01','HYPRZ8Q5006II04J01');
INSERT INTO `a_member_user_dept` VALUES ('HXZGVRBH000XHB0601','HY9XXIS5000T3DD501');
INSERT INTO `a_member_user_dept` VALUES ('HXZGWPZV002I1J1601','HYPR7S3N00BWKOZ001');
INSERT INTO `a_member_user_dept` VALUES ('HY9XQN2L000WGH9Q01','HYPS1ROT007T1AG601');

--
-- 用户所属角色
--

DROP TABLE IF EXISTS `a_member_user_role`;
CREATE TABLE `a_member_user_role` (
  `user_id` CHAR(20) NOT NULL,
  `role` VARCHAR(80) NOT NULL,
  PRIMARY KEY (`user_id`,`role`),
  FOREIGN KEY (`user_id`) REFERENCES `a_member_user` (`id`) ON DELETE CASCADE
);

CREATE INDEX `IK_a_member_user_role_user` ON `a_member_user_role` (`user_id`);
CREATE INDEX `IK_a_member_user_role_role` ON `a_member_user_role` (`role`);

INSERT INTO `a_member_user_role` VALUES ('1','manage');
INSERT INTO `a_member_user_role` VALUES ('1','manage/global/note');
INSERT INTO `a_member_user_role` VALUES ('1','manage/global/file');
INSERT INTO `a_member_user_role` VALUES ('1','manage/member/user/search');
INSERT INTO `a_member_user_role` VALUES ('1','manage/member/user/create');
INSERT INTO `a_member_user_role` VALUES ('1','manage/member/user/update');
INSERT INTO `a_member_user_role` VALUES ('1','manage/member/user/delete');
INSERT INTO `a_member_user_role` VALUES ('1','manage/member/dept/search');
INSERT INTO `a_member_user_role` VALUES ('1','manage/member/dept/create');
INSERT INTO `a_member_user_role` VALUES ('1','manage/member/dept/update');
INSERT INTO `a_member_user_role` VALUES ('1','manage/member/dept/delete');
INSERT INTO `a_member_user_role` VALUES ('1','manage/matrix/form/search');
INSERT INTO `a_member_user_role` VALUES ('1','manage/matrix/form/create');
INSERT INTO `a_member_user_role` VALUES ('1','manage/matrix/form/update');
INSERT INTO `a_member_user_role` VALUES ('1','manage/matrix/form/delete');
INSERT INTO `a_member_user_role` VALUES ('1','manage/matrix/unit/search');
INSERT INTO `a_member_user_role` VALUES ('1','manage/matrix/unit/create');
INSERT INTO `a_member_user_role` VALUES ('1','manage/matrix/unit/update');
INSERT INTO `a_member_user_role` VALUES ('1','manage/matrix/unit/delete');

--
-- 用户关联账户
--

DROP TABLE IF EXISTS `a_member_user_link`;
CREATE TABLE `a_member_user_link` (
  `user_id` CHAR(20) NOT NULL,
  `link_id` CHAR(20) NOT NULL,
  PRIMARY KEY (`user_id`,`link_id`),
  FOREIGN KEY (`user_id`) REFERENCES `a_member_user` (`id`) ON DELETE CASCADE
);

--
-- 用户登录关联
--

DROP TABLE IF EXISTS `a_member_user_open`;
CREATE TABLE `a_member_user_open` (
  `user_id` CHAR(20) NOT NULL,
  `appid` VARCHAR(100) NOT NULL,
  `opnid` VARCHAR(100) NOT NULL,
  `ctime` UNSIGNED INT(10) DEFAULT NULL,
  PRIMARY KEY (`user_id`,`appid`),
  FOREIGN KEY (`user_id`) REFERENCES `a_member_user` (`id`) ON DELETE CASCADE
);

CREATE INDEX `IK_a_member_user_open_user` ON `a_member_user_open` (`user_id`);
CREATE INDEX `IK_a_member_user_open_appid` ON `a_member_user_open` (`appid`);
CREATE INDEX `IK_a_member_user_open_opnid` ON `a_member_user_open` (`opnid`);

--
-- 用户登录凭证
--

DROP TABLE IF EXISTS `a_member_user_sign`;
CREATE TABLE `a_member_user_sign` (
  `user_id` CHAR(20) NOT NULL,
  `appid` VARCHAR(100) NOT NULL,
  `sesid` VARCHAR(100) NOT NULL,
  `ctime` UNSIGNED INT(10) DEFAULT NULL,
  PRIMARY KEY (`user_id`,`appid`),
  FOREIGN KEY (`user_id`) REFERENCES `a_member_user` (`id`) ON DELETE CASCADE
);

CREATE INDEX `IK_a_member_user_sign_user` ON `a_member_user_sign` (`user_id`);
CREATE INDEX `IK_a_member_user_sign_appid` ON `a_member_user_sign` (`appid`);
CREATE INDEX `IK_a_member_user_sign_sesid` ON `a_member_user_sign` (`sesid`);
