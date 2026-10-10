package com.burcuduzen.dayflow.task;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.regex.*;

@Component
public class NaturalLanguageTaskParser {
    public record ParsedTask(String title, OffsetDateTime dueDate, TaskPriority priority,
                             Integer estimatedMinutes, Recurrence recurrence, List<String> recognized) {}

    private static final Pattern DURATION = Pattern.compile("(?iu)\\b(\\d{1,4})\\s*(dk|dakika|dakikalık|saat|saatlik)\\b");
    private static final Pattern ISO_DATE = Pattern.compile("\\b(\\d{4}-\\d{2}-\\d{2})\\b");
    private static final Pattern CLOCK = Pattern.compile("(?iu)\\b(?:saat\\s*)?([01]?\\d|2[0-3])[:.]([0-5]\\d)\\b");
    private static final Pattern HOUR = Pattern.compile("(?iu)\\bsaat\\s+([01]?\\d|2[0-3])\\b");
    private static final Map<String, DayOfWeek> DAYS = Map.of(
        "pazartesi", DayOfWeek.MONDAY, "salı", DayOfWeek.TUESDAY, "çarşamba", DayOfWeek.WEDNESDAY,
        "perşembe", DayOfWeek.THURSDAY, "cuma", DayOfWeek.FRIDAY, "cumartesi", DayOfWeek.SATURDAY,
        "pazar", DayOfWeek.SUNDAY);

    public ParsedTask parse(String input, ZoneId zone, ZonedDateTime now) {
        if (input == null || input.isBlank()) badRequest("Görev cümlesi boş olamaz.");
        String working = input.strip();
        List<String> recognized = new ArrayList<>();

        TaskPriority priority = TaskPriority.MEDIUM;
        if (contains(working, "acil|yüksek\\s+öncelik(?:li)?")) {
            priority = TaskPriority.HIGH; recognized.add("priority");
            working = remove(working, "acil|yüksek\\s+öncelik(?:li)?");
        } else if (contains(working, "düşük\\s+öncelik(?:li)?")) {
            priority = TaskPriority.LOW; recognized.add("priority");
            working = remove(working, "düşük\\s+öncelik(?:li)?");
        }

        Recurrence recurrence = Recurrence.NONE;
        if (contains(working, "her\\s+gün")) recurrence = Recurrence.DAILY;
        else if (contains(working, "her\\s+hafta")) recurrence = Recurrence.WEEKLY;
        else if (contains(working, "her\\s+ay")) recurrence = Recurrence.MONTHLY;
        if (recurrence != Recurrence.NONE) {
            recognized.add("recurrence");
            working = remove(working, "her\\s+(?:gün|hafta|ay)");
        }

        Integer minutes = null;
        Matcher duration = DURATION.matcher(working);
        if (duration.find()) {
            int amount = Integer.parseInt(duration.group(1));
            minutes = duration.group(2).toLowerCase(Locale.forLanguageTag("tr")).startsWith("saat")
                ? amount * 60 : amount;
            if (minutes <= 0 || minutes > 1440) badRequest("Tahmini süre 1 ile 1440 dakika arasında olmalıdır.");
            recognized.add("estimatedMinutes");
            working = duration.replaceFirst(" ");
        }

        LocalDate date = null;
        if (contains(working, "bugün")) {
            date = now.toLocalDate(); working = remove(working, "bugün"); recognized.add("date");
        } else if (contains(working, "yarın")) {
            date = now.toLocalDate().plusDays(1); working = remove(working, "yarın"); recognized.add("date");
        } else {
            Matcher isoDate = ISO_DATE.matcher(working);
            if (isoDate.find()) {
                try { date = LocalDate.parse(isoDate.group(1)); }
                catch (DateTimeParseException ex) { badRequest("Tarih geçersiz."); }
                working = isoDate.replaceFirst(" "); recognized.add("date");
            } else {
                for (var entry : DAYS.entrySet()) {
                    if (contains(working, entry.getKey())) {
                        date = now.toLocalDate().with(TemporalAdjusters.nextOrSame(entry.getValue()));
                        working = remove(working, entry.getKey()); recognized.add("date"); break;
                    }
                }
            }
        }

        LocalTime time = null;
        Matcher clock = CLOCK.matcher(working);
        if (clock.find()) {
            time = LocalTime.of(Integer.parseInt(clock.group(1)), Integer.parseInt(clock.group(2)));
            working = clock.replaceFirst(" "); recognized.add("time");
        } else {
            Matcher hour = HOUR.matcher(working);
            if (hour.find()) {
                time = LocalTime.of(Integer.parseInt(hour.group(1)), 0);
                working = hour.replaceFirst(" "); recognized.add("time");
            }
        }

        if (recurrence != Recurrence.NONE && date == null) date = now.toLocalDate();
        if (date != null && time == null) time = LocalTime.of(18, 0);
        if (date == null && time != null) {
            date = now.toLocalDate();
            if (!time.isAfter(now.toLocalTime())) date = date.plusDays(1);
        }
        if (date != null && recurrence != Recurrence.NONE && !date.atTime(time).atZone(zone).isAfter(now)) {
            date = date.plusDays(1);
        }
        OffsetDateTime dueDate = date == null ? null : date.atTime(time).atZone(zone).toOffsetDateTime();

        String title = working.replaceAll("[,:;]+", " ").replaceAll("\\s+", " ").strip();
        title = title.replaceAll("(?iu)^(görev|yapılacak)\\s+", "").strip();
        if (title.isBlank()) badRequest("Görev başlığı anlaşılamadı.");
        if (title.length() > 200) badRequest("Görev başlığı 200 karakterden uzun olamaz.");
        return new ParsedTask(title, dueDate, priority, minutes, recurrence, List.copyOf(recognized));
    }

    private boolean contains(String value, String expression) {
        return Pattern.compile("(?iu)\\b(?:" + expression + ")\\b").matcher(value).find();
    }

    private String remove(String value, String expression) {
        return Pattern.compile("(?iu)\\b(?:" + expression + ")\\b").matcher(value).replaceFirst(" ");
    }

    private void badRequest(String message) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
