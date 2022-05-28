--DB=normal

--
-- 简单键值存储
--

DROP TABLE IF EXISTS `a_normal_data`;
CREATE TABLE `a_normal_data` (
    `id` VARCHAR(95) NOT NULL,
    `data`  TEXT NOT NULL, /* 存 JSON, 换 BLOB 则改用 Java 序列化 */
    `xtime` INTEGER(10) DEFAULT 0,
    `mtime` INTEGER(10) DEFAULT 0,
    PRIMARY KEY (`id`)
);

CREATE INDEX `IK_a_normal_data_xtime` ON `a_normal_data` (`xtime`);
CREATE INDEX `IK_a_normal_data_mtime` ON `a_normal_data` (`mtime`);

--
-- 会话数据存储
--

DROP TABLE IF EXISTS `a_normal_sess`;
CREATE TABLE `a_normal_sess` (
    `id` VARCHAR(95) NOT NULL,
    `data`  TEXT NOT NULL, /* 存 JSON, 换 BLOB 则改用 Java 序列化 */
    `xtime` INTEGER(10) DEFAULT 0,
    `mtime` INTEGER(10) DEFAULT 0,
    PRIMARY KEY (`id`)
);

CREATE INDEX `IK_a_normal_sess_xtime` ON `a_normal_sess` (`xtime`);
CREATE INDEX `IK_a_normal_sess_mtime` ON `a_normal_sess` (`mtime`);
