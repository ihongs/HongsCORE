-- DB: module
-- DT: -1M

DELETE FROM `a_matrix_data` WHERE `state` = 0 AND `etime` < '{{yyyy/MM/dd}}';
