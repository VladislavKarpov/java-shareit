package ru.practicum.shareit.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/requests")
public class ItemRequestController {

    public static final String HEADER_USER = "X-Sharer-User-Id";

    private final ItemRequestClient client;

    @PostMapping
    public ResponseEntity<String> create(@RequestHeader(HEADER_USER) long userId,
                                         @Valid @RequestBody ItemRequestCreateDto dto) {
        return client.create(userId, dto);
    }

    @GetMapping
    public ResponseEntity<String> getOwn(@RequestHeader(HEADER_USER) long userId) {
        return client.getOwn(userId);
    }

    @GetMapping("/all")
    public ResponseEntity<String> getAll(@RequestHeader(HEADER_USER) long userId,
                                         @PositiveOrZero @RequestParam(defaultValue = "0") int from,
                                         @Positive @RequestParam(defaultValue = "10") int size) {
        return client.getAllOther(userId, from, size);
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<String> getById(@RequestHeader(HEADER_USER) long userId,
                                          @PathVariable long requestId) {
        return client.getById(userId, requestId);
    }
}