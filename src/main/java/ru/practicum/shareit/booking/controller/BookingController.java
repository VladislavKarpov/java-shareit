package ru.practicum.shareit.booking.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingState;
import ru.practicum.shareit.booking.service.BookingService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/bookings")
public class BookingController {

    public static final String HEADER_USER = "X-Sharer-User-Id";

    private final BookingService bookingService;

    @PostMapping
    public BookingDto create(@RequestHeader(HEADER_USER) Long userId,
                             @Valid @RequestBody BookingCreateDto dto) {
        return bookingService.create(userId, dto);
    }

    @PatchMapping("/{bookingId}")
    public BookingDto approve(@RequestHeader(HEADER_USER) Long userId,
                              @PathVariable Long bookingId,
                              @RequestParam Boolean approved) {
        return bookingService.approve(userId, bookingId, approved);
    }

    @GetMapping("/{bookingId}")
    public BookingDto getById(@RequestHeader(HEADER_USER) Long userId,
                              @PathVariable Long bookingId) {
        return bookingService.getById(userId, bookingId);
    }

    @GetMapping
    public List<BookingDto> getUserBookings(@RequestHeader(HEADER_USER) Long userId,
                                            @RequestParam(required = false, defaultValue = "ALL") String state) {
        return bookingService.getUserBookings(userId, BookingState.from(state));
    }

    @GetMapping("/owner")
    public List<BookingDto> getOwnerBookings(@RequestHeader(HEADER_USER) Long userId,
                                             @RequestParam(required = false, defaultValue = "ALL") String state) {
        return bookingService.getOwnerBookings(userId, BookingState.from(state));
    }
}
