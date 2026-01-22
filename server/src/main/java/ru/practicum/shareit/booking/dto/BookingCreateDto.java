package ru.practicum.shareit.booking.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingCreateDto {

    @NotNull
    private Long itemId;

    @NotNull
    @FutureOrPresent(message = "Start must be in the present or future")
    private LocalDateTime start;

    @NotNull
    @Future(message = "End must be in the future")
    private LocalDateTime end;
}
