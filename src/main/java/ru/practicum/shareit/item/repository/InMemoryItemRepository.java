package ru.practicum.shareit.item.repository;

import org.springframework.stereotype.Repository;
import ru.practicum.shareit.item.model.Item;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryItemRepository {
    private final Map<Long, Item> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public Item save(Item item) {
        if (item.getId() == null) {
            item.setId(idGenerator.getAndIncrement());
        }
        storage.put(item.getId(), item);
        return item;
    }

    public Optional<Item> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    public List<Item> findAll() {
        return new ArrayList<>(storage.values());
    }

    public List<Item> findByOwnerId(Long ownerId) {
        return storage.values()
                .stream()
                .filter(item -> item.getOwner() != null)
                .filter(item -> Objects.equals(item.getOwner().getId(), ownerId))
                .collect(Collectors.toList());
    }

    public List<Item> searchByText(String text) {
        if (text == null || text.isBlank()) return Collections.emptyList();
        String lower = text.toLowerCase();
        return storage.values().stream()
                .filter(i -> Boolean.TRUE.equals(i.getAvailable()))
                .filter(i -> (i.getName() != null && i.getName().toLowerCase().contains(lower)) ||
                        (i.getDescription() != null && i.getDescription().toLowerCase().contains(lower)))
                .collect(Collectors.toList());
    }

    public void delete(Long id) {
        storage.remove(id);
    }
}