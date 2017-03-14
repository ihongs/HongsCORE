-- DB: common

--
-- 简单键值存储
--

DROP TABLE IF EXISTS `a_common_record`;
CREATE TABLE `a_common_record` (
    `id` VARCHAR(96) NOT NULL,
    `data`  BLOB NOT NULL,
    `xtime` UNSIGNED INT(11) DEFAULT 0,
    `mtime` UNSIGNED INT(11) DEFAULT 0,
    PRIMARY KEY (`id`)
);

CREATE INDEX `IK_a_common_record_xtime` ON `a_common_record` (`xtime`);
CREATE INDEX `IK_a_common_record_mtime` ON `a_common_record` (`mtime`);

--
-- 会话数据存储
--

DROP TABLE IF EXISTS `a_common_sesion`;
CREATE TABLE `a_common_sesion` (
    `id` VARCHAR(96) NOT NULL,
    `data`  BLOB NOT NULL,
    `xtime` UNSIGNED INT(11) DEFAULT 0,
    `mtime` UNSIGNED INT(11) DEFAULT 0,
    PRIMARY KEY (`id`)
);

CREATE INDEX `IK_a_common_sesion_xtime` ON `a_common_sesion` (`xtime`);
CREATE INDEX `IK_a_common_sesion_mtime` ON `a_common_sesion` (`mtime`);
