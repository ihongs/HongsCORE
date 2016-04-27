-- DB: medium

--
-- 文章
--

DROP TABLE IF EXISTS `a_medium_article`;
CREATE TABLE `a_medium_article` (
  `id` char(20) NOT NULL,
  `user_id` char(20) DEFAULT NULL,
  `insp_id` char(20) DEFAULT NULL,
  `type` char(20) DEFAULT 'default',
  `temp` char(20) DEFAULT 'default',
  `kind` char(20) DEFAULT 'html', /* html,md */
  `name` varchar(255) NOT NULL,
  `href` varchar(255) DEFAULT NULL,
  `snap` varchar(255) DEFAULT NULL,
  `word` text DEFAULT NULL,
  `note` text DEFAULT NULL,
  `html` longtext DEFAULT NULL,
  `data` longtext DEFAULT NULL,
  `ctime` int(11) DEFAULT NULL,
  `mtime` int(11) DEFAULT NULL,
  `score` int(11) DEFAULT '0' ,
  `state` tinyint(1) DEFAULT '1',
  `stave` tinyint(1) DEFAULT '0', /* 1:干净模式, 2:禁止评论 */
  `count_browses` int(11) DEFAULT '0',
  `count_consent` int(11) DEFAULT '0',
  `count_dissent` int(11) DEFAULT '0',
  `count_comment` int(11) DEFAULT '0',
  `score_consent` int(11) DEFAULT '0',
  `means_consent` int(11) DEFAULT '0',
  PRIMARY KEY (`id`)
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
  `id` char(20) NOT NULL,
  `pid` char(20) DEFAULT NULL,
  `type` char(20) DEFAULT 'default',
  `temp` char(20) DEFAULT 'default',
  `name` varchar(200) NOT NULL,
  `word` varchar(1000) DEFAULT NULL,
  `note` varchar(1000) DEFAULT NULL,
  `mtime` int(11) DEFAULT NULL,
  `seria` int(11) DEFAULT '0' ,
  `state` tinyint(1) DEFAULT '1',
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
  `id` char(20) NOT NULL,
  `link_id` char(20) NOT NULL,
  `sect_id` char(20) NOT NULL,
  `link` char(20) DEFAULT 'article',
  `sect` char(20) DEFAULT 'section',
  `mtime` int(11) DEFAULT NULL,
  `seria` int(11) DEFAULT '0' ,
  PRIMARY KEY (`id`)
);

CREATE INDEX `a_medium_segment_sect` ON `a_medium_segment` (`sect_id`);
CREATE INDEX `a_medium_segment_link` ON `a_medium_segment` (`link_id`);
CREATE INDEX `a_medium_segment_type` ON `a_medium_segment` (`link`);
CREATE INDEX `a_medium_segment_type` ON `a_medium_segment` (`sect`);
CREATE INDEX `a_medium_segment_mtime` ON `a_medium_segment` (`mtime`);
CREATE INDEX `a_medium_segment_seria` ON `a_medium_segment` (`seria`);
CREATE UNIQUE INDEX `UK_a_medium_segment_link` ON `a_medium_segment` (`link_id`,`sect_id`,`link`,`sect`);

--
-- 评论
--

DROP TABLE IF EXISTS `a_medium_comment`;
CREATE TABLE `a_medium_comment` (
  `id` char(20) NOT NULL,
  `pid` char(20) DEFAULT NULL,
  `user_id` char(20) NOT NULL,
  `mate_id` char(20) NOT NULL,
  `link_id` char(20) NOT NULL,
  `link` char(20) NOT NULL,
  `note` text NOT NULL,
  `ctime` int(11) DEFAULT NULL,
  `mtime` int(11) DEFAULT NULL,
  `state` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`id`)
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
  `id` char(20) NOT NULL,
  `user_id` char(20) NOT NULL,
  `link_id` char(20) NOT NULL,
  `link` char(20) NOT NULL,
  `note` text DEFAULT NULL,
  `ctime` int(11) DEFAULT NULL,
  `mtime` int(11) DEFAULT NULL,
  `cause` tinyint(2) DEFAULT '0',
  `state` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`id`)
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
  `id` char(20) NOT NULL,
  `user_id` char(20) NOT NULL,
  `link_id` char(20) NOT NULL,
  `link` char(20) NOT NULL,
  `ctime` int(11) DEFAULT NULL,
  `mtime` int(11) DEFAULT NULL,
  `score` tinyint(1) DEFAULT '1',
  `state` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`id`)
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
  `id` char(20) NOT NULL,
  `sess_id` char(64) DEFAULT NULL,
  `user_id` char(20) DEFAULT NULL,
  `link_id` char(20) NOT NULL,
  `link` char(20) NOT NULL,
  `ctime` int(11) DEFAULT NULL,
  `state` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`id`)
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
  `id` char(20) NOT NULL,
  `link_id` char(20) NOT NULL,
  `link` char(20) NOT NULL,
  `name` varchar(200) NOT NULL,
  `note` varchar(200) DEFAULT NULL,
  `mtime` int(11) DEFAULT NULL,
  `score` int(11) DEFAULT '0' ,
  `state` tinyint(1) DEFAULT '1',
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
  `id` char(20) NOT NULL,
  `link_id` char(20) NOT NULL,
  `link` char(20) NOT NULL,
  `name` varchar(200) NOT NULL,
  `note` varchar(200) DEFAULT NULL,
  `mtime` int(11) DEFAULT NULL,
  `score` int(11) DEFAULT '0' ,
  `state` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`id`)
);

CREATE INDEX `a_medium_statics_link` ON `a_medium_statics` (`link_id`);
CREATE INDEX `a_medium_statics_type` ON `a_medium_statics` (`link`);
CREATE INDEX `a_medium_statics_name` ON `a_medium_statics` (`name`);
CREATE INDEX `a_medium_statics_mtime` ON `a_medium_statics` (`mtime`);
CREATE INDEX `a_medium_statics_score` ON `a_medium_species` (`score`);
CREATE INDEX `a_medium_statics_state` ON `a_medium_statics` (`state`);
CREATE UNIQUE INDEX `UK_a_medium_statics_link` ON `a_medium_statics` (`link_id`,`link`,`name`);
