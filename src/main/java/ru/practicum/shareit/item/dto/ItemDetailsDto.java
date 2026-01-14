package ru.practicum.shareit.item.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemDetailsDto {
    private Long id;
    private String name;
    private String description;
    private Boolean available;

    private BookingShortDto lastBooking;
    private BookingShortDto nextBooking;

    private List<CommentDto> comments;
}
