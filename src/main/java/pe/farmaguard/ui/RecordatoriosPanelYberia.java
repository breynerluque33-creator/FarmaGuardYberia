package pe.farmaguard.ui;

import com.formdev.flatlaf.FlatClientProperties;
import pe.farmaguard.model.MedicamentoYberia;
import pe.farmaguard.model.MomentoComidaYberia;
import pe.farmaguard.model.NivelRiesgoYberia;
import pe.farmaguard.model.RecordatorioYberia;
import pe.farmaguard.service.MedicamentoYberiaService;
import pe.farmaguard.service.RecordatorioYberiaService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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

public class RecordatoriosPanelYberia extends JPanel {
    private static final DateTimeFormatter HORA_ISO_YBERIA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter HORA_YBERIA = DateTimeFormatter.ofPattern("h:mm a", Locale.US);
    private static final DateTimeFormatter FECHA_YBERIA = DateTimeFormatter.ofPattern("dd/MM");
    private static final int REVISION_MS_YBERIA = 20_000;
    private static final int SEMANAL_YBERIA = 168;
    private static final int MAX_SEMANAS_YBERIA = 52;
    private static final DateTimeFormatter DIA_SEMANA_YBERIA = DateTimeFormatter.ofPattern("EEEE", Locale.forLanguageTag("es-PE"));

    private final RecordatorioYberiaService recordatorioYberiaService;
    private final MedicamentoYberiaService medicamentoYberiaService;

    private final CampoBusquedaYberia medicamentoYberia;
    private final SegmentadoYberia<MomentoComidaYberia> momentoYberia = new SegmentadoYberia<>(
            List.of(MomentoComidaYberia.ANTES, MomentoComidaYberia.DURANTE, MomentoComidaYberia.DESPUES, MomentoComidaYberia.INDIFERENTE),
            RecordatoriosPanelYberia::textoMomento);
    private final SegmentadoYberia<Integer> frecuenciaYberia = new SegmentadoYberia<>(List.of(4, 6, 8, 12, 24, SEMANAL_YBERIA),
            h -> h == SEMANAL_YBERIA ? "Semanal" : h == 24 ? "1 al día" : h + " h");
    private final JLabel ayudaFrecuenciaYberia = EstilosYberia.etiqueta(" ", "small", EstilosYberia::textoSecundario);
    private final JSpinner horaInicioYberia = new JSpinner(new SpinnerDateModel());
    private final SpinnerNumberModel modeloDuracionYberia = new SpinnerNumberModel(5, 1, 365, 1);
    private final JSpinner duracionYberia = new JSpinner(modeloDuracionYberia);
    private final JLabel etiquetaDuracionYberia = EstilosYberia.etiqueta("Durante (días)", "h4");
    private final JTextField notasYberia = new JTextField();
    private final JLabel mensajeFormularioYberia = EstilosYberia.etiqueta(" ", "small",
            () -> EstilosYberia.textoNivel(NivelRiesgoYberia.PELIGRO));
    private final JLabel tituloFormularioYberia = EstilosYberia.etiqueta("Nuevo recordatorio", "h2");
    private final JLabel etiquetaHoraYberia = EstilosYberia.etiqueta("Primera toma (hoy)", "h4");
    private final BotonPrimarioYberia guardarYberia = new BotonPrimarioYberia("Guardar recordatorio", IconosYberia.mas(14, () -> Color.WHITE));
    private final JButton cancelarEdicionYberia = EstilosYberia.botonSecundario("Cancelar");
    private final InterruptorYberia conComidasYberia = new InterruptorYberia(true);
    private final JPanel filaComidasYberia = EstilosYberia.transparente(new BorderLayout(10, 0));
    private final JLabel ayudaComidasYberia = EstilosYberia.etiqueta(" ", "small", EstilosYberia::textoSecundario);
    private final JPanel atajosYberia = EstilosYberia.transparente(new FlowLayout(FlowLayout.LEFT, 0, 0));
    private final BotonPrimarioYberia nuevoYberia = new BotonPrimarioYberia("Nuevo recordatorio", IconosYberia.mas(14, () -> Color.WHITE));
    private final JComponent formularioYberia;
    private JDialog dialogoYberia;
    private RecordatorioYberia enEdicionYberia;

    private final PanelVerticalYberia agendaYberia = new PanelVerticalYberia();
    private final JLabel proximaTomaYberia = EstilosYberia.etiqueta(" ", "h4", EstilosYberia::acento);
    private final Set<String> avisadasYberia = new HashSet<>();

    public RecordatoriosPanelYberia(RecordatorioYberiaService recordatorioYberiaService, MedicamentoYberiaService medicamentoYberiaService) {
        super(new GridBagLayout());
        this.recordatorioYberiaService = recordatorioYberiaService;
        this.medicamentoYberiaService = medicamentoYberiaService;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(4, 20, 16, 20));

        medicamentoYberia = new CampoBusquedaYberia(medicamentoYberiaService.listarTodos(), "Busca en el catálogo o escribe el nombre");
        medicamentoYberia.alElegir(m -> {
            medicamentoYberia.setTextoSinSugerir(m.getNombre());
            sugerirDesdeCatalogo(m);
        });

        formularioYberia = crearFormulario();
        nuevoYberia.addActionListener(e -> abrirNuevo());

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.weightx = 1;
        add(crearAgenda(), c);

        refrescar();
        recordatorioYberiaService.alCambiar(() -> SwingUtilities.invokeLater(this::refrescar));
        new Timer(REVISION_MS_YBERIA, e -> revisarTomas()).start();
    }

    private static String textoMomento(MomentoComidaYberia momentoYberia) {
        return switch (momentoYberia) {
            case ANTES -> "Antes";
            case DURANTE -> "Durante";
            case DESPUES -> "Después";
            case INDIFERENTE -> "Da igual";
        };
    }

    private JComponent crearFormulario() {
        momentoYberia.setSeleccion(MomentoComidaYberia.DESPUES);
        frecuenciaYberia.setSeleccion(8);
        frecuenciaYberia.alCambiar(h -> {
            ajustarDuracion();
            actualizarComidas();
        });
        momentoYberia.alCambiar(m -> actualizarComidas());
        conComidasYberia.addActionListener(e -> actualizarComidas());

        horaInicioYberia.setEditor(new JSpinner.DateEditor(horaInicioYberia, "HH:mm"));
        for (JSpinner selector : List.of(horaInicioYberia, duracionYberia)) {
            JTextField texto = ((JSpinner.DefaultEditor) selector.getEditor()).getTextField();
            texto.setHorizontalAlignment(JTextField.CENTER);
            texto.setMargin(new Insets(6, 8, 6, 8));
        }
        fijarHora(LocalTime.now().plusHours(1).truncatedTo(ChronoUnit.HOURS));
        notasYberia.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Ej.: con un vaso de agua, no manejar");
        notasYberia.setMargin(new Insets(8, 8, 8, 8));

        TarjetaYberia tarjeta = new TarjetaYberia(new BorderLayout(), EstilosYberia::superficie, 22);
        PanelVerticalYberia columna = new PanelVerticalYberia();
        columna.agregar(tituloFormularioYberia);
        columna.espacio(2);
        columna.agregar(EstilosYberia.parrafo("Te avisaremos a la hora de cada toma mientras FarmaGuard esté abierto.",
                EstilosYberia::textoSecundario));

        campo(columna, "¿Qué medicamento?", medicamentoYberia);
        campo(columna, "¿Cuándo, respecto a la comida?", momentoYberia);
        campo(columna, "¿Con qué frecuencia?", frecuenciaYberia);
        columna.espacio(4);
        columna.agregar(ayudaFrecuenciaYberia);

        PanelVerticalYberia textosComidas = new PanelVerticalYberia();
        textosComidas.agregar(EstilosYberia.etiqueta("Tomarlo junto a mis comidas", "h4"));
        textosComidas.espacio(2);
        textosComidas.agregar(ayudaComidasYberia);
        JPanel columnaInterruptor = EstilosYberia.transparente(new BorderLayout());
        columnaInterruptor.add(conComidasYberia, BorderLayout.NORTH);
        filaComidasYberia.add(columnaInterruptor, BorderLayout.WEST);
        filaComidasYberia.add(textosComidas, BorderLayout.CENTER);
        columna.espacio(12);
        columna.agregar(filaComidasYberia);

        JPanel horaYDuracion = EstilosYberia.transparente(new GridLayout(1, 2, 12, 0));
        PanelVerticalYberia columnaHora = new PanelVerticalYberia();
        columnaHora.agregar(etiquetaHoraYberia);
        columnaHora.espacio(6);
        columnaHora.agregar(horaInicioYberia);
        PanelVerticalYberia columnaDuracion = new PanelVerticalYberia();
        columnaDuracion.agregar(etiquetaDuracionYberia);
        columnaDuracion.espacio(6);
        columnaDuracion.agregar(duracionYberia);
        horaYDuracion.add(columnaHora);
        horaYDuracion.add(columnaDuracion);
        columna.espacio(16);
        columna.agregar(horaYDuracion);

        atajosYberia.add(EstilosYberia.etiqueta("Rápido:", "small", EstilosYberia::textoSecundario));
        for (LocalTime hora : List.of(LocalTime.of(8, 0), LocalTime.of(13, 0), LocalTime.of(20, 0))) {
            JButton atajo = EstilosYberia.botonSecundario(hora.format(HORA_YBERIA));
            atajo.putClientProperty(FlatClientProperties.STYLE_CLASS, "small");
            atajo.setMargin(new Insets(2, 10, 2, 10));
            atajo.addActionListener(e -> fijarHora(hora));
            atajosYberia.add(Box.createHorizontalStrut(6));
            atajosYberia.add(atajo);
        }
        columna.espacio(8);
        columna.agregar(atajosYberia);

        campo(columna, "Notas (opcional)", notasYberia);

        columna.espacio(18);
        guardarYberia.addActionListener(e -> guardar());
        cancelarEdicionYberia.addActionListener(e -> cerrarFormulario());
        JPanel filaGuardar = EstilosYberia.transparente(new BorderLayout(8, 0));
        filaGuardar.add(guardarYberia, BorderLayout.CENTER);
        filaGuardar.add(cancelarEdicionYberia, BorderLayout.EAST);
        columna.agregar(filaGuardar);
        columna.espacio(4);
        columna.agregar(mensajeFormularioYberia);

        tarjeta.add(columna, BorderLayout.CENTER);
        actualizarComidas();
        return tarjeta;
    }

    private void actualizarComidas() {
        Optional<RecordatorioYberiaService.PlanComidasYberia> plan = planActual();
        filaComidasYberia.setVisible(plan.isPresent());
        boolean alineado = plan.isPresent() && conComidasYberia.isSelected();
        horaInicioYberia.setEnabled(!alineado);
        for (Component atajo : atajosYberia.getComponents()) {
            atajo.setEnabled(!alineado);
        }
        if (plan.isEmpty()) {
            return;
        }
        String horas = plan.get().horasYberia().stream().map(HORA_YBERIA::format).collect(Collectors.joining(" · "));
        String comidas = switch (plan.get().horasYberia().size()) {
            case 1 -> "con el desayuno";
            case 2 -> "con el desayuno y la cena";
            default -> "con el desayuno, el almuerzo y la cena";
        };
        ayudaComidasYberia.setText(alineado ? "Tomas " + comidas + ": " + horas : "Se usará la hora de la primera toma.");
        if (alineado) {
            fijarHora(plan.get().horasYberia().get(0));
        }
    }

    private Optional<RecordatorioYberiaService.PlanComidasYberia> planActual() {
        Integer cadaHoras = frecuenciaYberia.getSeleccion();
        if (cadaHoras == null || esSemanal()) {
            return Optional.empty();
        }
        return RecordatorioYberiaService.planConComidas(momentoYberia.getSeleccion(), cadaHoras, LocalDateTime.now());
    }

    private void abrirNuevo() {
        salirDeEdicion();
        momentoYberia.setSeleccion(MomentoComidaYberia.DESPUES);
        frecuenciaYberia.setSeleccion(8);
        ajustarDuracion();
        fijarHora(LocalTime.now().plusHours(1).truncatedTo(ChronoUnit.HOURS));
        conComidasYberia.setSelected(true);
        actualizarComidas();
        mostrarFormulario();
    }

    private void mostrarFormulario() {
        Window ventana = SwingUtilities.getWindowAncestor(this);
        if (dialogoYberia == null) {
            dialogoYberia = new JDialog(ventana, Dialog.ModalityType.DOCUMENT_MODAL);
            JPanel contenido = new JPanel(new BorderLayout());
            contenido.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            PanelVerticalYberia columna = new PanelVerticalYberia();
            columna.agregar(formularioYberia);
            contenido.add(EstilosYberia.scroll(columna), BorderLayout.CENTER);
            dialogoYberia.setContentPane(contenido);
            dialogoYberia.getRootPane().registerKeyboardAction(e -> cerrarFormulario(),
                    KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
            dialogoYberia.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            dialogoYberia.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    cerrarFormulario();
                }
            });
        }
        dialogoYberia.setTitle(enEdicionYberia == null ? "Nuevo recordatorio" : "Editar recordatorio");
        dialogoYberia.pack();
        int alto = ventana == null ? dialogoYberia.getHeight() : Math.min(dialogoYberia.getHeight(), ventana.getHeight() - 40);
        dialogoYberia.setSize(Math.max(500, dialogoYberia.getWidth()), alto);
        dialogoYberia.setLocationRelativeTo(ventana);
        SwingUtilities.invokeLater(medicamentoYberia::requestFocusInWindow);
        dialogoYberia.setVisible(true);
    }

    private void cerrarFormulario() {
        salirDeEdicion();
        if (dialogoYberia != null) {
            dialogoYberia.setVisible(false);
        }
    }

    private static void campo(PanelVerticalYberia columna, String etiqueta, JComponent control) {
        columna.espacio(16);
        columna.agregar(EstilosYberia.etiqueta(etiqueta, "h4"));
        columna.espacio(6);
        columna.agregar(control);
    }

    private void fijarHora(LocalTime hora) {
        horaInicioYberia.setValue(Date.from(LocalDate.now().atTime(hora).atZone(ZoneId.systemDefault()).toInstant()));
    }

    private void sugerirDesdeCatalogo(MedicamentoYberia m) {
        momentoYberia.setSeleccion(m.getMomentoComida());
        if (RecordatorioYberiaService.esSemanal(m)) {
            frecuenciaYberia.setSeleccion(SEMANAL_YBERIA);
        } else {
            frecuenciaYberia.setSeleccion(RecordatorioYberiaService.frecuenciaPara(m.getUnidadesDiaHabitual()));
        }
        ajustarDuracion();
        if (!esSemanal()) {
            modeloDuracionYberia.setValue(RecordatorioYberiaService.diasSugeridos(m));
        }
        actualizarComidas();
    }

    private void editar(RecordatorioYberia r) {
        enEdicionYberia = r;
        medicamentoYberia.setTextoSinSugerir(r.getMedicamento());
        momentoYberia.setSeleccion(r.getMomentoComida());
        frecuenciaYberia.setSeleccion(r.isSemanal() ? SEMANAL_YBERIA : RecordatorioYberiaService.FRECUENCIAS_YBERIA.stream()
                .filter(h -> h >= r.getFrecuenciaHoras())
                .findFirst()
                .orElse(24));
        ajustarDuracion();
        int cantidad = r.isSemanal() ? (r.getDuracionDias() - 1) / 7 + 1 : r.getDuracionDias();
        modeloDuracionYberia.setValue(Math.min(cantidad, (Integer) modeloDuracionYberia.getMaximum()));
        fijarHora(LocalTime.parse(r.getHoraInicio()));
        conComidasYberia.setSelected(r.tieneHorasFijas() || RecordatorioYberiaService
                .planConComidas(r.getMomentoComida(), r.getFrecuenciaHoras(), LocalDateTime.now())
                .map(p -> p.horasYberia().contains(LocalTime.parse(r.getHoraInicio())))
                .orElse(false));
        notasYberia.setText(r.getNotas() == null ? "" : r.getNotas());
        mensajeFormularioYberia.setText(" ");

        LocalDate inicio = recordatorioYberiaService.fechaInicio(r);
        tituloFormularioYberia.setText("Editar recordatorio");
        etiquetaHoraYberia.setText(inicio.equals(LocalDate.now()) ? "Primera toma (hoy)"
                : "Primera toma (" + inicio.format(FECHA_YBERIA) + ")");
        guardarYberia.setText("Guardar cambios");
        ayudaFrecuenciaYberia.setText(esSemanal() ? "Una sola toma cada " + inicio.format(DIA_SEMANA_YBERIA) + "." : " ");
        actualizarComidas();
        mostrarFormulario();
    }

    private void salirDeEdicion() {
        enEdicionYberia = null;
        tituloFormularioYberia.setText("Nuevo recordatorio");
        etiquetaHoraYberia.setText("Primera toma (hoy)");
        guardarYberia.setText("Guardar recordatorio");
        medicamentoYberia.setTextoSinSugerir("");
        notasYberia.setText("");
        mensajeFormularioYberia.setText(" ");
        ajustarDuracion();
    }

    private void ajustarDuracion() {
        boolean semanal = esSemanal();
        String etiqueta = semanal ? "Durante (semanas)" : "Durante (días)";
        if (!etiqueta.equals(etiquetaDuracionYberia.getText())) {
            etiquetaDuracionYberia.setText(etiqueta);
            modeloDuracionYberia.setMaximum(semanal ? MAX_SEMANAS_YBERIA : 365);
            modeloDuracionYberia.setValue(semanal ? 4 : 5);
        }
        ayudaFrecuenciaYberia.setText(semanal
                ? "Una sola toma cada " + LocalDate.now().format(DIA_SEMANA_YBERIA) + ", empezando hoy."
                : " ");
    }

    private boolean esSemanal() {
        return Integer.valueOf(SEMANAL_YBERIA).equals(frecuenciaYberia.getSeleccion());
    }

    private void guardar() {
        String nombre = medicamentoYberia.getText().trim();
        LocalTime hora = ((Date) horaInicioYberia.getValue()).toInstant().atZone(ZoneId.systemDefault())
                .toLocalTime().truncatedTo(ChronoUnit.MINUTES);
        Integer cadaHoras = frecuenciaYberia.getSeleccion();
        MomentoComidaYberia comida = momentoYberia.getSeleccion();
        MomentoComidaYberia momentoComida = comida == null ? MomentoComidaYberia.INDIFERENTE : comida;
        int cantidad = (Integer) duracionYberia.getValue();
        String fechaInicio = enEdicionYberia != null ? enEdicionYberia.getFechaInicio() : LocalDate.now().toString();
        String horasFijas = null;
        Optional<RecordatorioYberiaService.PlanComidasYberia> plan = planActual();
        if (plan.isPresent() && conComidasYberia.isSelected()) {
            horasFijas = plan.get().horasFijasYberia();
            if (enEdicionYberia == null) {
                hora = plan.get().primeraYberia().toLocalTime();
                fechaInicio = plan.get().primeraYberia().toLocalDate().toString();
            } else {
                hora = plan.get().horasYberia().get(0);
            }
        }
        RecordatorioYberia recordatorioYberia = esSemanal()
                ? RecordatorioYberia.cadaDias(nombre, 7, (cantidad - 1) * 7 + 1, momentoComida,
                        hora.format(HORA_ISO_YBERIA), fechaInicio, notasYberia.getText().trim())
                : new RecordatorioYberia(nombre, cadaHoras == null ? 0 : cadaHoras, cantidad, momentoComida,
                        hora.format(HORA_ISO_YBERIA), fechaInicio, notasYberia.getText().trim()).conHorasFijas(horasFijas);
        try {
            if (enEdicionYberia != null) {
                recordatorioYberiaService.editar(enEdicionYberia.getId(), recordatorioYberia);
            } else {
                recordatorioYberiaService.guardar(recordatorioYberia);
            }
        } catch (IllegalArgumentException ex) {
            mensajeFormularioYberia.setText(ex.getMessage());
            return;
        }
        cerrarFormulario();
    }

    private JComponent crearAgenda() {
        JPanel panel = EstilosYberia.transparente(new BorderLayout(0, 4));

        PanelVerticalYberia cabecera = new PanelVerticalYberia();
        JPanel titulos = EstilosYberia.transparente(new GridLayout(0, 1, 0, 2));
        titulos.setBorder(BorderFactory.createEmptyBorder(6, 6, 4, 6));
        titulos.add(EstilosYberia.etiqueta("Mi agenda", "h2"));
        titulos.add(EstilosYberia.etiqueta("Tus medicamentos ordenados por la próxima toma.", null, EstilosYberia::textoSecundario));
        JPanel contenedorNuevo = EstilosYberia.transparente(new GridBagLayout());
        contenedorNuevo.add(nuevoYberia);
        JPanel filaTitulo = EstilosYberia.transparente(new BorderLayout(12, 0));
        filaTitulo.add(titulos, BorderLayout.CENTER);
        filaTitulo.add(contenedorNuevo, BorderLayout.EAST);
        cabecera.agregar(filaTitulo);

        TarjetaYberia banner = new TarjetaYberia(new BorderLayout(12, 0), EstilosYberia::acentoSuave, 12);
        banner.setBorde(null);
        banner.add(new JLabel(IconosYberia.reloj(22)), BorderLayout.WEST);
        banner.add(proximaTomaYberia, BorderLayout.CENTER);
        cabecera.agregar(banner);

        panel.add(cabecera, BorderLayout.NORTH);
        panel.add(EstilosYberia.scroll(agendaYberia), BorderLayout.CENTER);
        return panel;
    }

    private void refrescar() {
        LocalDateTime ahora = LocalDateTime.now();
        agendaYberia.removeAll();

        record EntradaYberia(RecordatorioYberia recordatorioYberia, Optional<LocalDateTime> proximaYberia) {
        }
        List<EntradaYberia> entradas = recordatorioYberiaService.listar().stream()
                .map(r -> new EntradaYberia(r, recordatorioYberiaService.proximaToma(r, ahora)))
                .sorted(Comparator.comparing((EntradaYberia e) -> e.proximaYberia().orElse(LocalDateTime.MAX))
                        .thenComparing(e -> e.recordatorioYberia().getHoraInicio()))
                .toList();

        if (entradas.isEmpty()) {
            agendaYberia.agregar(estadoVacio());
        }
        for (EntradaYberia entrada : entradas) {
            agendaYberia.agregar(tarjetaRecordatorio(entrada.recordatorioYberia(), entrada.proximaYberia(), ahora));
        }
        agendaYberia.revalidate();
        agendaYberia.repaint();
        actualizarProximaToma();
    }

    private static JComponent estadoVacio() {
        TarjetaYberia tarjeta = new TarjetaYberia(new BorderLayout(14, 0), EstilosYberia::superficie, 22);
        JPanel columnaIcono = EstilosYberia.transparente(new BorderLayout());
        columnaIcono.add(new JLabel(IconosYberia.reloj(34)), BorderLayout.NORTH);
        tarjeta.add(columnaIcono, BorderLayout.WEST);
        PanelVerticalYberia textos = new PanelVerticalYberia();
        textos.agregar(EstilosYberia.etiqueta("Aún no tienes recordatorios", "h3"));
        textos.espacio(4);
        textos.agregar(EstilosYberia.parrafo("Pulsa «Nuevo recordatorio» (arriba a la derecha): elige el medicamento, "
                + "si va antes, durante o después de comer y cada cuánto se toma. También puedes crearlos todos "
                + "de una vez desde «Analizar compra» con «Agregar lista a Recordatorios».", EstilosYberia::textoSecundario));
        tarjeta.add(textos, BorderLayout.CENTER);
        return tarjeta;
    }

    private JComponent tarjetaRecordatorio(RecordatorioYberia r, Optional<LocalDateTime> proximaYberia, LocalDateTime ahora) {
        boolean terminado = recordatorioYberiaService.fechaFin(r).isBefore(ahora.toLocalDate());
        TarjetaYberia tarjeta = new TarjetaYberia(new BorderLayout(16, 0), EstilosYberia::superficie, 16);
        tarjeta.setAtenuada(!r.isActivo() || terminado);

        LocalTime hora = proximaYberia.map(LocalDateTime::toLocalTime).orElse(LocalTime.parse(r.getHoraInicio()));
        String cuando;
        if (proximaYberia.isPresent()) {
            cuando = capitalizar(dia(proximaYberia.get().toLocalDate(), ahora.toLocalDate()));
        } else if (!r.isActivo()) {
            cuando = "En pausa";
        } else if (terminado) {
            cuando = "Terminado";
        } else {
            cuando = "Sin tomas próximas";
        }
        PanelVerticalYberia columnaHora = new PanelVerticalYberia();
        columnaHora.agregar(EstilosYberia.etiqueta(hora.format(HORA_YBERIA), "h2", EstilosYberia::acento));
        columnaHora.agregar(EstilosYberia.etiqueta(cuando, "small", EstilosYberia::textoSecundario));
        columnaHora.setPreferredSize(new Dimension(108, columnaHora.getPreferredSize().height));
        tarjeta.add(columnaHora, BorderLayout.WEST);

        PanelVerticalYberia detalle = new PanelVerticalYberia();
        detalle.agregar(EstilosYberia.etiqueta(r.getMedicamento(), "h3"));
        detalle.espacio(6);
        JPanel insignias = EstilosYberia.transparente(new FlowLayout(FlowLayout.LEFT, 0, 0));
        insignias.add(new InsigniaYberia(r.getMomentoComida().getEtiqueta(), EstilosYberia::acentoSuave, EstilosYberia::acento));
        insignias.add(Box.createHorizontalStrut(6));
        insignias.add(new InsigniaYberia(frecuenciaTexto(r), EstilosYberia::superficieSuave, EstilosYberia::textoSecundario));
        detalle.agregar(insignias);
        detalle.espacio(6);
        String horario = recordatorioYberiaService.horarios(r).stream().map(HORA_YBERIA::format).collect(Collectors.joining(" · "));
        detalle.agregar(EstilosYberia.parrafo(r.isSemanal()
                ? "Toma: " + horario + " cada " + recordatorioYberiaService.fechaInicio(r).format(DIA_SEMANA_YBERIA)
                : "Tomas: " + horario, EstilosYberia::textoSecundario));
        detalle.agregar(EstilosYberia.parrafo(vigencia(r, ahora.toLocalDate()), EstilosYberia::textoSecundario));
        if (r.getNotas() != null && !r.getNotas().isBlank()) {
            detalle.agregar(EstilosYberia.parrafo("Nota: " + r.getNotas(), EstilosYberia::textoPrincipal));
        }
        tarjeta.add(detalle, BorderLayout.CENTER);

        InterruptorYberia interruptor = new InterruptorYberia(r.isActivo());
        interruptor.setToolTipText(r.isActivo() ? "Pausar recordatorio" : "Activar recordatorio");
        interruptor.addActionListener(e -> recordatorioYberiaService.cambiarActivo(r.getId(), interruptor.isSelected()));
        JButton editar = EstilosYberia.botonIcono(IconosYberia.editar(18), "Editar recordatorio");
        editar.addActionListener(e -> editar(r));
        JButton eliminar = EstilosYberia.botonIcono(IconosYberia.eliminar(18), "Eliminar recordatorio");
        eliminar.addActionListener(e -> eliminar(r));

        JPanel acciones = EstilosYberia.transparente(new GridBagLayout());
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
        String rango = "Del " + inicio.format(FECHA_YBERIA) + " al " + fin.format(FECHA_YBERIA);
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
        if (r.tieneHorasFijas()) {
            return "Con cada comida";
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
        return "el " + fecha.format(DIA_SEMANA_YBERIA) + " " + fecha.format(FECHA_YBERIA);
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
            if (enEdicionYberia != null && enEdicionYberia.getId().equals(r.getId())) {
                salirDeEdicion();
            }
            recordatorioYberiaService.eliminar(r.getId());
        }
    }

    private void actualizarProximaToma() {
        LocalDateTime ahora = LocalDateTime.now();
        proximaTomaYberia.setText(recordatorioYberiaService.proximaToma(ahora)
                .map(t -> "Próxima toma: " + t.recordatorioYberia().getMedicamento() + ", "
                        + dia(t.cuandoYberia().toLocalDate(), ahora.toLocalDate())
                        + " a las " + t.cuandoYberia().format(HORA_YBERIA)
                        + " (" + t.recordatorioYberia().getMomentoComida().getEtiqueta().toLowerCase() + ")")
                .orElse("No hay tomas programadas próximamente."));
    }

    private void revisarTomas() {
        LocalDateTime ahora = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        for (RecordatorioYberia r : recordatorioYberiaService.tomasEn(ahora)) {
            if (avisadasYberia.add(r.getId() + "@" + ahora)) {
                Window ventana = SwingUtilities.getWindowAncestor(this);
                String detalle = r.getMomentoComida().getEtiqueta()
                        + (r.getNotas() == null || r.getNotas().isBlank() ? "" : ". " + r.getNotas());
                NotificacionYberia.mostrar(ventana, "Recordatorio · " + ahora.format(HORA_YBERIA), "Toma tu " + r.getMedicamento(),
                        detalle);
            }
        }
        actualizarProximaToma();
    }
}
