package com.shopmanager.order;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

@Service
public class ReceiptPdfService {

    private static final ZoneId TASHKENT = ZoneId.of("Asia/Tashkent");
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(TASHKENT);
    private static final Color INK = new Color(28, 16, 48);
    private static final Color MUTED = new Color(90, 70, 120);
    private static final Color VIOLET = new Color(91, 33, 182);
    private static final Color ROW = new Color(245, 240, 252);

    public byte[] render(ShopOrder order) {
        try {
            byte[] fontBytes = getClass().getResourceAsStream("/fonts/DejaVuSans.ttf").readAllBytes();
            BaseFont base = BaseFont.createFont("DejaVuSans.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true,
                    fontBytes, null);
            Font title = new Font(base, 22, Font.NORMAL, VIOLET);
            Font heading = new Font(base, 14, Font.NORMAL, INK);
            Font body = new Font(base, 10, Font.NORMAL, INK);
            Font muted = new Font(base, 9, Font.NORMAL, MUTED);
            Font white = new Font(base, 9, Font.NORMAL, Color.WHITE);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 46, 46, 42, 46);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            document.add(new Paragraph("nova", title));
            document.add(new Paragraph("Магазин техники и мелочей для дома", muted));
            document.add(spacer(body, 8));
            document.add(new Paragraph("Чек об оплате № NV-" + order.getId(), heading));
            document.add(new Paragraph("Дата: " + WHEN.format(order.getCreatedAt()) + " (Ташкент)", body));
            document.add(new Paragraph("Покупатель: " + order.getCustomer().getName(), body));
            document.add(new Paragraph("Email: " + order.getCustomer().getEmail(), body));
            if (order.getCustomer().getPhone() != null && !order.getCustomer().getPhone().isBlank()) {
                document.add(new Paragraph("Телефон: " + order.getCustomer().getPhone(), body));
            }
            document.add(new Paragraph("Адрес доставки: " + order.getAddress(), body));
            document.add(new Paragraph("Оплата: учебная симуляция, карта не сохраняется", muted));
            document.add(spacer(body, 12));

            PdfPTable table = new PdfPTable(new float[] { 4.2f, 1f, 1.6f, 1.6f });
            table.setWidthPercentage(100);
            header(table, white, "Товар", "Кол-во", "Цена", "Сумма");
            boolean shade = false;
            for (OrderItem item : order.getItems()) {
                Color background = shade ? ROW : Color.WHITE;
                cell(table, body, item.getProductName() + "\n" + item.getSku(), background, Element.ALIGN_LEFT);
                cell(table, body, String.valueOf(item.getQuantity()), background, Element.ALIGN_RIGHT);
                cell(table, body, money(item.getUnitPrice()), background, Element.ALIGN_RIGHT);
                cell(table, body, money(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))),
                        background, Element.ALIGN_RIGHT);
                shade = !shade;
            }
            document.add(table);
            document.add(spacer(body, 10));
            document.add(new Paragraph("Итого: " + money(order.getTotal()), heading));
            document.add(new Paragraph("Статус: " + order.getStatus().getTitle(), body));
            document.add(spacer(body, 8));
            document.add(new Paragraph("Спасибо за покупку в nova.", muted));

            float y = writer.getVerticalPosition(true);
            if (y < 170) {
                document.newPage();
                y = writer.getVerticalPosition(true);
            }
            drawStamp(writer.getDirectContent(), base, 470, y - 78);
            document.close();
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Не удалось собрать чек", ex);
        }
    }

    private static void drawStamp(PdfContentByte canvas, BaseFont font, float cx, float cy) {
        canvas.saveState();
        canvas.setColorStroke(VIOLET);
        canvas.setLineWidth(2.4f);
        canvas.circle(cx, cy, 62);
        canvas.stroke();
        canvas.setLineWidth(1f);
        canvas.circle(cx, cy, 52);
        canvas.stroke();
        canvas.beginText();
        canvas.setFontAndSize(font, 16);
        canvas.setColorFill(VIOLET);
        canvas.showTextAligned(Element.ALIGN_CENTER, "NOVA", cx, cy - 5, 0);
        canvas.endText();
        arcText(canvas, font, "МАГАЗИН ТЕХНИКИ", cx, cy, 42, 155, 25);
        arcText(canvas, font, "ОПЛАЧЕНО", cx, cy, 42, 205, 335);
        canvas.restoreState();
    }

    private static void arcText(PdfContentByte canvas, BaseFont font, String text, float cx, float cy, float radius,
            double startDeg, double endDeg) {
        int count = text.length();
        double span = endDeg - startDeg;
        if (span > 180) {
            span -= 360;
        } else if (span < -180) {
            span += 360;
        }
        canvas.setFontAndSize(font, 7.5f);
        canvas.setColorFill(VIOLET);
        for (int i = 0; i < count; i++) {
            double deg = startDeg + span * (i + 0.5) / count;
            double rad = Math.toRadians(deg);
            float x = cx + (float) (radius * Math.cos(rad));
            float y = cy + (float) (radius * Math.sin(rad));
            canvas.beginText();
            canvas.showTextAligned(Element.ALIGN_CENTER, String.valueOf(text.charAt(i)), x, y, (float) deg - 90);
            canvas.endText();
        }
    }

    private static Paragraph spacer(Font font, float after) {
        Paragraph paragraph = new Paragraph(" ", font);
        paragraph.setSpacingAfter(after);
        return paragraph;
    }

    private static void header(PdfPTable table, Font font, String... titles) {
        for (String title : titles) {
            PdfPCell cell = new PdfPCell(new Phrase(title, font));
            cell.setBackgroundColor(VIOLET);
            cell.setPadding(6);
            cell.setBorderColor(VIOLET);
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            table.addCell(cell);
        }
    }

    private static void cell(PdfPTable table, Font font, String text, Color background, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(background);
        cell.setPadding(6);
        cell.setBorderColor(new Color(220, 210, 235));
        cell.setHorizontalAlignment(align);
        table.addCell(cell);
    }

    private static String money(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        DecimalFormat format = new DecimalFormat("#,##0.00", symbols);
        return format.format(value) + " ₽";
    }
}
