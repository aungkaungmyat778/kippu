-- A layout describes the seat grid of one bus type. Trips copy nothing from it:
-- the Seats service builds each trip's seats from this layout (M2).
CREATE TABLE bus_layouts (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    code          VARCHAR(20)  NOT NULL UNIQUE,
    name          VARCHAR(100) NOT NULL,
    row_count     INT          NOT NULL CHECK (row_count > 0),
    column_labels VARCHAR(20)  NOT NULL,   -- for example 'A,B,C,D'
    aisle_after   INT          NOT NULL    -- the aisle sits after this column number
);

CREATE TABLE layout_seats (
    id        UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    layout_id UUID        NOT NULL REFERENCES bus_layouts (id) ON DELETE CASCADE,
    seat_no   VARCHAR(5)  NOT NULL,
    row_no    INT         NOT NULL,
    col_label VARCHAR(2)  NOT NULL,
    UNIQUE (layout_id, seat_no)
);
