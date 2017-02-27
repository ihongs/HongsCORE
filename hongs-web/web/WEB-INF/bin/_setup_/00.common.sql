-- DB: common

--
-- 简单键值存储
--

DROP TABLE IF EXISTS `a_common_record`;
CREATE TABLE `a_common_record` (
    `id` VARCHAR(64) NOT NULL,
    `data`  BLOB NOT NULL,
    `xtime` UNSIGNED INT(11) DEFAULT 0,
    `mtime` UNSIGNED INT(11) DEFAULT 0,
    PRIMARY KEY (`id`)
);

CREATE INDEX `IK_a_common_record_xtime` ON `a_member_dept` (`xtime`);
CREATE INDEX `IK_a_common_record_mtime` ON `a_member_dept` (`mtime`);
