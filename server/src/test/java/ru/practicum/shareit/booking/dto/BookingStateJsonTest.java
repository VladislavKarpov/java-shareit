package ru.practicum.shareit.booking.dto;

import org.junit.jupiter.api.Test;
import ru.practicum.shareit.exception.ValidationException;

import static org.assertj.core.api.Assertions.*;

public class BookingStateJsonTest {

    @Test
    public void from_null_returnsALL() {
        assertThat(BookingState.from(null)).isEqualTo(BookingState.ALL);
    }

    @Test
    public void from_caseInsensitive() {
        assertThat(BookingState.from("waiting")).isEqualTo(BookingState.WAITING);
        assertThat(BookingState.from("AlL")).isEqualTo(BookingState.ALL);
    }

    @Test
    public void from_unknown_throwsValidationException() {
        assertThatThrownBy(() -> BookingState.from("WTF"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Unknown state: WTF");
    }
}