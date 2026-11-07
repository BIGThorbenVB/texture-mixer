package com.texturemixer.core;

import java.util.regex.Pattern;

/** Glob-Regel für Pfade innerhalb eines Packs. "*" = ohne "/", "**" = mit "/". Pfade ohne "assets/" gelten für assets/minecraft/. */
public final class GlobRule {
    private final String prefix;
    private final Pattern pattern;

    private GlobRule(String prefix, Pattern pattern) {
        this.prefix = prefix;
        this.pattern = pattern;
    }

    public static String absolute(String path) {
        return path.startsWith("assets/") ? path : "assets/minecraft/" + path;
    }

    public static GlobRule of(String glob) {
        String g = absolute(glob);
        int w = -1;
        for (int i = 0; i < g.length(); i++) {
            char c = g.charAt(i);
            if (c == '*' || c == '?') { w = i; break; }
        }
        String prefix = w < 0 ? g : g.substring(0, w);
        StringBuilder re = new StringBuilder();
        for (int i = 0; i < g.length(); i++) {
            char c = g.charAt(i);
            if (c == '*') {
                if (i + 1 < g.length() && g.charAt(i + 1) == '*') { re.append(".*"); i++; }
                else re.append("[^/]*");
            } else if (c == '?') {
                re.append("[^/]");
            } else {
                re.append(Pattern.quote(String.valueOf(c)));
            }
        }
        if (g.endsWith(".png")) re.append("(?:\\.mcmeta)?");
        return new GlobRule(prefix, Pattern.compile(re.toString()));
    }

    public boolean matches(String path) {
        return path.startsWith(prefix) && pattern.matcher(path).matches();
    }
}
