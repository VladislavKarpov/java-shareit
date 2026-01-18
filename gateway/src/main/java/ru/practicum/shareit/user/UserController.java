package ru.practicum.shareit.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.user.dto.UserDto;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserClient client;

    @PostMapping
    public ResponseEntity<String> create(@Valid @RequestBody UserDto dto) {
        return client.create(dto);
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<String> update(@PathVariable long userId,
                                         @RequestBody UserDto dto) {
        return client.update(userId, dto);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<String> getById(@PathVariable long userId) {
        return client.getById(userId);
    }

    @GetMapping
    public ResponseEntity<String> getAll() {
        return client.getAll();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<String> delete(@PathVariable long userId) {
        return client.deleteById(userId);
    }
}