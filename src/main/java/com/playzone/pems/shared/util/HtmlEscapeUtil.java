package com.playzone.pems.shared.util;

public final class HtmlEscapeUtil {

    private HtmlEscapeUtil() {}

    public static String escapar(String valor) {
        if (valor == null) return "";
        return valor
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
