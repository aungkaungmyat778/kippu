package dev.kippu.seat.service;

public class InvalidHoldRequestException extends RuntimeException {
    public InvalidHoldRequestException(String message) {
        super(message);
    }
}
