package ru.practicum.shareit.booking.service;

import lombok.RequiredArgsConstructor;
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
        var booker = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        var item = itemRepository.findById(dto.getItemId())
                .orElseThrow(() -> new NotFoundException("Item not found: " + dto.getItemId()));

        if (!Boolean.TRUE.equals(item.getAvailable())) {
            throw new ValidationException("Item is not available");
        }
        if (item.getOwner().getId().equals(userId)) {
            throw new NotFoundException("Owner cannot book own item");
        }
        if (!dto.getEnd().isAfter(dto.getStart())) {
            throw new ValidationException("Invalid booking time: end must be after start");
        }

        boolean overlap = bookingRepository.existsOverlap(
                item.getId(),
                Booking.BookingStatus.APPROVED,
                dto.getStart(),
                dto.getEnd()
        );
        if (overlap) {
            throw new ValidationException("Booking time overlaps with existing approved booking");
        }

        Booking booking = BookingMapper.fromCreateDto(dto, item, booker);
        booking = bookingRepository.save(booking);

        return BookingMapper.toDto(booking);
    }

    @Override
    @Transactional
    public BookingDto approve(Long ownerId, Long bookingId, boolean approved) {
        var booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));

        if (!booking.getItem().getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException("Only owner can approve/reject booking");
        }
        if (booking.getStatus() != Booking.BookingStatus.WAITING) {
            throw new ValidationException("Booking status already decided");
        }

        booking.setStatus(approved ? Booking.BookingStatus.APPROVED : Booking.BookingStatus.REJECTED);
        booking = bookingRepository.save(booking);

        return BookingMapper.toDto(booking);
    }

    @Override
    public BookingDto getById(Long userId, Long bookingId) {
        var booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));

        boolean isBooker = booking.getBooker().getId().equals(userId);
        boolean isOwner = booking.getItem().getOwner().getId().equals(userId);

        if (!isBooker && !isOwner) {
            throw new NotFoundException("Access denied");
        }

        return BookingMapper.toDto(booking);
    }

    @Override
    public List<BookingDto> getUserBookings(Long userId, BookingState state, int from, int size) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

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

        return bookingsPage.getContent().stream().map(BookingMapper::toDto).toList();
    }

    @Override
    public List<BookingDto> getOwnerBookings(Long ownerId, BookingState state, int from, int size) {
        userRepository.findById(ownerId)
                .orElseThrow(() -> new NotFoundException("User not found: " + ownerId));

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

        return bookingsPage.getContent().stream().map(BookingMapper::toDto).toList();
    }
}