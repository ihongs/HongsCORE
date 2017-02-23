-- DB: common

--
-- 简单键值存储
--

DROP TABLE IF EXISTS `a_common_record`;
CREATE TABLE `a_common_record` (
    `id` VARCHAR(64) NOT NULL,
    `data` BLOB NOT NULL,
    `ctime` UNSIGNED INT(11) DEFAULT 0,
    `xtime` UNSIGNED INT(11) DEFAULT 0,
    PRIMARY KEY (`id`)
);
