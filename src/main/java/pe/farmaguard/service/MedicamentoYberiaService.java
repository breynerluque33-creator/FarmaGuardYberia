package pe.farmaguard.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.farmaguard.model.MedicamentoYberia;
import pe.farmaguard.repository.MedicamentoYberiaRepository;
import pe.farmaguard.util.Texto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class MedicamentoYberiaService {

    private static final String SEPARADORES = "[,;\\n\\r+]+";
    private static final String CONCENTRACION = "\\d+([.,]\\d+)?\\s*(mg|g|ml|mcg)?\\b";

    private final MedicamentoYberiaRepository repositorioYberia;

    public MedicamentoYberiaService(MedicamentoYberiaRepository repositorioYberia) {
        this.repositorioYberia = repositorioYberia;
    }

    public List<MedicamentoYberia> listarTodos() {
        return repositorioYberia.findAll(Sort.by("nombre"));
    }

    public Optional<MedicamentoYberia> buscar(String texto) {
        List<MedicamentoYberia> catalogo = repositorioYberia.findAll(Sort.by("id"));
        String consulta = Texto.normalizar(texto);
        return buscarEn(catalogo, consulta)
                .or(() -> buscarEn(catalogo, consulta.replaceAll(CONCENTRACION, "").trim()));
    }

    public InterpretacionLista interpretarLista(String texto) {
        List<MedicamentoYberia> encontrados = new ArrayList<>();
        List<String> noReconocidos = new ArrayList<>();
        for (String linea : texto.split(SEPARADORES)) {
            String nombre = linea
                    .replaceFirst("^[\\s\\-*•\\d.)]+", "")
                    .replaceFirst("(?i)\\s+x\\s*\\d+$", "")
                    .trim();
            if (nombre.isEmpty()) {
                continue;
            }
            buscar(nombre).ifPresentOrElse(m -> {
                if (!encontrados.contains(m)) {
                    encontrados.add(m);
                }
            }, () -> noReconocidos.add(nombre));
        }
        return new InterpretacionLista(encontrados, noReconocidos);
    }

    public InterpretacionLista interpretarFrase(String texto) {
        List<MedicamentoYberia> catalogo = repositorioYberia.findAll(Sort.by("id"));
        Map<String, MedicamentoYberia> candidatos = new LinkedHashMap<>();
        for (MedicamentoYberia m : catalogo) {
            candidatos.putIfAbsent(normalizarParaFrase(m.getNombre()), m);
        }
        for (MedicamentoYberia m : catalogo) {
            if (m.getPrincipios().size() == 1) {
                candidatos.putIfAbsent(normalizarParaFrase(m.getPrincipioActivo()), m);
            }
        }

        StringBuilder restante = new StringBuilder(" " + normalizarParaFrase(texto) + " ");
        TreeMap<Integer, MedicamentoYberia> porPosicion = new TreeMap<>();
        candidatos.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<String, MedicamentoYberia> e) -> e.getKey().length()).reversed())
                .forEach(e -> {
                    Matcher matcher = Pattern.compile("(?<![\\p{L}\\d])" + Pattern.quote(e.getKey()) + "(?![\\p{L}\\d])")
                            .matcher(restante);
                    while (matcher.find()) {
                        porPosicion.putIfAbsent(matcher.start(), e.getValue());
                        restante.replace(matcher.start(), matcher.end(), " ".repeat(matcher.end() - matcher.start()));
                    }
                });
        List<MedicamentoYberia> encontrados = porPosicion.values().stream().distinct().toList();
        return new InterpretacionLista(encontrados, List.of());
    }

    private static String normalizarParaFrase(String texto) {
        return Texto.normalizar(texto)
                .replaceAll("(\\d)\\s*(mg|g|ml|mcg)\\b", "$1 $2")
                .replaceAll("[^\\p{L}\\d.]+", " ")
                .trim();
    }

    private static Optional<MedicamentoYberia> buscarEn(List<MedicamentoYberia> catalogo, String consulta) {
        if (consulta.isEmpty()) {
            return Optional.empty();
        }
        Optional<MedicamentoYberia> resultado = primero(catalogo, m -> Texto.normalizar(m.getNombre()).equals(consulta))
                .or(() -> primero(catalogo, m -> Texto.normalizar(m.getPrincipioActivo()).equals(consulta)))
                .or(() -> primero(catalogo, m -> Texto.normalizar(m.getNombre()).startsWith(consulta)));
        if (resultado.isPresent() || consulta.length() < 4) {
            return resultado;
        }
        return primero(catalogo, m -> Texto.normalizar(m.getNombre()).contains(consulta)
                || Texto.normalizar(m.getPrincipioActivo()).contains(consulta));
    }

    private static Optional<MedicamentoYberia> primero(List<MedicamentoYberia> catalogo, Predicate<MedicamentoYberia> condicion) {
        return catalogo.stream().filter(condicion).findFirst();
    }
}
