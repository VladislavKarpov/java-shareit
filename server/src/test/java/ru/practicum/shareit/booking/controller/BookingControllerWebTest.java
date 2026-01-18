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

@WebMvcTest(controllers = BookingController.class)
@Import(ErrorHandler.class)
class BookingControllerWebTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @MockBean BookingService bookingService;

    private static final String HEADER_USER = BookingController.HEADER_USER;

    @Test
    void create_ok() throws Exception {
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.status").value("WAITING"));

        ArgumentCaptor<BookingCreateDto> captor = ArgumentCaptor.forClass(BookingCreateDto.class);
        verify(bookingService).create(eq(2L), captor.capture());
        assertThat(captor.getValue().getItemId()).isEqualTo(1L);
        verifyNoMoreInteractions(bookingService);
    }

    @Test
    void create_validationError_returns400_withFieldMap() throws Exception {
        // itemId null + start/end null => @NotNull сработает и попадет в MethodArgumentNotValidException
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
    void approve_ok() throws Exception {
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
    void getById_ok() throws Exception {
        when(bookingService.getById(7L, 10L)).thenReturn(BookingDto.builder().id(10L).build());

        mvc.perform(get("/bookings/10").header(HEADER_USER, 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));

        verify(bookingService).getById(7L, 10L);
        verifyNoMoreInteractions(bookingService);
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(bookingService.getById(7L, 10L)).thenThrow(new NotFoundException("Booking not found"));

        mvc.perform(get("/bookings/10").header(HEADER_USER, 7L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Booking not found"));

        verify(bookingService).getById(7L, 10L);
        verifyNoMoreInteractions(bookingService);
    }

    @Test
    void getUserBookings_ok_defaultStateALL() throws Exception {
        when(bookingService.getUserBookings(7L, BookingState.ALL)).thenReturn(List.of());

        mvc.perform(get("/bookings").header(HEADER_USER, 7L))
                .andExpect(status().isOk());

        verify(bookingService).getUserBookings(7L, BookingState.ALL);
        verifyNoMoreInteractions(bookingService);
    }

    @Test
    void getUserBookings_unknownState_returns400() throws Exception {
        mvc.perform(get("/bookings")
                        .header(HEADER_USER, 7L)
                        .param("state", "WTF"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unknown state: WTF"));

        verifyNoInteractions(bookingService);
    }

    @Test
    void getOwnerBookings_ok() throws Exception {
        when(bookingService.getOwnerBookings(7L, BookingState.WAITING)).thenReturn(List.of());

        mvc.perform(get("/bookings/owner")
                        .header(HEADER_USER, 7L)
                        .param("state", "WAITING"))
                .andExpect(status().isOk());

        verify(bookingService).getOwnerBookings(7L, BookingState.WAITING);
        verifyNoMoreInteractions(bookingService);
    }

    @Test
    void getOwnerBookings_serviceValidation_returns400() throws Exception {
        when(bookingService.getOwnerBookings(7L, BookingState.ALL))
                .thenThrow(new ValidationException("bad"));

        mvc.perform(get("/bookings/owner")
                        .header(HEADER_USER, 7L)
                        .param("state", "ALL"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad"));

        verify(bookingService).getOwnerBookings(7L, BookingState.ALL);
        verifyNoMoreInteractions(bookingService);
    }
}