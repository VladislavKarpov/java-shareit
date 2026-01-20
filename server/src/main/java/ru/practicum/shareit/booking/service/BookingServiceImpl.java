package ru.practicum.shareit.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingState;
import ru.practicum.shareit.booking.mapper.BookingMapper;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    @Override
    @Transactional
    public BookingDto create(Long userId, BookingCreateDto dto) {
        log.info("BookingService.create: userId={}, itemId={}, start={}, end={}",
                userId, dto.getItemId(), dto.getStart(), dto.getEnd());

        var booker = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("BookingService.create: user not found userId={}", userId);
                    return new NotFoundException("User not found: " + userId);
                });

        var item = itemRepository.findById(dto.getItemId())
                .orElseThrow(() -> {
                    log.warn("BookingService.create: item not found itemId={}", dto.getItemId());
                    return new NotFoundException("Item not found: " + dto.getItemId());
                });

        if (!Boolean.TRUE.equals(item.getAvailable())) {
            log.warn("BookingService.create: item not available itemId={}", item.getId());
            throw new ValidationException("Item is not available");
        }
        if (item.getOwner().getId().equals(userId)) {
            log.warn("BookingService.create: owner tried to book own item userId={}, itemId={}", userId, item.getId());
            throw new NotFoundException("Owner cannot book own item");
        }
        if (!dto.getEnd().isAfter(dto.getStart())) {
            log.warn("BookingService.create: invalid time start={}, end={}", dto.getStart(), dto.getEnd());
            throw new ValidationException("Invalid booking time: end must be after start");
        }

        boolean overlap = bookingRepository.existsOverlap(
                item.getId(),
                Booking.BookingStatus.APPROVED,
                dto.getStart(),
                dto.getEnd()
        );
        if (overlap) {
            log.warn("BookingService.create: overlap detected itemId={}, start={}, end={}",
                    item.getId(), dto.getStart(), dto.getEnd());
            throw new ValidationException("Booking time overlaps with existing approved booking");
        }

        Booking booking = BookingMapper.fromCreateDto(dto, item, booker);
        booking = bookingRepository.save(booking);

        log.info("BookingService.create: created bookingId={}, status={}", booking.getId(), booking.getStatus());
        return BookingMapper.toDto(booking);
    }

    @Override
    @Transactional
    public BookingDto approve(Long ownerId, Long bookingId, boolean approved) {
        log.info("BookingService.approve: ownerId={}, bookingId={}, approved={}", ownerId, bookingId, approved);

        var booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> {
                    log.warn("BookingService.approve: booking not found bookingId={}", bookingId);
                    return new NotFoundException("Booking not found: " + bookingId);
                });

        if (!booking.getItem().getOwner().getId().equals(ownerId)) {
            log.warn("BookingService.approve: forbidden ownerId={}, bookingId={}, realOwnerId={}",
                    ownerId, bookingId, booking.getItem().getOwner().getId());
            throw new ForbiddenException("Only owner can approve/reject booking");
        }
        if (booking.getStatus() != Booking.BookingStatus.WAITING) {
            log.warn("BookingService.approve: status already decided bookingId={}, status={}",
                    bookingId, booking.getStatus());
            throw new ValidationException("Booking status already decided");
        }

        booking.setStatus(approved ? Booking.BookingStatus.APPROVED : Booking.BookingStatus.REJECTED);
        booking = bookingRepository.save(booking);

        log.info("BookingService.approve: updated bookingId={}, status={}", booking.getId(), booking.getStatus());
        return BookingMapper.toDto(booking);
    }

    @Override
    public BookingDto getById(Long userId, Long bookingId) {
        log.info("BookingService.getById: userId={}, bookingId={}", userId, bookingId);

        var booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> {
                    log.warn("BookingService.getById: booking not found bookingId={}", bookingId);
                    return new NotFoundException("Booking not found: " + bookingId);
                });

        boolean isBooker = booking.getBooker().getId().equals(userId);
        boolean isOwner = booking.getItem().getOwner().getId().equals(userId);

        if (!isBooker && !isOwner) {
            log.warn("BookingService.getById: access denied userId={}, bookingId={}, bookerId={}, ownerId={}",
                    userId, bookingId, booking.getBooker().getId(), booking.getItem().getOwner().getId());
            throw new NotFoundException("Access denied");
        }

        log.info("BookingService.getById: success bookingId={}", bookingId);
        return BookingMapper.toDto(booking);
    }

    @Override
    public List<BookingDto> getUserBookings(Long userId, BookingState state, int from, int size) {
        log.info("BookingService.getUserBookings: userId={}, state={}, from={}, size={}", userId, state, from, size);

        userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("BookingService.getUserBookings: user not found userId={}", userId);
                    return new NotFoundException("User not found: " + userId);
                });

        int safeFrom = Math.max(from, 0);
        int safeSize = Math.max(size, 1);

        PageRequest page = PageRequest.of(
                safeFrom / safeSize,
                safeSize,
                Sort.by(Sort.Direction.DESC, "start")
        );

        LocalDateTime now = LocalDateTime.now();

        var bookingsPage = switch (state) {
            case ALL -> bookingRepository.findByBooker_Id(userId, page);
            case CURRENT -> bookingRepository.findByBooker_IdAndStartIsBeforeAndEndIsAfter(userId, now, now, page);
            case PAST -> bookingRepository.findByBooker_IdAndEndIsBefore(userId, now, page);
            case FUTURE -> bookingRepository.findByBooker_IdAndStartIsAfter(userId, now, page);
            case WAITING -> bookingRepository.findByBooker_IdAndStatus(userId, Booking.BookingStatus.WAITING, page);
            case REJECTED -> bookingRepository.findByBooker_IdAndStatus(userId, Booking.BookingStatus.REJECTED, page);
        };

        var result = bookingsPage.getContent().stream().map(BookingMapper::toDto).toList();
        log.info("BookingService.getUserBookings: resultSize={}", result.size());
        return result;
    }

    @Override
    public List<BookingDto> getOwnerBookings(Long ownerId, BookingState state, int from, int size) {
        log.info("BookingService.getOwnerBookings: ownerId={}, state={}, from={}, size={}", ownerId, state, from, size);

        userRepository.findById(ownerId)
                .orElseThrow(() -> {
                    log.warn("BookingService.getOwnerBookings: user not found ownerId={}", ownerId);
                    return new NotFoundException("User not found: " + ownerId);
                });

        int safeFrom = Math.max(from, 0);
        int safeSize = Math.max(size, 1);

        PageRequest page = PageRequest.of(
                safeFrom / safeSize,
                safeSize,
                Sort.by(Sort.Direction.DESC, "start")
        );

        LocalDateTime now = LocalDateTime.now();

        var bookingsPage = switch (state) {
            case ALL -> bookingRepository.findByItem_Owner_Id(ownerId, page);
            case CURRENT -> bookingRepository.findByItem_Owner_IdAndStartIsBeforeAndEndIsAfter(ownerId, now, now, page);
            case PAST -> bookingRepository.findByItem_Owner_IdAndEndIsBefore(ownerId, now, page);
            case FUTURE -> bookingRepository.findByItem_Owner_IdAndStartIsAfter(ownerId, now, page);
            case WAITING ->
                    bookingRepository.findByItem_Owner_IdAndStatus(ownerId, Booking.BookingStatus.WAITING, page);
            case REJECTED ->
                    bookingRepository.findByItem_Owner_IdAndStatus(ownerId, Booking.BookingStatus.REJECTED, page);
        };

        var result = bookingsPage.getContent().stream().map(BookingMapper::toDto).toList();
        log.info("BookingService.getOwnerBookings: resultSize={}", result.size());
        return result;
    }
}