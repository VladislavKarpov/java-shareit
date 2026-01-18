package ru.practicum.shareit.booking;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.booking.dto.BookingCreateDto;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/bookings")
public class BookingController {

    public static final String HEADER_USER = "X-Sharer-User-Id";

    private final BookingClient client;

    @PostMapping
    public ResponseEntity<String> create(@RequestHeader(HEADER_USER) long userId,
                                         @Valid @RequestBody BookingCreateDto dto) {
        return client.create(userId, dto);
    }

    @PatchMapping("/{bookingId}")
    public ResponseEntity<String> approve(@RequestHeader(HEADER_USER) long userId,
                                          @PathVariable long bookingId,
                                          @RequestParam Boolean approved) {
        return client.approve(userId, bookingId, approved);
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<String> getById(@RequestHeader(HEADER_USER) long userId,
                                          @PathVariable long bookingId) {
        return client.getById(userId, bookingId);
    }

    @GetMapping
    public ResponseEntity<String> getUserBookings(@RequestHeader(HEADER_USER) long userId,
                                                  @RequestParam(defaultValue = "ALL") String state,
                                                  @PositiveOrZero @RequestParam(defaultValue = "0") int from,
                                                  @Positive @RequestParam(defaultValue = "10") int size) {
        return client.getUserBookings(userId, state, from, size);
    }

    @GetMapping("/owner")
    public ResponseEntity<String> getOwnerBookings(@RequestHeader(HEADER_USER) long userId,
                                                   @RequestParam(defaultValue = "ALL") String state,
                                                   @PositiveOrZero @RequestParam(defaultValue = "0") int from,
                                                   @Positive @RequestParam(defaultValue = "10") int size) {
        return client.getOwnerBookings(userId, state, from, size);
    }
}