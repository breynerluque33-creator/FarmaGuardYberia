package pe.farmaguard.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.farmaguard.model.InteraccionYberia;
import pe.farmaguard.model.MedicamentoYberia;
import pe.farmaguard.model.NivelRiesgo;
import pe.farmaguard.repository.InteraccionYberiaRepository;
import pe.farmaguard.util.Texto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CompatibilidadYberiaService {

    private static final String ANTIBIOTICO = "Antibiótico";
    private static final String SUPLEMENTO = "Suplemento vitamínico";
    private static final String PREFIJO_GRUPO = "grupo:";

    private static final Set<String> CATEGORIAS_COMBINABLES = Set.of(
            "antihipertensivo", "diuretico", "antidiabetico", "hipolipemiante (colesterol)",
            "anticonvulsivante", "antianemico y prenatal", "prostata");

    private final InteraccionYberiaRepository interaccionYberiaRepository;

    public CompatibilidadYberiaService(InteraccionYberiaRepository interaccionYberiaRepository) {
        this.interaccionYberiaRepository = interaccionYberiaRepository;
    }

    public ResultadoAnalisis analizar(List<ItemCompra> items, List<String> noReconocidos) {
        List<InteraccionYberia> reglas = interaccionYberiaRepository.findAll();
        List<Alerta> alertas = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            for (int j = i + 1; j < items.size(); j++) {
                alertas.addAll(compararPar(items.get(i).medicamento(), items.get(j).medicamento(), reglas));
            }
        }
        items.forEach(item -> alertas.addAll(revisarProducto(item.medicamento())));
        alertas.addAll(revisarDosisDiaria(items));

        if (!noReconocidos.isEmpty()) {
            alertas.add(new Alerta(NivelRiesgo.PRECAUCION, "Productos no reconocidos",
                    "No encontramos en el catálogo: " + String.join(", ", noReconocidos)
                            + ". FarmaGuard no puede evaluarlos: verifica el nombre o consulta al químico farmacéutico."));
        }

        alertas.sort(Comparator.comparing(Alerta::nivel).reversed());
        NivelRiesgo general = alertas.stream()
                .map(Alerta::nivel)
                .max(Comparator.naturalOrder())
                .orElse(NivelRiesgo.SEGURO);
        return new ResultadoAnalisis(List.copyOf(items), List.copyOf(alertas), general);
    }

    public List<InteraccionConocida> interaccionesConocidas(MedicamentoYberia medicamentoYberia) {
        List<InteraccionConocida> conocidas = new ArrayList<>();
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
            conocidas.add(new InteraccionConocida(con, reglaYberia.getNivelRiesgo(), reglaYberia.getExplicacion()));
        }
        conocidas.sort(Comparator.comparing(InteraccionConocida::nivel).reversed());
        return conocidas;
    }

    private List<Alerta> compararPar(MedicamentoYberia a, MedicamentoYberia b, List<InteraccionYberia> reglas) {
        List<Alerta> alertas = new ArrayList<>();
        String par = a.getNombre() + " + " + b.getNombre();

        List<String> compartidos = a.getPrincipios().stream()
                .filter(pa -> b.getPrincipios().stream().anyMatch(pb -> Texto.iguales(pa, pb)))
                .toList();
        if (!compartidos.isEmpty()) {
            alertas.add(new Alerta(NivelRiesgo.PELIGRO, "Duplicidad: " + par,
                    "Ambos contienen " + String.join(", ", compartidos).toLowerCase()
                            + ". Es el mismo componente con otro nombre o presentación: tomarlos juntos duplica la dosis. "
                            + "Es una venta redundante, basta con uno de los dos."));
        }

        alertas.addAll(aplicarReglas(a, b, reglas.stream().filter(r -> !esReglaDeGrupo(r)).toList()));
        if (alertas.isEmpty()) {
            alertas.addAll(aplicarReglas(a, b, reglas.stream().filter(CompatibilidadYberiaService::esReglaDeGrupo).toList()));
        }

        if (alertas.isEmpty() && Texto.iguales(a.getCategoria(), b.getCategoria())
                && !CATEGORIAS_COMBINABLES.contains(Texto.normalizar(a.getCategoria()))) {
            alertas.add(new Alerta(NivelRiesgo.PRECAUCION, "Posible venta redundante: " + par,
                    "Ambos son del grupo \"" + a.getCategoria() + "\" y sirven para lo mismo. Normalmente basta con uno. "
                            + "Pregunta por qué te ofrecen los dos antes de comprarlos."));
        }
        return alertas;
    }

    private static List<Alerta> aplicarReglas(MedicamentoYberia a, MedicamentoYberia b, List<InteraccionYberia> reglas) {
        boolean compuesto = a.getPrincipios().size() > 1 || b.getPrincipios().size() > 1;
        List<Alerta> alertas = new ArrayList<>();
        for (InteraccionYberia reglaYberia : reglas) {
            String[] componentes = componentesAfectados(reglaYberia, a, b);
            if (componentes != null) {
                String prefijo = compuesto ? "(" + componentes[0] + " con " + componentes[1] + ") " : "";
                alertas.add(new Alerta(reglaYberia.getNivelRiesgo(), "Interacción: " + a.getNombre() + " + " + b.getNombre(),
                        prefijo + reglaYberia.getExplicacion()));
            }
        }
        return alertas;
    }

    private static String[] componentesAfectados(InteraccionYberia reglaYberia, MedicamentoYberia a, MedicamentoYberia b) {
        for (String pa : a.getPrincipios()) {
            for (String pb : b.getPrincipios()) {
                if (Texto.iguales(pa, pb)) {
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
                ? Texto.iguales(grupo(termino), medicamentoYberia.getCategoria())
                : Texto.iguales(termino, principio);
    }

    private static boolean aplicaA(String termino, MedicamentoYberia medicamentoYberia) {
        return medicamentoYberia.getPrincipios().stream().anyMatch(p -> coincide(termino, medicamentoYberia, p));
    }

    private static boolean esReglaDeGrupo(InteraccionYberia reglaYberia) {
        return esGrupo(reglaYberia.getPrincipioA()) || esGrupo(reglaYberia.getPrincipioB());
    }

    private static boolean esGrupo(String termino) {
        return Texto.normalizar(termino).startsWith(PREFIJO_GRUPO);
    }

    private static String grupo(String termino) {
        return termino.substring(termino.indexOf(':') + 1).trim();
    }

    private List<Alerta> revisarProducto(MedicamentoYberia m) {
        List<Alerta> alertas = new ArrayList<>();

        if (m.isRequiereReceta()) {
            if (Texto.iguales(m.getCategoria(), ANTIBIOTICO)) {
                alertas.add(new Alerta(NivelRiesgo.PRECAUCION, "Antibiótico: " + m.getNombre(),
                        "En Perú los antibióticos se venden solo con receta médica. Tomarlos sin necesidad "
                                + "(por ejemplo, para una gripe, que es causada por virus) no ayuda y genera bacterias resistentes. "
                                + "Si te lo ofrecieron sin receta, consulta primero a un médico."));
            } else {
                alertas.add(new Alerta(NivelRiesgo.PRECAUCION, "Requiere receta médica: " + m.getNombre(),
                        m.getNombre() + " es de venta con receta. Si nadie te lo recetó y te lo ofrecen en la botica, "
                                + "pide una explicación o consulta a un médico antes de comprarlo."));
            }
        }

        if (m.getDosisMg() != null && m.getDosisMaxUnidadMg() != null && m.getDosisMg() > m.getDosisMaxUnidadMg()) {
            alertas.add(new Alerta(NivelRiesgo.PRECAUCION, "Concentración alta: " + m.getNombre(),
                    "Cada unidad tiene " + mg(m.getDosisMg()) + " de " + m.getPrincipioPrincipal().toLowerCase()
                            + ", más que la dosis habitual sin receta (" + mg(m.getDosisMaxUnidadMg()) + "). "
                            + "Para una molestia común suele bastar una presentación menor. Pregunta si realmente la necesitas."));
        }

        if (Texto.iguales(m.getCategoria(), SUPLEMENTO)) {
            alertas.add(new Alerta(NivelRiesgo.PRECAUCION, "Posible venta innecesaria: " + m.getNombre(),
                    "Los suplementos suelen ofrecerse como venta adicional. Solo hacen falta si tienes una deficiencia "
                            + "comprobada o te los indicó un médico. Una alimentación variada normalmente es suficiente."));
        }
        return alertas;
    }

    private List<Alerta> revisarDosisDiaria(List<ItemCompra> items) {
        Map<String, List<ItemCompra>> porPrincipio = new LinkedHashMap<>();
        for (ItemCompra item : items) {
            if (item.medicamento().getDosisMg() != null) {
                porPrincipio.computeIfAbsent(Texto.normalizar(item.medicamento().getPrincipioPrincipal()),
                        k -> new ArrayList<>()).add(item);
            }
        }

        List<Alerta> alertas = new ArrayList<>();
        for (List<ItemCompra> grupo : porPrincipio.values()) {
            OptionalDouble limite = grupo.stream()
                    .map(i -> i.medicamento().getDosisMaxDiariaMg())
                    .filter(Objects::nonNull)
                    .mapToDouble(Double::doubleValue)
                    .max();
            if (limite.isEmpty()) {
                continue;
            }
            double total = grupo.stream()
                    .mapToDouble(i -> i.medicamento().getDosisMg() * i.unidadesDia())
                    .sum();
            if (total > limite.getAsDouble()) {
                String principio = grupo.get(0).medicamento().getPrincipioPrincipal();
                String desglose = grupo.stream()
                        .map(i -> i.medicamento().getNombre() + ": " + i.unidadesDia() + " × " + mg(i.medicamento().getDosisMg()))
                        .collect(Collectors.joining(", "));
                alertas.add(new Alerta(NivelRiesgo.PELIGRO, "Dosis diaria excesiva de " + principio.toLowerCase(),
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
