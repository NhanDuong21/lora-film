package com.lorafilm.booking.food;

import com.lorafilm.booking.common.exception.BusinessException;
import com.lorafilm.booking.food.client.DbFoodCatalogClientImpl;
import com.lorafilm.booking.food.client.FoodCatalogItem;
import com.lorafilm.booking.food.enums.ProductType;
import com.lorafilm.booking.food.repository.FoodCatalogItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DbFoodCatalogClientImplTest {

    @Mock
    private FoodCatalogItemRepository repository;

    private DbFoodCatalogClientImpl service;

    @BeforeEach
    void setUp() {
        service = new DbFoodCatalogClientImpl(repository);
    }

    @Test
    void shouldNormalizeNewProductAndProtectLifecycleFields() {
        FoodCatalogItem item = catalogItem(null, " combo_family ", " Combo gia dinh ");
        item.setDeleted(true);
        item.setDisabled(true);
        when(repository.findByCodeIgnoreCase("COMBO_FAMILY")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FoodCatalogItem saved = service.addProduct(item);

        assertEquals("COMBO_FAMILY", saved.getCode());
        assertEquals("Combo gia dinh", saved.getName());
        assertEquals("VND", saved.getCurrency());
        assertFalse(saved.isDeleted());
        assertFalse(saved.isDisabled());
    }

    @Test
    void shouldRejectDuplicateCodeIgnoringCase() {
        FoodCatalogItem item = catalogItem(null, "pop_l", "Bap rang lon");
        when(repository.findByCodeIgnoreCase("POP_L"))
                .thenReturn(Optional.of(catalogItem(1L, "POP_L", "Existing")));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.addProduct(item));

        assertEquals("CONCESSION_CODE_EXISTS", exception.getErrorCode());
        verify(repository, never()).save(any());
    }

    @Test
    void shouldRestoreArchivedProductAsPaused() {
        FoodCatalogItem item = catalogItem(4L, "COKE_L", "Nuoc ngot lon");
        item.setDeleted(true);
        item.setDisabled(true);
        when(repository.findById(4L)).thenReturn(Optional.of(item));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FoodCatalogItem restored = service.restoreProduct(4L);

        assertFalse(restored.isDeleted());
        assertFalse(restored.isDisabled());
        assertFalse(restored.isActive());
        assertFalse(restored.isSellable());
    }

    @Test
    void repeatedRestoreShouldNotPauseAnActiveProduct() {
        FoodCatalogItem item = catalogItem(4L, "COKE_L", "Nuoc ngot lon");
        when(repository.findById(4L)).thenReturn(Optional.of(item));

        FoodCatalogItem restored = service.restoreProduct(4L);

        assertTrue(restored.isActive());
        assertTrue(restored.isSellable());
        verify(repository, never()).save(any());
    }

    private FoodCatalogItem catalogItem(Long id, String code, String name) {
        return new FoodCatalogItem(
                id,
                code,
                name,
                ProductType.COMBO,
                null,
                new BigDecimal("129000"),
                true,
                true,
                false,
                false,
                null);
    }
}
