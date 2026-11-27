package pe.farmaguard.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.farmaguard.model.InteraccionYberia;
import pe.farmaguard.model.MedicamentoYberia;
import pe.farmaguard.model.NivelRiesgoYberia;
import pe.farmaguard.repository.InteraccionYberiaRepository;
import pe.farmaguard.util.SinonimosYberia;
import pe.farmaguard.util.TextoYberia;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CompatibilidadYberiaService {
    private static final String ANTIBIOTICO_YBERIA = "Antibiótico";
    private static final String SUPLEMENTO_YBERIA = "Suplemento vitamínico";
    private static final String PREFIJO_GRUPO_YBERIA = "grupo:";

    private static final Set<String> CATEGORIAS_COMBINABLES_YBERIA = Set.of(
            "antihipertensivo", "diuretico", "antidiabetico", "hipolipemiante (colesterol)",
            "anticonvulsivante", "antianemico y prenatal", "prostata", "suplemento vitaminico",
            "antiparasitario", "malestar estomacal", "probiotico", "rehidratacion oral",
            "antianginoso", "antiarritmico", "inmunosupresor");

    private static final Set<String> CATEGORIAS_BAJO_RIESGO_YBERIA = Set.of(
            "suplemento vitaminico", "antianemico y prenatal", "probiotico", "rehidratacion oral", "malestar estomacal");

    private final InteraccionYberiaRepository interaccionYberiaRepository;

    public CompatibilidadYberiaService(InteraccionYberiaRepository interaccionYberiaRepository) {
        this.interaccionYberiaRepository = interaccionYberiaRepository;
    }

    public ResultadoAnalisisYberia analizar(List<ItemCompraYberia> items, List<String> noReconocidos) {
        List<InteraccionYberia> reglas = interaccionYberiaRepository.findAll();
        List<AlertaYberia> alertas = new ArrayList<>();
        List<MedicamentoYberia> medicamentos = items.stream().map(ItemCompraYberia::medicamentoYberia).distinct().toList();

        alertas.addAll(revisarDuplicidades(medicamentos));

        Map<String, LinkedHashSet<MedicamentoYberia>> redundantesPorCategoria = new LinkedHashMap<>();
        for (int i = 0; i < medicamentos.size(); i++) {
            for (int j = i + 1; j < medicamentos.size(); j++) {
                MedicamentoYberia a = medicamentos.get(i);
                MedicamentoYberia b = medicamentos.get(j);
                List<AlertaYberia> delPar = compararPar(a, b, reglas);
                alertas.addAll(delPar);
                if (delPar.isEmpty() && !compartenPrincipio(a, b) && TextoYberia.iguales(a.getCategoria(), b.getCategoria())
                        && !CATEGORIAS_COMBINABLES_YBERIA.contains(TextoYberia.normalizar(a.getCategoria()))) {
                    LinkedHashSet<MedicamentoYberia> grupo = redundantesPorCategoria
                            .computeIfAbsent(a.getCategoria(), k -> new LinkedHashSet<>());
                    grupo.add(a);
                    grupo.add(b);
                }
            }
        }
        redundantesPorCategoria.forEach((categoria, grupo) -> alertas.add(new AlertaYberia(NivelRiesgoYberia.PRECAUCION,
                "Posible venta redundante: " + nombres(grupo),
                (grupo.size() == 2 ? "Ambos son" : "Todos son") + " del grupo \"" + categoria
                        + "\" y suelen usarse para lo mismo. Normalmente basta con uno. "
                        + "Pregunta por qué te ofrecen " + (grupo.size() == 2 ? "los dos" : "todos") + " antes de comprarlos.")));

        medicamentos.forEach(m -> alertas.addAll(revisarProducto(m)));
        alertas.addAll(revisarDosisDiaria(items));

        if (!noReconocidos.isEmpty()) {
            alertas.add(new AlertaYberia(NivelRiesgoYberia.PRECAUCION, "Productos no reconocidos",
                    "No encontramos en el catálogo: " + String.join(", ", noReconocidos)
                            + ". FarmaGuard no puede evaluarlos: verifica el nombre o consulta al químico farmacéutico."));
        }

        List<AlertaYberia> unicas = new ArrayList<>(new LinkedHashSet<>(alertas));
        alertas.clear();
        alertas.addAll(unicas);
        alertas.sort(Comparator.comparing(AlertaYberia::nivelYberia).reversed());
        NivelRiesgoYberia general = alertas.stream()
                .map(AlertaYberia::nivelYberia)
                .max(Comparator.naturalOrder())
                .orElse(NivelRiesgoYberia.SEGURO);
        return new ResultadoAnalisisYberia(List.copyOf(items), List.copyOf(alertas), general);
    }

    public List<InteraccionConocidaYberia> interaccionesConocidas(MedicamentoYberia medicamentoYberia) {
        List<InteraccionConocidaYberia> conocidas = new ArrayList<>();
        for (InteraccionYberia reglaYberia : interaccionYberiaRepository.findAll()) {
            boolean ladoA = aplicaA(reglaYberia.getPrincipioA(), medicamentoYberia);
            boolean ladoB = aplicaA(reglaYberia.getPrincipioB(), medicamentoYberia);
            if (!ladoA && !ladoB) {
                continue;
            }
            String otro = ladoA ? reglaYberia.getPrincipioB() : reglaYberia.getPrincipioA();
            String con = esGrupo(otro)
                    ? "Con otro medicamento del grupo " + grupo(otro).toLowerCase()
                    : "Con " + otro.toLowerCase();
            conocidas.add(new InteraccionConocidaYberia(con, reglaYberia.getNivelRiesgo(), reglaYberia.getExplicacion()));
        }
        conocidas.sort(Comparator.comparing(InteraccionConocidaYberia::nivelYberia).reversed());
        return conocidas;
    }

    private static List<AlertaYberia> revisarDuplicidades(List<MedicamentoYberia> medicamentos) {
        Map<String, String> nombrePrincipio = new LinkedHashMap<>();
        Map<String, LinkedHashSet<MedicamentoYberia>> porPrincipio = new LinkedHashMap<>();
        for (MedicamentoYberia m : medicamentos) {
            for (String principio : m.getPrincipios()) {
                String clave = SinonimosYberia.canonico(principio);
                nombrePrincipio.putIfAbsent(clave, principio);
                porPrincipio.computeIfAbsent(clave, k -> new LinkedHashSet<>()).add(m);
            }
        }
        Map<Set<MedicamentoYberia>, List<String>> principiosPorGrupo = new LinkedHashMap<>();
        porPrincipio.forEach((clave, productos) -> {
            if (productos.size() > 1) {
                principiosPorGrupo.computeIfAbsent(productos, k -> new ArrayList<>()).add(nombrePrincipio.get(clave));
            }
        });

        List<AlertaYberia> alertas = new ArrayList<>();
        principiosPorGrupo.forEach((productos, principios) -> {
            boolean dos = productos.size() == 2;
            boolean bajoRiesgo = productos.stream()
                    .allMatch(m -> CATEGORIAS_BAJO_RIESGO_YBERIA.contains(TextoYberia.normalizar(m.getCategoria())));
            String componentes = String.join(", ", principios).toLowerCase();
            alertas.add(new AlertaYberia(bajoRiesgo ? NivelRiesgoYberia.PRECAUCION : NivelRiesgoYberia.PELIGRO,
                    "Duplicidad: " + nombres(productos),
                    (dos ? "Ambos contienen " : "Todos contienen ") + componentes
                            + ". Es el mismo componente con otro nombre o presentación: tomarlos juntos "
                            + (bajoRiesgo ? "no suma beneficio" : "duplica la dosis")
                            + ". Es una venta redundante, basta con uno."));
        });
        return alertas;
    }

    private static boolean compartenPrincipio(MedicamentoYberia a, MedicamentoYberia b) {
        return a.getPrincipios().stream().anyMatch(pa -> b.getPrincipios().stream().anyMatch(pb -> TextoYberia.iguales(pa, pb)));
    }

    private static String nombres(Collection<MedicamentoYberia> productos) {
        return productos.stream().map(MedicamentoYberia::getNombre).collect(Collectors.joining(" + "));
    }

    private List<AlertaYberia> compararPar(MedicamentoYberia a, MedicamentoYberia b, List<InteraccionYberia> reglas) {
        List<AlertaYberia> alertas = new ArrayList<>(aplicarReglas(a, b, reglas.stream().filter(r -> !esReglaDeGrupo(r)).toList()));
        if (alertas.isEmpty() && !compartenPrincipio(a, b)) {
            alertas.addAll(aplicarReglas(a, b, reglas.stream().filter(CompatibilidadYberiaService::esReglaDeGrupo).toList()));
        }
        return alertas;
    }

    private static List<AlertaYberia> aplicarReglas(MedicamentoYberia a, MedicamentoYberia b, List<InteraccionYberia> reglas) {
        boolean compuesto = a.getPrincipios().size() > 1 || b.getPrincipios().size() > 1;
        List<AlertaYberia> alertas = new ArrayList<>();
        for (InteraccionYberia reglaYberia : reglas) {
            String[] componentes = componentesAfectados(reglaYberia, a, b);
            if (componentes != null) {
                String prefijo = compuesto ? "(" + componentes[0] + " con " + componentes[1] + ") " : "";
                alertas.add(new AlertaYberia(reglaYberia.getNivelRiesgo(), "Interacción: " + a.getNombre() + " + " + b.getNombre(),
                        prefijo + reglaYberia.getExplicacion()));
            }
        }
        return alertas;
    }

    private static String[] componentesAfectados(InteraccionYberia reglaYberia, MedicamentoYberia a, MedicamentoYberia b) {
        for (String pa : a.getPrincipios()) {
            for (String pb : b.getPrincipios()) {
                if (TextoYberia.iguales(pa, pb)) {
                    continue;
                }
                boolean directo = coincide(reglaYberia.getPrincipioA(), a, pa) && coincide(reglaYberia.getPrincipioB(), b, pb);
                boolean inverso = coincide(reglaYberia.getPrincipioA(), b, pb) && coincide(reglaYberia.getPrincipioB(), a, pa);
                if (directo || inverso) {
                    return new String[]{pa, pb};
                }
            }
        }
        return null;
    }

    private static boolean coincide(String termino, MedicamentoYberia medicamentoYberia, String principio) {
        return esGrupo(termino)
                ? TextoYberia.iguales(grupo(termino), medicamentoYberia.getCategoria())
                        && TextoYberia.iguales(principio, medicamentoYberia.getPrincipioPrincipal())
                : TextoYberia.iguales(termino, principio);
    }

    private static boolean aplicaA(String termino, MedicamentoYberia medicamentoYberia) {
        return medicamentoYberia.getPrincipios().stream().anyMatch(p -> coincide(termino, medicamentoYberia, p));
    }

    private static boolean esReglaDeGrupo(InteraccionYberia reglaYberia) {
        return esGrupo(reglaYberia.getPrincipioA()) || esGrupo(reglaYberia.getPrincipioB());
    }

    private static boolean esGrupo(String termino) {
        return TextoYberia.normalizar(termino).startsWith(PREFIJO_GRUPO_YBERIA);
    }

    private static String grupo(String termino) {
        return termino.substring(termino.indexOf(':') + 1).trim();
    }

    private List<AlertaYberia> revisarProducto(MedicamentoYberia m) {
        List<AlertaYberia> alertas = new ArrayList<>();

        if (m.isRequiereReceta()) {
            if (TextoYberia.iguales(m.getCategoria(), ANTIBIOTICO_YBERIA)) {
                alertas.add(new AlertaYberia(NivelRiesgoYberia.PRECAUCION, "Antibiótico: " + m.getNombre(),
                        "En Perú los antibióticos se venden solo con receta médica. Tomarlos sin necesidad "
                                + "(por ejemplo, para una gripe, que es causada por virus) no ayuda y genera bacterias resistentes. "
                                + "Si te lo ofrecieron sin receta, consulta primero a un médico."));
            } else {
                alertas.add(new AlertaYberia(NivelRiesgoYberia.PRECAUCION, "Requiere receta médica: " + m.getNombre(),
                        m.getNombre() + " es de venta con receta. Si nadie te lo recetó y te lo ofrecen en la botica, "
                                + "pide una explicación o consulta a un médico antes de comprarlo."));
            }
        }

        if (m.getDosisMg() != null && m.getDosisMaxUnidadMg() != null && m.getDosisMg() > m.getDosisMaxUnidadMg()) {
            alertas.add(new AlertaYberia(NivelRiesgoYberia.PRECAUCION, "Concentración alta: " + m.getNombre(),
                    "Cada unidad tiene " + mg(m.getDosisMg()) + " de " + m.getPrincipioPrincipal().toLowerCase()
                            + ", más que la dosis habitual sin receta (" + mg(m.getDosisMaxUnidadMg()) + "). "
                            + "Para una molestia común suele bastar una presentación menor. Pregunta si realmente la necesitas."));
        }

        if (TextoYberia.iguales(m.getCategoria(), SUPLEMENTO_YBERIA)) {
            alertas.add(new AlertaYberia(NivelRiesgoYberia.PRECAUCION, "Posible venta innecesaria: " + m.getNombre(),
                    "Los suplementos suelen ofrecerse como venta adicional. Solo hacen falta si tienes una deficiencia "
                            + "comprobada o te los indicó un médico. Una alimentación variada normalmente es suficiente."));
        }
        return alertas;
    }

    private List<AlertaYberia> revisarDosisDiaria(List<ItemCompraYberia> items) {
        Map<String, List<ItemCompraYberia>> porPrincipio = new LinkedHashMap<>();
        for (ItemCompraYberia item : items) {
            if (item.medicamentoYberia().getDosisMg() != null) {
                porPrincipio.computeIfAbsent(SinonimosYberia.canonico(item.medicamentoYberia().getPrincipioPrincipal()),
                        k -> new ArrayList<>()).add(item);
            }
        }

        List<AlertaYberia> alertas = new ArrayList<>();
        for (List<ItemCompraYberia> grupo : porPrincipio.values()) {
            OptionalDouble limite = grupo.stream()
                    .map(i -> i.medicamentoYberia().getDosisMaxDiariaMg())
                    .filter(Objects::nonNull)
                    .mapToDouble(Double::doubleValue)
                    .max();
            if (limite.isEmpty()) {
                continue;
            }
            double total = grupo.stream()
                    .mapToDouble(i -> i.medicamentoYberia().getDosisMg() * i.unidadesDiaYberia())
                    .sum();
            if (total > limite.getAsDouble()) {
                String principio = grupo.get(0).medicamentoYberia().getPrincipioPrincipal();
                String desglose = grupo.stream()
                        .map(i -> i.medicamentoYberia().getNombre() + ": " + i.unidadesDiaYberia() + " × " + mg(i.medicamentoYberia().getDosisMg()))
                        .collect(Collectors.joining(", "));
                alertas.add(new AlertaYberia(NivelRiesgoYberia.PELIGRO, "Dosis diaria excesiva de " + principio.toLowerCase(),
                        "Con lo que planeas tomar (" + desglose + ") sumas " + mg(total) + " de " + principio.toLowerCase()
                                + " al día. El máximo recomendado para un adulto es " + mg(limite.getAsDouble())
                                + ". Reduce las unidades o elimina los productos repetidos."));
            }
        }
        return alertas;
    }

    private static String mg(double valor) {
        return valor == Math.rint(valor) ? String.format("%.0f mg", valor) : String.format("%.1f mg", valor);
    }
}
