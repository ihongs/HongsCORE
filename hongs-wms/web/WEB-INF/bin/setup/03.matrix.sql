--DB=matrix

--
-- 单元
--

DROP TABLE IF EXISTS `a_matrix_furl`;
CREATE TABLE `a_matrix_furl` (
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
  FOREIGN KEY (`pid`) REFERENCES `a_matrix_furl` (`id`) ON DELETE CASCADE
);

CREATE INDEX `IK_a_matrix_furl_pid` ON `a_matrix_furl` (`pid`);
CREATE INDEX `IK_a_matrix_furl_state` ON `a_matrix_furl` (`state`);
CREATE INDEX `IK_a_matrix_furl_boost` ON `a_matrix_furl` (`boost` DESC);
CREATE INDEX `IK_a_matrix_furl_ctime` ON `a_matrix_furl` (`ctime` DESC);
CREATE INDEX `IK_a_matrix_furl_mtime` ON `a_matrix_furl` (`mtime` DESC);
CREATE UNIQUE INDEX `UK_a_matrix_furl_name` ON `a_matrix_furl` (`name`,`pid`);

--
-- 表单
--

DROP TABLE IF EXISTS `a_matrix_form`;
CREATE TABLE `a_matrix_form` (
  `id` CHAR(16) NOT NULL,
  `furl_id` CHAR(16) NOT NULL,
  `name` VARCHAR(200) NOT NULL,
  `icon` VARCHAR(100) DEFAULT NULL,
  `note` TEXT,
  `conf` TEXT NOT NULL,
  `ctime` INTEGER(10) DEFAULT NULL,
  `mtime` INTEGER(10) DEFAULT NULL,
  `boost` INTEGER DEFAULT '0',
  `state` TINYINT DEFAULT '1',
  PRIMARY KEY (`id`),
  FOREIGN KEY (`furl_id`) REFERENCES `a_matrix_furl` (`id`) ON DELETE CASCADE
);

CREATE INDEX `IK_a_matrix_form_furl` ON `a_matrix_form` (`furl_id`);
CREATE INDEX `IK_a_matrix_form_state` ON `a_matrix_form` (`state`);
CREATE INDEX `IK_a_matrix_form_boost` ON `a_matrix_form` (`boost` DESC);
CREATE INDEX `IK_a_matrix_form_ctime` ON `a_matrix_form` (`ctime` DESC);
CREATE INDEX `IK_a_matrix_form_mtime` ON `a_matrix_form` (`mtime` DESC);
CREATE UNIQUE INDEX `UK_a_matrix_form_name` ON `a_matrix_form` (`name`,`furl_id`);

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
  `meno` VARCHAR(100) DEFAULT NULL,
  `data` LONGTEXT NOT NULL,
  `ctime` INTEGER(10) NOT NULL,
  `etime` INTEGER(10) NOT NULL,
  `rtime` INTEGER(10) DEFAULT NULL, /* 从哪个时间点恢复 */
  `state` TINYINT DEFAULT '1',
  PRIMARY KEY (`id`,`form_id`,`ctime`)
);

CREATE INDEX `IK_a_matrix_data_id` ON `a_matrix_data` (`id`);
CREATE INDEX `IK_a_matrix_data_form` ON `a_matrix_data` (`form_id`);
CREATE INDEX `IK_a_matrix_data_user` ON `a_matrix_data` (`user_id`);
CREATE INDEX `IK_a_matrix_data_meno` ON `a_matrix_data` (`meno`);
CREATE INDEX `IK_a_matrix_data_state` ON `a_matrix_data` (`state`);
CREATE INDEX `IK_a_matrix_data_etime` ON `a_matrix_data` (`etime`);
CREATE INDEX `IK_a_matrix_data_rtime` ON `a_matrix_data` (`rtime`);
CREATE INDEX `IK_a_matrix_data_ctime` ON `a_matrix_data` (`ctime` DESC);
CREATE UNIQUE INDEX `UK_a_matrix_data_uk` ON `a_matrix_data` (`id`,`form_id`,`etime`);

--
-- 字段
--

DROP TABLE IF EXISTS `a_matrix_feed`;
CREATE TABLE `a_matrix_feed` (
  `fn` CHAR(16) NOT NULL,
  `ft` CHAR(16) NOT NULL
);

CREATE INDEX `IK_a_matrix_feed_fn` ON `a_matrix_feed` (`fn`);
CREATE INDEX `IK_a_matrix_feed_ft` ON `a_matrix_feed` (`ft`);
CREATE UNIQUE INDEX `UK_a_matrix_feed_fx` ON `a_matrix_feed` (`fn`,`ft`);

--
-- 预定内置关联资源
--

INSERT INTO a_matrix_furl (`id`, `pid`, `name`, `icon`, `note`, `ctime`, `mtime`, `boost`, `state`)
VALUES ('-', NULL, 'Base', '', '', 0, 0, 0, 1),
       ('0', NULL, 'Root', '', '', 0, 0, 0, 1);

INSERT INTO a_matrix_form (`id`, `furl_id`, `name`, `icon`, `note`, `conf`, `ctime`, `mtime`, `boost`, `state`)
VALUES ('user', '-', '用户', '', '', '{
    "form":"user",
    "conf":"master",
    "data-vk":"id",
    "data-tk":"name",
    "data-at":"centra/master/user/list",
    "data-al":"centra/master/user/pick.html",
    "data-rl":""
}', 0, 0, 0, 6);

INSERT INTO a_matrix_feed (`fn`, `ft`)
VALUES ('url'   , 'url'),
       ('url1'  , 'url'),
       ('url2'  , 'url'),
       ('url3'  , 'url'),
       ('url4'  , 'url'),
       ('url5'  , 'url'),
       ('url6'  , 'url'),
       ('url7'  , 'url'),
       ('url8'  , 'url'),
       ('url9'  , 'url'),
       ('phone' , 'tel'),
       ('phone1', 'tel'),
       ('phone2', 'tel'),
       ('phone3', 'tel'),
       ('phone4', 'tel'),
       ('phone5', 'tel'),
       ('phone6', 'tel'),
       ('phone7', 'tel'),
       ('phone8', 'tel'),
       ('phone9', 'tel'),
       ('phone' , 'sms'),
       ('phone1', 'sms'),
       ('phone2', 'sms'),
       ('phone3', 'sms'),
       ('phone4', 'sms'),
       ('phone5', 'sms'),
       ('phone6', 'sms'),
       ('phone7', 'sms'),
       ('phone8', 'sms'),
       ('phone9', 'sms'),
       ('email' , 'email'),
       ('email1', 'email'),
       ('email2', 'email'),
       ('email3', 'email'),
       ('email4', 'email'),
       ('email5', 'email'),
       ('email6', 'email'),
       ('email7', 'email'),
       ('email8', 'email'),
       ('email9', 'email'),
       ('time'  , 'date'),
       ('time1' , 'date'),
       ('time2' , 'date'),
       ('time3' , 'date'),
       ('time4' , 'date'),
       ('time5' , 'date'),
       ('time6' , 'date'),
       ('time7' , 'date'),
       ('time8' , 'date'),
       ('time9' , 'date'),
       ('file'  , 'file'),
       ('file1' , 'file'),
       ('file2' , 'file'),
       ('file3' , 'file'),
       ('file4' , 'file'),
       ('file5' , 'file'),
       ('file6' , 'file'),
       ('file7' , 'file'),
       ('file8' , 'file'),
       ('file9' , 'file'),
       ('text'  , 'text'),
       ('text1' , 'text'),
       ('text2' , 'text'),
       ('text3' , 'text'),
       ('text4' , 'text'),
       ('text5' , 'text'),
       ('text6' , 'text'),
       ('text7' , 'text'),
       ('text8' , 'text'),
       ('text9' , 'text'),
       ('text'  , 'textarea'),
       ('text1' , 'textarea'),
       ('text2' , 'textarea'),
       ('text3' , 'textarea'),
       ('text4' , 'textarea'),
       ('text5' , 'textarea'),
       ('text6' , 'textarea'),
       ('text7' , 'textarea'),
       ('text8' , 'textarea'),
       ('text9' , 'textarea'),
       ('text'  , 'textview'),
       ('text1' , 'textview'),
       ('text2' , 'textview'),
       ('text3' , 'textview'),
       ('text4' , 'textview'),
       ('text5' , 'textview'),
       ('text6' , 'textview'),
       ('text7' , 'textview'),
       ('text8' , 'textview'),
       ('text9' , 'textview');
