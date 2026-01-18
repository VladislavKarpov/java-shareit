package ru.practicum.shareit.request.service;

import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;

import java.util.List;

public interface ItemRequestService {

    ItemRequestDto create(Long userId, ItemRequestCreateDto dto);

    List<ItemRequestDto> getOwn(Long userId);

    List<ItemRequestDto> getAllOthers(Long userId, int from, int size);

    ItemRequestDto getById(Long userId, Long requestId);
}