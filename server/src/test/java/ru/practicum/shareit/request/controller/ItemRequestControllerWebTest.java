package ru.practicum.shareit.request.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.exception.ErrorHandler;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.service.ItemRequestService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(ErrorHandler.class)
@WebMvcTest(controllers = ItemRequestController.class)
public class ItemRequestControllerWebTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private ItemRequestService service;

    private static final String HEADER_USER = ItemRequestController.HEADER_USER;

    @Test
    public void create_ok() throws Exception {
        ItemRequestCreateDto req = new ItemRequestCreateDto();
        req.setDescription("need");

        ItemRequestDto resp = ItemRequestDto.builder()
                .id(1L)
                .description("need")
                .created(LocalDateTime.now())
                .items(List.of())
                .build();

        when(service.create(eq(10L), any(ItemRequestCreateDto.class))).thenReturn(resp);

        mvc.perform(post("/requests")
                        .header(HEADER_USER, 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(service).create(eq(10L), any(ItemRequestCreateDto.class));
        verifyNoMoreInteractions(service);
    }

    @Test
    public void create_validation_returns400_fieldMap() throws Exception {
        ItemRequestCreateDto req = new ItemRequestCreateDto();

        mvc.perform(post("/requests")
                        .header(HEADER_USER, 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.description").exists());

        verifyNoInteractions(service);
    }

    @Test
    public void getOwn_ok() throws Exception {
        when(service.getOwn(10L)).thenReturn(List.of());

        mvc.perform(get("/requests").header(HEADER_USER, 10L))
                .andExpect(status().isOk());

        verify(service).getOwn(10L);
        verifyNoMoreInteractions(service);
    }

    @Test
    public void getAll_ok_withFromSize() throws Exception {
        when(service.getAllOthers(10L, 0, 10)).thenReturn(List.of());

        mvc.perform(get("/requests/all")
                        .header(HEADER_USER, 10L)
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk());

        verify(service).getAllOthers(10L, 0, 10);
        verifyNoMoreInteractions(service);
    }

    @Test
    public void getById_ok() throws Exception {
        when(service.getById(10L, 5L)).thenReturn(ItemRequestDto.builder().id(5L).build());

        mvc.perform(get("/requests/5").header(HEADER_USER, 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5L));

        verify(service).getById(10L, 5L);
        verifyNoMoreInteractions(service);
    }
}