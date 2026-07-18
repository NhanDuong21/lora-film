UPDATE cinema_operating_hours
SET close_time = '23:59:59'
WHERE close_time = '24:00:00';

UPDATE cinema_operating_hours
SET open_time = '00:00:00'
WHERE open_time = '24:00:00';
