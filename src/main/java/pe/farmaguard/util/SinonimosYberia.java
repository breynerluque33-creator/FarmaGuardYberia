package pe.farmaguard.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class SinonimosYberia {
    private static final String ARCHIVO_YBERIA = "/sinonimos.txt";
    private static final int MIN_PREFIJO_YBERIA = 3;

    private static final Map<String, String> CANONICO_YBERIA = new LinkedHashMap<>();
    private static final Map<String, List<String>> OTROS_NOMBRES_YBERIA = new LinkedHashMap<>();
    private static final Map<String, List<String>> MARCAS_YBERIA = new LinkedHashMap<>();
    private static final Pattern PATRON_YBERIA;
    private static final Map<String, String> CACHE_YBERIA = new ConcurrentHashMap<>();

    static {
        cargar();
        String alternativas = CANONICO_YBERIA.keySet().stream()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .map(Pattern::quote)
                .collect(Collectors.joining("|"));
        PATRON_YBERIA = alternativas.isEmpty() ? null
                : Pattern.compile("(?<![\\p{L}\\d])(?:" + alternativas + ")(?![\\p{L}\\d])");
    }

    private SinonimosYberia() {
    }

    public static String canonico(String texto) {
        String normalizado = preparar(texto);
        if (PATRON_YBERIA == null || normalizado.isEmpty()) {
            return normalizado;
        }
        return CACHE_YBERIA.computeIfAbsent(normalizado, t -> {
            Matcher matcher = PATRON_YBERIA.matcher(t);
            return matcher.replaceAll(r -> Matcher.quoteReplacement(CANONICO_YBERIA.get(r.group())));
        });
    }

    public static Set<String> terminosDeBusqueda(String consulta) {
        Set<String> terminos = new LinkedHashSet<>();
        String canonica = canonico(consulta);
        if (canonica.isEmpty()) {
            return terminos;
        }
        terminos.add(canonica);
        String normalizada = preparar(consulta);
        if (normalizada.length() >= MIN_PREFIJO_YBERIA) {
            CANONICO_YBERIA.forEach((clave, valor) -> {
                if (clave.startsWith(normalizada)) {
                    terminos.add(valor);
                }
            });
        }
        return terminos;
    }

    public static boolean coincide(String texto, Set<String> terminos) {
        String objetivo = canonico(texto);
        return terminos.stream().anyMatch(objetivo::contains);
    }

    public static List<String> otrosNombres(String principio) {
        return OTROS_NOMBRES_YBERIA.getOrDefault(canonico(principio), List.of());
    }

    public static List<String> marcas(String principio) {
        return MARCAS_YBERIA.getOrDefault(canonico(principio), List.of());
    }

    private static String preparar(String texto) {
        return TextoYberia.normalizar(texto)
                .replaceAll("(?<=\\p{L})[.\\-]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static void cargar() {
        try (InputStream entrada = SinonimosYberia.class.getResourceAsStream(ARCHIVO_YBERIA)) {
            if (entrada == null) {
                return;
            }
            BufferedReader lector = new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8));
            Map<String, List<String>> seccion = OTROS_NOMBRES_YBERIA;
            String linea;
            while ((linea = lector.readLine()) != null) {
                linea = linea.strip();
                if (linea.isEmpty() || linea.startsWith("#")) {
                    continue;
                }
                if (linea.equalsIgnoreCase("[sinonimos]")) {
                    seccion = OTROS_NOMBRES_YBERIA;
                    continue;
                }
                if (linea.equalsIgnoreCase("[marcas]")) {
                    seccion = MARCAS_YBERIA;
                    continue;
                }
                int igual = linea.indexOf('=');
                if (igual < 0) {
                    continue;
                }
                String canonico = preparar(linea.substring(0, igual));
                CANONICO_YBERIA.putIfAbsent(canonico, canonico);
                for (String alias : linea.substring(igual + 1).split(",")) {
                    String limpio = alias.strip();
                    String clave = preparar(limpio);
                    if (clave.isEmpty() || clave.equals(canonico)) {
                        continue;
                    }
                    CANONICO_YBERIA.put(clave, canonico);
                    seccion.computeIfAbsent(canonico, k -> new ArrayList<>()).add(limpio);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + ARCHIVO_YBERIA, e);
        }
    }
}