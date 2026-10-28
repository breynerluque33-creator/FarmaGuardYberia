package pe.farmaguard.ui;

import com.formdev.flatlaf.FlatClientProperties;
import pe.farmaguard.model.MedicamentoYberia;
import pe.farmaguard.model.MomentoComida;
import pe.farmaguard.model.NivelRiesgo;
import pe.farmaguard.model.RecordatorioYberia;
import pe.farmaguard.service.MedicamentoYberiaService;
import pe.farmaguard.service.RecordatorioYberiaService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class RecordatoriosPanel extends JPanel {

    private static final DateTimeFormatter HORA_ISO = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("h:mm a", Locale.US);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM");
    private static final int REVISION_MS = 20_000;
    private static final int SEMANAL = 168;
    private static final int MAX_SEMANAS = 52;
    private static final DateTimeFormatter DIA_SEMANA = DateTimeFormatter.ofPattern("EEEE", Locale.forLanguageTag("es-PE"));

    private final RecordatorioYberiaService recordatorioYberiaService;
    private final MedicamentoYberiaService medicamentoYberiaService;

    private final CampoBusqueda medicamentoYberia;
    private final Segmentado<MomentoComida> momento = new Segmentado<>(
            List.of(MomentoComida.ANTES, MomentoComida.DURANTE, MomentoComida.DESPUES, MomentoComida.INDIFERENTE),
            RecordatoriosPanel::textoMomento);
    private final Segmentado<Integer> frecuencia = new Segmentado<>(List.of(4, 6, 8, 12, 24, SEMANAL),
            h -> h == SEMANAL ? "Semanal" : h == 24 ? "1 al día" : h + " h");
    private final JLabel ayudaFrecuencia = Estilos.etiqueta(" ", "small", Estilos::textoSecundario);
    private final JSpinner horaInicio = new JSpinner(new SpinnerDateModel());
    private final SpinnerNumberModel modeloDuracion = new SpinnerNumberModel(5, 1, 365, 1);
    private final JSpinner duracion = new JSpinner(modeloDuracion);
    private final JLabel etiquetaDuracion = Estilos.etiqueta("Durante (días)", "h4");
    private final JTextField notas = new JTextField();
    private final JLabel mensajeFormulario = Estilos.etiqueta(" ", "small",
            () -> Estilos.textoNivel(NivelRiesgo.PELIGRO));
    private final JLabel tituloFormulario = Estilos.etiqueta("Nuevo recordatorio", "h2");
    private final JLabel etiquetaHora = Estilos.etiqueta("Primera toma (hoy)", "h4");
    private final BotonPrimario guardar = new BotonPrimario("Guardar recordatorio", Iconos.mas(14, () -> Color.WHITE));
    private final JButton cancelarEdicion = Estilos.botonSecundario("Cancelar");
    private RecordatorioYberia enEdicion;

    private final PanelVertical agenda = new PanelVertical();
    private final JLabel proximaToma = Estilos.etiqueta(" ", "h4", Estilos::acento);
    private final Set<String> avisadas = new HashSet<>();

    public RecordatoriosPanel(RecordatorioYberiaService recordatorioYberiaService, MedicamentoYberiaService medicamentoYberiaService) {
        super(new GridBagLayout());
        this.recordatorioYberiaService = recordatorioYberiaService;
        this.medicamentoYberiaService = medicamentoYberiaService;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(4, 20, 16, 20));

        medicamentoYberia = new CampoBusqueda(medicamentoYberiaService.listarTodos(), "Busca en el catálogo o escribe el nombre");
        medicamentoYberia.alElegir(m -> {
            medicamentoYberia.setTextoSinSugerir(m.getNombre());
            sugerirDesdeCatalogo(m);
        });

        PanelVertical columnaFormulario = new PanelVertical();
        columnaFormulario.agregar(crearFormulario());
        JScrollPane scrollFormulario = Estilos.scroll(columnaFormulario);
        scrollFormulario.setPreferredSize(new Dimension(420, 300));
        scrollFormulario.setMinimumSize(new Dimension(380, 300));

        JComponent derecha = crearAgenda();
        derecha.setPreferredSize(new Dimension(420, 300));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridx = 0;
        c.weightx = 0.4;
        c.insets = new Insets(0, 0, 0, 10);
        add(scrollFormulario, c);
        c.gridx = 1;
        c.weightx = 0.6;
        c.insets = new Insets(0, 10, 0, 0);
        add(derecha, c);

        refrescar();
        recordatorioYberiaService.alCambiar(() -> SwingUtilities.invokeLater(this::refrescar));
        new Timer(REVISION_MS, e -> revisarTomas()).start();
    }

    private static String textoMomento(MomentoComida momento) {
        return switch (momento) {
            case ANTES -> "Antes";
            case DURANTE -> "Durante";
            case DESPUES -> "Después";
            case INDIFERENTE -> "Da igual";
        };
    }

    private JComponent crearFormulario() {
        momento.setSeleccion(MomentoComida.DESPUES);
        frecuencia.setSeleccion(8);
        frecuencia.alCambiar(h -> ajustarDuracion());

        horaInicio.setEditor(new JSpinner.DateEditor(horaInicio, "HH:mm"));
        for (JSpinner selector : List.of(horaInicio, duracion)) {
            JTextField texto = ((JSpinner.DefaultEditor) selector.getEditor()).getTextField();
            texto.setHorizontalAlignment(JTextField.CENTER);
            texto.setMargin(new Insets(6, 8, 6, 8));
        }
        fijarHora(LocalTime.now().plusHours(1).truncatedTo(ChronoUnit.HOURS));
        notas.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Ej.: con un vaso de agua, no manejar");
        notas.setMargin(new Insets(8, 8, 8, 8));

        Tarjeta tarjeta = new Tarjeta(new BorderLayout(), Estilos::superficie, 22);
        PanelVertical columna = new PanelVertical();
        columna.agregar(tituloFormulario);
        columna.espacio(2);
        columna.agregar(Estilos.parrafo("Te avisaremos a la hora de cada toma mientras FarmaGuard esté abierto.",
                Estilos::textoSecundario));

        campo(columna, "¿Qué medicamento?", medicamentoYberia);
        campo(columna, "¿Cuándo, respecto a la comida?", momento);
        campo(columna, "¿Con qué frecuencia?", frecuencia);
        columna.espacio(4);
        columna.agregar(ayudaFrecuencia);

        JPanel horaYDuracion = Estilos.transparente(new GridLayout(1, 2, 12, 0));
        PanelVertical columnaHora = new PanelVertical();
        columnaHora.agregar(etiquetaHora);
        columnaHora.espacio(6);
        columnaHora.agregar(horaInicio);
        PanelVertical columnaDuracion = new PanelVertical();
        columnaDuracion.agregar(etiquetaDuracion);
        columnaDuracion.espacio(6);
        columnaDuracion.agregar(duracion);
        horaYDuracion.add(columnaHora);
        horaYDuracion.add(columnaDuracion);
        columna.espacio(16);
        columna.agregar(horaYDuracion);

        JPanel atajos = Estilos.transparente(new FlowLayout(FlowLayout.LEFT, 0, 0));
        atajos.add(Estilos.etiqueta("Rápido:", "small", Estilos::textoSecundario));
        for (LocalTime hora : List.of(LocalTime.of(8, 0), LocalTime.of(13, 0), LocalTime.of(20, 0))) {
            JButton atajo = Estilos.botonSecundario(hora.format(HORA));
            atajo.putClientProperty(FlatClientProperties.STYLE_CLASS, "small");
            atajo.setMargin(new Insets(2, 10, 2, 10));
            atajo.addActionListener(e -> fijarHora(hora));
            atajos.add(Box.createHorizontalStrut(6));
            atajos.add(atajo);
        }
        columna.espacio(8);
        columna.agregar(atajos);

        campo(columna, "Notas (opcional)", notas);

        columna.espacio(18);
        guardar.addActionListener(e -> guardar());
        cancelarEdicion.addActionListener(e -> salirDeEdicion());
        cancelarEdicion.setVisible(false);
        JPanel filaGuardar = Estilos.transparente(new BorderLayout(8, 0));
        filaGuardar.add(guardar, BorderLayout.CENTER);
        filaGuardar.add(cancelarEdicion, BorderLayout.EAST);
        columna.agregar(filaGuardar);
        columna.espacio(4);
        columna.agregar(mensajeFormulario);

        tarjeta.add(columna, BorderLayout.CENTER);
        return tarjeta;
    }

    private static void campo(PanelVertical columna, String etiqueta, JComponent control) {
        columna.espacio(16);
        columna.agregar(Estilos.etiqueta(etiqueta, "h4"));
        columna.espacio(6);
        columna.agregar(control);
    }

    private void fijarHora(LocalTime hora) {
        horaInicio.setValue(Date.from(LocalDate.now().atTime(hora).atZone(ZoneId.systemDefault()).toInstant()));
    }

    private void sugerirDesdeCatalogo(MedicamentoYberia m) {
        momento.setSeleccion(m.getMomentoComida());
        if (RecordatorioYberiaService.esSemanal(m)) {
            frecuencia.setSeleccion(SEMANAL);
        } else {
            frecuencia.setSeleccion(RecordatorioYberiaService.frecuenciaPara(m.getUnidadesDiaHabitual()));
        }
        ajustarDuracion();
        if (!esSemanal()) {
            modeloDuracion.setValue(RecordatorioYberiaService.diasSugeridos(m));
        }
    }

    private void editar(RecordatorioYberia r) {
        enEdicion = r;
        medicamentoYberia.setTextoSinSugerir(r.getMedicamento());
        momento.setSeleccion(r.getMomentoComida());
        frecuencia.setSeleccion(r.isSemanal() ? SEMANAL : RecordatorioYberiaService.FRECUENCIAS.stream()
                .filter(h -> h >= r.getFrecuenciaHoras())
                .findFirst()
                .orElse(24));
        ajustarDuracion();
        int cantidad = r.isSemanal() ? (r.getDuracionDias() - 1) / 7 + 1 : r.getDuracionDias();
        modeloDuracion.setValue(Math.min(cantidad, (Integer) modeloDuracion.getMaximum()));
        fijarHora(LocalTime.parse(r.getHoraInicio()));
        notas.setText(r.getNotas() == null ? "" : r.getNotas());
        mensajeFormulario.setText(" ");

        LocalDate inicio = recordatorioYberiaService.fechaInicio(r);
        tituloFormulario.setText("Editar recordatorio");
        etiquetaHora.setText(inicio.equals(LocalDate.now()) ? "Primera toma (hoy)"
                : "Primera toma (" + inicio.format(FECHA) + ")");
        guardar.setText("Guardar cambios");
        cancelarEdicion.setVisible(true);
        ayudaFrecuencia.setText(esSemanal() ? "Una sola toma cada " + inicio.format(DIA_SEMANA) + "." : " ");
        medicamentoYberia.requestFocusInWindow();
    }

    private void salirDeEdicion() {
        enEdicion = null;
        tituloFormulario.setText("Nuevo recordatorio");
        etiquetaHora.setText("Primera toma (hoy)");
        guardar.setText("Guardar recordatorio");
        cancelarEdicion.setVisible(false);
        medicamentoYberia.setTextoSinSugerir("");
        notas.setText("");
        mensajeFormulario.setText(" ");
        ajustarDuracion();
    }

    private void ajustarDuracion() {
        boolean semanal = esSemanal();
        String etiqueta = semanal ? "Durante (semanas)" : "Durante (días)";
        if (!etiqueta.equals(etiquetaDuracion.getText())) {
            etiquetaDuracion.setText(etiqueta);
            modeloDuracion.setMaximum(semanal ? MAX_SEMANAS : 365);
            modeloDuracion.setValue(semanal ? 4 : 5);
        }
        ayudaFrecuencia.setText(semanal
                ? "Una sola toma cada " + LocalDate.now().format(DIA_SEMANA) + ", empezando hoy."
                : " ");
    }

    private boolean esSemanal() {
        return Integer.valueOf(SEMANAL).equals(frecuencia.getSeleccion());
    }

    private void guardar() {
        String nombre = medicamentoYberia.getText().trim();
        LocalTime hora = ((Date) horaInicio.getValue()).toInstant().atZone(ZoneId.systemDefault())
                .toLocalTime().truncatedTo(ChronoUnit.MINUTES);
        Integer cadaHoras = frecuencia.getSeleccion();
        MomentoComida comida = momento.getSeleccion();
        MomentoComida momentoComida = comida == null ? MomentoComida.INDIFERENTE : comida;
        int cantidad = (Integer) duracion.getValue();
        String fechaInicio = enEdicion != null ? enEdicion.getFechaInicio() : LocalDate.now().toString();
        RecordatorioYberia recordatorioYberia = esSemanal()
                ? RecordatorioYberia.cadaDias(nombre, 7, (cantidad - 1) * 7 + 1, momentoComida,
                        hora.format(HORA_ISO), fechaInicio, notas.getText().trim())
                : new RecordatorioYberia(nombre, cadaHoras == null ? 0 : cadaHoras, cantidad, momentoComida,
                        hora.format(HORA_ISO), fechaInicio, notas.getText().trim());
        try {
            if (enEdicion != null) {
                recordatorioYberiaService.editar(enEdicion.getId(), recordatorioYberia);
            } else {
                recordatorioYberiaService.guardar(recordatorioYberia);
            }
        } catch (IllegalArgumentException ex) {
            mensajeFormulario.setText(ex.getMessage());
            return;
        }
        salirDeEdicion();
    }

    private JComponent crearAgenda() {
        JPanel panel = Estilos.transparente(new BorderLayout(0, 4));

        PanelVertical cabecera = new PanelVertical();
        JPanel titulos = Estilos.transparente(new GridLayout(0, 1, 0, 2));
        titulos.setBorder(BorderFactory.createEmptyBorder(6, 6, 4, 6));
        titulos.add(Estilos.etiqueta("Mi agenda", "h2"));
        titulos.add(Estilos.etiqueta("Tus medicamentos ordenados por la próxima toma.", null, Estilos::textoSecundario));
        cabecera.agregar(titulos);

        Tarjeta banner = new Tarjeta(new BorderLayout(12, 0), Estilos::acentoSuave, 12);
        banner.setBorde(null);
        banner.add(new JLabel(Iconos.reloj(22)), BorderLayout.WEST);
        banner.add(proximaToma, BorderLayout.CENTER);
        cabecera.agregar(banner);

        panel.add(cabecera, BorderLayout.NORTH);
        panel.add(Estilos.scroll(agenda), BorderLayout.CENTER);
        return panel;
    }

    private void refrescar() {
        LocalDateTime ahora = LocalDateTime.now();
        agenda.removeAll();

        record Entrada(RecordatorioYberia recordatorio, Optional<LocalDateTime> proxima) {
        }
        List<Entrada> entradas = recordatorioYberiaService.listar().stream()
                .map(r -> new Entrada(r, recordatorioYberiaService.proximaToma(r, ahora)))
                .sorted(Comparator.comparing((Entrada e) -> e.proxima().orElse(LocalDateTime.MAX))
                        .thenComparing(e -> e.recordatorio().getHoraInicio()))
                .toList();

        if (entradas.isEmpty()) {
            agenda.agregar(estadoVacio());
        }
        for (Entrada entrada : entradas) {
            agenda.agregar(tarjetaRecordatorio(entrada.recordatorio(), entrada.proxima(), ahora));
        }
        agenda.revalidate();
        agenda.repaint();
        actualizarProximaToma();
    }

    private static JComponent estadoVacio() {
        Tarjeta tarjeta = new Tarjeta(new BorderLayout(14, 0), Estilos::superficie, 22);
        JPanel columnaIcono = Estilos.transparente(new BorderLayout());
        columnaIcono.add(new JLabel(Iconos.reloj(34)), BorderLayout.NORTH);
        tarjeta.add(columnaIcono, BorderLayout.WEST);
        PanelVertical textos = new PanelVertical();
        textos.agregar(Estilos.etiqueta("Aún no tienes recordatorios", "h3"));
        textos.espacio(4);
        textos.agregar(Estilos.parrafo("Crea uno con el formulario: elige el medicamento, la hora de la primera toma "
                + "y si va antes, durante o después de comer.", Estilos::textoSecundario));
        tarjeta.add(textos, BorderLayout.CENTER);
        return tarjeta;
    }

    private JComponent tarjetaRecordatorio(RecordatorioYberia r, Optional<LocalDateTime> proxima, LocalDateTime ahora) {
        boolean terminado = recordatorioYberiaService.fechaFin(r).isBefore(ahora.toLocalDate());
        Tarjeta tarjeta = new Tarjeta(new BorderLayout(16, 0), Estilos::superficie, 16);
        tarjeta.setAtenuada(!r.isActivo() || terminado);

        LocalTime hora = proxima.map(LocalDateTime::toLocalTime).orElse(LocalTime.parse(r.getHoraInicio()));
        String cuando;
        if (proxima.isPresent()) {
            cuando = capitalizar(dia(proxima.get().toLocalDate(), ahora.toLocalDate()));
        } else if (!r.isActivo()) {
            cuando = "En pausa";
        } else if (terminado) {
            cuando = "Terminado";
        } else {
            cuando = "Sin tomas próximas";
        }
        PanelVertical columnaHora = new PanelVertical();
        columnaHora.agregar(Estilos.etiqueta(hora.format(HORA), "h2", Estilos::acento));
        columnaHora.agregar(Estilos.etiqueta(cuando, "small", Estilos::textoSecundario));
        columnaHora.setPreferredSize(new Dimension(108, columnaHora.getPreferredSize().height));
        tarjeta.add(columnaHora, BorderLayout.WEST);

        PanelVertical detalle = new PanelVertical();
        detalle.agregar(Estilos.etiqueta(r.getMedicamento(), "h3"));
        detalle.espacio(6);
        JPanel insignias = Estilos.transparente(new FlowLayout(FlowLayout.LEFT, 0, 0));
        insignias.add(new Insignia(r.getMomentoComida().getEtiqueta(), Estilos::acentoSuave, Estilos::acento));
        insignias.add(Box.createHorizontalStrut(6));
        insignias.add(new Insignia(frecuenciaTexto(r), Estilos::superficieSuave, Estilos::textoSecundario));
        detalle.agregar(insignias);
        detalle.espacio(6);
        String horario = recordatorioYberiaService.horarios(r).stream().map(HORA::format).collect(Collectors.joining(" · "));
        detalle.agregar(Estilos.parrafo(r.isSemanal()
                ? "Toma: " + horario + " cada " + recordatorioYberiaService.fechaInicio(r).format(DIA_SEMANA)
                : "Tomas: " + horario, Estilos::textoSecundario));
        detalle.agregar(Estilos.parrafo(vigencia(r, ahora.toLocalDate()), Estilos::textoSecundario));
        if (r.getNotas() != null && !r.getNotas().isBlank()) {
            detalle.agregar(Estilos.parrafo("Nota: " + r.getNotas(), Estilos::textoPrincipal));
        }
        tarjeta.add(detalle, BorderLayout.CENTER);

        Interruptor interruptor = new Interruptor(r.isActivo());
        interruptor.setToolTipText(r.isActivo() ? "Pausar recordatorio" : "Activar recordatorio");
        interruptor.addActionListener(e -> recordatorioYberiaService.cambiarActivo(r.getId(), interruptor.isSelected()));
        JButton editar = Estilos.botonIcono(Iconos.editar(18), "Editar recordatorio");
        editar.addActionListener(e -> editar(r));
        JButton eliminar = Estilos.botonIcono(Iconos.eliminar(18), "Eliminar recordatorio");
        eliminar.addActionListener(e -> eliminar(r));

        JPanel acciones = Estilos.transparente(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.insets = new Insets(0, 0, 8, 0);
        acciones.add(interruptor, c);
        c.insets = new Insets(0, 0, 2, 0);
        acciones.add(editar, c);
        c.insets = new Insets(0, 0, 0, 0);
        acciones.add(eliminar, c);
        tarjeta.add(acciones, BorderLayout.EAST);
        return tarjeta;
    }

    private String vigencia(RecordatorioYberia r, LocalDate hoy) {
        LocalDate inicio = recordatorioYberiaService.fechaInicio(r);
        LocalDate fin = recordatorioYberiaService.fechaFin(r);
        String rango = "Del " + inicio.format(FECHA) + " al " + fin.format(FECHA);
        if (fin.isBefore(hoy)) {
            return rango + " · tratamiento terminado";
        }
        if (r.isSemanal()) {
            LocalDate desde = hoy.isBefore(inicio) ? inicio : hoy;
            long transcurridos = ChronoUnit.DAYS.between(inicio, desde);
            LocalDate siguiente = inicio.plusDays((transcurridos + 6) / 7 * 7);
            long tomas = siguiente.isAfter(fin) ? 0 : ChronoUnit.DAYS.between(siguiente, fin) / 7 + 1;
            return rango + " · " + (tomas <= 1 ? "última toma" : "quedan " + tomas + " tomas");
        }
        long dias = ChronoUnit.DAYS.between(hoy, fin) + 1;
        return rango + " · " + (dias == 1 ? "último día" : "quedan " + dias + " días");
    }

    private static String frecuenciaTexto(RecordatorioYberia r) {
        if (r.isSemanal()) {
            return "1 vez por semana";
        }
        return r.getFrecuenciaHoras() == 24 ? "1 vez al día" : "Cada " + r.getFrecuenciaHoras() + " h";
    }

    private static String dia(LocalDate fecha, LocalDate hoy) {
        if (fecha.equals(hoy)) {
            return "hoy";
        }
        if (fecha.equals(hoy.plusDays(1))) {
            return "mañana";
        }
        return "el " + fecha.format(DIA_SEMANA) + " " + fecha.format(FECHA);
    }

    private static String capitalizar(String texto) {
        String sinArticulo = texto.startsWith("el ") ? texto.substring(3) : texto;
        return Character.toUpperCase(sinArticulo.charAt(0)) + sinArticulo.substring(1);
    }

    private void eliminar(RecordatorioYberia r) {
        int respuesta = JOptionPane.showConfirmDialog(this,
                "¿Eliminar el recordatorio de " + r.getMedicamento() + "?",
                "Eliminar recordatorio", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (respuesta == JOptionPane.YES_OPTION) {
            if (enEdicion != null && enEdicion.getId().equals(r.getId())) {
                salirDeEdicion();
            }
            recordatorioYberiaService.eliminar(r.getId());
        }
    }

    private void actualizarProximaToma() {
        LocalDateTime ahora = LocalDateTime.now();
        proximaToma.setText(recordatorioYberiaService.proximaToma(ahora)
                .map(t -> "Próxima toma: " + t.recordatorio().getMedicamento() + ", "
                        + dia(t.cuando().toLocalDate(), ahora.toLocalDate())
                        + " a las " + t.cuando().format(HORA)
                        + " (" + t.recordatorio().getMomentoComida().getEtiqueta().toLowerCase() + ")")
                .orElse("No hay tomas programadas próximamente."));
    }

    private void revisarTomas() {
        LocalDateTime ahora = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        for (RecordatorioYberia r : recordatorioYberiaService.tomasEn(ahora)) {
            if (avisadas.add(r.getId() + "@" + ahora)) {
                Window ventana = SwingUtilities.getWindowAncestor(this);
                String detalle = r.getMomentoComida().getEtiqueta()
                        + (r.getNotas() == null || r.getNotas().isBlank() ? "" : ". " + r.getNotas());
                Notificacion.mostrar(ventana, "Recordatorio · " + ahora.format(HORA), "Toma tu " + r.getMedicamento(),
                        detalle);
            }
        }
        actualizarProximaToma();
    }
}
