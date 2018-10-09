-- DB: normal
-- DT: -1D

DELETE FROM a_normal_record WHERE xtime < '{{%s}}';

DELETE FROM a_normal_sesion WHERE xtime < '{{%s}}';
