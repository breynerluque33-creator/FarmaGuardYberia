package pe.farmaguard.util;

import java.text.Normalizer;
import java.util.Locale;

public final class TextoYberia {
    private TextoYberia() {
    }

    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    public static boolean iguales(String a, String b) {
        return SinonimosYberia.canonico(a).equals(SinonimosYberia.canonico(b));
    }
}
