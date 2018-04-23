-- DB: module
-- DT: -1M

DELETE FROM `a_matrix_data` WHERE (`state` = 1 AND `etime` < '{{yyyy/MM/dd}}') OR (`state` = 0 `ctime` < '{{yyyy/MM/dd}}');
