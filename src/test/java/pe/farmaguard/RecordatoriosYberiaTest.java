package pe.farmaguard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pe.farmaguard.model.MedicamentoYberia;
import pe.farmaguard.model.MomentoComidaYberia;
import pe.farmaguard.model.RecordatorioYberia;
import pe.farmaguard.service.ItemCompraYberia;
import pe.farmaguard.service.MedicamentoYberiaService;
import pe.farmaguard.service.RecordatorioYberiaService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite:file:farmaguard-test?mode=memory&cache=shared")
class RecordatoriosYberiaTest {
    @Autowired
    private MedicamentoYberiaService medicamentosYberia;

    @Autowired
    private RecordatorioYberiaService recordatoriosYberia;

    private MedicamentoYberia medicamento(String nombre) {
        return medicamentosYberia.buscar(nombre).orElseThrow();
    }

    @Test
    void primeraTomaVaConLaSiguienteComidaSegunElMomento() {
        LocalDateTime media = LocalDate.of(2026, 10, 21).atTime(10, 15);
        assertThat(RecordatorioYberiaService.primeraToma(MomentoComidaYberia.ANTES, media)).isEqualTo(media.withHour(12).withMinute(30));
        assertThat(RecordatorioYberiaService.primeraToma(MomentoComidaYberia.DURANTE, media)).isEqualTo(media.withHour(13).withMinute(0));
        assertThat(RecordatorioYberiaService.primeraToma(MomentoComidaYberia.DESPUES, media)).isEqualTo(media.withHour(13).withMinute(30));
        assertThat(RecordatorioYberiaService.primeraToma(MomentoComidaYberia.INDIFERENTE, media)).isEqualTo(media.withMinute(20));

        LocalDateTime noche = media.withHour(21);
        assertThat(RecordatorioYberiaService.primeraToma(MomentoComidaYberia.DESPUES, noche))
                .isEqualTo(media.toLocalDate().plusDays(1).atTime(8, 30));
    }

    @Test
    void frecuenciaYDiasSalenDelCatalogo() {
        assertThat(RecordatorioYberiaService.frecuenciaPara(3)).isEqualTo(8);
        assertThat(RecordatorioYberiaService.frecuenciaPara(2)).isEqualTo(12);
        assertThat(RecordatorioYberiaService.frecuenciaPara(1)).isEqualTo(24);
        assertThat(RecordatorioYberiaService.frecuenciaPara(5)).isEqualTo(6);

        assertThat(RecordatorioYberiaService.diasSugeridos(medicamento("Ibuprofeno"))).isEqualTo(5);
        assertThat(RecordatorioYberiaService.diasSugeridos(medicamento("Paracetamol")))
                .isEqualTo(RecordatorioYberiaService.DIAS_POR_DEFECTO_YBERIA);
    }

    @Test
    void agregaLaListaDelAnalisisSinDuplicarYAvisaALaAgenda() {
        AtomicInteger avisos = new AtomicInteger();
        recordatoriosYberia.alCambiar(avisos::incrementAndGet);
        MedicamentoYberia naproxeno = medicamento("Naproxeno");
        MedicamentoYberia loratadina = medicamento("Loratadina");
        LocalDateTime ahora = LocalDate.now().atTime(10, 0);

        RecordatorioYberiaService.ResultadoAutomaticoYberia resultado = recordatoriosYberia.agregarAutomaticos(
                List.of(ItemCompraYberia.conDosisHabitual(naproxeno), ItemCompraYberia.conDosisHabitual(loratadina)), ahora);

        assertThat(resultado.creadosYberia()).extracting(RecordatorioYberia::getMedicamento)
                .containsExactly(naproxeno.getNombre(), loratadina.getNombre());
        RecordatorioYberia creado = resultado.creadosYberia().get(0);
        assertThat(creado.getId()).isNotNull();
        assertThat(creado.getMomentoComida()).isEqualTo(naproxeno.getMomentoComida());
        assertThat(creado.getFrecuenciaHoras())
                .isEqualTo(RecordatorioYberiaService.frecuenciaPara(naproxeno.getUnidadesDiaHabitual()));
        assertThat(avisos.get()).isPositive();

        RecordatorioYberiaService.ResultadoAutomaticoYberia repetido = recordatoriosYberia.agregarAutomaticos(
                List.of(ItemCompraYberia.conDosisHabitual(naproxeno)), ahora);
        assertThat(repetido.creadosYberia()).isEmpty();
        assertThat(repetido.omitidosYberia()).containsExactly(naproxeno.getNombre());
    }

    @Test
    void editarCambiaLosDatosYConservaElId() {
        RecordatorioYberia original = recordatoriosYberia.guardar(new RecordatorioYberia("Omeprazol", 24, 14, MomentoComidaYberia.ANTES,
                "07:30", LocalDate.now().toString(), ""));
        RecordatorioYberia editado = recordatoriosYberia.editar(original.getId(), new RecordatorioYberia("Omeprazol", 12, 7,
                MomentoComidaYberia.ANTES, "07:00", original.getFechaInicio(), "En ayunas"));

        assertThat(editado.getId()).isEqualTo(original.getId());
        assertThat(editado.getFrecuenciaHoras()).isEqualTo(12);
        assertThat(editado.getDuracionDias()).isEqualTo(7);
        assertThat(editado.getNotas()).isEqualTo("En ayunas");
        assertThatThrownBy(() -> recordatoriosYberia.editar(original.getId(), new RecordatorioYberia("Omeprazol", 0, 7,
                MomentoComidaYberia.ANTES, "07:00", original.getFechaInicio(), "")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void medicamentoConComidasTresVecesAlDiaVaConCadaComida() {
        LocalDateTime media = LocalDate.of(2026, 10, 21).atTime(10, 15);
        RecordatorioYberiaService.PlanComidasYberia plan = RecordatorioYberiaService
                .planConComidas(MomentoComidaYberia.DESPUES, 8, media).orElseThrow();
        assertThat(plan.horasYberia()).containsExactly(LocalTime.of(8, 30), LocalTime.of(13, 30), LocalTime.of(20, 30));
        assertThat(plan.primeraYberia()).isEqualTo(media.withHour(13).withMinute(30));
        assertThat(plan.horasFijasYberia()).isEqualTo("08:30,13:30,20:30");

        assertThat(RecordatorioYberiaService.planConComidas(MomentoComidaYberia.ANTES, 12, media).orElseThrow().horasYberia())
                .containsExactly(LocalTime.of(7, 30), LocalTime.of(19, 30));
        assertThat(RecordatorioYberiaService.planConComidas(MomentoComidaYberia.ANTES, 24, media).orElseThrow().primeraYberia())
                .isEqualTo(media.toLocalDate().plusDays(1).atTime(7, 30));
        assertThat(RecordatorioYberiaService.planConComidas(MomentoComidaYberia.INDIFERENTE, 8, media)).isEmpty();
    }

    @Test
    void recordatorioAutomaticoRespetaElMomentoDeComidaDelMedicamento() {
        MedicamentoYberia ibuprofeno = medicamento("Ibuprofeno");
        MedicamentoYberia omeprazol = medicamento("Omeprazol");
        LocalDateTime ahora = LocalDate.now().atTime(10, 15);

        RecordatorioYberia conComida = recordatoriosYberia.sugerir(ibuprofeno, 8, 5, ahora);
        assertThat(conComida.getMomentoComida()).isEqualTo(MomentoComidaYberia.DESPUES);
        assertThat(recordatoriosYberia.horarios(conComida))
                .containsExactly(LocalTime.of(8, 30), LocalTime.of(13, 30), LocalTime.of(20, 30));

        RecordatorioYberia enAyunas = recordatoriosYberia.sugerir(omeprazol, 24, 14, ahora);
        assertThat(enAyunas.getMomentoComida()).isEqualTo(MomentoComidaYberia.ANTES);
        assertThat(enAyunas.getHoraInicio()).isEqualTo("07:30");

        RecordatorioYberia guardado = recordatoriosYberia.guardar(conComida);
        assertThat(guardado.getHorasFijas()).isEqualTo("08:30,13:30,20:30");
    }
}
