package ru.practicum.shareit.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.exception.EmailAlreadyExistsException;
import ru.practicum.shareit.exception.ErrorHandler;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.service.UserService;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
@Import(ErrorHandler.class)
class UserControllerWebTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper mapper;

    @MockBean
    UserService userService;

    @Test
    void create_ok() throws Exception {
        UserDto req = new UserDto(null, "u", "u@mail.com");
        UserDto resp = new UserDto(1L, "u", "u@mail.com");
        when(userService.create(any(UserDto.class))).thenReturn(resp);

        mvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(userService).create(any(UserDto.class));
        verifyNoMoreInteractions(userService);
    }

    @Test
    void create_validation_returns400_fieldMap() throws Exception {
        UserDto req = new UserDto(null, "", "bad");

        mvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.email").exists());

        verifyNoInteractions(userService);
    }

    @Test
    void create_emailExists_returns409() throws Exception {
        UserDto req = new UserDto(null, "u", "u@mail.com");
        when(userService.create(any(UserDto.class)))
                .thenThrow(new EmailAlreadyExistsException("Email already exists: u@mail.com"));

        mvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Email already exists: u@mail.com"));

        verify(userService).create(any(UserDto.class));
        verifyNoMoreInteractions(userService);
    }

    @Test
    void update_ok() throws Exception {
        UserDto patch = new UserDto(null, "new", null);
        when(userService.update(eq(1L), any(UserDto.class)))
                .thenReturn(new UserDto(1L, "new", "u@mail.com"));

        mvc.perform(patch("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(patch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("new"));

        verify(userService).update(eq(1L), any(UserDto.class));
        verifyNoMoreInteractions(userService);
    }

    @Test
    void getById_ok() throws Exception {
        when(userService.getById(1L)).thenReturn(new UserDto(1L, "u", "u@mail.com"));

        mvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("u@mail.com"));

        verify(userService).getById(1L);
        verifyNoMoreInteractions(userService);
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(userService.getById(1L)).thenThrow(new NotFoundException("User not found: 1"));

        mvc.perform(get("/users/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found: 1"));

        verify(userService).getById(1L);
        verifyNoMoreInteractions(userService);
    }

    @Test
    void getAll_ok() throws Exception {
        when(userService.getAll()).thenReturn(List.of(new UserDto(1L, "u", "u@mail.com")));

        mvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));

        verify(userService).getAll();
        verifyNoMoreInteractions(userService);
    }

    @Test
    void delete_noContent() throws Exception {
        mvc.perform(delete("/users/1"))
                .andExpect(status().isNoContent());

        verify(userService).delete(1L);
        verifyNoMoreInteractions(userService);
    }
}