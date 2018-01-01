-- DB: medium

--
-- 评论
--

DROP TABLE IF EXISTS `a_medium_comment`;
CREATE TABLE `a_medium_comment` (
  `id` CHAR(20) NOT NULL,
  `prev_id` CHAR(20) DEFAULT NULL, /* 回复哪 */
  `mate_id` CHAR(20) DEFAULT NULL, /* 回复谁 */
  `user_id` CHAR(20) NOT NULL,
  `link_id` CHAR(20) NOT NULL,
  `link` CHAR(20) NOT NULL,
  `note` TEXT NOT NULL,
  `ctime` INTEGER(11) DEFAULT NULL,
  `mtime` INTEGER(11) DEFAULT NULL,
  `state` TINYINT(2) DEFAULT '1',
  PRIMARY KEY (`id`)
);

CREATE INDEX `IK_a_medium_comment_prev` ON `a_medium_comment` (`prev_id`);
CREATE INDEX `IK_a_medium_comment_mate` ON `a_medium_comment` (`mate_id`);
CREATE INDEX `IK_a_medium_comment_user` ON `a_medium_comment` (`user_id`);
CREATE INDEX `IK_a_medium_comment_link` ON `a_medium_comment` (`link_id`);
CREATE INDEX `IK_a_medium_comment_type` ON `a_medium_comment` (`link`);
CREATE INDEX `IK_a_medium_comment_ctime` ON `a_medium_comment` (`ctime`);
CREATE INDEX `IK_a_medium_comment_mtime` ON `a_medium_comment` (`mtime`);
CREATE INDEX `IK_a_medium_comment_state` ON `a_medium_comment` (`state`);

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
  `ctime` INTEGER(11) DEFAULT NULL,
  `mtime` INTEGER(11) DEFAULT NULL,
  `cause` TINYINT(2) DEFAULT '1',
  `state` TINYINT(2) DEFAULT '1',
  PRIMARY KEY (`id`)
);

CREATE INDEX `IK_a_medium_dissent_user` ON `a_medium_dissent` (`user_id`);
CREATE INDEX `IK_a_medium_dissent_link` ON `a_medium_dissent` (`link_id`);
CREATE INDEX `IK_a_medium_dissent_type` ON `a_medium_dissent` (`link`);
CREATE INDEX `IK_a_medium_dissent_ctime` ON `a_medium_dissent` (`ctime`);
CREATE INDEX `IK_a_medium_dissent_mtime` ON `a_medium_dissent` (`mtime`);
CREATE INDEX `IK_a_medium_dissent_cause` ON `a_medium_dissent` (`cause`);
CREATE INDEX `IK_a_medium_dissent_state` ON `a_medium_dissent` (`state`);
CREATE UNIQUE INDEX `UK_a_medium_dissent_link` ON `a_medium_dissent` (`link`,`link_id`,`user_id`);

--
-- 赞同
--

DROP TABLE IF EXISTS `a_medium_endorse`;
CREATE TABLE `a_medium_endorse` (
  `id` CHAR(20) NOT NULL,
  `user_id` CHAR(20) NOT NULL,
  `link_id` CHAR(20) NOT NULL,
  `link` CHAR(20) NOT NULL,
  `ctime` INTEGER(11) DEFAULT NULL,
  `mtime` INTEGER(11) DEFAULT NULL,
  `score` TINYINT(2) DEFAULT '1',
  `state` TINYINT(2) DEFAULT '1',
  PRIMARY KEY (`id`)
);

CREATE INDEX `IK_a_medium_endorse_user` ON `a_medium_endorse` (`user_id`);
CREATE INDEX `IK_a_medium_endorse_link` ON `a_medium_endorse` (`link_id`);
CREATE INDEX `IK_a_medium_endorse_type` ON `a_medium_endorse` (`link`);
CREATE INDEX `IK_a_medium_endorse_ctime` ON `a_medium_endorse` (`ctime`);
CREATE INDEX `IK_a_medium_endorse_mtime` ON `a_medium_endorse` (`mtime`);
CREATE INDEX `IK_a_medium_endorse_score` ON `a_medium_endorse` (`score`);
CREATE INDEX `IK_a_medium_endorse_state` ON `a_medium_endorse` (`state`);
CREATE UNIQUE INDEX `UK_a_medium_endorse_link` ON `a_medium_endorse` (`link`,`link_id`,`user_id`);

--
-- 阅读
--

DROP TABLE IF EXISTS `a_medium_impress`;
CREATE TABLE `a_medium_impress` (
  `id` CHAR(20) NOT NULL,
  `sess_id` CHAR(64) DEFAULT '',
  `user_id` CHAR(20) DEFAULT '',
  `link_id` CHAR(20) NOT NULL,
  `link` CHAR(20) NOT NULL,
  `ctime` INTEGER(11) DEFAULT NULL,
  `state` TINYINT(2) DEFAULT '1',
  PRIMARY KEY (`id`)
);

CREATE INDEX `IK_a_medium_impress_sess` ON `a_medium_impress` (`sess_id`);
CREATE INDEX `IK_a_medium_impress_user` ON `a_medium_impress` (`user_id`);
CREATE INDEX `IK_a_medium_impress_link` ON `a_medium_impress` (`link_id`);
CREATE INDEX `IK_a_medium_impress_type` ON `a_medium_impress` (`link`);
CREATE INDEX `IK_a_medium_impress_ctime` ON `a_medium_impress` (`ctime`);
CREATE INDEX `IK_a_medium_impress_state` ON `a_medium_impress` (`state`);
CREATE UNIQUE INDEX `UK_a_medium_impress_link` ON `a_medium_impress` (`link`,`link_id`,`user_id`,`sess_id`);

--
-- 统计
--

DROP TABLE IF EXISTS `a_medium_statist`;
CREATE TABLE `a_medium_statist` (
  `id` CHAR(20) NOT NULL,
  `link_id` CHAR(20) NOT NULL,
  `link` CHAR(20) NOT NULL,
  `comment_count` INTEGER(11) DEFAULT '0', /* 评论量 */
  `dissent_count` INTEGER(11) DEFAULT '0', /* 举报量 */
  `impress_count` INTEGER(11) DEFAULT '0', /* 阅读量 */
  `endorse_count` INTEGER(11) DEFAULT '0', /* 评价量 */
  `endorse_score` INTEGER(11) DEFAULT '0', /* 评分, endorse.score 总和 */
  `ctime` INTEGER(11) DEFAULT NULL,
  `mtime` INTEGER(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
);

CREATE INDEX `IK_a_medium_statist_link` ON `a_medium_statist` (`link_id`);
CREATE INDEX `IK_a_medium_statist_type` ON `a_medium_statist` (`link`);
CREATE INDEX `IK_a_medium_statist_ctime` ON `a_medium_statist` (`ctime`);
CREATE INDEX `IK_a_medium_statist_mtime` ON `a_medium_statist` (`mtime`);
CREATE INDEX `IK_a_medium_statist_comment_count` ON `a_medium_statist` (`comment_count`);
CREATE INDEX `IK_a_medium_statist_dissent_count` ON `a_medium_statist` (`dissent_count`);
CREATE INDEX `IK_a_medium_statist_impress_count` ON `a_medium_statist` (`impress_count`);
CREATE INDEX `IK_a_medium_statist_endorse_count` ON `a_medium_statist` (`endorse_count`);
CREATE INDEX `IK_a_medium_statist_endorse_score` ON `a_medium_statist` (`endorse_score`);
CREATE UNIQUE INDEX `UK_a_medium_statist_link` ON `a_medium_statist` (`link`,`link_id`);
