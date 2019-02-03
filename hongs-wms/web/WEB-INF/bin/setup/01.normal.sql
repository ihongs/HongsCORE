-- DB: normal

--
-- 简单键值存储
--

DROP TABLE IF EXISTS `a_normal_record`;
CREATE TABLE `a_normal_record` (
    `id` VARCHAR(95) NOT NULL,
    `data`  BLOB NOT NULL,
    `xtime` INTEGER(10) UNSIGNED DEFAULT 0,
    `mtime` INTEGER(10) UNSIGNED DEFAULT 0,
    PRIMARY KEY (`id`)
);

CREATE INDEX `IK_a_normal_record_xtime` ON `a_normal_record` (`xtime`);
CREATE INDEX `IK_a_normal_record_mtime` ON `a_normal_record` (`mtime`);

--
-- 会话数据存储
--

DROP TABLE IF EXISTS `a_normal_sesion`;
CREATE TABLE `a_normal_sesion` (
    `id` VARCHAR(95) NOT NULL,
    `data`  BLOB NOT NULL,
    `xtime` INTEGER(10) UNSIGNED DEFAULT 0,
    `mtime` INTEGER(10) UNSIGNED DEFAULT 0,
    PRIMARY KEY (`id`)
);

CREATE INDEX `IK_a_normal_sesion_xtime` ON `a_normal_sesion` (`xtime`);
CREATE INDEX `IK_a_normal_sesion_mtime` ON `a_normal_sesion` (`mtime`);
