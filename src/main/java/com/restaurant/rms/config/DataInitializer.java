package com.restaurant.rms.config;

import com.restaurant.rms.entity.*;
import com.restaurant.rms.entity.enums.Role;
import com.restaurant.rms.entity.enums.TableStatus;
import com.restaurant.rms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Seeds the database with baseline data on application startup.
 *
 * <p>Why {@link ApplicationRunner} and not {@code @PostConstruct}?
 * ApplicationRunner runs <em>after</em> the Spring context (including the
 * DataSource and JPA EntityManagerFactory) is fully initialised.
 * {@code @PostConstruct} runs during bean construction, before JPA is ready
 * for writes in some configurations.</p>
 *
 * <p>Idempotent — each seed block checks for existence before inserting.
 * Safe to redeploy without duplicating data.</p>
 *
 * <p>Passwords: all seed passwords are BCrypt-hashed via the production
 * {@link PasswordEncoder} bean; plain-text passwords never touch the database.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UserRepository              userRepository;
    private final CategoryRepository          categoryRepository;
    private final MenuItemRepository          menuItemRepository;
    private final RestaurantTableRepository   tableRepository;
    private final InventoryItemRepository     inventoryItemRepository;
    private final PasswordEncoder             passwordEncoder;

    /**
     * @Transactional ensures all seed inserts are committed in one transaction.
     * If any insert fails, the entire seed is rolled back — no half-seeded state.
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("=== DataInitializer: starting seed check ===");

        int users      = seedUsers();
        int cats       = seedCategories();
        int items      = seedMenuItems();
        int tables     = seedTables();
        int inventory  = seedInventory();

        log.info("=== DataInitializer complete: {} users, {} categories, {} menu items, " +
                 "{} tables, {} inventory items seeded ===",
                 users, cats, items, tables, inventory);
    }

    // =========================================================================
    // Users (Part D: passwords encoded with BCryptPasswordEncoder(12))
    // =========================================================================

    private int seedUsers() {
        int count = 0;

        // ── Admin ────────────────────────────────────────────────────────────
        if (!userRepository.existsByUsername("admin")) {
            userRepository.save(User.builder()
                .username("admin")
                .email("admin@restaurant.com")
                // BCrypt-hashed at strength 12 — plain text never stored.
                .password(passwordEncoder.encode("Admin@123"))
                .role(Role.ADMIN)
                .isActive(true)
                .build());
            log.info("  Seeded ADMIN user: admin");
            count++;
        }

        // ── Manager ──────────────────────────────────────────────────────────
        if (!userRepository.existsByUsername("manager1")) {
            userRepository.save(User.builder()
                .username("manager1")
                .email("manager1@restaurant.com")
                .password(passwordEncoder.encode("Manager@123"))
                .role(Role.MANAGER)
                .phone("555-0101")
                .isActive(true)
                .build());
            log.info("  Seeded MANAGER user: manager1");
            count++;
        }

        // ── Waiters (2) ───────────────────────────────────────────────────────
        if (!userRepository.existsByUsername("waiter1")) {
            userRepository.save(User.builder()
                .username("waiter1")
                .email("waiter1@restaurant.com")
                .password(passwordEncoder.encode("Waiter@123"))
                .role(Role.WAITER)
                .phone("555-0201")
                .isActive(true)
                .build());
            log.info("  Seeded WAITER user: waiter1");
            count++;
        }

        if (!userRepository.existsByUsername("waiter2")) {
            userRepository.save(User.builder()
                .username("waiter2")
                .email("waiter2@restaurant.com")
                .password(passwordEncoder.encode("Waiter@123"))
                .role(Role.WAITER)
                .phone("555-0202")
                .isActive(true)
                .build());
            log.info("  Seeded WAITER user: waiter2");
            count++;
        }

        // ── Chef (1) ──────────────────────────────────────────────────────────
        if (!userRepository.existsByUsername("chef1")) {
            userRepository.save(User.builder()
                .username("chef1")
                .email("chef1@restaurant.com")
                .password(passwordEncoder.encode("Chef@123"))
                .role(Role.CHEF)
                .phone("555-0301")
                .isActive(true)
                .build());
            log.info("  Seeded CHEF user: chef1");
            count++;
        }

        return count;
    }

    // =========================================================================
    // Categories (5)
    // =========================================================================

    private int seedCategories() {
        record CatSeed(String name, String desc, int order) {}
        List<CatSeed> seeds = List.of(
            new CatSeed("Starters",   "Appetisers and light bites to begin your meal", 1),
            new CatSeed("Mains",      "Hearty main-course dishes",                     2),
            new CatSeed("Desserts",   "Sweet treats to finish your meal",              3),
            new CatSeed("Beverages",  "Hot and cold drinks",                           4),
            new CatSeed("Specials",   "Chef's daily specials and seasonal dishes",     5)
        );

        int count = 0;
        for (CatSeed s : seeds) {
            if (!categoryRepository.existsByName(s.name())) {
                categoryRepository.save(Category.builder()
                    .name(s.name())
                    .description(s.desc())
                    .displayOrder(s.order())
                    .isActive(true)
                    .build());
                log.info("  Seeded category: {}", s.name());
                count++;
            }
        }
        return count;
    }

    // =========================================================================
    // Menu items (15)
    // =========================================================================

    private int seedMenuItems() {
        // Resolve categories — skip item seeding if categories are missing.
        Category starters   = categoryRepository.findByName("Starters").orElse(null);
        Category mains      = categoryRepository.findByName("Mains").orElse(null);
        Category desserts   = categoryRepository.findByName("Desserts").orElse(null);
        Category beverages  = categoryRepository.findByName("Beverages").orElse(null);
        Category specials   = categoryRepository.findByName("Specials").orElse(null);

        if (starters == null || mains == null || desserts == null
                || beverages == null || specials == null) {
            log.warn("  Skipping menu item seed — categories not yet persisted");
            return 0;
        }

        record ItemSeed(String name, String desc, BigDecimal price,
                        Category cat, boolean veg, int prepMins, String imageUrl) {}

        List<ItemSeed> seeds = List.of(
            // Starters (3)
            new ItemSeed("Garlic Bread",       "Toasted bread with garlic butter",          bd("4.50"),  starters, true,  5,
                "https://images.unsplash.com/photo-1573140247632-f8fd74997d5c?w=400&q=80"),
            new ItemSeed("Soup of the Day",    "Chef's daily soup with crusty roll",         bd("6.00"),  starters, false, 10,
                "https://images.unsplash.com/photo-1547592166-23ac45744acd?w=400&q=80"),
            new ItemSeed("Chicken Wings",      "Crispy wings with BBQ sauce",                bd("8.50"),  starters, false, 15,
                "https://images.unsplash.com/photo-1567620832903-9fc6debc209f?w=400&q=80"),

            // Mains (5)
            new ItemSeed("Grilled Salmon",     "Atlantic salmon with seasonal veg",          bd("18.00"), mains,    false, 20,
                "https://images.unsplash.com/photo-1519708227418-c8fd9a32b7a2?w=400&q=80"),
            new ItemSeed("Beef Burger",        "8 oz beef patty with fries",                 bd("14.00"), mains,    false, 15,
                "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=400&q=80"),
            new ItemSeed("Margherita Pizza",   "Classic tomato, mozzarella, basil",          bd("12.00"), mains,    true,  20,
                "https://images.unsplash.com/photo-1574071318508-1cdbab80d002?w=400&q=80"),
            new ItemSeed("Pasta Primavera",    "Penne with seasonal vegetables",             bd("11.00"), mains,    true,  15,
                "https://images.unsplash.com/photo-1621996346565-e3dbc646d9a9?w=400&q=80"),
            new ItemSeed("Ribeye Steak",       "300 g ribeye with chips and salad",          bd("28.00"), mains,    false, 25,
                "https://images.unsplash.com/photo-1558030006-450675393462?w=400&q=80"),

            // Desserts (3)
            new ItemSeed("Chocolate Fondant",  "Warm chocolate cake with ice cream",         bd("7.00"),  desserts, true,  15,
                "https://images.unsplash.com/photo-1563805042-7684c019e1cb?w=400&q=80"),
            new ItemSeed("Cheesecake",         "New York style with berry coulis",           bd("6.50"),  desserts, true,  5,
                "https://images.unsplash.com/photo-1533134242443-d4fd215305ad?w=400&q=80"),
            new ItemSeed("Tiramisu",           "Classic Italian coffee dessert",             bd("6.00"),  desserts, true,  5,
                "https://images.unsplash.com/photo-1571877227200-a0d98ea607e9?w=400&q=80"),

            // Beverages (2)
            new ItemSeed("Fresh Orange Juice", "Freshly squeezed orange juice 300 ml",      bd("3.50"),  beverages,true,  2,
                "https://images.unsplash.com/photo-1600271886742-f049cd451bba?w=400&q=80"),
            new ItemSeed("Sparkling Water",    "San Pellegrino 750 ml",                      bd("2.50"),  beverages,true,  1,
                "https://images.unsplash.com/photo-1556679343-c7306c1976bc?w=400&q=80"),

            // Specials (2)
            new ItemSeed("Chef's Fish Special","Daily catch with market vegetables",         bd("22.00"), specials, false, 25,
                "https://images.unsplash.com/photo-1467003909585-2f8a72700288?w=400&q=80"),
            new ItemSeed("Vegan Risotto",      "Seasonal vegetable risotto, dairy-free",    bd("13.00"), specials, true,  20,
                "https://images.unsplash.com/photo-1476124369491-e7addf5db371?w=400&q=80")
        );

        int count = 0;
        for (ItemSeed s : seeds) {
            // Use name as the uniqueness key.
            var existing = menuItemRepository.findByCategory(s.cat()).stream()
                    .filter(m -> m.getName().equals(s.name()))
                    .findFirst();
            if (existing.isEmpty()) {
                menuItemRepository.save(MenuItem.builder()
                    .name(s.name())
                    .description(s.desc())
                    .price(s.price())
                    .category(s.cat())
                    .isAvailable(true)
                    .isVegetarian(s.veg())
                    .preparationTimeMinutes(s.prepMins())
                    .imageUrl(s.imageUrl())
                    .build());
                log.info("  Seeded menu item: {}", s.name());
                count++;
            } else {
                // Backfill imageUrl for items seeded before images were added
                MenuItem item = existing.get();
                if (item.getImageUrl() == null || item.getImageUrl().isBlank()) {
                    item.setImageUrl(s.imageUrl());
                    menuItemRepository.save(item);
                    log.info("  Updated image for menu item: {}", s.name());
                }
            }
        }
        return count;
    }

    // =========================================================================
    // Tables (10)
    // =========================================================================

    private int seedTables() {
        int count = 0;
        // Table numbers 1-10, alternating 2- and 4-seat configurations.
        for (int n = 1; n <= 10; n++) {
            if (!tableRepository.existsByTableNumber(n)) {
                tableRepository.save(RestaurantTable.builder()
                    .tableNumber(n)
                    .capacity(n % 3 == 0 ? 2 : 4)   // tables 3, 6, 9 seat 2; rest seat 4
                    .status(TableStatus.AVAILABLE)
                    .locationDescription(n <= 5 ? "Main dining room" : "Garden terrace")
                    .build());
                log.info("  Seeded table #{}", n);
                count++;
            }
        }
        return count;
    }

    // =========================================================================
    // Inventory (20 items)
    // =========================================================================

    private int seedInventory() {
        record InvSeed(String name, String unit, BigDecimal stock,
                       BigDecimal minStock, BigDecimal cost, String supplier) {}

        List<InvSeed> seeds = List.of(
            new InvSeed("Flour",           "kg",     bd("50"),  bd("10"),  bd("1.20"),  "Millers Co"),
            new InvSeed("Sugar",           "kg",     bd("30"),  bd("5"),   bd("0.90"),  "Millers Co"),
            new InvSeed("Butter",          "kg",     bd("20"),  bd("5"),   bd("4.50"),  "Dairy Direct"),
            new InvSeed("Eggs",            "piece",  bd("200"), bd("50"),  bd("0.25"),  "Farm Fresh"),
            new InvSeed("Chicken Breast",  "kg",     bd("25"),  bd("10"),  bd("7.00"),  "Meat Market"),
            new InvSeed("Beef Mince",      "kg",     bd("20"),  bd("8"),   bd("9.00"),  "Meat Market"),
            new InvSeed("Salmon Fillet",   "kg",     bd("15"),  bd("5"),   bd("14.00"), "Fish Supplies"),
            new InvSeed("Ribeye Steak",    "kg",     bd("12"),  bd("4"),   bd("22.00"), "Meat Mark