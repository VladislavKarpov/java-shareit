package ru.practicum.shareit.item.service;

import lombok.RequiredArgsConstructor;
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
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {

    private static final Sort COMMENTS_SORT = Sort.by(Sort.Direction.DESC, "created");

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;

    @Override
    @Transactional
    public ItemDto create(Long userId, ItemDto itemDto) {
        var owner = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        var item = ItemMapper.toItem(itemDto, owner);
        item = itemRepository.save(item);

        return ItemMapper.toItemDto(item);
    }

    @Override
    @Transactional
    public ItemDto update(Long userId, Long itemId, ItemDto update) {
        var item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found: " + itemId));

        if (!item.getOwner().getId().equals(userId)) {
            throw new ForbiddenException("User is not owner of this item");
        }

        if (update.getName() != null) item.setName(update.getName());
        if (update.getDescription() != null) item.setDescription(update.getDescription());
        if (update.getAvailable() != null) item.setAvailable(update.getAvailable());

        item = itemRepository.save(item);
        return ItemMapper.toItemDto(item);
    }

    @Override
    public ItemDetailsDto getById(Long userId, Long itemId) {

        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        var item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found: " + itemId));

        var comments = commentRepository.findByItem_Id(itemId, COMMENTS_SORT)
                .stream()
                .map(CommentMapper::toDto)
                .toList();

        var dto = ItemMapper.toDetailsDto(item, comments);

        if (item.getOwner().getId().equals(userId)) {
            LocalDateTime now = LocalDateTime.now();
            List<Booking> bookings = bookingRepository.findByItem_IdAndStatus(itemId, Booking.BookingStatus.APPROVED);
            fillLastNext(dto, bookings, now);
        }

        return dto;
    }

    @Override
    public List<ItemOwnerDto> getOwnerItems(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        List<Item> items = itemRepository.findByOwner_Id(userId);
        if (items.isEmpty()) {
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

        return result;
    }

    @Override
    public List<ItemDto> search(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        return itemRepository.search(text).stream()
                .map(ItemMapper::toItemDto)
                .toList();
    }

    @Override
    @Transactional
    public CommentDto addComment(Long userId, Long itemId, CommentCreateDto dto) {
        var author = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        var item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found: " + itemId));

        boolean canComment = bookingRepository.existsByBooker_IdAndItem_IdAndEndIsBeforeAndStatus(
                userId, itemId, LocalDateTime.now(), Booking.BookingStatus.APPROVED);

        if (!canComment) {
            throw new ValidationException("User has not completed a booking for this item");
        }

        var comment = CommentMapper.toComment(dto, item, author);
        comment = commentRepository.save(comment);

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
