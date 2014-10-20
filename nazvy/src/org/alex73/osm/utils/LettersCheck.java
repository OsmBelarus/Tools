package org.alex73.osm.utils;

import java.util.regex.Pattern;

public class LettersCheck {

    static final Pattern RE_ALLOWED_CHARS_BE = Pattern
            .compile("([^1234567890ЁЙЦУКЕНГШЎЗХФЫВАПРОЛДЖЭЯЧСМІТЬБЮ’ёйцукенгшўзхфывапролджэячсмітьбю \\/\\-]+)");
    static final Pattern RE_ALLOWED_CHARS_RU = Pattern
            .compile("([^1234567890ЁЙЦУКЕНГШЩЗХФЫВАПРОЛДЖЭЯЧСМИТЬБЮъёйцукенгшщзхфывапролджэячсмитьбю \\/\\-]+)");

    public static String checkBe(String str) {
        String r = RE_ALLOWED_CHARS_BE.matcher(str).replaceAll("<b><u>$1</u></b>");
        return r.equals(str) ? null : r;
    }

    public static String checkRu(String str) {
        String r = RE_ALLOWED_CHARS_RU.matcher(str).replaceAll("<b><u>$1</u></b>");
        return r.equals(str) ? null : r;
    }
}
