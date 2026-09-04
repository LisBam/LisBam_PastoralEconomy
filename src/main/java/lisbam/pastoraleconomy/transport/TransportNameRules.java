package lisbam.pastoraleconomy.transport;

import java.util.Collection;

/** Server-side alias validation and default-name construction. */
public final class TransportNameRules {
    public static final int MAX_CODE_POINTS = 24;

    private TransportNameRules() {
    }

    public static boolean isValidAlias(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        int codePoints = 0;
        boolean hasVisibleCharacter = false;
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            codePoints++;
            if (codePoints > MAX_CODE_POINTS || codePoint == 0x00A7 || Character.isISOControl(codePoint)) {
                return false;
            }
            if (!Character.isWhitespace(codePoint)) {
                hasVisibleCharacter = true;
            }
            offset += Character.charCount(codePoint);
        }
        return hasVisibleCharacter;
    }

    public static boolean isUniqueAlias(String candidate, Collection<String> existingAliases, String ignoredExisting) {
        if (existingAliases == null) {
            return true;
        }
        for (String existing : existingAliases) {
            if (candidate.equals(existing) && !candidate.equals(ignoredExisting)) {
                return false;
            }
        }
        return true;
    }

    public static String nextHomeAlias(Collection<String> existingAliases) {
        if (isUniqueAlias("家", existingAliases, null)) {
            return "家";
        }
        int suffix = 2;
        while (suffix < Integer.MAX_VALUE) {
            String candidate = "家 #" + suffix;
            if (isUniqueAlias(candidate, existingAliases, null)) {
                return candidate;
            }
            suffix++;
        }
        throw new IllegalStateException("No unique home alias remains.");
    }

    public static String selfBuiltAlias(int sequence, Collection<String> existingAliases) {
        int candidateSequence = Math.max(1, sequence);
        while (candidateSequence < Integer.MAX_VALUE) {
            String candidate = String.format(java.util.Locale.ROOT, "自建站点 #%03d", candidateSequence);
            if (isUniqueAlias(candidate, existingAliases, null)) {
                return candidate;
            }
            candidateSequence++;
        }
        throw new IllegalStateException("No unique self-built station alias remains.");
    }
}
