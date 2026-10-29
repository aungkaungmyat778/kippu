-- A booking is one customer's purchase of 1 to 4 seats on one trip.
-- Prices are copied from the trip at booking time, so later fare changes never alter old bookings.
CREATE TABLE bookings (
    id             UUID         PRIMARY KEY,
    version        BIGINT       NOT NULL,        -- optimistic locking: a stale update fails instead of overwriting
    reference      VARCHAR(12)  NOT NULL UNIQUE, -- short code shown to the customer, e.g. KP-7H3K9QWM
    trip_id        UUID         NOT NULL,        -- owned by Catalog, so no foreign key
    status         VARCHAR(20)  NOT NULL,
    failure_reason VARCHAR(60),
    customer_name  VARCHAR(100) NOT NULL,
    customer_email VARCHAR(200) NOT NULL,
    seat_count     INT          NOT NULL,
    fare_yen       INT          NOT NULL,        -- price of one seat
    total_yen      INT          NOT NULL,
    departure_at   TIMESTAMPTZ  NOT NULL,
    payment_id     UUID,                         -- owned by Payment, so no foreign key
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,

    CONSTRAINT chk_booking_status     CHECK (status IN ('PENDING', 'CONFIRMED', 'FAILED')),
    CONSTRAINT chk_booking_seat_count CHECK (seat_count BETWEEN 1 AND 4),
    CONSTRAINT chk_booking_total      CHECK (total_yen = fare_yen * seat_count),
    -- A FAILED booking may still carry a payment_id: that is the "paid but not confirmed" case
    -- that a later refund step has to find.
    CONSTRAINT chk_booking_state CHECK (
        (status = 'PENDING'   AND failure_reason IS NULL     AND payment_id IS NULL)
     OR (status = 'CONFIRMED' AND failure_reason IS NULL     AND payment_id IS NOT NULL)
     OR (status = 'FAILED'    AND failure_reason IS NOT NULL)
    )
);

CREATE TABLE booking_seats (
    booking_id UUID       NOT NULL REFERENCES bookings (id) ON DELETE CASCADE,
    seat_no    VARCHAR(5) NOT NULL,
    PRIMARY KEY (booking_id, seat_no)
);

CREATE INDEX idx_bookings_trip   ON bookings (trip_id);
CREATE INDEX idx_bookings_email  ON bookings (customer_email);
CREATE INDEX idx_bookings_status ON bookings (status);
