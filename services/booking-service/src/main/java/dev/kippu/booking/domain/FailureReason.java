package dev.kippu.booking.domain;

/** Why a booking ended in FAILED. Stored as text, so new reasons need no migration. */
public final class FailureReason {

    public static final String SEAT_UNAVAILABLE = "SEAT_UNAVAILABLE";
    public static final String UNKNOWN_SEAT = "UNKNOWN_SEAT";
    public static final String SEAT_SERVICE_UNAVAILABLE = "SEAT_SERVICE_UNAVAILABLE";
    public static final String PAYMENT_DECLINED = "PAYMENT_DECLINED";
    /** The payment call failed and we do not know whether money was taken (M4 handles this). */
    public static final String PAYMENT_UNAVAILABLE = "PAYMENT_UNAVAILABLE";
    /** Money was taken but the seats could not be confirmed. Needs a refund (M4). */
    public static final String PAID_BUT_NOT_CONFIRMED = "PAID_BUT_NOT_CONFIRMED";

    private FailureReason() {
    }
}
