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

    public record Slide(String kicker, String title, String accent, String text, String slug, String tone,
            List<String> skus) {
    }

    public record Perk(String icon, String title, String text) {
    }

    public List<Slide> slides() {
        return List.of(
                new Slide("Смартфоны", "Nova для каждого дня", "каждого дня",
                        "Компактные и Pro — экран, память и запас хода", "phones", "sun",
                        List.of("PHN-X15P", "PHN-AIR", "PHN-MINI")),
                new Slide("Аудио", "Музыка без проводов", "без проводов",
                        "Наушники, колонки и микрофоны линейки Wave", "audio", "mint",
                        List.of("AUD-WH1", "AUD-WB1", "AUD-WS1")),
                new Slide("Дом и офис", "Мелочи, которые остаются", "остаются",
                        "Лампы, кружки, кресла и кабели на каждый день", "home", "lilac",
                        List.of("OFF-DL2", "HOM-PLT", "HOM-MUG")));
    }

    public List<Perk> perks() {
        return List.of(
                new Perk("truck", "Доставка бесплатно", "Привезём по адресу из профиля, без доплат за километры"),
                new Perk("split", "12 платежей", "Та же цена, разделённая на 12 частей. Без кредита"),
                new Perk("shield", "Честный остаток", "Товар списывается со склада в момент заказа — дважды не продадим"),
                new Perk("receipt", "PDF-чек", "После оплаты чек можно скачать и распечатать"));
    }

    public String titleLead(Slide slide) {
        if (slide == null || slide.accent() == null || !slide.title().endsWith(slide.accent())) {
            return slide == null ? "" : slide.title();
        }
        return slide.title().substring(0, slide.title().length() - slide.accent().length()).stripTrailing();
    }

    public String excerpt(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > 64 ? text.substring(0, 63).stripTrailing() + "…" : text;
    }

    public int stockPercent(int stock) {
        return Math.max(4, Math.min(100, stock * 100 / 40));
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

    public String categoryPhoto(String slug) {
        if (slug == null) {
            return null;
        }
        return switch (slug) {
            case "phones", "audio", "office", "accessories", "home" -> "/img/categories/" + slug + ".jpg";
            default -> null;
        };
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
