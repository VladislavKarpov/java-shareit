package ru.practicum.shareit.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingState;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.exception.ErrorHandler;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(ErrorHandler.class)
@WebMvcTest(controllers = BookingController.class)
public class BookingControllerWebTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private BookingService bookingService;

    private static final String HEADER_USER = BookingController.HEADER_USER;

    @Test
    public void create_ok() throws Exception {
        BookingCreateDto req = new BookingCreateDto();
        req.setItemId(1L);
        req.setStart(LocalDateTime.now().plusDays(1));
        req.setEnd(LocalDateTime.now().plusDays(2));

        BookingDto resp = BookingDto.builder()
                .id(10L)
                .status(Booking.BookingStatus.WAITING)
                .build();

        when(bookingService.create(eq(2L), any(BookingCreateDto.class))).thenReturn(resp);

        mvc.perform(post("/bookings")
                        .header(HEADER_USER, 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.status").value("WAITING"));

        ArgumentCaptor<BookingCreateDto> captor = ArgumentCaptor.forClass(BookingCreateDto.class);
        verify(bookingService).create(eq(2L), captor.capture());
        assertThat(captor.getValue().getItemId()).isEqualTo(1L);
        verifyNoMoreInteractions(bookingService);
    }

    @Test
    public void create_validationError_returns400_withFieldMap() throws Exception {
        BookingCreateDto req = new BookingCreateDto();

        mvc.perform(post("/bookings")
                        .header(HEADER_USER, 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.itemId").exists())
                .andExpect(jsonPath("$.start").exists())
                .andExpect(jsonPath("$.end").exists());

        verifyNoInteractions(bookingService);
    }

    @Test
    public void approve_ok() throws Exception {
        when(bookingService.approve(5L, 10L, true))
                .thenReturn(BookingDto.builder().id(10L).status(Booking.BookingStatus.APPROVED).build());

        mvc.perform(patch("/bookings/10")
                        .header(HEADER_USER, 5L)
                        .param("approved", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        verify(bookingService).approve(5L, 10L, true);
        verifyNoMoreInteractions(bookingService);
    }

    @Test
    public void getById_ok() throws Exception {
        when(bookingService.getById(7L, 10L)).thenReturn(BookingDto.builder().id(10L).build());

        mvc.perform(get("/bookings/10").header(HEADER_USER, 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));

        verify(bookingService).getById(7L, 10L);
        verifyNoMoreInteractions(bookingService);
    }

    @Test
    public void getById_notFound_returns404() throws Exception {
        when(bookingService.getById(7L, 10L)).thenThrow(new NotFoundException("Booking not found"));

        mvc.perform(get("/bookings/10").header(HEADER_USER, 7L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Booking not found"));

        verify(bookingService).getById(7L, 10L);
        verifyNoMoreInteractions(bookingService);
    }

    @Test
    public void getUserBookings_ok_defaultStateALL_defaultPaging() throws Exception {
        when(bookingService.getUserBookings(7L, BookingState.ALL, 0, 10)).thenReturn(List.of());

        mvc.perform(get("/bookings").header(HEADER_USER, 7L))
                .andExpect(status().isOk());

        verify(bookingService).getUserBookings(7L, BookingState.ALL, 0, 10);
        verifyNoMoreInteractions(bookingService);
    }

    @Test
    public void getUserBookings_ok_customPaging() throws Exception {
        when(bookingService.getUserBookings(7L, BookingState.FUTURE, 20, 5)).thenReturn(List.of());

        mvc.perform(get("/bookings")
                        .header(HEADER_USER, 7L)
                        .param("state", "FUTURE")
                        .param("from", "20")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(bookingService).getUserBookings(7L, BookingState.FUTURE, 20, 5);
        verifyNoMoreInteractions(bookingService);
    }

    @Test
    public void getUserBookings_unknownState_returns400() throws Exception {
        mvc.perform(get("/bookings")
                        .header(HEADER_USER, 7L)
                        .param("state", "WTF"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unknown state: WTF"));

        verifyNoInteractions(bookingService);
    }

    @Test
    public void getOwnerBookings_ok_defaultPaging() throws Exception {
        when(bookingService.getOwnerBookings(7L, BookingState.WAITING, 0, 10)).thenReturn(List.of());

        mvc.perform(get("/bookings/owner")
                        .header(HEADER_USER, 7L)
                        .param("state", "WAITING"))
                .andExpect(status().isOk());

        verify(bookingService).getOwnerBookings(7L, BookingState.WAITING, 0, 10);
        verifyNoMoreInteractions(bookingService);
    }

    @Test
    public void getOwnerBookings_serviceValidation_returns400() throws Exception {
        when(bookingService.getOwnerBookings(7L, BookingState.ALL, 0, 10))
                .thenThrow(new ValidationException("bad"));

        mvc.perform(get("/bookings/owner")
                        .header(HEADER_USER, 7L)
                        .param("state", "ALL"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad"));

        verify(bookingService).getOwnerBookings(7L, BookingState.ALL, 0, 10);
        verifyNoMoreInteractions(bookingService);
    }
}