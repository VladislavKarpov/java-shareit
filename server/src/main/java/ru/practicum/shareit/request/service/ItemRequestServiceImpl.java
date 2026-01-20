package ru.practicum.shareit.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestItemDto;
import ru.practicum.shareit.request.mapper.ItemRequestMapper;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemRequestServiceImpl implements ItemRequestService {

    private static final Sort CREATED_DESC = Sort.by(Sort.Direction.DESC, "created");

    private final ItemRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    @Override
    @Transactional
    public ItemRequestDto create(Long userId, ItemRequestCreateDto dto) {
        log.info("ItemRequestService.create: userId={}, descriptionLen={}",
                userId, dto.getDescription() == null ? null : dto.getDescription().length());

        var requestor = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("ItemRequestService.create: user not found userId={}", userId);
                    return new NotFoundException("User not found: " + userId);
                });

        ItemRequest request = ItemRequest.builder()
                .description(dto.getDescription())
                .requestor(requestor)
                .created(LocalDateTime.now())
                .build();

        request = requestRepository.save(request);

        log.info("ItemRequestService.create: created requestId={}", request.getId());
        return ItemRequestMapper.toDto(request, List.of());
    }

    @Override
    public List<ItemRequestDto> getOwn(Long userId) {
        log.info("ItemRequestService.getOwn: userId={}", userId);

        userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("ItemRequestService.getOwn: user not found userId={}", userId);
                    return new NotFoundException("User not found: " + userId);
                });

        List<ItemRequest> requests = requestRepository.findByRequestor_IdOrderByCreatedDesc(userId);
        var result = attachItems(requests);

        log.info("ItemRequestService.getOwn: resultSize={}", result.size());
        return result;
    }

    @Override
    public List<ItemRequestDto> getAllOthers(Long userId, int from, int size) {
        log.info("ItemRequestService.getAllOthers: userId={}, from={}, size={}", userId, from, size);

        userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("ItemRequestService.getAllOthers: user not found userId={}", userId);
                    return new NotFoundException("User not found: " + userId);
                });

        int safeFrom = Math.max(from, 0);
        int safeSize = Math.max(size, 1);

        int page = safeFrom / safeSize;
        int offset = safeFrom % safeSize;

        List<ItemRequest> pageRequests = requestRepository
                .findByRequestor_IdNotOrderByCreatedDesc(userId, PageRequest.of(page, safeSize, CREATED_DESC));

        if (offset >= pageRequests.size()) {
            log.info("ItemRequestService.getAllOthers: offset beyond page -> empty result (offset={}, pageSize={})",
                    offset, pageRequests.size());
            return List.of();
        }

        var result = attachItems(pageRequests.subList(offset, pageRequests.size()));
        log.info("ItemRequestService.getAllOthers: resultSize={}", result.size());
        return result;
    }

    @Override
    public ItemRequestDto getById(Long userId, Long requestId) {
        log.info("ItemRequestService.getById: userId={}, requestId={}", userId, requestId);

        userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("ItemRequestService.getById: user not found userId={}", userId);
                    return new NotFoundException("User not found: " + userId);
                });

        ItemRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> {
                    log.warn("ItemRequestService.getById: request not found requestId={}", requestId);
                    return new NotFoundException("Request not found: " + requestId);
                });

        List<ItemRequestItemDto> items = itemRepository.findByRequest_IdIn(List.of(requestId)).stream()
                .filter(i -> i.getRequest() != null)
                .map(ItemRequestMapper::toItemDto)
                .toList();

        log.info("ItemRequestService.getById: itemsAttached={}", items.size());
        return ItemRequestMapper.toDto(request, items);
    }

    private List<ItemRequestDto> attachItems(List<ItemRequest> requests) {
        if (requests == null || requests.isEmpty()) return List.of();

        List<Long> requestIds = requests.stream().map(ItemRequest::getId).toList();

        Map<Long, List<ItemRequestItemDto>> itemsByRequestId = itemRepository.findByRequest_IdIn(requestIds)
                .stream()
                .filter(i -> i.getRequest() != null)
                .collect(Collectors.groupingBy(
                        i -> i.getRequest().getId(),
                        Collectors.mapping(ItemRequestMapper::toItemDto, Collectors.toList())
                ));

        return requests.stream()
                .map(r -> ItemRequestMapper.toDto(r, itemsByRequestId.getOrDefault(r.getId(), List.of())))
                .toList();
    }
}