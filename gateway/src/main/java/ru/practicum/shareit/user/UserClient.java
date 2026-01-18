package ru.practicum.shareit.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import ru.practicum.shareit.client.BaseClient;

import java.util.Map;

@Component
public class UserClient extends BaseClient {

    public UserClient(@Value("${shareit-server.url}") String serverUrl,
                      RestTemplateBuilder builder) {
        super(serverUrl, builder, "/users");
    }

    public ResponseEntity<String> create(Object body) {
        return post("", 0, body);
    }

    public ResponseEntity<String> update(long userId, Object body) {
        return patch("/{userId}", 0, body, Map.of("userId", userId));
    }

    public ResponseEntity<String> getById(long userId) {
        return get("/{userId}", 0, Map.of("userId", userId));
    }

    public ResponseEntity<String> getAll() {
        return get("", 0);
    }

    public ResponseEntity<String> deleteById(long userId) {
        return delete("/{userId}", 0, Map.of("userId", userId));
    }
}