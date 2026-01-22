package ru.practicum.shareit.request;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import ru.practicum.shareit.client.BaseClient;

import java.util.Map;

@Component
public class ItemRequestClient extends BaseClient {

    public ItemRequestClient(@Value("${shareit-server.url}") String serverUrl,
                             RestTemplateBuilder builder) {
        super(serverUrl, builder, "/requests");
    }

    public ResponseEntity<String> create(long userId, Object body) {
        return post("", userId, body);
    }

    public ResponseEntity<String> getOwn(long userId) {
        return get("", userId);
    }

    public ResponseEntity<String> getAllOther(long userId, int from, int size) {
        return get("/all?from={from}&size={size}", userId,
                Map.<String, Object>of("from", from, "size", size));
    }

    public ResponseEntity<String> getById(long userId, long requestId) {
        return get("/{requestId}", userId,
                Map.<String, Object>of("requestId", requestId));
    }
}