package pe.farmaguard.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.farmaguard.model.MedicamentoYberia;
import pe.farmaguard.repository.MedicamentoYberiaRepository;
import pe.farmaguard.util.SinonimosYberia;
import pe.farmaguard.util.TextoYberia;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
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
    private static final String SEPARADORES_LINEA_YBERIA = "[,;\\n\\r]+";
    private static final String CONCENTRACION_YBERIA = "\\d+([.,]\\d+)?\\s*(mg|g|ml|mcg)?\\b";

    private final MedicamentoYberiaRepository repositorioYberia;

    public MedicamentoYberiaService(MedicamentoYberiaRepository repositorioYberia) {
        this.repositorioYberia = repositorioYberia;
    }

    public List<MedicamentoYberia> listarTodos() {
        return repositorioYberia.findAll(Sort.by("nombreYberia"));
    }

    public Optional<MedicamentoYberia> buscar(String texto) {
        List<MedicamentoYberia> catalogo = repositorioYberia.findAll(Sort.by("idYberia"));
        String consulta = SinonimosYberia.canonico(texto);
        return buscarEn(catalogo, consulta)
                .or(() -> buscarEn(catalogo, SinonimosYberia.canonico(TextoYberia.normalizar(texto).replaceAll(CONCENTRACION_YBERIA, ""))));
    }

    public InterpretacionListaYberia interpretarLista(String texto) {
        List<MedicamentoYberia> encontrados = new ArrayList<>();
        List<String> noReconocidos = new ArrayList<>();
        for (String linea : texto.split(SEPARADORES_LINEA_YBERIA)) {
            String renglon = limpiar(linea);
            if (renglon.isEmpty()) {
                continue;
            }
            Optional<MedicamentoYberia> completo = renglon.contains("+") ? buscarCompuesto(renglon) : Optional.empty();
            List<String> partes = completo.isPresent() ? List.of(renglon) : List.of(renglon.split("\\+"));
            for (String parte : partes) {
                String nombre = limpiar(parte);
                if (nombre.isEmpty()) {
                    continue;
                }
                Optional<MedicamentoYberia> hallado = completo.isPresent() ? completo : buscar(nombre);
                hallado.ifPresentOrElse(m -> {
                    if (!encontrados.contains(m)) {
                        encontrados.add(m);
                    }
                }, () -> noReconocidos.add(nombre));
            }
        }
        return new InterpretacionListaYberia(encontrados, noReconocidos);
    }

    private static String limpiar(String texto) {
        return texto
                .replaceFirst("^[\\s\\-*•\\d.)]+", "")
                .replaceFirst("(?i)\\s+x\\s*\\d+$", "")
                .trim();
    }

    private Optional<MedicamentoYberia> buscarCompuesto(String renglon) {
        Set<String> buscados = componentes(renglon);
        return repositorioYberia.findAll(Sort.by("idYberia")).stream()
                .filter(m -> componentes(m.getNombre()).equals(buscados) || componentes(m.getPrincipioActivo()).equals(buscados))
                .findFirst();
    }

    private static Set<String> componentes(String texto) {
        return Arrays.stream(texto.split("\\+"))
                .map(p -> SinonimosYberia.canonico(TextoYberia.normalizar(p).replaceAll(CONCENTRACION_YBERIA, "").replaceAll("[^\\p{L}\\s]", " ")))
                .filter(p -> !p.isEmpty())
                .collect(Collectors.toSet());
    }

    public InterpretacionListaYberia interpretarFrase(String texto) {
        List<MedicamentoYberia> catalogo = repositorioYberia.findAll(Sort.by("idYberia"));
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
        return new InterpretacionListaYberia(encontrados, List.of());
    }

    private static String normalizarParaFrase(String texto) {
        return SinonimosYberia.canonico(texto)
                .replaceAll("(\\d)\\s*(mg|g|ml|mcg)\\b", "$1 $2")
                .replaceAll("[^\\p{L}\\d.]+", " ")
                .trim();
    }

    private static Optional<MedicamentoYberia> buscarEn(List<MedicamentoYberia> catalogo, String consulta) {
        if (consulta.isEmpty()) {
            return Optional.empty();
        }
        Optional<MedicamentoYberia> resultado = primero(catalogo, m -> SinonimosYberia.canonico(m.getNombre()).equals(consulta))
                .or(() -> primero(catalogo, m -> SinonimosYberia.canonico(m.getPrincipioActivo()).equals(consulta)))
                .or(() -> primero(catalogo, m -> SinonimosYberia.canonico(m.getNombre()).startsWith(consulta)));
        if (resultado.isPresent() || consulta.length() < 4) {
            return resultado;
        }
        return primero(catalogo, m -> SinonimosYberia.canonico(m.getNombre()).contains(consulta)
                || SinonimosYberia.canonico(m.getPrincipioActivo()).contains(consulta));
    }

    private static Optional<MedicamentoYberia> primero(List<MedicamentoYberia> catalogo, Predicate<MedicamentoYberia> condicion) {
        return catalogo.stream().filter(condicion).findFirst();
    }
}
