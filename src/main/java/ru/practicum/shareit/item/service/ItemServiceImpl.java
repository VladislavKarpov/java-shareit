package ru.practicum.shareit.item.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.*;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.CommentRepository;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;

    public ItemServiceImpl(ItemRepository itemRepository,
                           UserRepository userRepository,
                           BookingRepository bookingRepository,
                           CommentRepository commentRepository) {
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.commentRepository = commentRepository;
    }

    @Override
    @Transactional
    public ItemDto create(Long userId, ItemDto itemDto) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        Item item = ItemMapper.toItem(itemDto, user);
        item = itemRepository.save(item);
        return ItemMapper.toItemDto(item);
    }

    @Override
    @Transactional
    public ItemDto update(Long userId, Long itemId, ItemDto update) {
        Item item = itemRepository.findById(itemId)
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

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found: " + itemId));

        ItemDetailsDto dto = ItemDetailsDto.builder()
                .id(item.getId())
                .name(item.getName())
                .description(item.getDescription())
                .available(item.getAvailable())
                .comments(getComments(itemId))
                .build();

        if (item.getOwner().getId().equals(userId)) {
            fillLastNext(dto, itemId);
        }

        return dto;
    }

    @Override
    public List<ItemOwnerDto> getOwnerItems(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        List<Item> items = itemRepository.findByOwner_Id(userId);

        return items.stream().map(item -> {
            ItemOwnerDto dto = ItemOwnerDto.builder()
                    .id(item.getId())
                    .name(item.getName())
                    .description(item.getDescription())
                    .available(item.getAvailable())
                    .comments(getComments(item.getId()))
                    .build();

            fillLastNext(dto, item.getId());
            return dto;
        }).toList();
    }

    @Override
    public List<ItemDto> search(String text) {
        if (text == null || text.isBlank()) return List.of();
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

        Comment comment = Comment.builder()
                .text(dto.getText())
                .item(item)
                .author(author)
                .created(LocalDateTime.now())
                .build();

        comment = commentRepository.save(comment);

        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .authorName(comment.getAuthor().getName())
                .created(comment.getCreated())
                .build();
    }

    private List<CommentDto> getComments(Long itemId) {
        return commentRepository.findByItem_Id(itemId, Sort.by(Sort.Direction.DESC, "created"))
                .stream()
                .map(c -> CommentDto.builder()
                        .id(c.getId())
                        .text(c.getText())
                        .authorName(c.getAuthor().getName())
                        .created(c.getCreated())
                        .build())
                .toList();
    }

    private void fillLastNext(ItemOwnerDto dto, Long itemId) {
        fillLastNextInternal(itemId, (last, next) -> {
            dto.setLastBooking(last);
            dto.setNextBooking(next);
        });
    }

    private void fillLastNext(ItemDetailsDto dto, Long itemId) {
        fillLastNextInternal(itemId, (last, next) -> {
            dto.setLastBooking(last);
            dto.setNextBooking(next);
        });
    }

    private void fillLastNextInternal(Long itemId, LastNextSetter setter) {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings = bookingRepository.findByItem_IdAndStatus(itemId, Booking.BookingStatus.APPROVED);

        Booking last = bookings.stream()
                .filter(b -> !b.getStart().isAfter(now))
                .max(Comparator.comparing(Booking::getStart))
                .orElse(null);

        Booking next = bookings.stream()
                .filter(b -> b.getStart().isAfter(now))
                .min(Comparator.comparing(Booking::getStart))
                .orElse(null);

        BookingShortDto lastDto = (last == null) ? null : new BookingShortDto(last.getId(), last.getBooker().getId());
        BookingShortDto nextDto = (next == null) ? null : new BookingShortDto(next.getId(), next.getBooker().getId());

        setter.set(lastDto, nextDto);
    }

    @FunctionalInterface
    private interface LastNextSetter {
        void set(BookingShortDto last, BookingShortDto next);
    }
}
