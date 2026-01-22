package ru.practicum.shareit.item.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.*;
import ru.practicum.shareit.item.mapper.CommentMapper;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.CommentRepository;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {

    private static final Sort COMMENTS_SORT = Sort.by(Sort.Direction.DESC, "created");

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;
    private final ItemRequestRepository itemRequestRepository;

    @Override
    @Transactional
    public ItemDto create(Long userId, ItemDto itemDto) {
        log.info("ItemService.create: userId={}, name={}, requestId={}",
                userId, itemDto.getName(), itemDto.getRequestId());

        var owner = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("ItemService.create: user not found userId={}", userId);
                    return new NotFoundException("User not found: " + userId);
                });

        var item = ItemMapper.toItem(itemDto, owner);

        if (itemDto.getRequestId() != null) {
            var request = itemRequestRepository.findById(itemDto.getRequestId())
                    .orElseThrow(() -> {
                        log.warn("ItemService.create: request not found requestId={}", itemDto.getRequestId());
                        return new NotFoundException("Request not found: " + itemDto.getRequestId());
                    });
            item.setRequest(request);
        }

        item = itemRepository.save(item);
        log.info("ItemService.create: created itemId={}", item.getId());
        return ItemMapper.toItemDto(item);
    }

    @Override
    @Transactional
    public ItemDto update(Long userId, Long itemId, ItemDto update) {
        log.info("ItemService.update: userId={}, itemId={}, patch(name={}, desc={}, available={})",
                userId, itemId, update.getName(), update.getDescription(), update.getAvailable());

        var item = itemRepository.findById(itemId)
                .orElseThrow(() -> {
                    log.warn("ItemService.update: item not found itemId={}", itemId);
                    return new NotFoundException("Item not found: " + itemId);
                });

        if (!item.getOwner().getId().equals(userId)) {
            log.warn("ItemService.update: forbidden userId={}, itemId={}, ownerId={}",
                    userId, itemId, item.getOwner().getId());
            throw new ForbiddenException("User is not owner of this item");
        }

        if (update.getName() != null) item.setName(update.getName());
        if (update.getDescription() != null) item.setDescription(update.getDescription());
        if (update.getAvailable() != null) item.setAvailable(update.getAvailable());

        item = itemRepository.save(item);
        log.info("ItemService.update: updated itemId={}", item.getId());
        return ItemMapper.toItemDto(item);
    }

    @Override
    public ItemDetailsDto getById(Long userId, Long itemId) {
        log.info("ItemService.getById: userId={}, itemId={}", userId, itemId);

        userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("ItemService.getById: user not found userId={}", userId);
                    return new NotFoundException("User not found: " + userId);
                });

        var item = itemRepository.findById(itemId)
                .orElseThrow(() -> {
                    log.warn("ItemService.getById: item not found itemId={}", itemId);
                    return new NotFoundException("Item not found: " + itemId);
                });

        var comments = commentRepository.findByItem_Id(itemId, COMMENTS_SORT)
                .stream()
                .map(CommentMapper::toDto)
                .toList();

        var dto = ItemMapper.toDetailsDto(item, comments);

        if (item.getOwner().getId().equals(userId)) {
            LocalDateTime now = LocalDateTime.now();
            List<Booking> bookings = bookingRepository.findByItem_IdAndStatus(itemId, Booking.BookingStatus.APPROVED);
            fillLastNext(dto, bookings, now);
            log.info("ItemService.getById: owner view, bookingsLoaded={}, commentsLoaded={}",
                    bookings.size(), comments.size());
        } else {
            log.info("ItemService.getById: non-owner view, commentsLoaded={}", comments.size());
        }

        return dto;
    }

    @Override
    public List<ItemOwnerDto> getOwnerItems(Long userId) {
        log.info("ItemService.getOwnerItems: userId={}", userId);

        userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("ItemService.getOwnerItems: user not found userId={}", userId);
                    return new NotFoundException("User not found: " + userId);
                });

        List<Item> items = itemRepository.findByOwner_Id(userId);
        if (items.isEmpty()) {
            log.info("ItemService.getOwnerItems: no items for userId={}", userId);
            return List.of();
        }

        List<Long> itemIds = items.stream().map(Item::getId).toList();

        Map<Long, List<CommentDto>> commentsByItemId = commentRepository.findByItem_IdIn(itemIds, COMMENTS_SORT)
                .stream()
                .collect(Collectors.groupingBy(
                        c -> c.getItem().getId(),
                        Collectors.mapping(CommentMapper::toDto, Collectors.toList())
                ));

        Map<Long, List<Booking>> bookingsByItemId = bookingRepository
                .findByItem_IdInAndStatus(itemIds, Booking.BookingStatus.APPROVED)
                .stream()
                .collect(Collectors.groupingBy(b -> b.getItem().getId()));

        LocalDateTime now = LocalDateTime.now();

        List<ItemOwnerDto> result = new ArrayList<>(items.size());
        for (Item item : items) {
            List<CommentDto> comments = commentsByItemId.getOrDefault(item.getId(), List.of());
            ItemOwnerDto dto = ItemMapper.toOwnerDto(item, comments);

            List<Booking> bookings = bookingsByItemId.getOrDefault(item.getId(), List.of());
            fillLastNext(dto, bookings, now);

            result.add(dto);
        }

        log.info("ItemService.getOwnerItems: items={}, commentsBuckets={}, bookingBuckets={}",
                result.size(), commentsByItemId.size(), bookingsByItemId.size());
        return result;
    }

    @Override
    public List<ItemDto> search(String text) {
        log.info("ItemService.search: text='{}'", text);

        if (text == null || text.isBlank()) {
            log.info("ItemService.search: blank query -> empty result");
            return List.of();
        }

        var result = itemRepository.search(text).stream()
                .map(ItemMapper::toItemDto)
                .toList();

        log.info("ItemService.search: resultSize={}", result.size());
        return result;
    }

    @Override
    @Transactional
    public CommentDto addComment(Long userId, Long itemId, CommentCreateDto dto) {
        log.info("ItemService.addComment: userId={}, itemId={}, textLen={}",
                userId, itemId, dto.getText() == null ? null : dto.getText().length());

        var author = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("ItemService.addComment: user not found userId={}", userId);
                    return new NotFoundException("User not found: " + userId);
                });

        var item = itemRepository.findById(itemId)
                .orElseThrow(() -> {
                    log.warn("ItemService.addComment: item not found itemId={}", itemId);
                    return new NotFoundException("Item not found: " + itemId);
                });

        boolean canComment = bookingRepository.existsByBooker_IdAndItem_IdAndEndIsBeforeAndStatus(
                userId, itemId, LocalDateTime.now(), Booking.BookingStatus.APPROVED);

        if (!canComment) {
            log.warn("ItemService.addComment: user has no completed booking userId={}, itemId={}", userId, itemId);
            throw new ValidationException("User has not completed a booking for this item");
        }

        var comment = CommentMapper.toComment(dto, item, author);
        comment = commentRepository.save(comment);

        log.info("ItemService.addComment: created commentId={} for itemId={}", comment.getId(), itemId);
        return CommentMapper.toDto(comment);
    }

    private void fillLastNext(ItemOwnerDto dto, List<Booking> bookings, LocalDateTime now) {
        fillLastNextInternal(bookings, now, (last, next) -> {
            dto.setLastBooking(last);
            dto.setNextBooking(next);
        });
    }

    private void fillLastNext(ItemDetailsDto dto, List<Booking> bookings, LocalDateTime now) {
        fillLastNextInternal(bookings, now, (last, next) -> {
            dto.setLastBooking(last);
            dto.setNextBooking(next);
        });
    }

    private void fillLastNextInternal(List<Booking> bookings,
                                      LocalDateTime now,
                                      BiConsumer<BookingShortDto, BookingShortDto> setter) {

        Booking last = bookings.stream()
                .filter(b -> !b.getEnd().isAfter(now)) // end <= now
                .max(Comparator.comparing(Booking::getEnd))
                .orElse(null);

        Booking next = bookings.stream()
                .filter(b -> b.getStart().isAfter(now)) // start > now
                .min(Comparator.comparing(Booking::getStart))
                .orElse(null);

        BookingShortDto lastDto = (last == null)
                ? null
                : new BookingShortDto(last.getId(), last.getBooker().getId());

        BookingShortDto nextDto = (next == null)
                ? null
                : new BookingShortDto(next.getId(), next.getBooker().getId());

        setter.accept(lastDto, nextDto);
    }
}