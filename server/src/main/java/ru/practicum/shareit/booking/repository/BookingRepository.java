package ru.practicum.shareit.booking.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.shareit.booking.model.Booking;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Page<Booking> findByBooker_Id(Long bookerId, Pageable pageable);

    Page<Booking> findByBooker_IdAndStartIsAfter(Long bookerId, LocalDateTime start, Pageable pageable);

    Page<Booking> findByBooker_IdAndEndIsBefore(Long bookerId, LocalDateTime end, Pageable pageable);

    Page<Booking> findByBooker_IdAndStartIsBeforeAndEndIsAfter(
            Long bookerId, LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<Booking> findByBooker_IdAndStatus(Long bookerId, Booking.BookingStatus status, Pageable pageable);

    Page<Booking> findByItem_Owner_Id(Long ownerId, Pageable pageable);

    Page<Booking> findByItem_Owner_IdAndStartIsAfter(Long ownerId, LocalDateTime start, Pageable pageable);

    Page<Booking> findByItem_Owner_IdAndEndIsBefore(Long ownerId, LocalDateTime end, Pageable pageable);

    Page<Booking> findByItem_Owner_IdAndStartIsBeforeAndEndIsAfter(
            Long ownerId, LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<Booking> findByItem_Owner_IdAndStatus(Long ownerId, Booking.BookingStatus status, Pageable pageable);

    List<Booking> findByItem_IdAndStatus(Long itemId, Booking.BookingStatus status);

    List<Booking> findByItem_IdInAndStatus(Collection<Long> itemIds, Booking.BookingStatus status);

    boolean existsByBooker_IdAndItem_IdAndEndIsBeforeAndStatus(
            Long bookerId, Long itemId, LocalDateTime time, Booking.BookingStatus status);

    List<Booking> findByBooker_Id(Long bookerId, Sort sort);

    List<Booking> findByItem_Owner_Id(Long ownerId, Sort sort);

    @Query("""
            select count(b) > 0
            from Booking b
            where b.item.id = :itemId
              and b.status = :status
              and b.start < :end
              and b.end > :start
            """)
    boolean existsOverlap(@Param("itemId") Long itemId,
                          @Param("status") Booking.BookingStatus status,
                          @Param("start") LocalDateTime start,
                          @Param("end") LocalDateTime end);
}