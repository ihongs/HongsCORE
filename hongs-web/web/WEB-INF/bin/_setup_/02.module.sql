-- DB: module

--
-- 单元
--

DROP TABLE IF EXISTS `a_module_unit`;
CREATE TABLE `a_module_unit` (
  `id` char(20) NOT NULL,
  `pid` char(20) DEFAULT NULL,
  `name` varchar(200) NOT NULL,
  `note` text,
  `snum` int(11) DEFAULT '0',
  `ctime` int(11) DEFAULT NULL,
  `mtime` int(11) DEFAULT NULL,
  `boost` int(11) DEFAULT NULL,
  `state` tinyint(4) DEFAULT '1',
  PRIMARY KEY (`id`)
);

CREATE INDEX `IK_a_module_unit_unit` ON `a_member_dept` (`pid`);
CREATE INDEX `IK_a_module_unit_state` ON `a_module_unit` (`state`);
CREATE INDEX `IK_a_module_unit_ctime` ON `a_module_unit` (`ctime`);
CREATE INDEX `IK_a_module_unit_mtime` ON `a_module_unit` (`mtime`);
CREATE UNIQUE INDEX `UK_a_module_unit_name` ON `a_module_unit` (`name`,`pid`);

--
-- 表单
--

DROP TABLE IF EXISTS `a_module_form`;
CREATE TABLE `a_module_form` (
  `id` char(20) NOT NULL,
  `unit_id` char(20) NOT NULL,
  `name` varchar(200) NOT NULL,
  `note` text,
  `conf` text NOT NULL,
  `snum` int(11) DEFAULT '0',
  `ctime` int(11) DEFAULT NULL,
  `mtime` int(11) DEFAULT NULL,
  `boost` int(11) DEFAULT NULL,
  `state` tinyint(4) DEFAULT '1',
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
  `id` char(20) NOT NULL,
  `form_id` char(20) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `data` longtext NOT NULL,
  `ctime` bigint(15) DEFAULT NULL,
  `etime` bigint(15) DEFAULT NULL,
  `rtime` bigint(15) DEFAULT NULL,
  `state` tinyint(4) DEFAULT '1',
  PRIMARY KEY (`id`,`etime`),
  FOREIGN KEY (`form_id`) REFERENCES `a_module_form` (`id`) ON DELETE CASCADE
);

CREATE INDEX `IK_a_module_data_state` ON `a_module_data` (`state`);
CREATE INDEX `IK_a_module_data_ctime` ON `a_module_data` (`ctime`);
CREATE INDEX `IK_a_module_data_etime` ON `a_module_data` (`etime`);
