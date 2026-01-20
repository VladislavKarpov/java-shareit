package ru.practicum.shareit.request.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.practicum.shareit.IntegrationTestBase;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.service.UserService;

import static org.assertj.core.api.Assertions.assertThat;

public class ItemRequestServiceImplIT extends IntegrationTestBase {

    @Autowired
    private ItemRequestService requestService;

    @Autowired
    private UserService userService;

    @Test
    public void getAllOthers_fromAndSize_workAsOffsetPagination() {
        UserDto u1 = userService.create(new UserDto(null, "u1", "u1@mail.com"));
        UserDto u2 = userService.create(new UserDto(null, "u2", "u2@mail.com"));

        for (int i = 1; i <= 3; i++) {
            ItemRequestCreateDto dto = new ItemRequestCreateDto();
            dto.setDescription("r" + i);
            requestService.create(u2.getId(), dto);
        }

        var page1 = requestService.getAllOthers(u1.getId(), 0, 2);
        var page2 = requestService.getAllOthers(u1.getId(), 2, 2);

        assertThat(page1).hasSize(2);
        assertThat(page2).hasSize(1);
    }
}