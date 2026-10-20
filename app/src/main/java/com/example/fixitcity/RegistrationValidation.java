package com.example.fixitcity;

/** Registration rules that do not depend on Android views or network access. */
public final class RegistrationValidation {
    public static final int MIN_PASSWORD_CHARACTERS = 8;
    public static final int MAX_EMAIL_CHARACTERS = 255;

    private RegistrationValidation() { }

    /**
     * Blank is allowed. Local South African numbers become +27 numbers.
     * International numbers need an explicit country code. This checks format,
     * not whether a number is assigned or belongs to the person registering.
     */
    public static String normalizeOptionalPhone(String input) {
        String value = input == null ? "" : input.trim();
        if (value.isEmpty()) return "";
        String number = value.replaceAll("[\\s\\p{Zs}().-]", "");
        if (number.isEmpty()) {
            // Whitespace alone is blank; punctuation alone is an invalid number.
            if (value.codePoints().allMatch(c -> Character.isWhitespace(c)
                    || Character.isSpaceChar(c))) return "";
            throw new IllegalArgumentException(phoneError());
        }
        if (number.matches("0[1-9][0-9]{8}")) {
            return "+27" + number.substring(1);
        }
        if (number.matches("\\+[1-9][0-9]{6,14}")) return number;
        throw new IllegalArgumentException(phoneError());
    }

    private static String phoneError() {
        return "Leave this blank, or enter a 10-digit South African number "
                + "starting with 0, or an international number starting with +.";
    }

    /** Returns a useful error, or null when the local password rules pass. */
    public static String passwordError(String password) {
        if (password == null || password.isEmpty()
                || password.codePoints().allMatch(c -> Character.isWhitespace(c)
                || Character.isSpaceChar(c))) {
            return "Please enter a password, not only spaces.";
        }
        if (password.codePointCount(0, password.length()) < MIN_PASSWORD_CHARACTERS) {
            return "Use at least " + MIN_PASSWORD_CHARACTERS + " characters.";
        }
        boolean lower = false, upper = false, digit = false, symbol = false;
        for (int i = 0; i < password.length(); ) {
            int codePoint = password.codePointAt(i);
            i += Character.charCount(codePoint);
            if (codePoint >= 'a' && codePoint <= 'z') lower = true;
            if (codePoint >= 'A' && codePoint <= 'Z') upper = true;
            if (codePoint >= '0' && codePoint <= '9') digit = true;
            // Supabase's allowed symbols are the printable ASCII punctuation characters.
            // Spaces and non-ASCII characters do not satisfy its symbol requirement.
            if (codePoint >= '!' && codePoint <= '~'
                    && !((codePoint >= 'a' && codePoint <= 'z')
                    || (codePoint >= 'A' && codePoint <= 'Z')
                    || (codePoint >= '0' && codePoint <= '9'))) symbol = true;
        }
        if (!lower) return "Add at least one lowercase letter (a-z).";
        if (!upper) return "Add at least one uppercase letter (A-Z).";
        if (!digit) return "Add at least one number (0-9).";
        if (!symbol) return "Add at least one symbol, such as ! or @.";
        // Preserve the exact password; the server remains authoritative.
        return null;
    }
}
