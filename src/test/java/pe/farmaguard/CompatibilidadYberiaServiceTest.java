package pe.farmaguard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pe.farmaguard.model.InteraccionYberia;
import pe.farmaguard.model.MedicamentoYberia;
import pe.farmaguard.model.MomentoComidaYberia;
import pe.farmaguard.model.NivelRiesgoYberia;
import pe.farmaguard.model.RecordatorioYberia;
import pe.farmaguard.service.AlertaYberia;
import pe.farmaguard.service.CompatibilidadYberiaService;
import pe.farmaguard.service.InterpretacionListaYberia;
import pe.farmaguard.service.ItemCompraYberia;
import pe.farmaguard.service.MedicamentoYberiaService;
import pe.farmaguard.service.RecordatorioYberiaService;
import pe.farmaguard.repository.InteraccionYberiaRepository;
import pe.farmaguard.repository.MedicamentoYberiaRepository;
import pe.farmaguard.service.ResultadoAnalisisYberia;
import pe.farmaguard.util.TextoYberia;

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

    private ResultadoAnalisisYberia analizar(String... nombres) {
        List<ItemCompraYberia> items = Arrays.stream(nombres)
                .map(n -> medicamentosYberia.buscar(n).orElseThrow())
                .map(ItemCompraYberia::conDosisHabitual)
                .toList();
        return compatibilidadYberia.analizar(items, List.of());
    }

    private static List<String> titulos(ResultadoAnalisisYberia resultado) {
        return resultado.alertasYberia().stream().map(AlertaYberia::tituloYberia).toList();
    }

    @Test
    void productoSinProblemasEsSeguro() {
        assertThat(analizar("Loratadina").nivelGeneralYberia()).isEqualTo(NivelRiesgoYberia.SEGURO);
    }

    @Test
    void mismoPrincipioActivoConDistintaMarcaEsDuplicidad() {
        ResultadoAnalisisYberia resultado = analizar("Paracetamol", "Panadol");
        assertThat(resultado.nivelGeneralYberia()).isEqualTo(NivelRiesgoYberia.PELIGRO);
        assertThat(titulos(resultado)).anyMatch(t -> t.startsWith("Duplicidad"));
    }

    @Test
    void dosAntiinflamatoriosSonIncompatibles() {
        ResultadoAnalisisYberia resultado = analizar("Advil", "Apronax");
        assertThat(resultado.nivelGeneralYberia()).isEqualTo(NivelRiesgoYberia.PELIGRO);
        assertThat(titulos(resultado)).anyMatch(t -> t.startsWith("Interacción"));
    }

    @Test
    void sumaDeParacetamolEntreProductosSuperaElMaximo() {
        assertThat(titulos(analizar("Paracetamol", "Panadol", "Panadol Antigripal")))
                .anyMatch(t -> t.startsWith("Dosis diaria excesiva"));
    }

    @Test
    void detectaInteraccionDeUnComponenteDentroDeUnProductoCombinado() {
        ResultadoAnalisisYberia resultado = analizar("Panadol Antigripal", "Losartán");
        assertThat(resultado.alertasYberia())
                .anyMatch(a -> a.tituloYberia().startsWith("Interacción") && a.detalleYberia().contains("Fenilefrina"));
    }

    @Test
    void mismoGrupoSinReglaEsPosibleVentaRedundante() {
        assertThat(titulos(analizar("Amoxicilina", "Azitromicina")))
                .anyMatch(t -> t.startsWith("Posible venta redundante"));
    }

    @Test
    void interpretaListaConMarcasTildesYConcentraciones() {
        InterpretacionListaYberia lista = medicamentosYberia.interpretarLista("1) PANADOL x2, amoxicilina 500mg\n- losartan\nxyz123");
        assertThat(lista.encontradosYberia()).extracting(MedicamentoYberia::getNombre)
                .containsExactly("Panadol", "Amoxicilina", "Losartán");
        assertThat(lista.noReconocidosYberia()).containsExactly("xyz123");
    }

    @Test
    void catalogoAmpliadoTieneEntre150Y250Medicamentos() {
        assertThat(medicamentoYberiaRepository.count()).isBetween(150L, 250L);
    }

    @Test
    void cadaReglaApuntaAComponentesOGruposExistentes() {
        Set<String> principios = medicamentoYberiaRepository.findAll().stream()
                .flatMap(m -> m.getPrincipios().stream())
                .map(TextoYberia::normalizar)
                .collect(Collectors.toSet());
        Set<String> categorias = medicamentoYberiaRepository.findAll().stream()
                .map(m -> TextoYberia.normalizar(m.getCategoria()))
                .collect(Collectors.toSet());
        for (InteraccionYberia reglaYberia : interaccionYberiaRepository.findAll()) {
            for (String termino : List.of(reglaYberia.getPrincipioA(), reglaYberia.getPrincipioB())) {
                String normalizado = TextoYberia.normalizar(termino);
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
        ResultadoAnalisisYberia resultado = analizar("Meloxicam", "Arcoxia");
        assertThat(resultado.nivelGeneralYberia()).isEqualTo(NivelRiesgoYberia.PELIGRO);
        assertThat(titulos(resultado)).anyMatch(t -> t.startsWith("Interacción"));
    }

    @Test
    void reglaEspecificaTienePrioridadSobreLaDeGrupo() {
        ResultadoAnalisisYberia resultado = analizar("Ibuprofeno", "Naproxeno");
        assertThat(titulos(resultado)).filteredOn(t -> t.startsWith("Interacción")).hasSize(1);
    }

    @Test
    void corticoideOInmunosupresorConAineEsAlertaGrave() {
        for (String corticoide : List.of("Dexametasona", "Prednisona", "Betametasona", "Deflazacort", "Metotrexato")) {
            for (String aine : List.of("Ibuprofeno", "Naproxeno", "Ketorolaco")) {
                assertThat(analizar(corticoide, aine).alertasYberia())
                        .as("%s + %s", corticoide, aine)
                        .anyMatch(a -> a.nivelYberia() == NivelRiesgoYberia.PELIGRO && a.tituloYberia().startsWith("Interacción"));
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
        assertThat(medicamentosYberia.interpretarFrase("Quiero analizar Paracetamol con Nastizol").encontradosYberia())
                .extracting(MedicamentoYberia::getNombre).containsExactly("Paracetamol", "Nastizol");
        assertThat(medicamentosYberia.interpretarFrase("Rp. 1. Ibuprofeno 800mg c/8h\n2. Panadol Antigripal").encontradosYberia())
                .extracting(MedicamentoYberia::getNombre).containsExactly("Ibuprofeno 800 mg", "Panadol Antigripal");
    }

    @Test
    void recordatorioSemanalSoloTieneTomasCada7Dias() {
        LocalDate inicio = LocalDate.now();
        RecordatorioYberia semanal = recordatoriosYberia.guardar(RecordatorioYberia.cadaDias("Metotrexato", 7, 22,
                MomentoComidaYberia.INDIFERENTE, "09:00", inicio.toString(), null));

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
        RecordatorioYberia guardado = recordatoriosYberia.guardar(new RecordatorioYberia("Amoxicilina", 8, 7, MomentoComidaYberia.INDIFERENTE,
                "08:00", LocalDate.now().toString(), null));
        assertThat(guardado.getId()).isNotNull();
        assertThat(recordatoriosYberia.horarios(guardado))
                .containsExactly(LocalTime.of(0, 0), LocalTime.of(8, 0), LocalTime.of(16, 0));
        assertThat(recordatoriosYberia.horariosDelDia(guardado, LocalDate.now()))
                .containsExactly(LocalTime.of(8, 0), LocalTime.of(16, 0));
    }

    @Test
    void sinonimosLlevanAlMismoPrincipioActivo() {
        for (String nombre : List.of("Clorfenamina", "clorfeniramina", "Maleato de clorfenamina", "MALEATO DE CLORFENIRAMINA",
                "maleato de clorfeniramina 4 mg")) {
            assertThat(medicamentosYberia.buscar(nombre)).as(nombre)
                    .get().extracting(MedicamentoYberia::getNombre).isEqualTo("Clorfenamina");
        }
        assertThat(medicamentosYberia.buscar("Acetaminofén")).get().extracting(MedicamentoYberia::getNombre).isEqualTo("Paracetamol");
        assertThat(medicamentosYberia.buscar("Xanax")).get().extracting(MedicamentoYberia::getNombre).isEqualTo("Alprazolam");
        assertThat(TextoYberia.iguales("Clorfeniramina", "Clorfenamina")).isTrue();
        assertThat(TextoYberia.iguales("Paracetamol", "Clorfenamina")).isFalse();
    }

    @Test
    void sinonimoEnUnaRecetaSeComparaComoElMismoComponente() {
        InterpretacionListaYberia lista = medicamentosYberia.interpretarLista("Maleato de clorfeniramina\nPanadol Antigripal");
        assertThat(lista.encontradosYberia()).extracting(MedicamentoYberia::getNombre)
                .containsExactly("Clorfenamina", "Panadol Antigripal");
        ResultadoAnalisisYberia resultado = compatibilidadYberia.analizar(
                lista.encontradosYberia().stream().map(ItemCompraYberia::conDosisHabitual).toList(), List.of());
        assertThat(titulos(resultado)).anyMatch(t -> t.startsWith("Duplicidad"));
    }

    @Test
    void marcaConVariosComponentesMuestraTodos() {
        MedicamentoYberia drFlu = medicamentosYberia.buscar("dr flu").orElseThrow();
        assertThat(drFlu.getPrincipios()).contains("Paracetamol", "Clorfenamina", "Dextrometorfano", "Fenilefrina");
        assertThat(titulos(analizar("Dr. Flu", "Panadol"))).anyMatch(t -> t.startsWith("Duplicidad"));
        assertThat(analizar("Dr. Flu", "Sertralina").alertasYberia()).anyMatch(a -> a.tituloYberia().startsWith("Interacción"));
    }

    @Test
    void marcasPeruanasNuevasSeReconocenYSeComparan() {
        assertThat(medicamentosYberia.buscar("Abudron BR")).get().extracting(MedicamentoYberia::getNombre).isEqualTo("Abubron BR");
        assertThat(medicamentosYberia.buscar("Repriman")).get().extracting(MedicamentoYberia::getNombre).isEqualTo("Metamizol");
        assertThat(medicamentosYberia.buscar("Findaler")).get().extracting(MedicamentoYberia::getNombre).isEqualTo("Cetirizina");

        assertThat(titulos(analizar("Abubron BR", "Amoxicilina"))).anyMatch(t -> t.startsWith("Duplicidad"));
        assertThat(analizar("Dolomax Plus", "Advil").nivelGeneralYberia()).isEqualTo(NivelRiesgoYberia.PELIGRO);
        assertThat(analizar("Midol Extra Fuerte", "Warfarina").alertasYberia())
                .anyMatch(a -> a.nivelYberia() == NivelRiesgoYberia.PELIGRO && a.tituloYberia().startsWith("Interacción"));
        assertThat(analizar("Gaseovet MS", "Ciprofloxacino").alertasYberia())
                .anyMatch(a -> a.tituloYberia().startsWith("Interacción") && a.detalleYberia().contains("ciprofloxacino"));
        assertThat(titulos(analizar("Diovan HCT", "Losartán"))).anyMatch(t -> t.startsWith("Interacción"));
    }

    @Test
    void productoCompuestoEnUnaRecetaNoSeParteEnDos() {
        InterpretacionListaYberia lista = medicamentosYberia.interpretarLista(
                "Amoxicilina 875 mg + Ácido clavulánico 125 mg\nLosartán + Hidroclorotiazida");
        assertThat(lista.encontradosYberia()).extracting(MedicamentoYberia::getNombre)
                .containsExactly("Amoxicilina + Ácido clavulánico", "Losartán + Hidroclorotiazida");
        assertThat(lista.noReconocidosYberia()).isEmpty();

        InterpretacionListaYberia separados = medicamentosYberia.interpretarLista("Paracetamol + Loratadina");
        assertThat(separados.encontradosYberia()).extracting(MedicamentoYberia::getNombre)
                .containsExactly("Paracetamol", "Loratadina");
    }

    @Test
    void recetaGrandeAgrupaDuplicidadesEnUnaSolaAlerta() {
        ResultadoAnalisisYberia resultado = analizar("Paracetamol", "Panadol", "Nastizol", "Dr. Flu");
        List<String> duplicidades = titulos(resultado).stream().filter(t -> t.startsWith("Duplicidad")).toList();
        assertThat(duplicidades).hasSize(2);
        assertThat(resultado.alertasYberia()).doesNotHaveDuplicates();
    }

    @Test
    void suplementosRepetidosSonPrecaucionYNoPeligro() {
        ResultadoAnalisisYberia resultado = analizar("Vitamina C 1 g", "Redoxon");
        assertThat(resultado.alertasYberia()).filteredOn(a -> a.tituloYberia().startsWith("Duplicidad"))
                .allMatch(a -> a.nivelYberia() == NivelRiesgoYberia.PRECAUCION);
    }

    @Test
    void tratamientoHabitualNoDaFalsosPositivosDeRedundancia() {
        assertThat(titulos(analizar("Calcio + Vitamina D", "Ácido fólico", "Omega 3")))
                .noneMatch(t -> t.startsWith("Posible venta redundante"));
        assertThat(titulos(analizar("Albendazol", "Nitazoxanida")))
                .noneMatch(t -> t.startsWith("Posible venta redundante"));
    }
}
