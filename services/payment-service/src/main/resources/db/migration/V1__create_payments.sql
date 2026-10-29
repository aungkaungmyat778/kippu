-- One row per payment attempt that reached a decision. The idempotency key is unique, so the
-- database itself guarantees that retrying a request can never charge twice.
CREATE TABLE payments (
    id              UUID         PRIMARY KEY,
    booking_id      UUID         NOT NULL,
    idempotency_key VARCHAR(64)  NOT NULL UNIQUE,
    amount_yen      INT          NOT NULL CHECK (amount_yen > 0),
    method          VARCHAR(30)  NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    failure_reason  VARCHAR(60),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT chk_payment_status CHECK (status IN ('SUCCEEDED', 'DECLINED')),
    CONSTRAINT chk_payment_failure CHECK (
        (status = 'SUCCEEDED' AND failure_reason IS NULL)
     OR (status = 'DECLINED'  AND failure_reason IS NOT NULL)
    )
);

CREATE INDEX idx_payments_booking ON payments (booking_id);
