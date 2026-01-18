package ru.practicum.shareit.item;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.ItemDto;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/items")
public class ItemController {

    public static final String HEADER_USER = "X-Sharer-User-Id";

    private final ItemClient client;

    @PostMapping
    public ResponseEntity<String> create(@RequestHeader(HEADER_USER) long userId,
                                         @Valid @RequestBody ItemDto dto) {
        return client.create(userId, dto);
    }

    @PatchMapping("/{itemId}")
    public ResponseEntity<String> update(@RequestHeader(HEADER_USER) long userId,
                                         @PathVariable long itemId,
                                         @RequestBody ItemDto dto) {
        return client.update(userId, itemId, dto);
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<String> getById(@RequestHeader(HEADER_USER) long userId,
                                          @PathVariable long itemId) {
        return client.getById(userId, itemId);
    }

    @GetMapping
    public ResponseEntity<String> getOwnerItems(@RequestHeader(HEADER_USER) long userId) {
        return client.getOwnerItems(userId);
    }

    @GetMapping("/search")
    public ResponseEntity<String> search(@RequestParam String text) {
        return client.search(text);
    }

    @PostMapping("/{itemId}/comment")
    public ResponseEntity<String> addComment(@RequestHeader(HEADER_USER) long userId,
                                             @PathVariable long itemId,
                                             @Valid @RequestBody CommentCreateDto dto) {
        return client.addComment(userId, itemId, dto);
    }
}