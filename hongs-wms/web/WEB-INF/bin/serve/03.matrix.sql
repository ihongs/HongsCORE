-- DB: module
-- DT: -3M

DELETE FROM `a_matrix_data` WHERE (`state` = 0 AND `ctime` < '{{t}}') OR (`state` = 1 AND `etime` < '{{t}}');
