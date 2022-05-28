--DB=normal
--DT=-1D

DELETE FROM a_normal_data WHERE xtime < '{{t}}';

DELETE FROM a_normal_sess WHERE xtime < '{{t}}';
