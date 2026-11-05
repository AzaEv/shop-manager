package com.shopmanager.report;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.shopmanager.order.OrderItem;
import com.shopmanager.order.ShopOrder;
import com.shopmanager.report.ReportDtos.BreakdownRow;
import com.shopmanager.report.ReportDtos.ProductRow;
import com.shopmanager.report.ReportDtos.ShopAnalytics;
import com.shopmanager.report.ReportDtos.StatusRow;

final class OdsReport {

    private static final ZoneId ZONE = ZoneId.of("Asia/Tashkent");
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZONE);
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(ZONE);

    private OdsReport() {
    }

    static byte[] write(ShopAnalytics report, List<ShopOrder> orders, ReportView view) {
        String created = WHEN.format(Instant.now());
        String content = content(report, orders, created, view);
        try {
            ByteArrayOutputStream raw = new ByteArrayOutputStream();
            ZipOutputStream zip = new ZipOutputStream(raw);
            store(zip, "mimetype", "application/vnd.oasis.opendocument.spreadsheet".getBytes(StandardCharsets.US_ASCII));
            entry(zip, "META-INF/manifest.xml", manifest());
            entry(zip, "styles.xml", styles());
            entry(zip, "meta.xml", meta(created));
            entry(zip, "content.xml", content);
            zip.close();
            return raw.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Не удалось собрать отчёт ODS", ex);
        }
    }

    private static String content(ShopAnalytics report, List<ShopOrder> orders, String created, ReportView view) {
        StringBuilder xml = new StringBuilder();
        xml.append("""
                <?xml version="1.0" encoding="UTF-8"?>
                <office:document-content
                    xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
                    xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0"
                    xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0"
                    xmlns:table="urn:oasis:names:tc:opendocument:xmlns:table:1.0"
                    xmlns:fo="urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0"
                    xmlns:number="urn:oasis:names:tc:opendocument:xmlns:datastyle:1.0"
                    xmlns:svg="urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0"
                    office:version="1.3">
                <office:font-face-decls>
                  <style:font-face style:name="Liberation Sans" svg:font-family="'Liberation Sans'" style:font-family-generic="swiss"/>
                </office:font-face-decls>
                <office:automatic-styles>
                  <number:number-style style:name="n-int" number:language="ru" number:country="RU">
                    <number:number number:decimal-places="0" number:min-integer-digits="1" number:grouping="true"/>
                  </number:number-style>
                  <number:currency-style style:name="n-money" number:language="ru" number:country="RU">
                    <number:number number:decimal-places="2" number:min-integer-digits="1" number:grouping="true"/>
                    <number:text> ₽</number:text>
                  </number:currency-style>
                  <number:date-style style:name="n-date" number:language="ru" number:country="RU">
                    <number:day number:style="long"/><number:text>.</number:text>
                    <number:month number:style="long"/><number:text>.</number:text>
                    <number:year number:style="long"/><number:text> </number:text>
                    <number:hours number:style="long"/><number:text>:</number:text>
                    <number:minutes number:style="long"/>
                  </number:date-style>
                  <style:style style:name="title" style:family="table-cell">
                    <style:text-properties style:font-name="Liberation Sans" fo:font-size="16pt" fo:font-weight="bold" fo:color="#5b21b6"/>
                  </style:style>
                  <style:style style:name="note" style:family="table-cell">
                    <style:text-properties style:font-name="Liberation Sans" fo:font-size="10pt" fo:color="#5b4b70"/>
                  </style:style>
                  <style:style style:name="head" style:family="table-cell">
                    <style:table-cell-properties fo:background-color="#5b21b6" fo:wrap-option="wrap" style:vertical-align="middle"/>
                    <style:paragraph-properties fo:text-align="start"/>
                    <style:text-properties style:font-name="Liberation Sans" fo:color="#ffffff" fo:font-weight="bold"/>
                  </style:style>
                  <style:style style:name="text" style:family="table-cell">
                    <style:table-cell-properties style:vertical-align="middle"/>
                    <style:text-properties style:font-name="Liberation Sans" fo:color="#1c1028"/>
                  </style:style>
                  <style:style style:name="int" style:family="table-cell" style:data-style-name="n-int">
                    <style:table-cell-properties style:vertical-align="middle"/>
                    <style:text-properties style:font-name="Liberation Sans" fo:color="#1c1028"/>
                  </style:style>
                  <style:style style:name="money" style:family="table-cell" style:data-style-name="n-money">
                    <style:table-cell-properties style:vertical-align="middle"/>
                    <style:text-properties style:font-name="Liberation Sans" fo:color="#1c1028"/>
                  </style:style>
                  <style:style style:name="date" style:family="table-cell" style:data-style-name="n-date">
                    <style:table-cell-properties style:vertical-align="middle"/>
                    <style:text-properties style:font-name="Liberation Sans" fo:color="#1c1028"/>
                  </style:style>
                  <style:style style:name="wide" style:family="table-column">
                    <style:table-column-properties style:column-width="7.2cm"/>
                  </style:style>
                  <style:style style:name="mid" style:family="table-column">
                    <style:table-column-properties style:column-width="4.4cm"/>
                  </style:style>
                  <style:style style:name="narrow" style:family="table-column">
                    <style:table-column-properties style:column-width="2.8cm"/>
                  </style:style>
                </office:automatic-styles>
                <office:body><office:spreadsheet>
                """);
        summary(xml, report, created);
        statuses(xml, report);
        products(xml, report);
        breakdown(xml, report, view);
        orders(xml, orders);
        xml.append("</office:spreadsheet></office:body></office:document-content>");
        return xml.toString();
    }

    private static void summary(StringBuilder xml, ShopAnalytics report, String created) {
        sheet(xml, "Сводка", "wide", "mid");
        title(xml, "Отчёт о продажах nova", 2);
        note(xml, "Сформировано " + created + " (Ташкент). Выручка не включает отменённые заказы.", 2);
        xml.append("<table:table-row><table:table-cell/><table:table-cell/></table:table-row>");
        header(xml, "Показатель", "Значение");
        textRow(xml, "Клиенты", integer(report.customerCount()));
        textRow(xml, "Товары", integer(report.productCount()));
        textRow(xml, "Заказы", integer(report.orderCount()));
        textRow(xml, "Отменено", integer(report.cancelledCount()));
        textRow(xml, "Выручка", money(report.revenue()));
        textRow(xml, "Средний чек", money(report.averageCheck()));
        xml.append("</table:table>");
    }

    private static void statuses(StringBuilder xml, ShopAnalytics report) {
        sheet(xml, "Статусы", "mid", "narrow", "mid", "narrow");
        title(xml, "Заказы по статусам", 4);
        note(xml, "Доля считается от выручки без отменённых заказов.", 4);
        xml.append("<table:table-row><table:table-cell table:number-columns-repeated=\"4\"/></table:table-row>");
        header(xml, "Статус", "Заказов", "Сумма", "Доля выручки, %");
        for (StatusRow row : report.byStatus()) {
            xml.append("<table:table-row>");
            text(xml, row.status().getTitle());
            xml.append(integer(row.orderCount()));
            xml.append(money(row.amount()));
            xml.append(integer(row.percentOfRevenue()));
            xml.append("</table:table-row>");
        }
        xml.append("</table:table>");
    }

    private static void products(StringBuilder xml, ShopAnalytics report) {
        sheet(xml, "Товары", "wide", "narrow", "mid");
        title(xml, "Товары с наибольшей суммой", 3);
        note(xml, "Десять позиций. В сумму входят и отменённые заказы.", 3);
        xml.append("<table:table-row><table:table-cell table:number-columns-repeated=\"3\"/></table:table-row>");
        header(xml, "Товар", "Продано, шт.", "Сумма");
        for (ProductRow row : report.topProducts()) {
            xml.append("<table:table-row>");
            text(xml, row.productName());
            xml.append(integer(row.quantity()));
            xml.append(money(row.amount()));
            xml.append("</table:table-row>");
        }
        xml.append("</table:table>");
    }

    private static void breakdown(StringBuilder xml, ShopAnalytics report, ReportView view) {
        sheet(xml, "Сравнение", "wide", "narrow", "narrow", "mid", "narrow");
        title(xml, view.group().title(), 5);
        note(xml, "Отменённые заказы не входят. Доля считается от выручки.", 5);
        xml.append("<table:table-row><table:table-cell table:number-columns-repeated=\"5\"/></table:table-row>");
        header(xml, view.group().column(), "Заказов", "Продано, шт.", "Выручка", "Доля, %");
        for (BreakdownRow row : report.breakdown()) {
            xml.append("<table:table-row>");
            text(xml, row.label());
            xml.append(integer(row.orderCount()));
            xml.append(integer(row.quantity()));
            xml.append(money(row.amount()));
            xml.append(integer(row.percentOfRevenue()));
            xml.append("</table:table-row>");
        }
        xml.append("</table:table>");
    }

    private static void orders(StringBuilder xml, List<ShopOrder> orders) {
        sheet(xml, "Заказы", "narrow", "mid", "mid", "wide", "wide", "narrow", "mid", "narrow", "narrow", "mid", "mid");
        title(xml, "Все строки заказов", 11);
        note(xml, "Каждая строка — позиция заказа. Сумма строки пригодна для сложения.", 11);
        xml.append("<table:table-row><table:table-cell table:number-columns-repeated=\"11\"/></table:table-row>");
        header(xml, "Чек", "Дата", "Покупатель", "Email", "Адрес", "Статус", "Товар", "Артикул",
                "Количество", "Цена", "Сумма строки");
        for (ShopOrder order : orders) {
            List<OrderItem> items = order.getItems();
            if (items.isEmpty()) {
                orderRow(xml, order, null);
            } else {
                for (OrderItem item : items) {
                    orderRow(xml, order, item);
                }
            }
        }
        xml.append("</table:table>");
    }

    private static void orderRow(StringBuilder xml, ShopOrder order, OrderItem item) {
        xml.append("<table:table-row>");
        text(xml, "NV-" + order.getId());
        xml.append(date(order.getCreatedAt()));
        text(xml, order.getCustomer().getName());
        text(xml, order.getCustomer().getEmail());
        text(xml, order.getAddress());
        text(xml, order.getStatus().getTitle());
        if (item == null) {
            text(xml, "");
            text(xml, "");
            xml.append(integer(0));
            xml.append(money(BigDecimal.ZERO));
            xml.append(money(BigDecimal.ZERO));
        } else {
            text(xml, item.getProductName());
            text(xml, item.getSku());
            xml.append(integer(item.getQuantity()));
            xml.append(money(item.getUnitPrice()));
            BigDecimal line = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            xml.append(money(line));
        }
        xml.append("</table:table-row>");
    }

    private static void sheet(StringBuilder xml, String name, String... columns) {
        xml.append("<table:table table:name=\"").append(xml(name)).append("\">");
        for (String column : columns) {
            xml.append("<table:table-column table:style-name=\"").append(column).append("\"/>");
        }
    }

    private static void title(StringBuilder xml, String value, int span) {
        xml.append("<table:table-row>");
        xml.append("<table:table-cell table:style-name=\"title\" office:value-type=\"string\" table:number-columns-spanned=\"")
                .append(span).append("\"><text:p>").append(xml(value)).append("</text:p></table:table-cell>");
        for (int i = 1; i < span; i++) {
            xml.append("<table:covered-table-cell/>");
        }
        xml.append("</table:table-row>");
    }

    private static void note(StringBuilder xml, String value, int span) {
        xml.append("<table:table-row>");
        xml.append("<table:table-cell table:style-name=\"note\" office:value-type=\"string\" table:number-columns-spanned=\"")
                .append(span).append("\"><text:p>").append(xml(value)).append("</text:p></table:table-cell>");
        for (int i = 1; i < span; i++) {
            xml.append("<table:covered-table-cell/>");
        }
        xml.append("</table:table-row>");
    }

    private static void header(StringBuilder xml, String... labels) {
        xml.append("<table:table-row>");
        for (String label : labels) {
            xml.append("<table:table-cell table:style-name=\"head\" office:value-type=\"string\"><text:p>")
                    .append(xml(label)).append("</text:p></table:table-cell>");
        }
        xml.append("</table:table-row>");
    }

    private static void textRow(StringBuilder xml, String label, String valueCell) {
        xml.append("<table:table-row>");
        text(xml, label);
        xml.append(valueCell);
        xml.append("</table:table-row>");
    }

    private static void text(StringBuilder xml, String value) {
        xml.append("<table:table-cell table:style-name=\"text\" office:value-type=\"string\"><text:p>")
                .append(xml(value)).append("</text:p></table:table-cell>");
    }

    private static String integer(long value) {
        return "<table:table-cell table:style-name=\"int\" office:value-type=\"float\" office:value=\""
                + value + "\"><text:p>" + value + "</text:p></table:table-cell>";
    }

    private static String money(BigDecimal value) {
        BigDecimal scaled = value == null ? BigDecimal.ZERO.setScale(2) : value.setScale(2, java.math.RoundingMode.HALF_UP);
        String plain = scaled.toPlainString();
        return "<table:table-cell table:style-name=\"money\" office:value-type=\"currency\" office:currency=\"RUB\" office:value=\""
                + plain + "\"><text:p>" + plain.replace('.', ',') + " ₽</text:p></table:table-cell>";
    }

    private static String date(Instant instant) {
        if (instant == null) {
            return "<table:table-cell table:style-name=\"text\"/>";
        }
        return "<table:table-cell table:style-name=\"date\" office:value-type=\"date\" office:date-value=\""
                + ISO.format(instant) + "\"><text:p>" + xml(WHEN.format(instant)) + "</text:p></table:table-cell>";
    }

    private static String xml(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder safe = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '&' -> safe.append("&amp;");
                case '<' -> safe.append("&lt;");
                case '>' -> safe.append("&gt;");
                case '"' -> safe.append("&quot;");
                default -> {
                    if (ch == '\t' || ch == '\n' || ch == '\r' || ch >= ' ') {
                        safe.append(ch);
                    }
                }
            }
        }
        return safe.toString();
    }

    private static String manifest() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <manifest:manifest xmlns:manifest="urn:oasis:names:tc:opendocument:xmlns:manifest:1.0" manifest:version="1.3">
                  <manifest:file-entry manifest:full-path="/" manifest:version="1.3" manifest:media-type="application/vnd.oasis.opendocument.spreadsheet"/>
                  <manifest:file-entry manifest:full-path="content.xml" manifest:media-type="text/xml"/>
                  <manifest:file-entry manifest:full-path="styles.xml" manifest:media-type="text/xml"/>
                  <manifest:file-entry manifest:full-path="meta.xml" manifest:media-type="text/xml"/>
                  <manifest:file-entry manifest:full-path="META-INF/manifest.xml" manifest:media-type="text/xml"/>
                </manifest:manifest>
                """;
    }

    private static String styles() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <office:document-styles
                    xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
                    xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0"
                    xmlns:fo="urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0"
                    xmlns:svg="urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0"
                    office:version="1.3">
                  <office:font-face-decls>
                    <style:font-face style:name="Liberation Sans" svg:font-family="'Liberation Sans'" style:font-family-generic="swiss"/>
                  </office:font-face-decls>
                  <office:styles>
                    <style:default-style style:family="table-cell">
                      <style:text-properties style:font-name="Liberation Sans" fo:language="ru" fo:country="RU"/>
                    </style:default-style>
                  </office:styles>
                  <office:automatic-styles>
                    <style:page-layout style:name="pm1">
                      <style:page-layout-properties fo:page-width="29.7cm" fo:page-height="21cm" style:print-orientation="landscape"
                          fo:margin-top="1cm" fo:margin-bottom="1cm" fo:margin-left="1cm" fo:margin-right="1cm"/>
                    </style:page-layout>
                  </office:automatic-styles>
                  <office:master-styles>
                    <style:master-page style:name="Default" style:page-layout-name="pm1"/>
                  </office:master-styles>
                </office:document-styles>
                """;
    }

    private static String meta(String created) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <office:document-meta
                    xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
                    xmlns:meta="urn:oasis:names:tc:opendocument:xmlns:meta:1.0"
                    xmlns:dc="http://purl.org/dc/elements/1.1/"
                    office:version="1.3">
                  <office:meta>
                    <dc:title>Отчёт о продажах nova</dc:title>
                    <dc:language>ru</dc:language>
                    <meta:generator>nova</meta:generator>
                    <dc:description>Сформировано %s</dc:description>
                  </office:meta>
                </office:document-meta>
                """.formatted(xml(created));
    }

    private static void store(ZipOutputStream zip, String name, byte[] bytes) throws Exception {
        ZipEntry entry = new ZipEntry(name);
        CRC32 crc = new CRC32();
        crc.update(bytes);
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(bytes.length);
        entry.setCompressedSize(bytes.length);
        entry.setCrc(crc.getValue());
        zip.putNextEntry(entry);
        zip.write(bytes);
        zip.closeEntry();
    }

    private static void entry(ZipOutputStream zip, String name, String body) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(body.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
