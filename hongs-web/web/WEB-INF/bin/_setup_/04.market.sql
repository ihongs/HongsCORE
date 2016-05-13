-- DB: medium

--
-- 品类
--

DROP TABLE IF EXISTS `a_market_section`;
CREATE TABLE `a_market_section` (
  `id` char(20) NOT NULL,
  `conf` text DEFAULT NULL,
  `coll` text DEFAULT NULL,
  PRIMARY KEY (`id`)
);

--
-- 商品
--

DROP TABLE IF EXISTS `a_market_product`;
CREATE TABLE `a_market_product` (
  `id` char(20) NOT NULL,
  `conf` text DEFAULT NULL,
  `coll` text DEFAULT NULL,
  `price` float(6,2) DEFAULT 0.0,
  `prime` float(6,2) DEFAULT 0.0,
  PRIMARY KEY (`id`)
);

--
-- 账单
--

DROP TABLE IF EXISTS `a_market_package`;
CREATE TABLE `a_market_package` (
  `id` char(20) NOT NULL,
  `pid` char(20) DEFAULT NULL,
  `user_id` char(20) NOT NULL,
  `cost_type` tinyint(2) DEFAULT 0,
  `cost_time` int(11) DEFAULT NULL,
  `sent_time` int(11) DEFAULT NULL,
  `recv_time` int(11) DEFAULT NULL,
  `total` float(8,2) DEFAULT 0.0,
  `ctime` int(11) DEFAULT NULL,
  `mtime` int(11) DEFAULT NULL,
  `state` tinyint(1) DEFAULT 1, /* -1:挑选 1:就绪 2:付款 3:发出 4:送达 5:收到 */
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
  `id` char(20) NOT NULL,
  `package_id` char(20) NOT NULL,
  `code` char(20) NOT NULL,
  `note` varchar(255) NOT NULL,
  `ctime` int(11) DEFAULT NULL,
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
  `id` char(20) NOT NULL,
  `package_id` char(20) NOT NULL,
  `product_id` char(20) NOT NULL,
  `code` char(64) DEFAULT NULL,
  `coll` text DEFAULT NULL,
  `note` text DEFAULT NULL,
  `total` float(8,2) DEFAULT 0.0,
  `price` float(8,2) DEFAULT 0.0,
  `count` int(11) DEFAULT 1,
  `ctime` int(11) DEFAULT NULL,
  `mtime` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
);

CREATE INDEX `a_market_package_item_acco` ON `a_market_package_item` (`package_id`);
CREATE INDEX `a_market_package_item_prod` ON `a_market_package_item` (`product_id`);
CREATE INDEX `a_market_package_item_code` ON `a_market_package_item` (`code`);
CREATE UNIQUE INDEX `UK_a_market_package_item_code` ON `a_market_package_item` (`package_id`, `product_id`, `code`);
