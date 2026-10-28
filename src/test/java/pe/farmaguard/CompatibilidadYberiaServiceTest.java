package pe.farmaguard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pe.farmaguard.model.InteraccionYberia;
import pe.farmaguard.model.MedicamentoYberia;
import pe.farmaguard.model.MomentoComida;
import pe.farmaguard.model.NivelRiesgo;
import pe.farmaguard.model.RecordatorioYberia;
import pe.farmaguard.service.Alerta;
import pe.farmaguard.service.CompatibilidadYberiaService;
import pe.farmaguard.service.InterpretacionLista;
import pe.farmaguard.service.ItemCompra;
import pe.farmaguard.service.MedicamentoYberiaService;
import pe.farmaguard.service.RecordatorioYberiaService;
import pe.farmaguard.repository.InteraccionYberiaRepository;
import pe.farmaguard.repository.MedicamentoYberiaRepository;
import pe.farmaguard.service.ResultadoAnalisis;
import pe.farmaguard.util.Texto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite:file:farmaguard-test?mode=memory&cache=shared")
class CompatibilidadYberiaServiceTest {

    @Autowired
    private MedicamentoYberiaService medicamentosYberia;

    @Autowired
    private CompatibilidadYberiaService compatibilidadYberia;

    @Autowired
    private RecordatorioYberiaService recordatoriosYberia;

    @Autowired
    private MedicamentoYberiaRepository medicamentoYberiaRepository;

    @Autowired
    private InteraccionYberiaRepository interaccionYberiaRepository;

    private ResultadoAnalisis analizar(String... nombres) {
        List<ItemCompra> items = Arrays.stream(nombres)
                .map(n -> medicamentosYberia.buscar(n).orElseThrow())
                .map(ItemCompra::conDosisHabitual)
                .toList();
        return compatibilidadYberia.analizar(items, List.of());
    }

    private static List<String> titulos(ResultadoAnalisis resultado) {
        return resultado.alertas().stream().map(Alerta::titulo).toList();
    }

    @Test
    void productoSinProblemasEsSeguro() {
        assertThat(analizar("Loratadina").nivelGeneral()).isEqualTo(NivelRiesgo.SEGURO);
    }

    @Test
    void mismoPrincipioActivoConDistintaMarcaEsDuplicidad() {
        ResultadoAnalisis resultado = analizar("Paracetamol", "Panadol");
        assertThat(resultado.nivelGeneral()).isEqualTo(NivelRiesgo.PELIGRO);
        assertThat(titulos(resultado)).anyMatch(t -> t.startsWith("Duplicidad"));
    }

    @Test
    void dosAntiinflamatoriosSonIncompatibles() {
        ResultadoAnalisis resultado = analizar("Advil", "Apronax");
        assertThat(resultado.nivelGeneral()).isEqualTo(NivelRiesgo.PELIGRO);
        assertThat(titulos(resultado)).anyMatch(t -> t.startsWith("Interacción"));
    }

    @Test
    void sumaDeParacetamolEntreProductosSuperaElMaximo() {
        assertThat(titulos(analizar("Paracetamol", "Panadol", "Panadol Antigripal")))
                .anyMatch(t -> t.startsWith("Dosis diaria excesiva"));
    }

    @Test
    void detectaInteraccionDeUnComponenteDentroDeUnProductoCombinado() {
        ResultadoAnalisis resultado = analizar("Panadol Antigripal", "Losartán");
        assertThat(resultado.alertas())
                .anyMatch(a -> a.titulo().startsWith("Interacción") && a.detalle().contains("Fenilefrina"));
    }

    @Test
    void mismoGrupoSinReglaEsPosibleVentaRedundante() {
        assertThat(titulos(analizar("Amoxicilina", "Azitromicina")))
                .anyMatch(t -> t.startsWith("Posible venta redundante"));
    }

    @Test
    void interpretaListaConMarcasTildesYConcentraciones() {
        InterpretacionLista lista = medicamentosYberia.interpretarLista("1) PANADOL x2, amoxicilina 500mg\n- losartan\nxyz123");
        assertThat(lista.encontrados()).extracting(MedicamentoYberia::getNombre)
                .containsExactly("Panadol", "Amoxicilina", "Losartán");
        assertThat(lista.noReconocidos()).containsExactly("xyz123");
    }

    @Test
    void catalogoAmpliadoTieneEntre150Y200Medicamentos() {
        assertThat(medicamentoYberiaRepository.count()).isBetween(150L, 200L);
    }

    @Test
    void cadaReglaApuntaAComponentesOGruposExistentes() {
        Set<String> principios = medicamentoYberiaRepository.findAll().stream()
                .flatMap(m -> m.getPrincipios().stream())
                .map(Texto::normalizar)
                .collect(Collectors.toSet());
        Set<String> categorias = medicamentoYberiaRepository.findAll().stream()
                .map(m -> Texto.normalizar(m.getCategoria()))
                .collect(Collectors.toSet());
        for (InteraccionYberia reglaYberia : interaccionYberiaRepository.findAll()) {
            for (String termino : List.of(reglaYberia.getPrincipioA(), reglaYberia.getPrincipioB())) {
                String normalizado = Texto.normalizar(termino);
                if (normalizado.startsWith("grupo:")) {
                    assertThat(categorias).as("grupo de la regla %d", reglaYberia.getId())
                            .contains(normalizado.substring("grupo:".length()).trim());
                } else {
                    assertThat(principios).as("componente de la regla %d", reglaYberia.getId()).contains(normalizado);
                }
            }
        }
    }

    @Test
    void reglaDeGrupoDetectaDosAntiinflamatoriosSinReglaEspecifica() {
        ResultadoAnalisis resultado = analizar("Meloxicam", "Arcoxia");
        assertThat(resultado.nivelGeneral()).isEqualTo(NivelRiesgo.PELIGRO);
        assertThat(titulos(resultado)).anyMatch(t -> t.startsWith("Interacción"));
    }

    @Test
    void reglaEspecificaTienePrioridadSobreLaDeGrupo() {
        ResultadoAnalisis resultado = analizar("Ibuprofeno", "Naproxeno");
        assertThat(titulos(resultado)).filteredOn(t -> t.startsWith("Interacción")).hasSize(1);
    }

    @Test
    void corticoideOInmunosupresorConAineEsAlertaGrave() {
        for (String corticoide : List.of("Dexametasona", "Prednisona", "Betametasona", "Deflazacort", "Metotrexato")) {
            for (String aine : List.of("Ibuprofeno", "Naproxeno", "Ketorolaco")) {
                assertThat(analizar(corticoide, aine).alertas())
                        .as("%s + %s", corticoide, aine)
                        .anyMatch(a -> a.nivel() == NivelRiesgo.PELIGRO && a.titulo().startsWith("Interacción"));
            }
        }
    }

    @Test
    void tratamientosCronicosCombinadosNoSeMarcanComoVentaRedundante() {
        assertThat(titulos(analizar("Losartán", "Amlodipino")))
                .noneMatch(t -> t.startsWith("Posible venta redundante"));
    }

    @Test
    void interpretaFraseDictadaYTextoDeReceta() {
        assertThat(medicamentosYberia.interpretarFrase("Quiero analizar Paracetamol con Nastizol").encontrados())
                .extracting(MedicamentoYberia::getNombre).containsExactly("Paracetamol", "Nastizol");
        assertThat(medicamentosYberia.interpretarFrase("Rp. 1. Ibuprofeno 800mg c/8h\n2. Panadol Antigripal").encontrados())
                .extracting(MedicamentoYberia::getNombre).containsExactly("Ibuprofeno 800 mg", "Panadol Antigripal");
    }

    @Test
    void recordatorioSemanalSoloTieneTomasCada7Dias() {
        LocalDate inicio = LocalDate.now();
        RecordatorioYberia semanal = recordatoriosYberia.guardar(RecordatorioYberia.cadaDias("Metotrexato", 7, 22,
                MomentoComida.INDIFERENTE, "09:00", inicio.toString(), null));

        assertThat(recordatoriosYberia.horariosDelDia(semanal, inicio)).containsExactly(LocalTime.of(9, 0));
        assertThat(recordatoriosYberia.horariosDelDia(semanal, inicio.plusDays(1))).isEmpty();
        assertThat(recordatoriosYberia.horariosDelDia(semanal, inicio.plusDays(6))).isEmpty();
        assertThat(recordatoriosYberia.horariosDelDia(semanal, inicio.plusDays(7))).containsExactly(LocalTime.of(9, 0));
        assertThat(recordatoriosYberia.horariosDelDia(semanal, inicio.plusDays(21))).containsExactly(LocalTime.of(9, 0));
        assertThat(recordatoriosYberia.horariosDelDia(semanal, inicio.plusDays(28))).isEmpty();

        assertThat(recordatoriosYberia.proximaToma(semanal, inicio.atTime(10, 0)))
                .contains(inicio.plusDays(7).atTime(9, 0));
    }

    @Test
    void guardaRecordatorioYCalculaHorarios() {
        RecordatorioYberia guardado = recordatoriosYberia.guardar(new RecordatorioYberia("Amoxicilina", 8, 7, MomentoComida.INDIFERENTE,
                "08:00", LocalDate.now().toString(), null));
        assertThat(guardado.getId()).isNotNull();
        assertThat(recordatoriosYberia.horarios(guardado))
                .containsExactly(LocalTime.of(0, 0), LocalTime.of(8, 0), LocalTime.of(16, 0));
        assertThat(recordatoriosYberia.horariosDelDia(guardado, LocalDate.now()))
                .containsExactly(LocalTime.of(8, 0), LocalTime.of(16, 0));
    }
}
