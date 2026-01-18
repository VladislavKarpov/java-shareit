package ru.practicum.shareit.booking;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import ru.practicum.shareit.client.BaseClient;

import java.util.Map;

@Component
public class BookingClient extends BaseClient {

    public BookingClient(@Value("${shareit-server.url}") String serverUrl,
                         RestTemplateBuilder builder) {
        super(serverUrl, builder, "/bookings");
    }

    public ResponseEntity<String> create(long userId, Object body) {
        return post("", userId, body);
    }

    public ResponseEntity<String> approve(long userId, long bookingId, boolean approved) {
        return patch("/{bookingId}?approved={approved}", userId, null,
                Map.<String, Object>of("bookingId", bookingId, "approved", approved));
    }

    public ResponseEntity<String> getById(long userId, long bookingId) {
        return get("/{bookingId}", userId,
                Map.<String, Object>of("bookingId", bookingId));
    }

    public ResponseEntity<String> getUserBookings(long userId, String state, int from, int size) {
        return get("?state={state}&from={from}&size={size}", userId,
                Map.<String, Object>of("state", state, "from", from, "size", size));
    }

    public ResponseEntity<String> getOwnerBookings(long userId, String state, int from, int size) {
        return get("/owner?state={state}&from={from}&size={size}", userId,
                Map.<String, Object>of("state", state, "from", from, "size", size));
    }
}