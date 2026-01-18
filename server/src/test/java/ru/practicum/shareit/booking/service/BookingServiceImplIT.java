package ru.practicum.shareit.booking.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.practicum.shareit.IntegrationTestBase;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingState;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.service.UserService;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class BookingServiceImplIT extends IntegrationTestBase {

    @Autowired
    BookingService bookingService;
    @Autowired
    UserService userService;
    @Autowired
    ItemService itemService;

    @Test
    void create_persistsWaitingBooking_andReturnsDtoWithItemAndBooker() {
        UserDto owner = userService.create(new UserDto(null, "owner", "owner@mail.com"));
        UserDto booker = userService.create(new UserDto(null, "booker", "booker@mail.com"));

        ItemDto item = itemService.create(owner.getId(),
                new ItemDto(null, "item", "desc", true, null, null));

        BookingCreateDto dto = new BookingCreateDto();
        dto.setItemId(item.getId());
        dto.setStart(LocalDateTime.now().plusDays(1));
        dto.setEnd(LocalDateTime.now().plusDays(2));

        BookingDto booking = bookingService.create(booker.getId(), dto);

        assertThat(booking.getId()).isNotNull();
        assertThat(booking.getStatus()).isEqualTo(Booking.BookingStatus.WAITING);
        assertThat(booking.getItem()).isNotNull();
        assertThat(booking.getItem().getId()).isEqualTo(item.getId());
        assertThat(booking.getBooker()).isNotNull();
        assertThat(booking.getBooker().getId()).isEqualTo(booker.getId());
    }

    @Test
    void create_throwsIfItemNotAvailable() {
        UserDto owner = userService.create(new UserDto(null, "owner2", "owner2@mail.com"));
        UserDto booker = userService.create(new UserDto(null, "booker2", "booker2@mail.com"));

        ItemDto item = itemService.create(owner.getId(),
                new ItemDto(null, "item2", "desc2", false, null, null));

        BookingCreateDto dto = new BookingCreateDto();
        dto.setItemId(item.getId());
        dto.setStart(LocalDateTime.now().plusDays(1));
        dto.setEnd(LocalDateTime.now().plusDays(2));

        assertThatThrownBy(() -> bookingService.create(booker.getId(), dto))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("not available");
    }

    @Test
    void create_throwsIfOwnerBooksOwnItem() {
        UserDto owner = userService.create(new UserDto(null, "owner3", "owner3@mail.com"));
        ItemDto item = itemService.create(owner.getId(),
                new ItemDto(null, "item3", "desc3", true, null, null));

        BookingCreateDto dto = new BookingCreateDto();
        dto.setItemId(item.getId());
        dto.setStart(LocalDateTime.now().plusDays(1));
        dto.setEnd(LocalDateTime.now().plusDays(2));

        assertThatThrownBy(() -> bookingService.create(owner.getId(), dto))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Owner cannot book own item");
    }

    @Test
    void create_throwsIfEndNotAfterStart() {
        UserDto owner = userService.create(new UserDto(null, "owner4", "owner4@mail.com"));
        UserDto booker = userService.create(new UserDto(null, "booker4", "booker4@mail.com"));
        ItemDto item = itemService.create(owner.getId(),
                new ItemDto(null, "item4", "desc4", true, null, null));

        LocalDateTime start = LocalDateTime.now().plusDays(2);

        BookingCreateDto dto = new BookingCreateDto();
        dto.setItemId(item.getId());
        dto.setStart(start);
        dto.setEnd(start); // строго равно -> end.isAfter(start) == false

        assertThatThrownBy(() -> bookingService.create(booker.getId(), dto))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("end must be after start");
    }

    @Test
    void create_throwsIfOverlapsWithApprovedBooking() {
        UserDto owner = userService.create(new UserDto(null, "owner5", "owner5@mail.com"));
        UserDto booker1 = userService.create(new UserDto(null, "booker5a", "booker5a@mail.com"));
        UserDto booker2 = userService.create(new UserDto(null, "booker5b", "booker5b@mail.com"));

        ItemDto item = itemService.create(owner.getId(),
                new ItemDto(null, "item5", "desc5", true, null, null));

        // первая бронь -> approve
        BookingCreateDto b1 = new BookingCreateDto();
        b1.setItemId(item.getId());
        b1.setStart(LocalDateTime.now().plusDays(10));
        b1.setEnd(LocalDateTime.now().plusDays(12));
        BookingDto created1 = bookingService.create(booker1.getId(), b1);
        bookingService.approve(owner.getId(), created1.getId(), true);

        // пересекается по времени
        BookingCreateDto b2 = new BookingCreateDto();
        b2.setItemId(item.getId());
        b2.setStart(LocalDateTime.now().plusDays(11));
        b2.setEnd(LocalDateTime.now().plusDays(13));

        assertThatThrownBy(() -> bookingService.create(booker2.getId(), b2))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("overlaps");
    }

    @Test
    void approve_onlyOwnerCanApprove_andOnlyWaiting_andNotStarted() {
        UserDto owner = userService.create(new UserDto(null, "owner6", "owner6@mail.com"));
        UserDto booker = userService.create(new UserDto(null, "booker6", "booker6@mail.com"));
        UserDto stranger = userService.create(new UserDto(null, "stranger6", "stranger6@mail.com"));

        ItemDto item = itemService.create(owner.getId(),
                new ItemDto(null, "item6", "desc6", true, null, null));

        BookingCreateDto dto = new BookingCreateDto();
        dto.setItemId(item.getId());
        dto.setStart(LocalDateTime.now().plusDays(1));
        dto.setEnd(LocalDateTime.now().plusDays(2));
        BookingDto created = bookingService.create(booker.getId(), dto);

        // чужой не может
        assertThatThrownBy(() -> bookingService.approve(stranger.getId(), created.getId(), true))
                .isInstanceOf(ForbiddenException.class);

        // owner может
        BookingDto approved = bookingService.approve(owner.getId(), created.getId(), true);
        assertThat(approved.getStatus()).isEqualTo(Booking.BookingStatus.APPROVED);

        // повторно нельзя
        assertThatThrownBy(() -> bookingService.approve(owner.getId(), created.getId(), true))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("already decided");
    }

    @Test
    void getById_allowsOnlyBookerOrOwner() {
        UserDto owner = userService.create(new UserDto(null, "owner7", "owner7@mail.com"));
        UserDto booker = userService.create(new UserDto(null, "booker7", "booker7@mail.com"));
        UserDto other = userService.create(new UserDto(null, "other7", "other7@mail.com"));

        ItemDto item = itemService.create(owner.getId(),
                new ItemDto(null, "item7", "desc7", true, null, null));

        BookingCreateDto dto = new BookingCreateDto();
        dto.setItemId(item.getId());
        dto.setStart(LocalDateTime.now().plusDays(1));
        dto.setEnd(LocalDateTime.now().plusDays(2));
        BookingDto created = bookingService.create(booker.getId(), dto);

        assertThat(bookingService.getById(booker.getId(), created.getId()).getId()).isEqualTo(created.getId());
        assertThat(bookingService.getById(owner.getId(), created.getId()).getId()).isEqualTo(created.getId());

        assertThatThrownBy(() -> bookingService.getById(other.getId(), created.getId()))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void getUserBookings_stateAll_waiting_rejected_work() {
        UserDto owner = userService.create(new UserDto(null, "owner8", "owner8@mail.com"));
        UserDto booker = userService.create(new UserDto(null, "booker8", "booker8@mail.com"));

        ItemDto item = itemService.create(owner.getId(),
                new ItemDto(null, "item8", "desc8", true, null, null));

        BookingCreateDto b1 = new BookingCreateDto();
        b1.setItemId(item.getId());
        b1.setStart(LocalDateTime.now().plusDays(5));
        b1.setEnd(LocalDateTime.now().plusDays(6));
        BookingDto waiting = bookingService.create(booker.getId(), b1);

        BookingDto rejected = bookingService.approve(owner.getId(), waiting.getId(), false);

        List<BookingDto> all = bookingService.getUserBookings(booker.getId(), BookingState.ALL);
        assertThat(all).extracting(BookingDto::getId).contains(waiting.getId());

        List<BookingDto> onlyRejected = bookingService.getUserBookings(booker.getId(), BookingState.REJECTED);
        assertThat(onlyRejected).hasSize(1);
        assertThat(onlyRejected.get(0).getStatus()).isEqualTo(Booking.BookingStatus.REJECTED);

        List<BookingDto> onlyWaiting = bookingService.getUserBookings(booker.getId(), BookingState.WAITING);
        assertThat(onlyWaiting).isEmpty();
    }
}