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
  `level` TINYINT(2) DEFAULT '1', /* 1 用户, 2 群聊 */
  `state` TINYINT(2) DEFAULT '1', /* 1 正常 */
  `ctime` UNSIGNED INT(11) DEFAULT NULL,
  `mtime` UNSIGNED INT(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
);

CREATE INDEX `IK_a_mesage_room_level` ON `a_mesage_room` (`level`);
CREATE INDEX `IK_a_mesage_room_state` ON `a_mesage_room` (`state`);
CREATE INDEX `IK_a_mesage_room_ctime` ON `a_mesage_room` (`ctime`);
CREATE INDEX `IK_a_mesage_room_mtime` ON `a_mesage_room` (`mtime`);

--
-- 聊天组员
--

DROP TABLE IF EXISTS `a_mesage_room_mate`;
CREATE TABLE `a_mesage_room_mate` (
  `room_id` VARCHAR(200) NOT NULL,
  `user_id` VARCHAR(200) NOT NULL,
  `name` VARCHAR(200) DEFAULT NULL, /* user_id 对应的名字 */
  `level` TINYINT(2) DEFAULT '1', /* 1 普通, 2 管理 */
  `state` TINYINT(2) DEFAULT '1', /* 1 正常, 2 不通知, 3 黑名单 */
  `ctime` UNSIGNED INT(11) DEFAULT NULL,
  `mtime` UNSIGNED INT(11) DEFAULT NULL,
  PRIMARY KEY (`room_id`, `user_id`),
  FOREIGN KEY (`user_id`) REFERENCES `a_member_user` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`room_id`) REFERENCES `a_mesage_room` (`id`) ON DELETE CASCADE
);

CREATE INDEX `IK_a_mesage_room_mate_room_id` ON `a_mesage_room_mate` (`room_id`);
CREATE INDEX `IK_a_mesage_room_mate_user_id` ON `a_mesage_room_mate` (`user_id`);
CREATE INDEX `IK_a_mesage_room_mate_level` ON `a_mesage_room_mate` (`level`);
CREATE INDEX `IK_a_mesage_room_mate_state` ON `a_mesage_room_mate` (`state`);
CREATE INDEX `IK_a_mesage_room_mate_ctime` ON `a_mesage_room_mate` (`ctime`);
CREATE INDEX `IK_a_mesage_room_mate_mtime` ON `a_mesage_room_mate` (`mtime`);

--
-- 好友关系
--

DROP TABLE IF EXISTS `a_mesage_user_mate`;
CREATE TABLE `a_mesage_user_mate` (
  `mate_id` VARCHAR(200) NOT NULL,
  `user_id` VARCHAR(200) NOT NULL,
  `name` VARCHAR(200) DEFAULT NULL, /* user_id 对应的名字 */
  `level` TINYINT(2) DEFAULT '1', /* 1 临时, 2 好友 */
  `state` TINYINT(2) DEFAULT '1', /* 1 正常, 2 不通知, 3 黑名单 */
  `ctime` UNSIGNED INT(11) DEFAULT NULL,
  `mtime` UNSIGNED INT(11) DEFAULT NULL,
  PRIMARY KEY (`mate_id`, `user_id`),
  FOREIGN KEY (`user_id`) REFERENCES `a_member_user` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`mate_id`) REFERENCES `a_member_user` (`id`) ON DELETE CASCADE
);

CREATE INDEX `IK_a_mesage_user_mate_mate_id` ON `a_mesage_user_mate` (`mate_id`);
CREATE INDEX `IK_a_mesage_user_mate_user_id` ON `a_mesage_user_mate` (`user_id`);
CREATE INDEX `IK_a_mesage_user_mate_level` ON `a_mesage_user_mate` (`level`);
CREATE INDEX `IK_a_mesage_user_mate_state` ON `a_mesage_user_mate` (`state`);
CREATE INDEX `IK_a_mesage_user_mate_ctime` ON `a_mesage_user_mate` (`ctime`);
CREATE INDEX `IK_a_mesage_user_mate_mtime` ON `a_mesage_user_mate` (`mtime`);

--
-- 终端状态
--

DROP TABLE IF EXISTS `a_mesage_user_term`;
CREATE TABLE `a_mesage_user_term` (
  `user_id` VARCHAR(200) NOT NULL,
  `type` VARCHAR(50) NOT NULL,
  `code` VARCHAR(200) NOT NULL,
  `name` VARCHAR(200) NOT NULL,
  `data` TEXT,
  `state` TINYINT(2) DEFAULT '1', /* 1 正常, 2 在线 */
  `ctime` UNSIGNED INT(11) DEFAULT NULL,
  `mtime` UNSIGNED INT(11) DEFAULT NULL,
  FOREIGN KEY (`user_id`) REFERENCES `a_member_user` (`id`) ON DELETE CASCADE
);

CREATE INDEX `IK_a_mesage_user_term_user_id` ON `a_mesage_user_mate` (`user_id`);
CREATE INDEX `IK_a_mesage_user_term_state` ON `a_mesage_user_mate` (`state`);
CREATE INDEX `IK_a_mesage_user_term_ctime` ON `a_mesage_user_mate` (`ctime`);
CREATE INDEX `IK_a_mesage_user_term_mtime` ON `a_mesage_user_mate` (`mtime`);

--
-- 消息
--

DROP TABLE IF EXISTS `a_mesage_note`;
CREATE TABLE `a_mesage_note` (
  `id`  VARCHAR(200) NOT NULL,
  `user_id` VARCHAR(200) NOT NULL, /* 发送者 */
  `room_id` VARCHAR(200) NOT NULL,
  `kind` CHAR(20) DEFAULT 'text',
  `data` TEXT,
  `state` TINYINT(2) DEFAULT '1', /* 1 正常 */
  `ctime` UNSIGNED INT(11) DEFAULT NULL,
  `stime` UNSIGNED DECIMAL(15, 0) DEFAULT NULL, /* 发送的时间, 精确到毫秒 */
  PRIMARY KEY (`id`),
  FOREIGN KEY (`user_id`) REFERENCES `a_member_user` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`room_id`) REFERENCES `a_mesage_room` (`id`) ON DELETE CASCADE
);

CREATE INDEX `IK_a_mesage_note_user_id` ON `a_mesage_note` (`user_id`);
CREATE INDEX `IK_a_mesage_note_room_id` ON `a_mesage_note` (`room_id`);
CREATE INDEX `IK_a_mesage_note_state` ON `a_mesage_note` (`state`);
CREATE INDEX `IK_a_mesage_note_stime` ON `a_mesage_note` (`stime`);
CREATE INDEX `IK_a_mesage_note_ctime` ON `a_mesage_note` (`ctime`);

--
-- 消息统计
--

DROP TABLE IF EXISTS `a_mesage_note_stat`;
CREATE TABLE `a_mesage_note_stat` (
  `mate_id` VARCHAR(200) NOT NULL, /* 接收者 */
  `user_id` VARCHAR(200) NOT NULL, /* 发送者 */
  `room_id` VARCHAR(200) NOT NULL,
  `data` TEXT,
  `stati` INTEGER(8) DEFAULT '0', /* 未读数量统计 */
  `state` TINYINT(2) DEFAULT '1', /* 1 正常 */
  `mtime` UNSIGNED INT(11) DEFAULT NULL,
  PRIMARY KEY (`mate_id`),
  FOREIGN KEY (`mate_id`) REFERENCES `a_member_user` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`user_id`) REFERENCES `a_member_user` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`room_id`) REFERENCES `a_mesage_room` (`id`) ON DELETE CASCADE
);

CREATE INDEX `IK_a_mesage_note_stat_user_id` ON `a_mesage_note_stat` (`user_id`);
CREATE INDEX `IK_a_mesage_note_stat_room_id` ON `a_mesage_note_stat` (`room_id`);
CREATE INDEX `IK_a_mesage_note_stat_stati` ON `a_mesage_note_stat` (`stati`);
CREATE INDEX `IK_a_mesage_note_stat_state` ON `a_mesage_note_stat` (`state`);
CREATE INDEX `IK_a_mesage_note_stat_mtime` ON `a_mesage_note_stat` (`mtime`);
