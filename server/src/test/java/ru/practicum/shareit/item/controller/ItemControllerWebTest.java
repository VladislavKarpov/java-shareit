package ru.practicum.shareit.item.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.exception.ErrorHandler;
import ru.practicum.shareit.item.dto.*;
import ru.practicum.shareit.item.service.ItemService;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(ErrorHandler.class)
@WebMvcTest(controllers = ItemController.class)
public class ItemControllerWebTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private ItemService service;

    private static final String HEADER_USER = ItemController.HEADER_USER;

    @Test
    public void create_ok() throws Exception {
        ItemDto req = ItemDto.builder()
                .name("n")
                .description("d")
                .available(true)
                .build();

        when(service.create(eq(1L), any(ItemDto.class)))
                .thenReturn(ItemDto.builder().id(10L).name("n").description("d").available(true).build());

        mvc.perform(post("/items")
                        .header(HEADER_USER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));

        verify(service).create(eq(1L), any(ItemDto.class));
        verifyNoMoreInteractions(service);
    }

    @Test
    public void create_validation_returns400_fieldMap() throws Exception {
        ItemDto req = ItemDto.builder()
                .name(" ")
                .description("")
                .build();

        mvc.perform(post("/items")
                        .header(HEADER_USER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.description").exists())
                .andExpect(jsonPath("$.available").exists());

        verifyNoInteractions(service);
    }

    @Test
    public void addComment_ok() throws Exception {
        CommentCreateDto req = new CommentCreateDto();
        req.setText("hi");

        when(service.addComment(eq(1L), eq(10L), any(CommentCreateDto.class)))
                .thenReturn(CommentDto.builder().id(5L).text("hi").authorName("u").build());

        mvc.perform(post("/items/10/comment")
                        .header(HEADER_USER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5L))
                .andExpect(jsonPath("$.text").value("hi"));

        verify(service).addComment(eq(1L), eq(10L), any(CommentCreateDto.class));
        verifyNoMoreInteractions(service);
    }

    @Test
    public void addComment_validation_returns400_fieldMap() throws Exception {
        CommentCreateDto req = new CommentCreateDto();

        mvc.perform(post("/items/10/comment")
                        .header(HEADER_USER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.text").exists());

        verifyNoInteractions(service);
    }

    @Test
    public void update_ok() throws Exception {
        ItemDto patch = ItemDto.builder().name("new").build();

        when(service.update(eq(1L), eq(10L), any(ItemDto.class)))
                .thenReturn(ItemDto.builder().id(10L).name("new").description("d").available(true).build());

        mvc.perform(patch("/items/10")
                        .header(HEADER_USER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(patch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("new"));

        verify(service).update(eq(1L), eq(10L), any(ItemDto.class));
        verifyNoMoreInteractions(service);
    }

    @Test
    public void getById_ok() throws Exception {
        when(service.getById(1L, 10L)).thenReturn(ItemDetailsDto.builder().id(10L).name("n").build());

        mvc.perform(get("/items/10").header(HEADER_USER, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));

        verify(service).getById(1L, 10L);
        verifyNoMoreInteractions(service);
    }

    @Test
    public void getOwnerItems_ok() throws Exception {
        when(service.getOwnerItems(1L)).thenReturn(List.of());

        mvc.perform(get("/items").header(HEADER_USER, 1L))
                .andExpect(status().isOk());

        verify(service).getOwnerItems(1L);
        verifyNoMoreInteractions(service);
    }

    @Test
    public void search_ok() throws Exception {
        when(service.search("q")).thenReturn(List.of());

        mvc.perform(get("/items/search").param("text", "q"))
                .andExpect(status().isOk());

        verify(service).search("q");
        verifyNoMoreInteractions(service);
    }
}