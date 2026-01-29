package org.schabi.newpipe.brave.misc;

public final class BraveStringEscapeUtils {
    private BraveStringEscapeUtils() {

    }

    public static String unescapeJava(
            final String escaped) {
        if (escaped == null) {
            return null;
        }

        final StringBuilder sb = new StringBuilder(escaped.length());
        for (int i = 0; i < escaped.length(); i++) {
            final char c = escaped.charAt(i);
            if (c == '\\' && i + 1 < escaped.length()) {
                final char next = escaped.charAt(++i);
                switch (next) {
                    case 'b' -> sb.append('\b');
                    case 't' -> sb.append('\t');
                    case 'n' -> sb.append('\n');
                    case 'f' -> sb.append('\f');
                    case 'r' -> sb.append('\r');
                    case '"' -> sb.append('"');
                    case '\'' -> sb.append('\'');
                    case '\\' -> sb.append('\\');
                    case 'u' -> {
                        if (i + 4 < escaped.length()) {
                            final String hex = escaped.substring(i + 1, i + 5);
                            sb.append((char) Integer.parseInt(hex, 16));
                            i += 4;
                        } else {
                            sb.append("\\u");
                        }
                    }
                    default -> sb.append('\\').append(next);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
