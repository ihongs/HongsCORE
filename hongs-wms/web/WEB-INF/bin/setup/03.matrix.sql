-- DB: matrix

--
-- 单元
--

DROP TABLE IF EXISTS `a_matrix_unit`;
CREATE TABLE `a_matrix_unit` (
  `id` CHAR(16) NOT NULL,
  `pid` CHAR(16) DEFAULT NULL,
  `name` VARCHAR(200) NOT NULL,
  `icon` VARCHAR(100) DEFAULT NULL,
  `note` TEXT,
  `ctime` INTEGER(10) DEFAULT NULL,
  `mtime` INTEGER(10) DEFAULT NULL,
  `boost` INTEGER DEFAULT '0',
  `state` TINYINT DEFAULT '1',
  PRIMARY KEY (`id`),
  FOREIGN KEY (`pid`) REFERENCES `a_matrix_unit` (`id`) ON DELETE CASCADE
);

CREATE INDEX `IK_a_matrix_unit_unit` ON `a_matrix_unit` (`pid`);
CREATE INDEX `IK_a_matrix_unit_state` ON `a_matrix_unit` (`state`);
CREATE INDEX `IK_a_matrix_unit_ctime` ON `a_matrix_unit` (`ctime`);
CREATE INDEX `IK_a_matrix_unit_mtime` ON `a_matrix_unit` (`mtime`);
CREATE UNIQUE INDEX `UK_a_matrix_unit_name` ON `a_matrix_unit` (`name`,`pid`);

--
-- 表单
--

DROP TABLE IF EXISTS `a_matrix_form`;
CREATE TABLE `a_matrix_form` (
  `id` CHAR(16) NOT NULL,
  `unit_id` CHAR(16) NOT NULL,
  `name` VARCHAR(200) NOT NULL,
  `icon` VARCHAR(100) DEFAULT NULL,
  `note` TEXT,
  `conf` TEXT NOT NULL,
  `ctime` INTEGER(10) DEFAULT NULL,
  `mtime` INTEGER(10) DEFAULT NULL,
  `boost` INTEGER DEFAULT '0',
  `state` TINYINT DEFAULT '1',
  PRIMARY KEY (`id`),
  FOREIGN KEY (`unit_id`) REFERENCES `a_matrix_unit` (`id`) ON DELETE CASCADE
);

CREATE INDEX `IK_a_matrix_form_unit` ON `a_matrix_form` (`unit_id`);
CREATE INDEX `IK_a_matrix_form_state` ON `a_matrix_form` (`state`);
CREATE INDEX `IK_a_matrix_form_ctime` ON `a_matrix_form` (`ctime`);
CREATE INDEX `IK_a_matrix_form_mtime` ON `a_matrix_form` (`mtime`);
CREATE UNIQUE INDEX `UK_a_matrix_form_name` ON `a_matrix_form` (`name`,`unit_id`);

--
-- 数据
--

DROP TABLE IF EXISTS `a_matrix_data`;
CREATE TABLE `a_matrix_data` (
  `id` CHAR(16) NOT NULL,
  `form_id` CHAR(16) NOT NULL,
  `user_id` CHAR(16) NOT NULL,
  `name` VARCHAR(255) DEFAULT NULL,
  `memo` VARCHAR(255) DEFAULT NULL,
  `data` LONGTEXT NOT NULL,
  `ctime` INTEGER(10) NOT NULL,
  `etime` INTEGER(10) NOT NULL,
  `rtime` INTEGER(10) DEFAULT NULL, /* 从哪个时间点恢复 */
  `state` TINYINT DEFAULT '1',
  PRIMARY KEY (`id`,`form_id`,`etime`)
);

CREATE INDEX `IK_a_matrix_data_form` ON `a_matrix_data` (`form_id`);
CREATE INDEX `IK_a_matrix_data_user` ON `a_matrix_data` (`user_id`);
CREATE INDEX `IK_a_matrix_data_state` ON `a_matrix_data` (`state`);
CREATE INDEX `IK_a_matrix_data_ctime` ON `a_matrix_data` (`ctime`);
CREATE INDEX `IK_a_matrix_data_etime` ON `a_matrix_data` (`etime`);
CREATE INDEX `IK_a_matrix_data_rtime` ON `a_matrix_data` (`rtime`);

--
-- 预定内置关联资源
--

INSERT INTO a_matrix_unit (`id`, `pid`, `name`, `icon`, `note`, `ctime`, `mtime`, `boost`, `state`)
VALUES ('-', NULL, '内置', '', '', 0, 0, 0, 1);

INSERT INTO a_matrix_form (`id`, `unit_id`, `name`, `icon`, `note`, `conf`, `ctime`, `mtime`, `boost`, `state`)
VALUES ('user', '-', '用户', '', '', '{
    "form":"user",
    "conf":"master",
    "data-vk":"id",
    "data-tk":"name",
    "data-at":"centra/master/user/list",
    "data-al":"centra/master/user/pick.html"
}', 0, 0, 0, 3);

