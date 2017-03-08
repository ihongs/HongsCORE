-- DB: module

--
-- 单元
--

DROP TABLE IF EXISTS `a_module_unit`;
CREATE TABLE `a_module_unit` (
  `id` CHAR(20) NOT NULL,
  `pid` CHAR(20) DEFAULT NULL,
  `name` VARCHAR(200) NOT NULL,
  `icon` VARCHAR(100) DEFAULT NULL,
  `note` TEXT,
  `snum` UNSIGNED INT(10) DEFAULT '0',
  `ctime` UNSIGNED INT(10) DEFAULT NULL,
  `mtime` UNSIGNED INT(10) DEFAULT NULL,
  `boost` UNSIGNED INT(10) DEFAULT NULL,
  `state` TINYINT(2) DEFAULT '1',
  PRIMARY KEY (`id`)
);

CREATE INDEX `IK_a_module_unit_unit` ON `a_module_unit` (`pid`);
CREATE INDEX `IK_a_module_unit_state` ON `a_module_unit` (`state`);
CREATE INDEX `IK_a_module_unit_ctime` ON `a_module_unit` (`ctime`);
CREATE INDEX `IK_a_module_unit_mtime` ON `a_module_unit` (`mtime`);
CREATE UNIQUE INDEX `UK_a_module_unit_name` ON `a_module_unit` (`name`,`pid`);

--
-- 表单
--

DROP TABLE IF EXISTS `a_module_form`;
CREATE TABLE `a_module_form` (
  `id` CHAR(20) NOT NULL,
  `unit_id` CHAR(20) NOT NULL,
  `name` VARCHAR(200) NOT NULL,
  `note` TEXT,
  `conf` TEXT NOT NULL,
  `snum` UNSIGNED INT(10) DEFAULT '0',
  `ctime` UNSIGNED INT(10) DEFAULT NULL,
  `mtime` UNSIGNED INT(10) DEFAULT NULL,
  `boost` UNSIGNED INT(10) DEFAULT NULL,
  `state` TINYINT(2) DEFAULT '1',
  PRIMARY KEY (`id`),
  FOREIGN KEY (`unit_id`) REFERENCES `a_module_unit` (`id`) ON DELETE CASCADE
);

CREATE INDEX `IK_a_module_form_state` ON `a_module_form` (`state`);
CREATE INDEX `IK_a_module_form_ctime` ON `a_module_form` (`ctime`);
CREATE INDEX `IK_a_module_form_mtime` ON `a_module_form` (`mtime`);
CREATE UNIQUE INDEX `UK_a_module_form_name` ON `a_module_form` (`name`,`unit_id`);

--
-- 数据
--

DROP TABLE IF EXISTS `a_module_data`;
CREATE TABLE `a_module_data` (
  `id` CHAR(20) NOT NULL,
  `form_id` CHAR(20) NOT NULL,
  `user_id` CHAR(20) NOT NULL,
  `name` VARCHAR(255) DEFAULT NULL,
  `note` VARCHAR(255) DEFAULT NULL,
  `data` LONGTEXT NOT NULL,
  `ctime` UNSIGNED INT(10) DEFAULT NULL,
  `etime` UNSIGNED INT(10) DEFAULT NULL,
  `rtime` UNSIGNED INT(10) DEFAULT NULL,
  `state` TINYINT(2) DEFAULT '1',
  PRIMARY KEY (`id`,`form_id`,`etime`),
  FOREIGN KEY (`form_id`) REFERENCES `a_module_form` (`id`) ON DELETE CASCADE
);

CREATE INDEX `IK_a_module_data_state` ON `a_module_data` (`state`);
CREATE INDEX `IK_a_module_data_ctime` ON `a_module_data` (`ctime`);
CREATE INDEX `IK_a_module_data_etime` ON `a_module_data` (`etime`);
