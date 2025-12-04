package ru.practicum.shareit.item.service;

import ru.practicum.shareit.item.dto.ItemDto;

import java.util.List;

public interface ItemService {
    ItemDto create(Long userId, ItemDto itemDto);

    ItemDto update(Long userId, Long itemId, ItemDto update);

    ItemDto getById(Long userId, Long itemId);

    List<ItemDto> getOwnerItems(Long userId);

    List<ItemDto> search(String text);
}