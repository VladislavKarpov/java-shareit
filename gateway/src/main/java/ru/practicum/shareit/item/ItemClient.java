package ru.practicum.shareit.item;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import ru.practicum.shareit.client.BaseClient;

import java.util.Map;

@Component
public class ItemClient extends BaseClient {

    public ItemClient(@Value("${shareit-server.url}") String serverUrl,
                      RestTemplateBuilder builder) {
        super(serverUrl, builder, "/items");
    }

    public ResponseEntity<String> create(long userId, Object body) {
        return post("", userId, body);
    }

    public ResponseEntity<String> update(long userId, long itemId, Object body) {
        return patch("/{itemId}", userId, body, Map.of("itemId", itemId));
    }

    public ResponseEntity<String> getById(long userId, long itemId) {
        return get("/{itemId}", userId, Map.of("itemId", itemId));
    }

    public ResponseEntity<String> getOwnerItems(long userId) {
        return get("", userId);
    }

    public ResponseEntity<String> search(String text) {
        return get("/search?text={text}", 0, Map.of("text", text));
    }

    public ResponseEntity<String> addComment(long userId, long itemId, Object body) {
        // ✅ теперь BaseClient умеет post с params — ничего не ломаем
        return post("/{itemId}/comment", userId, body, Map.of("itemId", itemId));
    }
}