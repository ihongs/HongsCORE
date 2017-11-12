-- DB: normal
-- DT: -1D

DELETE FROM a_normal_record WHERE xtime < '{{yyyy/MM/dd}}';

DELETE FROM a_normal_sesion WHERE xtime < '{{yyyy/MM/dd}}';
