-- DB: module
-- DT: -3M

DELETE FROM `a_master_user` WHERE (`state` = 0 AND `mtime` < '{{t}}');
