package com.shopmanager.report;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Objects;

import org.springframework.web.util.UriComponentsBuilder;

public class ReportLinks {

    public record Preset(String label, String url, boolean active) {
    }

    private final ReportView view;
    private final LocalDate today;

    public ReportLinks(ReportView view, LocalDate today) {
        this.view = view;
        this.today = today;
    }

    public String url(String path, String group, String sort, String dir) {
        return build(path, group, sort, dir, view.from(), view.to());
    }

    public String page(String group, String sort, String dir) {
        return url("/admin/report", group, sort, dir);
    }

    public String export(String path) {
        return url(path, view.group().code(), view.sort().code(), view.dir());
    }

    public List<Preset> presets() {
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate lastMonthStart = monthStart.minusMonths(1);
        return List.of(
                preset("Сегодня", today, today),
                preset("Вчера", today.minusDays(1), today.minusDays(1)),
                preset("Эта неделя", today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), today),
                preset("7 дней", today.minusDays(6), today),
                preset("30 дней", today.minusDays(29), today),
                preset("Этот месяц", monthStart, today),
                preset("Прошлый месяц", lastMonthStart, monthStart.minusDays(1)),
                preset("Этот год", today.withDayOfYear(1), today),
                preset("Всё время", null, null));
    }

    private Preset preset(String label, LocalDate from, LocalDate to) {
        boolean active = Objects.equals(from, view.from()) && Objects.equals(to, view.to());
        String url = build("/admin/report", view.group().code(), view.sort().code(), view.dir(), from, to);
        return new Preset(label, url, active);
    }

    private String build(String path, String group, String sort, String dir, LocalDate from, LocalDate to) {
        UriComponentsBuilder uri = UriComponentsBuilder.fromPath(path)
                .queryParam("group", group)
                .queryParam("sort", sort)
                .queryParam("dir", dir);
        if (from != null) {
            uri.queryParam("from", from);
        }
        if (to != null) {
            uri.queryParam("to", to);
        }
        for (String status : view.statusParams()) {
            uri.queryParam("status", status);
        }
        if (view.categoryId() != null) {
            uri.queryParam("category", view.categoryId());
        }
        if (view.top() != ReportView.DEFAULT_TOP) {
            uri.queryParam("top", view.top());
        }
        return uri.encode().build().toUriString();
    }
}
