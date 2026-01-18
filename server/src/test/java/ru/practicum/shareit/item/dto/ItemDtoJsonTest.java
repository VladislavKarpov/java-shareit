package ru.practicum.shareit.item.dto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class ItemDtoJsonTest {

    @Autowired
    private JacksonTester<ItemDto> json;

    @Test
    void serialize_containsAllFields() throws Exception {
        ItemDto dto = ItemDto.builder()
                .id(1L)
                .name("n")
                .description("d")
                .available(true)
                .requestId(2L)
                .ownerId(3L)
                .build();

        var written = json.write(dto);

        assertThat(written).extractingJsonPathNumberValue("$.id").isEqualTo(1);
        assertThat(written).extractingJsonPathStringValue("$.name").isEqualTo("n");
        assertThat(written).extractingJsonPathStringValue("$.description").isEqualTo("d");
        assertThat(written).extractingJsonPathBooleanValue("$.available").isEqualTo(true);
        assertThat(written).extractingJsonPathNumberValue("$.requestId").isEqualTo(2);
        assertThat(written).extractingJsonPathNumberValue("$.ownerId").isEqualTo(3);
    }

    @Test
    void deserialize_readsFields() throws Exception {
        String content = """
                {
                  "id": 10,
                  "name": "item",
                  "description": "desc",
                  "available": true,
                  "requestId": 7,
                  "ownerId": 5
                }
                """;

        ItemDto parsed = json.parseObject(content);

        assertThat(parsed.getId()).isEqualTo(10L);
        assertThat(parsed.getName()).isEqualTo("item");
        assertThat(parsed.getDescription()).isEqualTo("desc");
        assertThat(parsed.getAvailable()).isTrue();
        assertThat(parsed.getRequestId()).isEqualTo(7L);
        assertThat(parsed.getOwnerId()).isEqualTo(5L);
    }
}