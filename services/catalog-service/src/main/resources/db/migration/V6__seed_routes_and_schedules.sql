-- Sample development data: routes, timetables and fares are made up, not real operators.
INSERT INTO routes (origin_terminal_id, destination_terminal_id, duration_minutes)
SELECT o.id, d.id, v.minutes
FROM (VALUES
    ('TYO-BUSTA',    'OSA-UMEDA',    540),
    ('OSA-UMEDA',    'TYO-BUSTA',    540),
    ('TYO-YAESU',    'KYO-STN',      480),
    ('KYO-STN',      'TYO-YAESU',    480),
    ('TYO-BUSTA',    'NGO-MEITETSU', 360),
    ('NGO-MEITETSU', 'TYO-BUSTA',    360)
) AS v(origin_code, dest_code, minutes)
JOIN terminals o ON o.code = v.origin_code
JOIN terminals d ON d.code = v.dest_code;

INSERT INTO schedules (route_id, layout_id, departure_time, days_of_week, fare_yen)
SELECT r.id, l.id, v.dep::time, v.days, v.fare
FROM (VALUES
    ('TYO-BUSTA',    'OSA-UMEDA',    'STD-2X2', '22:30', 'MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY', 6500),
    ('TYO-BUSTA',    'OSA-UMEDA',    'PRM-2X1', '23:00', 'MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY', 9800),
    ('OSA-UMEDA',    'TYO-BUSTA',    'STD-2X2', '22:45', 'MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY', 6500),
    ('OSA-UMEDA',    'TYO-BUSTA',    'PRM-2X1', '23:15', 'MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY', 9800),
    ('TYO-YAESU',    'KYO-STN',      'STD-2X2', '22:00', 'MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY', 6000),
    ('KYO-STN',      'TYO-YAESU',    'STD-2X2', '22:15', 'MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY', 6000),
    ('TYO-BUSTA',    'NGO-MEITETSU', 'STD-2X2', '07:30', 'MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY',                 3500),
    ('NGO-MEITETSU', 'TYO-BUSTA',    'STD-2X2', '17:00', 'MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY', 3500)
) AS v(origin_code, dest_code, layout_code, dep, days, fare)
JOIN terminals o ON o.code = v.origin_code
JOIN terminals d ON d.code = v.dest_code
JOIN routes r ON r.origin_terminal_id = o.id AND r.destination_terminal_id = d.id
JOIN bus_layouts l ON l.code = v.layout_code;
