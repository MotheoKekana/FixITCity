package com.example.fixitcity.data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/** Displays local wall times as stored, and converts explicitly offset timestamps. */
public final class NotificationDates {
    private NotificationDates() {}

    public static String format(String raw) {
        return format(raw, ZoneId.systemDefault(), Locale.getDefault());
    }

    static String format(String raw, ZoneId zone, Locale locale) {
        if (raw == null || raw.trim().isEmpty()) return "Date unavailable";
        String value = raw.trim();
        if (value.length() > 10 && value.charAt(10) == ' ') {
            value = value.substring(0, 10) + "T" + value.substring(11);
        }
        // Accept PostgreSQL offset forms +00, +0000 and +00:00.
        value = value.replaceFirst("([+-]\\d{2})(\\d{2})$", "$1:$2");
        if (value.length() > 10) value = value.replaceFirst("([+-]\\d{2})$", "$1:00");
        try {
            Instant instant;
            if (value.endsWith("Z") || value.matches(".*[+-]\\d{2}:\\d{2}$")) {
                instant = OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                        .toInstant();
            } else {
                // This project's timestamp-without-time-zone rows are displayed
                // as the wall time sent by the API. Do not add two hours.
                instant = LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        .atZone(zone).toInstant();
            }
            return DateTimeFormatter.ofPattern("d MMM uuuu 'at' HH:mm", locale)
                    .withZone(zone).format(instant);
        } catch (DateTimeParseException invalid) {
            return "Date unavailable";
        }
    }
}
