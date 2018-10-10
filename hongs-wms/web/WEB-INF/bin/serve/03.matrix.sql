-- DB: module
-- DT: -1M

DELETE FROM `a_matrix_data` WHERE (`state` = 1 AND `etime` < '{{t}}') OR (`state` = 0 `ctime` < '{{t}}');
