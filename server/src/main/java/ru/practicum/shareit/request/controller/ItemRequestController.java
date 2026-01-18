package ru.practicum.shareit.request.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.service.ItemRequestService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/requests")
public class ItemRequestController {

    public static final String HEADER_USER = "X-Sharer-User-Id";
    private final ItemRequestService service;

    @PostMapping
    public ResponseEntity<ItemRequestDto> create(@RequestHeader(HEADER_USER) Long userId,
                                                 @Valid @RequestBody ItemRequestCreateDto dto) {
        return ResponseEntity.ok(service.create(userId, dto));
    }

    @GetMapping
    public ResponseEntity<List<ItemRequestDto>> getOwn(@RequestHeader(HEADER_USER) Long userId) {
        return ResponseEntity.ok(service.getOwn(userId));
    }

    @GetMapping("/all")
    public ResponseEntity<List<ItemRequestDto>> getAll(@RequestHeader(HEADER_USER) Long userId,
                                                       @RequestParam(defaultValue = "0") int from,
                                                       @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(service.getAllOthers(userId, from, size));
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<ItemRequestDto> getById(@RequestHeader(HEADER_USER) Long userId,
                                                  @PathVariable Long requestId) {
        return ResponseEntity.ok(service.getById(userId, requestId));
    }
}