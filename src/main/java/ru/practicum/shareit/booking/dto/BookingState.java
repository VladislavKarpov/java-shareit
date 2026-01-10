package ru.practicum.shareit.booking.dto;

import ru.practicum.shareit.exception.ValidationException;

public enum BookingState {
    ALL, CURRENT, PAST, FUTURE, WAITING, REJECTED;

    public static BookingState from(String value) {
        if (value == null) return ALL;
        try {
            return BookingState.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Unknown state: " + value);
        }
    }
}
