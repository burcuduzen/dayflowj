package com.burcuduzen.dayflow.gmail;

import java.time.*;
import java.util.*;
import java.util.regex.*;

/** Conservative Turkish text extraction. Relative dates refer to the message timestamp. */
public class MailEventParser {
    public record Draft(String title, OffsetDateTime startAt, OffsetDateTime endAt, boolean automatic, String reason) {}
    private static final Pattern CLOCK = Pattern.compile("(?iu)\\b(?:saat\\s*)?([01]?\\d|2[0-3])[:.]([0-5]\\d)\\b");
    private static final Pattern DATE = Pattern.compile("\\b(\\d{4}-\\d{2}-\\d{2})\\b");
    private static final Pattern DURATION = Pattern.compile("(?iu)\\b(\\d{1,3})\\s*(?:dk|dakika)\\b");
    public Draft parse(String subject, String body, ZonedDateTime sentAt, Instant now) {
        String text = (subject + "\n" + body).toLowerCase(Locale.forLanguageTag("tr"));
        if (!Pattern.compile("(?iu)toplantı|görüşme|randevu|mülakat|meeting|interview").matcher(text).find()) return null;
        String title = subject == null || subject.isBlank() ? "Mailden görüşme" : subject.strip();
        title = title.substring(0, Math.min(200, title.length()));
        LocalDate date = null;
        Matcher dates = DATE.matcher(text); int dateCount = 0;
        while (dates.find()) { dateCount++; try { date = LocalDate.parse(dates.group(1)); } catch (DateTimeException ignored) {} }
        boolean tomorrow = text.contains("yarın"), today = text.contains("bugün");
        if (tomorrow && date == null) date = sentAt.toLocalDate().plusDays(1);
        if (today && date == null) date = sentAt.toLocalDate();
        Matcher clock = CLOCK.matcher(text); LocalTime time = null; int timeCount = 0;
        while (clock.find()) { timeCount++; time = LocalTime.of(Integer.parseInt(clock.group(1)), Integer.parseInt(clock.group(2))); }
        OffsetDateTime start = date != null && time != null ? date.atTime(time).atZone(sentAt.getZone()).toOffsetDateTime() : null;
        int duration = 30; Matcher minutes = DURATION.matcher(text);
        if (minutes.find()) duration = Math.max(1, Math.min(1440, Integer.parseInt(minutes.group(1))));
        String reason;
        boolean confirmed = Pattern.compile("kesinleş|onaylan|gerçekleşecektir|yapılacaktır|davetlisiniz").matcher(text).find();
        boolean uncertain = Pattern.compile("\\?|gelebilir|uygun mus|görüşelim|iptal|değiş|ertelen|teklif|öner|olabilir|kesinleşmedi|kesinleşmemiş|onaylanmadı|onaylanmamış|gerçekleşmeyecek|yapılmayacak").matcher(text).find();
        boolean multiple = dateCount > 1 || timeCount > 1 || (tomorrow && today) || (dateCount > 0 && (tomorrow || today));
        boolean future = start != null && start.toInstant().isAfter(now);
        boolean automatic = future && confirmed && !uncertain && !multiple;
        if (start == null) reason = "Kesin tarih ve saat bulunamadı; tarih ve saati sen seç.";
        else if (!future) reason = "Maildeki tarih geçmişte; kontrol et.";
        else if (multiple) reason = "Birden fazla tarih veya saat var; kontrol et.";
        else if (uncertain) reason = "Teklif, iptal veya değişiklik olabilir; kontrol et.";
        else if (!confirmed) reason = "Toplantının kesinleştiğini kontrol et.";
        else reason = "Kesin tarih, saat ve onay ifadesi bulundu.";
        if (!minutes.reset().find()) reason += " Süre belirtilmedi; 30 dakika önerildi.";
        return new Draft(title, start, start == null ? null : start.plusMinutes(duration), automatic, reason);
    }
}
