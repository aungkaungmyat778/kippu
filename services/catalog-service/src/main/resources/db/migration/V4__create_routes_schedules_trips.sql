-- route    = a fixed path between two terminals (Tokyo to Osaka)
-- schedule = a recurring timetable entry for a route (daily at 22:30)
-- trip     = one real run of a schedule on one date. This is what customers book.
CREATE TABLE routes (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    origin_terminal_id      UUID NOT NULL REFERENCES terminals (id),
    destination_terminal_id UUID NOT NULL REFERENCES terminals (id),
    duration_minutes        INT  NOT NULL CHECK (duration_minutes > 0),
    UNIQUE (origin_terminal_id, destination_terminal_id),
    CHECK (origin_terminal_id <> destination_terminal_id)
);

CREATE TABLE schedules (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    route_id       UUID        NOT NULL REFERENCES routes (id),
    layout_id      UUID        NOT NULL REFERENCES bus_layouts (id),
    departure_time TIME        NOT NULL,            -- local time in the service time zone
    days_of_week   VARCHAR(80) NOT NULL,            -- 'MONDAY,TUESDAY,...'
    fare_yen       INT         NOT NULL CHECK (fare_yen > 0),
    active         BOOLEAN     NOT NULL DEFAULT true
);

CREATE TABLE trips (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    schedule_id  UUID        REFERENCES schedules (id),
    route_id     UUID        NOT NULL REFERENCES routes (id),
    layout_id    UUID        NOT NULL REFERENCES bus_layouts (id),
    departure_at TIMESTAMPTZ NOT NULL,
    arrival_at   TIMESTAMPTZ NOT NULL,
    fare_yen     INT         NOT NULL CHECK (fare_yen > 0),  -- copied from the schedule at creation
    status       VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (schedule_id, departure_at),                       -- makes trip generation safe to repeat
    CHECK (arrival_at > departure_at)
);

CREATE INDEX idx_trips_route_departure ON trips (route_id, departure_at);
