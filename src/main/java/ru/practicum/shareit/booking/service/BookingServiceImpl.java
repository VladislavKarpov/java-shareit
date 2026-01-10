package ru.practicum.shareit.booking.service;

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
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    public BookingServiceImpl(BookingRepository bookingRepository,
                              UserRepository userRepository,
                              ItemRepository itemRepository) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
    }

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

        if (dto.getStart() == null || dto.getEnd() == null || !dto.getEnd().isAfter(dto.getStart())) {
            throw new ValidationException("Invalid booking time");
        }

        Booking booking = Booking.builder()
                .start(dto.getStart())
                .end(dto.getEnd())
                .item(item)
                .booker(booker)
                .status(Booking.BookingStatus.WAITING)
                .build();

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
    public List<BookingDto> getUserBookings(Long userId, BookingState state) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        Sort sort = Sort.by(Sort.Direction.DESC, "start");
        LocalDateTime now = LocalDateTime.now();

        List<Booking> bookings = switch (state) {
            case ALL -> bookingRepository.findByBooker_Id(userId, sort);
            case CURRENT -> bookingRepository.findByBooker_IdAndStartIsBeforeAndEndIsAfter(userId, now, now, sort);
            case PAST -> bookingRepository.findByBooker_IdAndEndIsBefore(userId, now, sort);
            case FUTURE -> bookingRepository.findByBooker_IdAndStartIsAfter(userId, now, sort);
            case WAITING -> bookingRepository.findByBooker_IdAndStatus(userId, Booking.BookingStatus.WAITING, sort);
            case REJECTED -> bookingRepository.findByBooker_IdAndStatus(userId, Booking.BookingStatus.REJECTED, sort);
        };

        return bookings.stream().map(BookingMapper::toDto).toList();
    }

    @Override
    public List<BookingDto> getOwnerBookings(Long ownerId, BookingState state) {
        userRepository.findById(ownerId)
                .orElseThrow(() -> new NotFoundException("User not found: " + ownerId));

        Sort sort = Sort.by(Sort.Direction.DESC, "start");
        LocalDateTime now = LocalDateTime.now();

        List<Booking> bookings = switch (state) {
            case ALL -> bookingRepository.findByItem_Owner_Id(ownerId, sort);
            case CURRENT -> bookingRepository.findByItem_Owner_IdAndStartIsBeforeAndEndIsAfter(ownerId, now, now, sort);
            case PAST -> bookingRepository.findByItem_Owner_IdAndEndIsBefore(ownerId, now, sort);
            case FUTURE -> bookingRepository.findByItem_Owner_IdAndStartIsAfter(ownerId, now, sort);
            case WAITING ->
                    bookingRepository.findByItem_Owner_IdAndStatus(ownerId, Booking.BookingStatus.WAITING, sort);
            case REJECTED ->
                    bookingRepository.findByItem_Owner_IdAndStatus(ownerId, Booking.BookingStatus.REJECTED, sort);
        };

        return bookings.stream().map(BookingMapper::toDto).toList();
    }
}
