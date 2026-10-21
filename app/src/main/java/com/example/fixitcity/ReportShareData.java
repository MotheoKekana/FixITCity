package com.example.fixitcity;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class ReportShareData {
    public final String reference;
    public final String title;
    public final String status;
    public final boolean pendingSync;
    public final Integer priority;
    public final String sharedAt;

    public ReportShareData(String reference, String title, String status,
                           boolean pendingSync, Integer priority, ZonedDateTime now) {
        this.reference = reference == null ? "" : reference.trim();
        this.title = clean(title, "Municipal fault report", 160);
        this.status = pendingSync ? "Pending sync" : clean(status, "Status unavailable", 80);
        this.pendingSync = pendingSync;
        this.priority = pendingSync || priority == null || priority < 0 ? null : priority;
        this.sharedAt = now.format(DateTimeFormatter.ofPattern(
                "d MMM uuuu 'at' HH:mm z", Locale.getDefault()));
    }

    public boolean hasReference() {
        return !reference.isEmpty() && reference.length() <= 80
                && !reference.equalsIgnoreCase("Unavailable")
                && !reference.equals("FIX-0000-0000");
    }

    public String instructions() {
        return pendingSync
                ? "Saved on the sender's phone. This reference can be tracked once the report has synced."
                : "Open FixITCity, choose Track Report and enter the reference number above.";
    }

    public String caption() {
        return "FixITCity Report\n"
                + "Reference: " + reference + "\n"
                + "Issue: " + title + "\n"
                + "Status: " + status
                + (priority != null ? "\nPriority score: " + priority + "/100" : "")
                + "\n\n" + instructions()
                + "\nShared " + sharedAt
                + "\nThis is a saved snapshot; check the app for updates.";
    }

    private static String clean(String value, String fallback, int maxCharacters) {
        if (value == null) return fallback;
        String text = value.replaceAll("[\\s\\p{Zs}]+", " ").trim();
        if (text.isEmpty()) return fallback;
        int count = text.codePointCount(0, text.length());
        return count <= maxCharacters ? text
                : text.substring(0, text.offsetByCodePoints(0, maxCharacters - 1)) + "…";
    }
}
