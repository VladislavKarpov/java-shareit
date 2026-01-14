package ru.practicum.shareit.item.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.item.dto.*;
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

    @PostMapping("/{itemId}/comment")
    public ResponseEntity<CommentDto> addComment(@RequestHeader(HEADER_USER) Long userId,
                                                 @PathVariable Long itemId,
                                                 @Valid @RequestBody CommentCreateDto dto) {
        return ResponseEntity.ok(service.addComment(userId, itemId, dto));
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
    public ResponseEntity<ItemDetailsDto> getById(@RequestHeader(HEADER_USER) Long userId,
                                                  @PathVariable Long itemId) {
        return ResponseEntity.ok(service.getById(userId, itemId));
    }

    @GetMapping
    public ResponseEntity<List<ItemOwnerDto>> getOwnerItems(@RequestHeader(HEADER_USER) Long userId) {
        return ResponseEntity.ok(service.getOwnerItems(userId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ItemDto>> search(@RequestParam String text) {
        return ResponseEntity.ok(service.search(text));
    }


}