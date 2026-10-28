package pe.farmaguard.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.farmaguard.model.MedicamentoYberia;
import pe.farmaguard.model.MomentoComida;
import pe.farmaguard.model.RecordatorioYberia;
import pe.farmaguard.repository.RecordatorioYberiaRepository;
import pe.farmaguard.util.Texto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
public class RecordatorioYberiaService {

    public record TomaProgramada(RecordatorioYberia recordatorio, LocalDateTime cuando) {
    }

    public record ResultadoAutomatico(List<RecordatorioYberia> creados, List<String> omitidos) {
    }

    public static final List<Integer> FRECUENCIAS = List.of(4, 6, 8, 12, 24);
    public static final List<LocalTime> COMIDAS = List.of(LocalTime.of(8, 0), LocalTime.of(13, 0), LocalTime.of(20, 0));
    public static final int DIAS_POR_DEFECTO = 5;
    public static final int SEMANAS_POR_DEFECTO = 4;

    private static final DateTimeFormatter HORA_ISO = DateTimeFormatter.ofPattern("HH:mm");
    private static final Pattern DIAS = Pattern.compile("(\\d+)(?:\\s*a\\s*(\\d+))?\\s*dias");
    private static final int MINUTOS_COMIDA = 30;

    private final RecordatorioYberiaRepository repositorioYberia;
    private final List<Runnable> oyentes = new CopyOnWriteArrayList<>();

    public RecordatorioYberiaService(RecordatorioYberiaRepository repositorioYberia) {
        this.repositorioYberia = repositorioYberia;
    }

    public List<RecordatorioYberia> listar() {
        return repositorioYberia.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    public void alCambiar(Runnable oyente) {
        oyentes.add(oyente);
    }

    @Transactional
    public RecordatorioYberia guardar(RecordatorioYberia recordatorioYberia) {
        validar(recordatorioYberia);
        RecordatorioYberia guardado = repositorioYberia.save(recordatorioYberia);
        notificar();
        return guardado;
    }

    @Transactional
    public RecordatorioYberia editar(Long id, RecordatorioYberia datos) {
        validar(datos);
        RecordatorioYberia existente = repositorioYberia.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El recordatorio ya no existe."));
        existente.actualizarDesde(datos);
        RecordatorioYberia guardado = repositorioYberia.save(existente);
        notificar();
        return guardado;
    }

    @Transactional
    public void eliminar(Long id) {
        repositorioYberia.deleteById(id);
        notificar();
    }

    @Transactional
    public void cambiarActivo(Long id, boolean activo) {
        repositorioYberia.findById(id).ifPresent(r -> {
            r.setActivo(activo);
            repositorioYberia.save(r);
        });
        notificar();
    }

    @Transactional
    public ResultadoAutomatico agregarAutomaticos(List<ItemCompra> items, LocalDateTime ahora) {
        List<RecordatorioYberia> creados = new ArrayList<>();
        List<String> omitidos = new ArrayList<>();
        for (ItemCompra item : items) {
            MedicamentoYberia m = item.medicamento();
            if (tieneRecordatorioVigente(m.getNombre(), ahora.toLocalDate())) {
                omitidos.add(m.getNombre());
                continue;
            }
            RecordatorioYberia recordatorioYberia = sugerir(m, frecuenciaPara(item.unidadesDia()), diasSugeridos(m), ahora);
            validar(recordatorioYberia);
            creados.add(repositorioYberia.save(recordatorioYberia));
        }
        if (!creados.isEmpty()) {
            notificar();
        }
        return new ResultadoAutomatico(creados, omitidos);
    }

    public boolean tieneRecordatorioVigente(String medicamentoYberia, LocalDate hoy) {
        return listar().stream().anyMatch(r -> r.isActivo() && Texto.iguales(r.getMedicamento(), medicamentoYberia)
                && !fechaFin(r).isBefore(hoy));
    }

    public RecordatorioYberia sugerir(MedicamentoYberia m, int frecuenciaHoras, int dias, LocalDateTime ahora) {
        MomentoComida momento = m.getMomentoComida();
        LocalDateTime primera = primeraToma(momento, ahora);
        String hora = primera.toLocalTime().format(HORA_ISO);
        String fecha = primera.toLocalDate().toString();
        String notas = m.getDosisSugerida() == null ? "" : m.getDosisSugerida();
        if (esSemanal(m)) {
            return RecordatorioYberia.cadaDias(m.getNombre(), 7, (SEMANAS_POR_DEFECTO - 1) * 7 + 1, momento, hora, fecha,
                    notas);
        }
        return new RecordatorioYberia(m.getNombre(), frecuenciaHoras, dias, momento, hora, fecha, notas);
    }

    public static boolean esSemanal(MedicamentoYberia m) {
        return Texto.normalizar(m.getDosisSugerida()).contains("vez por semana");
    }

    public static int frecuenciaPara(int unidadesDia) {
        int horas = (int) Math.ceil(24.0 / Math.max(1, unidadesDia));
        return FRECUENCIAS.stream().filter(h -> h >= horas).findFirst().orElse(24);
    }

    public static int diasSugeridos(MedicamentoYberia m) {
        Matcher matcher = DIAS.matcher(Texto.normalizar(m.getDosisSugerida()));
        int dias = 0;
        while (matcher.find()) {
            dias = Math.max(dias, Integer.parseInt(matcher.group(2) != null ? matcher.group(2) : matcher.group(1)));
        }
        return dias >= 1 && dias <= 365 ? dias : DIAS_POR_DEFECTO;
    }

    public static LocalDateTime primeraToma(MomentoComida momento, LocalDateTime ahora) {
        LocalDateTime minimo = ahora.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1);
        if (momento == null || momento == MomentoComida.INDIFERENTE) {
            int resto = minimo.getMinute() % 5;
            return resto == 0 ? minimo : minimo.plusMinutes(5L - resto);
        }
        long desfase = switch (momento) {
            case ANTES -> -MINUTOS_COMIDA;
            case DESPUES -> MINUTOS_COMIDA;
            default -> 0;
        };
        LocalDate hoy = ahora.toLocalDate();
        return Stream.of(hoy, hoy.plusDays(1))
                .flatMap(dia -> COMIDAS.stream().map(comida -> dia.atTime(comida).plusMinutes(desfase)))
                .filter(t -> !t.isBefore(minimo))
                .findFirst()
                .orElseThrow();
    }

    private void notificar() {
        oyentes.forEach(Runnable::run);
    }

    public List<LocalTime> horarios(RecordatorioYberia r) {
        LocalTime inicio = LocalTime.parse(r.getHoraInicio());
        int tomasPorDia = Math.max(1, 24 / r.getFrecuenciaHoras());
        return IntStream.range(0, tomasPorDia)
                .mapToObj(i -> inicio.plusHours((long) i * r.getFrecuenciaHoras()))
                .sorted()
                .toList();
    }

    public LocalDate fechaInicio(RecordatorioYberia r) {
        return LocalDate.parse(r.getFechaInicio());
    }

    public LocalDate fechaFin(RecordatorioYberia r) {
        return fechaInicio(r).plusDays(r.getDuracionDias() - 1L);
    }

    public List<LocalTime> horariosDelDia(RecordatorioYberia r, LocalDate dia) {
        LocalDate inicio = fechaInicio(r);
        if (!r.isActivo() || dia.isBefore(inicio) || dia.isAfter(fechaFin(r))
                || ChronoUnit.DAYS.between(inicio, dia) % r.getIntervaloDias() != 0) {
            return List.of();
        }
        LocalTime horaInicio = LocalTime.parse(r.getHoraInicio());
        return horarios(r).stream()
                .filter(h -> !dia.equals(inicio) || !h.isBefore(horaInicio))
                .toList();
    }

    public List<RecordatorioYberia> tomasEn(LocalDateTime momento) {
        LocalTime minuto = momento.toLocalTime().truncatedTo(ChronoUnit.MINUTES);
        return listar().stream()
                .filter(r -> horariosDelDia(r, momento.toLocalDate()).contains(minuto))
                .toList();
    }

    public Optional<LocalDateTime> proximaToma(RecordatorioYberia r, LocalDateTime desde) {
        LocalDate hoy = desde.toLocalDate();
        return Stream.iterate(hoy, dia -> dia.plusDays(1))
                .limit(r.getIntervaloDias() + 1L)
                .flatMap(dia -> horariosDelDia(r, dia).stream().map(hora -> LocalDateTime.of(dia, hora)))
                .filter(t -> !t.isBefore(desde))
                .findFirst();
    }

    public Optional<TomaProgramada> proximaToma(LocalDateTime desde) {
        return listar().stream()
                .flatMap(r -> proximaToma(r, desde).map(cuando -> new TomaProgramada(r, cuando)).stream())
                .min(Comparator.comparing(TomaProgramada::cuando));
    }

    private static void validar(RecordatorioYberia r) {
        if (r.getMedicamento() == null || r.getMedicamento().isBlank()) {
            throw new IllegalArgumentException("Indica el nombre del medicamento.");
        }
        if (r.getFrecuenciaHoras() < 1 || r.getFrecuenciaHoras() > 24) {
            throw new IllegalArgumentException("La frecuencia debe estar entre 1 y 24 horas.");
        }
        if (r.getIntervaloDias() < 1 || r.getIntervaloDias() > 30) {
            throw new IllegalArgumentException("El intervalo debe estar entre 1 y 30 días.");
        }
        if (r.getDuracionDias() < 1 || r.getDuracionDias() > 365) {
            throw new IllegalArgumentException("La duración debe estar entre 1 y 365 días.");
        }
        try {
            LocalTime.parse(r.getHoraInicio());
            LocalDate.parse(r.getFechaInicio());
        } catch (DateTimeParseException | NullPointerException e) {
            throw new IllegalArgumentException("La fecha u hora de inicio no es válida.");
        }
    }
}
