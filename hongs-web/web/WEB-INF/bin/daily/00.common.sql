-- DB: common
-- DT: -1D

DELETE FROM a_common_record WHERE xtime < '{{yyyy/MM/dd}}';

DELETE FROM a_common_sesion WHERE xtime < '{{yyyy/MM/dd}}';
