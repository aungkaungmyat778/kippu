-- Sample development data.
INSERT INTO bus_layouts (code, name, row_count, column_labels, aisle_after) VALUES
    ('STD-2X2', 'Standard 2+2 (40 seats)', 10, 'A,B,C,D', 2),
    ('PRM-2X1', 'Premium 2+1 (24 seats)',   8, 'A,B,C',   2);

-- Build the seat grid: one seat per row and column, named like 1A, 1B, 10D.
INSERT INTO layout_seats (layout_id, seat_no, row_no, col_label)
SELECT l.id, r.n::text || c.label, r.n, c.label
FROM bus_layouts l
CROSS JOIN LATERAL generate_series(1, l.row_count) AS r(n)
CROSS JOIN LATERAL unnest(string_to_array(l.column_labels, ',')) AS c(label);
