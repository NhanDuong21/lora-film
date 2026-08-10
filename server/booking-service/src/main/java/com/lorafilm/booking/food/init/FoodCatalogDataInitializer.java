package com.lorafilm.booking.food.init;

import com.lorafilm.booking.food.client.FoodCatalogItem;
import com.lorafilm.booking.food.enums.ProductType;
import com.lorafilm.booking.food.repository.FoodCatalogItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class FoodCatalogDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(FoodCatalogDataInitializer.class);

    private final FoodCatalogItemRepository repository;

    public FoodCatalogDataInitializer(FoodCatalogItemRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() == 0) {
            log.info("Food catalog table is empty. Seeding default items...");

            FoodCatalogItem popcorn = new FoodCatalogItem(
                    null, "POP_L", "Large Popcorn", ProductType.FOOD, "popcorn_l.png",
                    new BigDecimal("50000.00"), true, true, false, false, "VND"
            );

            FoodCatalogItem coke = new FoodCatalogItem(
                    null, "COKE_L", "Large Coke", ProductType.DRINK, "coke_l.png",
                    new BigDecimal("30000.00"), true, true, false, false, "VND"
            );

            FoodCatalogItem combo = new FoodCatalogItem(
                    null, "COMBO_1", "Combo 1 (1 Popcorn, 1 Coke)", ProductType.COMBO, "combo_1.png",
                    new BigDecimal("75000.00"), true, true, false, false, "VND"
            );

            repository.saveAll(List.of(popcorn, coke, combo));
            log.info("Successfully seeded default food catalog items.");
        } else {
            log.debug("Food catalog table already has entries. Skipping seed.");
        }
    }
}
