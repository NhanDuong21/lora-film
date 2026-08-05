package com.project.notificationservice.entity;

import com.project.notificationservice.domain.NotificationTypes.Category;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationCategoryConverterTest {

    private final NotificationCategoryConverter converter = new NotificationCategoryConverter();

    @Test
    void mapsLegacyCategoriesToCurrentDomainCategories() {
        assertThat(converter.convertToEntityAttribute("BOOKING")).isEqualTo(Category.TRANSACTIONAL);
        assertThat(converter.convertToEntityAttribute("PROMOTION")).isEqualTo(Category.MARKETING);
    }

    @Test
    void writesOnlyCurrentCategoryNames() {
        assertThat(converter.convertToDatabaseColumn(Category.TRANSACTIONAL)).isEqualTo("TRANSACTIONAL");
        assertThat(converter.convertToDatabaseColumn(Category.MARKETING)).isEqualTo("MARKETING");
    }
}
