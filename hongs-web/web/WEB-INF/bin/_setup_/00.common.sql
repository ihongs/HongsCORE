-- DB: common

--
-- 简单键值存储
--

CREATE TABLE IF NOT EXISTS `a_common_record` (
    `id` VARCHAR(64) NOT NULL,
    `data` BLOB NOT NULL,
    `ctime` INT (11) UNSIGNED DEFAULT 0,
    `xtime` INT (11) UNSIGNED DEFAULT 0,
    PRIMARY KEY (`id`)
)
