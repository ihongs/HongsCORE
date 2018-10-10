-- DB: normal
-- DT: -1D

DELETE FROM a_normal_record WHERE xtime < '{{t}}';

DELETE FROM a_normal_sesion WHERE xtime < '{{t}}';
