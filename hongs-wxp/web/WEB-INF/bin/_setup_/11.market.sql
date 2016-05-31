-- DB: medium

--
-- 品类
--

DROP TABLE IF EXISTS `a_market_section`;
CREATE TABLE `a_market_section` (
  `id` CHAR(20) NOT NULL,
  `conf` TEXT DEFAULT NULL,
  `coll` TEXT DEFAULT NULL,
  PRIMARY KEY (`id`)
);

--
-- 商品
--

DROP TABLE IF EXISTS `a_market_product`;
CREATE TABLE `a_market_product` (
  `id` CHAR(20) NOT NULL,
  `conf` TEXT DEFAULT NULL,
  `coll` TEXT DEFAULT NULL,
  `price` float(6,2) DEFAULT 0.0,
  `prime` float(6,2) DEFAULT 0.0,
  PRIMARY KEY (`id`)
);

--
-- 账单
--

DROP TABLE IF EXISTS `a_market_package`;
CREATE TABLE `a_market_package` (
  `id` CHAR(20) NOT NULL,
  `pid` CHAR(20) DEFAULT NULL,
  `user_id` CHAR(20) NOT NULL,
  `cost_type` TINYINT(2) DEFAULT 0,
  `cost_time` UNSIGNED INT(11) DEFAULT NULL,
  `sent_time` UNSIGNED INT(11) DEFAULT NULL,
  `recv_time` UNSIGNED INT(11) DEFAULT NULL,
  `total` UNSIGNED FLOAT(8, 2) DEFAULT 0.0,
  `ctime` UNSIGNED INT(11) DEFAULT NULL,
  `mtime` UNSIGNED INT(11) DEFAULT NULL,
  `state` TINYINT(2) DEFAULT 1, /* -1:挑选 1:就绪 2:付款 3:发出 4:送达 5:收到 */
  PRIMARY KEY (`id`)
);

CREATE INDEX `a_market_package_pare` ON `a_market_package` (`pid`);
CREATE INDEX `a_market_package_user` ON `a_market_package` (`user_id`);
CREATE INDEX `a_market_package_cost` ON `a_market_package` (`cost_type`);

--
-- 账单流程
--

DROP TABLE IF EXISTS `a_market_package_flow`;
CREATE TABLE `a_market_package_flow` (
  `id` CHAR(20) NOT NULL,
  `package_id` CHAR(20) NOT NULL,
  `code` CHAR(20) NOT NULL,
  `note` VARCHAR(255) NOT NULL,
  `ctime` UNSIGNED INT(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
);

CREATE INDEX `a_market_package_flow_acco` ON `a_market_package_flow` (`package_id`);
CREATE INDEX `a_market_package_flow_code` ON `a_market_package_flow` (`code`);
CREATE UNIQUE INDEX `UK_a_market_package_flow_code` ON `a_market_package_flow` (`package_id`, `code`);

--
-- 账单条目
--

DROP TABLE IF EXISTS `a_market_package_item`;
CREATE TABLE `a_market_package_item` (
  `id` CHAR(20) NOT NULL,
  `package_id` CHAR(20) NOT NULL,
  `product_id` CHAR(20) NOT NULL,
  `code` CHAR(64) DEFAULT NULL,
  `coll` TEXT DEFAULT NULL,
  `note` TEXT DEFAULT NULL,
  `total` UNSIGNED FLOAT(8, 2) DEFAULT 0.0,
  `price` UNSIGNED FLOAT(8, 2) DEFAULT 0.0,
  `count` UNSIGNED INT(11) DEFAULT 1,
  `ctime` UNSIGNED INT(11) DEFAULT NULL,
  `mtime` UNSIGNED INT(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
);

CREATE INDEX `a_market_package_item_acco` ON `a_market_package_item` (`package_id`);
CREATE INDEX `a_market_package_item_prod` ON `a_market_package_item` (`product_id`);
CREATE INDEX `a_market_package_item_code` ON `a_market_package_item` (`code`);
CREATE UNIQUE INDEX `UK_a_market_package_item_code` ON `a_market_package_item` (`package_id`, `product_id`, `code`);
