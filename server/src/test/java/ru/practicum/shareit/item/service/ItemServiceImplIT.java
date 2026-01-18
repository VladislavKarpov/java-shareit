package ru.practicum.shareit.item.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.practicum.shareit.IntegrationTestBase;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.service.UserService;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class ItemServiceImplIT extends IntegrationTestBase {

    @Autowired
    ItemService itemService;
    @Autowired
    UserService userService;
    @Autowired
    BookingService bookingService;
    @Autowired
    BookingRepository bookingRepository;

    @Test
    void addComment_allowedOnlyAfterFinishedApprovedBooking() {
        UserDto owner = userService.create(new UserDto(null, "owner2", "owner2@mail.com"));
        UserDto booker = userService.create(new UserDto(null, "booker2", "booker2@mail.com"));

        ItemDto item = itemService.create(owner.getId(),
                new ItemDto(null, "item", "desc", true, null, null));

        CommentCreateDto comment = new CommentCreateDto();
        comment.setText("nice!");

        assertThatThrownBy(() -> itemService.addComment(booker.getId(), item.getId(), comment))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("has not completed");

        LocalDateTime base = LocalDateTime.now().plusDays(1);
        BookingCreateDto booking = new BookingCreateDto();
        booking.setItemId(item.getId());
        booking.setStart(base);
        booking.setEnd(base.plusDays(1));

        var created = bookingService.create(booker.getId(), booking);

        bookingService.approve(owner.getId(), created.getId(), true);

        var entity = bookingRepository.findById(created.getId()).orElseThrow();
        entity.setStart(LocalDateTime.now().minusDays(3));
        entity.setEnd(LocalDateTime.now().minusDays(2));
        bookingRepository.save(entity);

        var saved = itemService.addComment(booker.getId(), item.getId(), comment);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getText()).isEqualTo("nice!");
        assertThat(saved.getAuthorName()).isEqualTo(booker.getName());
        assertThat(saved.getCreated()).isNotNull();
    }
}