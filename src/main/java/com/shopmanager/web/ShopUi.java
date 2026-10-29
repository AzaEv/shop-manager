package com.shopmanager.web;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.shopmanager.catalog.Category;

@Component("ui")
public class ShopUi {

    private static final ZoneId ZONE = ZoneId.of("Asia/Tashkent");
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZONE);
    private static final Set<String> PHOTO_SKUS = Set.of(
            "PHN-X15", "PHN-X15P", "PHN-AIR", "PHN-MINI",
            "AUD-WB1", "AUD-WS1", "AUD-WH1", "AUD-MIC",
            "OFF-DL2", "OFF-NB5", "OFF-PEN", "OFF-MP1", "OFF-CH1",
            "ACC-USBC", "ACC-PB1", "ACC-CS1",
            "HOM-MUG", "HOM-PLT");

    public record Slide(String kicker, String title, String text, String slug, String tone) {
    }

    public List<Slide> slides() {
        return List.of(
                new Slide("Смартфоны", "Nova для каждого дня",
                        "Компактные и Pro — экран, память и запас хода", "phones", "sun"),
                new Slide("Аудио", "Музыка без проводов",
                        "Наушники, колонки и микрофоны линейки Wave", "audio", "mint"),
                new Slide("Дом и офис", "Мелочи, которые остаются",
                        "Лампы, кружки, кресла и кабели на каждый день", "home", "lilac"));
    }

    public String categoryHref(List<Category> categories, String slug) {
        if (categories == null || slug == null) {
            return "/";
        }
        return categories.stream()
                .filter(category -> slug.equals(category.getSlug()))
                .findFirst()
                .map(category -> "/?categoryId=" + category.getId())
                .orElse("/");
    }

    public String rub(BigDecimal amount) {
        return money(amount, 2) + " ₽";
    }

    public String monthly(BigDecimal amount) {
        if (amount == null) {
            return "0 ₽";
        }
        BigDecimal month = amount.divide(BigDecimal.valueOf(12), 0, RoundingMode.HALF_UP);
        return money(month, 0) + " ₽";
    }

    public BigDecimal line(BigDecimal price, int quantity) {
        if (price == null) {
            return BigDecimal.ZERO;
        }
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    public String when(Instant instant) {
        if (instant == null) {
            return "";
        }
        return WHEN.format(instant);
    }

    public String photo(String sku) {
        if (sku == null || sku.isBlank()) {
            return null;
        }
        String key = sku.trim().toUpperCase(Locale.ROOT);
        if (!PHOTO_SKUS.contains(key)) {
            return null;
        }
        return "/img/products/" + key + ".jpg";
    }

    public String heroPhoto(String slug) {
        if (slug == null) {
            return "/img/hero/phones.jpg";
        }
        return switch (slug) {
            case "audio" -> "/img/hero/audio.jpg";
            case "home" -> "/img/hero/home.jpg";
            default -> "/img/hero/phones.jpg";
        };
    }

    public String emoji(String slug) {
        if (slug == null) {
            return "🛍️";
        }
        return switch (slug) {
            case "phones" -> "📱";
            case "audio" -> "🎧";
            case "office" -> "💼";
            case "accessories" -> "🔌";
            case "home" -> "🏠";
            default -> "🛍️";
        };
    }

    public String tone(String slug) {
        if (slug == null) {
            return "default";
        }
        return switch (slug) {
            case "phones", "audio", "office", "accessories", "home" -> slug;
            default -> "default";
        };
    }

    public String initial(String name) {
        if (name == null || name.isBlank()) {
            return "N";
        }
        return name.trim().substring(0, 1).toUpperCase(Locale.ROOT);
    }

    private String money(BigDecimal amount, int fraction) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.forLanguageTag("ru-RU"));
        format.setMinimumFractionDigits(fraction);
        format.setMaximumFractionDigits(fraction);
        format.setGroupingUsed(true);
        return format.format(amount == null ? BigDecimal.ZERO : amount);
    }
}
