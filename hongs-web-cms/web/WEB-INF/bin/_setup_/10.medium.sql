-- DB: medium

--
-- 文章
--

DROP TABLE IF EXISTS `a_medium_article`;
CREATE TABLE `a_medium_article` (
  `id` CHAR(20) NOT NULL,
  `user_id` CHAR(20) DEFAULT NULL,
  `insp_id` CHAR(20) DEFAULT NULL,
  `type` CHAR(20) DEFAULT 'default',
  `temp` CHAR(20) DEFAULT 'default',
  `kind` CHAR(20) DEFAULT 'html', /* html,md */
  `name` VARCHAR(255) NOT NULL,
  `href` VARCHAR(255) DEFAULT NULL,
  `snap` VARCHAR(255) DEFAULT NULL,
  `word` TEXT DEFAULT NULL,
  `note` TEXT DEFAULT NULL,
  `html` LONGTEXT DEFAULT NULL,
  `data` LONGTEXT DEFAULT NULL,
  `ctime` UNSIGNED INT(11) DEFAULT NULL,
  `mtime` UNSIGNED INT(11) DEFAULT NULL,
  `score` UNSIGNED INT(11) DEFAULT '0' ,
  `state` TINYINT(2) DEFAULT '1',
  `stave` TINYINT(2) DEFAULT '0', /* 1:干净模式, 2:禁止评论 */
  `count_browses` UNSIGNED INT(11) DEFAULT '0',
  `count_consent` UNSIGNED INT(11) DEFAULT '0',
  `count_dissent` UNSIGNED INT(11) DEFAULT '0',
  `count_comment` UNSIGNED INT(11) DEFAULT '0',
  `score_consent` UNSIGNED INT(11) DEFAULT '0',
  `means_consent` UNSIGNED INT(11) DEFAULT '0',
  PRIMARY KEY (`id`),
  FOREIGN KEY (`user_id`) REFERENCES `a_member_user` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`insp_id`) REFERENCES `a_member_user` (`id`) ON DELETE CASCADE
);

CREATE INDEX `a_medium_article_user` ON `a_medium_article` (`user_id`);
CREATE INDEX `a_medium_article_insp` ON `a_medium_article` (`insp_id`);
CREATE INDEX `a_medium_article_type` ON `a_medium_article` (`type`);
CREATE INDEX `a_medium_article_ctime` ON `a_medium_article` (`ctime`);
CREATE INDEX `a_medium_article_mtime` ON `a_medium_article` (`mtime`);
CREATE INDEX `a_medium_article_score` ON `a_medium_article` (`score`);
CREATE INDEX `a_medium_article_state` ON `a_medium_article` (`state`);

--
-- 分类
--

DROP TABLE IF EXISTS `a_medium_section`;
CREATE TABLE `a_medium_section` (
  `id` CHAR(20) NOT NULL,
  `pid` CHAR(20) DEFAULT NULL,
  `type` CHAR(20) DEFAULT 'default',
  `temp` CHAR(20) DEFAULT 'default',
  `name` VARCHAR(200) NOT NULL,
  `word` VARCHAR(1000) DEFAULT NULL,
  `note` VARCHAR(1000) DEFAULT NULL,
  `mtime` UNSIGNED INT(11) DEFAULT NULL,
  `seria` UNSIGNED INT(11) DEFAULT '0' ,
  `state` TINYINT(2) DEFAULT '1',
  PRIMARY KEY (`id`)
);

CREATE INDEX `a_medium_section_sect` ON `a_medium_section` (`pid`);
CREATE INDEX `a_medium_section_type` ON `a_medium_section` (`type`);
CREATE INDEX `a_medium_section_mtime` ON `a_medium_section` (`mtime`);
CREATE INDEX `a_medium_section_seria` ON `a_medium_section` (`seria`);
CREATE INDEX `a_medium_section_state` ON `a_medium_section` (`state`);
CREATE UNIQUE INDEX `UK_a_medium_section_link` ON `a_medium_section` (`pid`,`type`,`name`);

--
-- 关联
--

DROP TABLE IF EXISTS `a_medium_segment`;
CREATE TABLE `a_medium_segment` (
  `id` CHAR(20) NOT NULL,
  `link_id` CHAR(20) NOT NULL,
  `sect_id` CHAR(20) NOT NULL,
  `link` CHAR(20) DEFAULT 'article',
  `sect` CHAR(20) DEFAULT 'section',
  `mtime` UNSIGNED INT(11) DEFAULT NULL,
  `seria` UNSIGNED INT(11) DEFAULT '0' ,
  PRIMARY KEY (`id`)
);

CREATE INDEX `a_medium_segment_sect` ON `a_medium_segment` (`sect_id`);
CREATE INDEX `a_medium_segment_link` ON `a_medium_segment` (`link_id`);
CREATE INDEX `a_medium_segment_type` ON `a_medium_segment` (`sect`);
CREATE INDEX `a_medium_segment_item` ON `a_medium_segment` (`link`);
CREATE INDEX `a_medium_segment_mtime` ON `a_medium_segment` (`mtime`);
CREATE INDEX `a_medium_segment_seria` ON `a_medium_segment` (`seria`);
CREATE UNIQUE INDEX `UK_a_medium_segment_link` ON `a_medium_segment` (`link_id`,`sect_id`,`link`,`sect`);

--
-- 评论
--

DROP TABLE IF EXISTS `a_medium_comment`;
CREATE TABLE `a_medium_comment` (
  `id` CHAR(20) NOT NULL,
  `pid` CHAR(20) DEFAULT NULL,
  `user_id` CHAR(20) NOT NULL,
  `mate_id` CHAR(20) NOT NULL,
  `link_id` CHAR(20) NOT NULL,
  `link` CHAR(20) NOT NULL,
  `note` TEXT NOT NULL,
  `ctime` UNSIGNED INT(11) DEFAULT NULL,
  `mtime` UNSIGNED INT(11) DEFAULT NULL,
  `state` TINYINT(2) DEFAULT '1',
  PRIMARY KEY (`id`),
  FOREIGN KEY (`user_id`) REFERENCES `a_member_user` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`mate_id`) REFERENCES `a_member_user` (`id`) ON DELETE CASCADE
);

CREATE INDEX `a_medium_comment_prev` ON `a_medium_comment` (`pid`);
CREATE INDEX `a_medium_comment_user` ON `a_medium_comment` (`user_id`);
CREATE INDEX `a_medium_comment_mate` ON `a_medium_comment` (`mate_id`);
CREATE INDEX `a_medium_comment_link` ON `a_medium_comment` (`link_id`);
CREATE INDEX `a_medium_comment_type` ON `a_medium_comment` (`link`);
CREATE INDEX `a_medium_comment_ctime` ON `a_medium_comment` (`ctime`);
CREATE INDEX `a_medium_comment_mtime` ON `a_medium_comment` (`mtime`);
CREATE INDEX `a_medium_comment_state` ON `a_medium_comment` (`state`);

--
-- 举报
--

DROP TABLE IF EXISTS `a_medium_dissent`;
CREATE TABLE `a_medium_dissent` (
  `id` CHAR(20) NOT NULL,
  `user_id` CHAR(20) NOT NULL,
  `link_id` CHAR(20) NOT NULL,
  `link` CHAR(20) NOT NULL,
  `note` TEXT DEFAULT NULL,
  `ctime` UNSIGNED INT(11) DEFAULT NULL,
  `mtime` UNSIGNED INT(11) DEFAULT NULL,
  `cause` TINYINT(2) DEFAULT '0',
  `state` TINYINT(2) DEFAULT '1',
  PRIMARY KEY (`id`),
  FOREIGN KEY (`user_id`) REFERENCES `a_member_user` (`id`) ON DELETE CASCADE
);

CREATE INDEX `a_medium_dissent_user` ON `a_medium_dissent` (`user_id`);
CREATE INDEX `a_medium_dissent_link` ON `a_medium_dissent` (`link_id`);
CREATE INDEX `a_medium_dissent_type` ON `a_medium_dissent` (`link`);
CREATE INDEX `a_medium_dissent_ctime` ON `a_medium_dissent` (`ctime`);
CREATE INDEX `a_medium_dissent_mtime` ON `a_medium_dissent` (`mtime`);
CREATE INDEX `a_medium_dissent_cause` ON `a_medium_dissent` (`cause`);
CREATE INDEX `a_medium_dissent_state` ON `a_medium_dissent` (`state`);
CREATE UNIQUE INDEX `UK_a_medium_dissent_link` ON `a_medium_dissent` (`user_id`,`link_id`,`link`);

--
-- 赞同
--

DROP TABLE IF EXISTS `a_medium_consent`;
CREATE TABLE `a_medium_consent` (
  `id` CHAR(20) NOT NULL,
  `user_id` CHAR(20) NOT NULL,
  `link_id` CHAR(20) NOT NULL,
  `link` CHAR(20) NOT NULL,
  `ctime` UNSIGNED INT(11) DEFAULT NULL,
  `mtime` UNSIGNED INT(11) DEFAULT NULL,
  `score` TINYINT(2) DEFAULT '1',
  `state` TINYINT(2) DEFAULT '1',
  PRIMARY KEY (`id`),
  FOREIGN KEY (`user_id`) REFERENCES `a_member_user` (`id`) ON DELETE CASCADE
);

CREATE INDEX `a_medium_consent_user` ON `a_medium_consent` (`user_id`);
CREATE INDEX `a_medium_consent_link` ON `a_medium_consent` (`link_id`);
CREATE INDEX `a_medium_consent_type` ON `a_medium_consent` (`link`);
CREATE INDEX `a_medium_consent_ctime` ON `a_medium_consent` (`ctime`);
CREATE INDEX `a_medium_consent_mtime` ON `a_medium_consent` (`mtime`);
CREATE INDEX `a_medium_consent_score` ON `a_medium_consent` (`score`);
CREATE INDEX `a_medium_consent_state` ON `a_medium_consent` (`state`);
CREATE UNIQUE INDEX `UK_a_medium_consent_link` ON `a_medium_consent` (`user_id`,`link_id`,`link`);

--
-- 浏览
--

DROP TABLE IF EXISTS `a_medium_browses`;
CREATE TABLE `a_medium_browses` (
  `id` CHAR(20) NOT NULL,
  `sess_id` CHAR(64) DEFAULT NULL,
  `user_id` CHAR(20) DEFAULT NULL,
  `link_id` CHAR(20) NOT NULL,
  `link` CHAR(20) NOT NULL,
  `ctime` UNSIGNED INT(11) DEFAULT NULL,
  `state` TINYINT(2) DEFAULT '1',
  PRIMARY KEY (`id`),
  FOREIGN KEY (`user_id`) REFERENCES `a_member_user` (`id`) ON DELETE CASCADE
);

CREATE INDEX `a_medium_browses_sess` ON `a_medium_browses` (`sess_id`);
CREATE INDEX `a_medium_browses_user` ON `a_medium_browses` (`user_id`);
CREATE INDEX `a_medium_browses_link` ON `a_medium_browses` (`link_id`);
CREATE INDEX `a_medium_browses_type` ON `a_medium_browses` (`link`);
CREATE INDEX `a_medium_browses_ctime` ON `a_medium_browses` (`ctime`);
CREATE INDEX `a_medium_browses_state` ON `a_medium_browses` (`state`);
CREATE UNIQUE INDEX `UK_a_medium_browses_link` ON `a_medium_browses` (`sess_id`, `user_id`,`link_id`,`link`);

--
-- 标记
--

DROP TABLE IF EXISTS `a_medium_species`;
CREATE TABLE `a_medium_species` (
  `id` CHAR(20) NOT NULL,
  `link_id` CHAR(20) NOT NULL,
  `link` CHAR(20) NOT NULL,
  `name` VARCHAR(200) NOT NULL,
  `note` VARCHAR(200) DEFAULT NULL,
  `mtime` UNSIGNED INT(11) DEFAULT NULL,
  `score` UNSIGNED INT(11) DEFAULT '0' ,
  `state` TINYINT(2) DEFAULT '1',
  PRIMARY KEY (`id`)
);

CREATE INDEX `a_medium_species_link` ON `a_medium_species` (`link_id`);
CREATE INDEX `a_medium_species_type` ON `a_medium_species` (`link`);
CREATE INDEX `a_medium_species_name` ON `a_medium_species` (`name`);
CREATE INDEX `a_medium_species_mtime` ON `a_medium_species` (`mtime`);
CREATE INDEX `a_medium_species_score` ON `a_medium_species` (`score`);
CREATE INDEX `a_medium_species_state` ON `a_medium_species` (`state`);
CREATE UNIQUE INDEX `UK_a_medium_species_link` ON `a_medium_species` (`link_id`,`link`,`name`);

--
-- 存储
--

DROP TABLE IF EXISTS `a_medium_statics`;
CREATE TABLE `a_medium_statics` (
  `id` CHAR(20) NOT NULL,
  `link_id` CHAR(20) NOT NULL,
  `link` CHAR(20) NOT NULL,
  `name` VARCHAR(200) NOT NULL,
  `note` VARCHAR(200) DEFAULT NULL,
  `mtime` UNSIGNED INT(11) DEFAULT NULL,
  `score` UNSIGNED INT(11) DEFAULT '0' ,
  `state` TINYINT(2) DEFAULT '1',
  PRIMARY KEY (`id`)
);

CREATE INDEX `a_medium_statics_link` ON `a_medium_statics` (`link_id`);
CREATE INDEX `a_medium_statics_type` ON `a_medium_statics` (`link`);
CREATE INDEX `a_medium_statics_name` ON `a_medium_statics` (`name`);
CREATE INDEX `a_medium_statics_mtime` ON `a_medium_statics` (`mtime`);
CREATE INDEX `a_medium_statics_score` ON `a_medium_species` (`score`);
CREATE INDEX `a_medium_statics_state` ON `a_medium_statics` (`state`);
CREATE UNIQUE INDEX `UK_a_medium_statics_link` ON `a_medium_statics` (`link_id`,`link`,`name`);
