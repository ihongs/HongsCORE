-- DB: member

--
-- 聊天室
--

DROP TABLE IF EXISTS `a_mesage_room`;
CREATE TABLE `a_mesage_room` (
  `id` CHAR(20) NOT NULL,
  `name` VARCHAR(200) DEFAULT NULL,
  `head` VARCHAR(100) DEFAULT NULL,
  `note` TEXT,
  `state` TINYINT(2) DEFAULT '1', /* 1 用户, 2 讨论组, 3 临时组 */
  `ctime` UNSIGNED INT(11) DEFAULT NULL,
  `mtime` UNSIGNED INT(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
);

CREATE INDEX `IK_a_mesage_room_state` ON `a_mesage_room` (`state`);
CREATE INDEX `IK_a_mesage_room_ctime` ON `a_mesage_room` (`ctime`);
CREATE INDEX `IK_a_mesage_room_mtime` ON `a_mesage_room` (`mtime`);

--
-- 聊天组员
--

DROP TABLE IF EXISTS `a_mesage_room_mate`;
CREATE TABLE `a_mesage_room_mate` (
  `rid` VARCHAR(200) NOT NULL,
  `uid` VARCHAR(200) NOT NULL,
  `name` VARCHAR(200) DEFAULT NULL, /* uid 对应的名字 */
  `state` TINYINT(2) DEFAULT '1', /* 1 普通, 2 管理员, 3 所有者 */
  `ctime` UNSIGNED INT(11) DEFAULT NULL,
  `mtime` UNSIGNED INT(11) DEFAULT NULL,
  PRIMARY KEY (`rid`, `uid`)
);

CREATE INDEX `IK_a_mesage_room_mate_rid` ON `a_mesage_room_mate` (`rid`);
CREATE INDEX `IK_a_mesage_room_mate_uid` ON `a_mesage_room_mate` (`uid`);
CREATE INDEX `IK_a_mesage_room_mate_state` ON `a_mesage_room_mate` (`state`);
CREATE INDEX `IK_a_mesage_room_mate_ctime` ON `a_mesage_room_mate` (`ctime`);
CREATE INDEX `IK_a_mesage_room_mate_mtime` ON `a_mesage_room_mate` (`mtime`);

--
-- 好友关系
--

DROP TABLE IF EXISTS `a_mesage_user_mate`;
CREATE TABLE `a_mesage_user_mate` (
  `mid` VARCHAR(200) NOT NULL,
  `uid` VARCHAR(200) NOT NULL,
  `name` VARCHAR(200) DEFAULT NULL, /* uid 对应的名字 */
  `state` TINYINT(2) DEFAULT '1', /* 1 普通, 2 不通知, 3 黑名单 */
  `ctime` UNSIGNED INT(11) DEFAULT NULL,
  `mtime` UNSIGNED INT(11) DEFAULT NULL,
  PRIMARY KEY (`mid`, `uid`)
);

CREATE INDEX `IK_a_mesage_user_mate_mid` ON `a_mesage_user_mate` (`mid`);
CREATE INDEX `IK_a_mesage_user_mate_uid` ON `a_mesage_user_mate` (`uid`);
CREATE INDEX `IK_a_mesage_user_mate_state` ON `a_mesage_user_mate` (`state`);
CREATE INDEX `IK_a_mesage_user_mate_ctime` ON `a_mesage_user_mate` (`ctime`);
CREATE INDEX `IK_a_mesage_user_mate_mtime` ON `a_mesage_user_mate` (`mtime`);

--
-- 消息
--

DROP TABLE IF EXISTS `a_mesage_note`;
CREATE TABLE `a_mesage_note` (
  `id`  VARCHAR(200) NOT NULL,
  `uid` VARCHAR(200) NOT NULL,
  `rid` VARCHAR(200) NOT NULL,
  `msg` TEXT,
  `stime` UNSIGNED INT(11) DEFAULT NULL,
  `state` TINYINT(2) DEFAULT '1', /* 1 未读, 2 已读 */
  PRIMARY KEY (`id`)
);

CREATE INDEX `IK_a_mesage_note_uid` ON `a_mesage_note` (`uid`);
CREATE INDEX `IK_a_mesage_note_rid` ON `a_mesage_note` (`rid`);
CREATE INDEX `IK_a_mesage_note_state` ON `a_mesage_note` (`state`);
CREATE INDEX `IK_a_mesage_note_ctime` ON `a_mesage_note` (`stime`);

--
-- 消息统计
--

DROP TABLE IF EXISTS `a_mesage_note_stat`;
CREATE TABLE `a_mesage_note_stat` (
  `uid` VARCHAR(200) NOT NULL,
  `unread_count` INTEGER(10) DEFAULT 0,
  `unread_final` TEXT,
  `state` TINYINT(2) DEFAULT '1', /* 1 无新, 2 有新 */
  `mtime` UNSIGNED INT(11) DEFAULT NULL,
  PRIMARY KEY (`uid`)
);

CREATE INDEX `IK_a_mesage_note_stat_uid` ON `a_mesage_note_stat` (`uid`);
CREATE INDEX `IK_a_mesage_note_stat_state` ON `a_mesage_note_stat` (`state`);
CREATE INDEX `IK_a_mesage_note_stat_mtime` ON `a_mesage_note_stat` (`mtime`);
