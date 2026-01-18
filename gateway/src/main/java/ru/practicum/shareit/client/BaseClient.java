package ru.practicum.shareit.client;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.io.IOException;
import java.util.Map;

public class BaseClient {

    protected final RestTemplate rest;
    private final String apiPrefix;

    public static final String HEADER_USER = "X-Sharer-User-Id";

    public BaseClient(String serverUrl, RestTemplateBuilder builder, String apiPrefix) {
        this.apiPrefix = apiPrefix;

        this.rest = builder
                .uriTemplateHandler(new DefaultUriBuilderFactory(serverUrl))
                .errorHandler(new DefaultResponseErrorHandler() {
                    @Override
                    public boolean hasError(ClientHttpResponse response) throws IOException {
                        return false;
                    }
                })
                .build();
    }

    protected ResponseEntity<String> get(String path, long userId) {
        return exchange(path, HttpMethod.GET, userId, null, null);
    }

    protected ResponseEntity<String> get(String path, long userId, Map<String, Object> params) {
        return exchange(path, HttpMethod.GET, userId, null, params);
    }

    protected ResponseEntity<String> post(String path, long userId, Object body) {
        return exchange(path, HttpMethod.POST, userId, body, null);
    }

    protected ResponseEntity<String> post(String path, long userId, Object body, Map<String, Object> params) {
        return exchange(path, HttpMethod.POST, userId, body, params);
    }

    protected ResponseEntity<String> patch(String path, long userId, Object body, Map<String, Object> params) {
        return exchange(path, HttpMethod.PATCH, userId, body, params);
    }

    protected ResponseEntity<String> delete(String path, long userId, Map<String, Object> params) {
        return exchange(path, HttpMethod.DELETE, userId, null, params);
    }

    private ResponseEntity<String> exchange(String path,
                                            HttpMethod method,
                                            long userId,
                                            @Nullable Object body,
                                            @Nullable Map<String, Object> params) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (userId > 0) {
            headers.add(HEADER_USER, String.valueOf(userId));
        }

        HttpEntity<Object> requestEntity = new HttpEntity<>(body, headers);

        String url = apiPrefix + path;

        try {
            ResponseEntity<String> response = (params == null)
                    ? rest.exchange(url, method, requestEntity, String.class)
                    : rest.exchange(url, method, requestEntity, String.class, params);
            return passThrough(response);

        } catch (RestClientResponseException e) {
            HttpHeaders respHeaders = new HttpHeaders();
            MediaType ct = safeContentType(e.getResponseHeaders());
            if (ct != null) respHeaders.setContentType(ct);
            return new ResponseEntity<>(e.getResponseBodyAsString(), respHeaders, HttpStatus.valueOf(e.getRawStatusCode()));
        }
    }

    private ResponseEntity<String> passThrough(ResponseEntity<String> response) {
        HttpHeaders out = new HttpHeaders();

        MediaType ct = safeContentType(response.getHeaders());
        if (ct != null) out.setContentType(ct);

        return new ResponseEntity<>(response.getBody(), out, response.getStatusCode());
    }

    @Nullable
    private MediaType safeContentType(@Nullable HttpHeaders headers) {
        if (headers == null) return null;
        try {
            return headers.getContentType();
        } catch (Exception ignored) {
            return null;
        }
    }
}