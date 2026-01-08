package ru.practicum.shareit.item.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.service.ItemService;


import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/items")
public class ItemController {
    private final ItemService service;
    public static final String HEADER_USER = "X-Sharer-User-Id";

    @PostMapping
    public ResponseEntity<ItemDto> create(@RequestHeader(HEADER_USER) Long userId,
                                          @Valid @RequestBody ItemDto itemDto) {
        return ResponseEntity.ok(service.create(userId, itemDto));
    }

    @PatchMapping("/{itemId}")
    public ResponseEntity<ItemDto> update(
            @RequestHeader(HEADER_USER) Long userId,
            @PathVariable Long itemId,
            @RequestBody ItemDto itemDto
    ) {
        return ResponseEntity.ok(service.update(userId, itemId, itemDto));
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<ItemDto> getById(@RequestHeader(HEADER_USER) Long userId,
                                           @PathVariable Long itemId) {
        return ResponseEntity.ok(service.getById(userId, itemId));
    }

    @GetMapping
    public ResponseEntity<List<ItemDto>> getOwnerItems(@RequestHeader(HEADER_USER) Long userId) {
        return ResponseEntity.ok(service.getOwnerItems(userId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ItemDto>> search(@RequestParam String text) {
        return ResponseEntity.ok(service.search(text));
    }
}