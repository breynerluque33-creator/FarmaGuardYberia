package pe.farmaguard.ui;

import com.formdev.flatlaf.FlatClientProperties;
import pe.farmaguard.model.MedicamentoYberia;
import pe.farmaguard.model.NivelRiesgoYberia;
import pe.farmaguard.model.RecordatorioYberia;
import pe.farmaguard.service.AlertaYberia;
import pe.farmaguard.service.CompatibilidadYberiaService;
import pe.farmaguard.service.InterpretacionListaYberia;
import pe.farmaguard.service.ItemCompraYberia;
import pe.farmaguard.service.MedicamentoYberiaService;
import pe.farmaguard.service.RecordatorioYberiaService;
import pe.farmaguard.service.ResultadoAnalisisYberia;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AnalisisPanelYberia extends JPanel {
    private static final DateTimeFormatter HORA_YBERIA = DateTimeFormatter.ofPattern("h:mm a", Locale.US);

    private final MedicamentoYberiaService medicamentoYberiaService;
    private final CompatibilidadYberiaService compatibilidadYberiaService;
    private final RecordatorioYberiaService recordatorioYberiaService;
    private final Runnable irARecordatoriosYberia;

    private final List<ItemCompraYberia> itemsYberia = new ArrayList<>();
    private final List<String> noReconocidosYberia = new ArrayList<>();

    private final CampoBusquedaYberia buscadorYberia;
    private final JLabel mensajeBuscadorYberia = EstilosYberia.etiqueta(" ", "small", () -> EstilosYberia.textoNivel(NivelRiesgoYberia.PELIGRO));
    private final JTextArea listaYberia = new JTextArea(4, 20);
    private final JPanel panelListaYberia = EstilosYberia.transparente(new BorderLayout(0, 8));
    private final PanelVerticalYberia filasYberia = new PanelVerticalYberia();
    private final InsigniaYberia contadorYberia = new InsigniaYberia("0 productos", EstilosYberia::superficieSuave, EstilosYberia::textoSecundario);
    private final BotonPrimarioYberia agregarRecordatoriosYberia = new BotonPrimarioYberia("Agregar lista a Recordatorios",
            IconosYberia.mas(14, () -> Color.WHITE));
    private final PanelVerticalYberia resultadosYberia = new PanelVerticalYberia();
    private final JScrollPane scrollResultadosYberia = EstilosYberia.scroll(resultadosYberia);

    public AnalisisPanelYberia(MedicamentoYberiaService medicamentoYberiaService, CompatibilidadYberiaService compatibilidadYberiaService,
                         RecordatorioYberiaService recordatorioYberiaService, Runnable irARecordatoriosYberia) {
        super(new GridBagLayout());
        this.medicamentoYberiaService = medicamentoYberiaService;
        this.compatibilidadYberiaService = compatibilidadYberiaService;
        this.recordatorioYberiaService = recordatorioYberiaService;
        this.irARecordatoriosYberia = irARecordatoriosYberia;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(4, 20, 16, 20));

        buscadorYberia = new CampoBusquedaYberia(medicamentoYberiaService.listarTodos(), "Busca: Panadol, Apronax, amoxicilina...");
        buscadorYberia.alElegir(m -> {
            agregar(m);
            buscadorYberia.setTextoSinSugerir("");
        });
        buscadorYberia.addActionListener(e -> agregarDesdeTexto());

        buscadorYberia.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                mensajeBuscadorYberia.setText(" ");
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                mensajeBuscadorYberia.setText(" ");
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });

        JPanel izquierda = EstilosYberia.transparente(new BorderLayout(0, 6));
        izquierda.add(crearTarjetaBuscador(), BorderLayout.NORTH);
        izquierda.add(crearTarjetaCompra(), BorderLayout.CENTER);
        izquierda.setPreferredSize(new Dimension(430, 300));
        izquierda.setMinimumSize(new Dimension(380, 300));

        JPanel derecha = EstilosYberia.transparente(new BorderLayout(0, 4));
        derecha.add(encabezado("Resultado del análisis", "Se actualiza al instante al agregar o quitar productos."),
                BorderLayout.NORTH);
        scrollResultadosYberia.setPreferredSize(new Dimension(300, 300));
        derecha.add(scrollResultadosYberia, BorderLayout.CENTER);
        derecha.setPreferredSize(new Dimension(420, 300));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridx = 0;
        c.weightx = 0.42;
        c.insets = new Insets(0, 0, 0, 10);
        add(izquierda, c);
        c.gridx = 1;
        c.weightx = 0.58;
        c.insets = new Insets(0, 10, 0, 0);
        add(derecha, c);

        refrescar();
    }

    public void enfocarBuscador() {
        buscadorYberia.requestFocusInWindow();
    }

    private JComponent crearTarjetaBuscador() {
        TarjetaYberia tarjeta = new TarjetaYberia(new BorderLayout(0, 12));
        tarjeta.add(encabezadoTarjeta("¿Qué te están ofreciendo?",
                "Escribe el nombre comercial o genérico y elige de la lista."), BorderLayout.NORTH);

        BotonPrimarioYberia agregar = new BotonPrimarioYberia("Agregar", IconosYberia.mas(14, () -> Color.WHITE));
        agregar.addActionListener(e -> agregarDesdeTexto());
        JPanel filaBuscador = EstilosYberia.transparente(new BorderLayout(10, 0));
        filaBuscador.add(buscadorYberia, BorderLayout.CENTER);
        filaBuscador.add(agregar, BorderLayout.EAST);

        listaYberia.setLineWrap(true);
        listaYberia.setWrapStyleWord(true);
        listaYberia.setMargin(new Insets(8, 10, 8, 10));
        listaYberia.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                "Un producto por línea o separados por comas.\nEj.: Panadol x2, Apronax 550, amoxicilina");
        JButton agregarLista = EstilosYberia.botonSecundario("Agregar todos");
        agregarLista.addActionListener(e -> agregarLista());
        JPanel filaAgregarLista = EstilosYberia.transparente(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        filaAgregarLista.add(agregarLista);
        panelListaYberia.add(new JScrollPane(listaYberia), BorderLayout.CENTER);
        panelListaYberia.add(filaAgregarLista, BorderLayout.SOUTH);
        panelListaYberia.setVisible(false);

        JButton alternarLista = EstilosYberia.botonEnlace("Pegar una lista o receta", IconosYberia.mas(12, EstilosYberia::acento));
        alternarLista.addActionListener(e -> {
            panelListaYberia.setVisible(!panelListaYberia.isVisible());
            alternarLista.setText(panelListaYberia.isVisible() ? "Ocultar lista" : "Pegar una lista o receta");
            if (panelListaYberia.isVisible()) {
                listaYberia.requestFocusInWindow();
            }
            revalidate();
        });

        PanelVerticalYberia cuerpo = new PanelVerticalYberia();
        cuerpo.agregar(filaBuscador);
        cuerpo.espacio(2);
        cuerpo.agregar(mensajeBuscadorYberia);
        cuerpo.agregar(alternarLista);
        cuerpo.agregar(panelListaYberia);
        tarjeta.add(cuerpo, BorderLayout.CENTER);
        return tarjeta;
    }

    private JComponent crearTarjetaCompra() {
        TarjetaYberia tarjeta = new TarjetaYberia(new BorderLayout(0, 8));
        JPanel cabecera = EstilosYberia.transparente(new BorderLayout());
        cabecera.add(EstilosYberia.etiqueta("Tu compra", "h3"), BorderLayout.WEST);
        JPanel contenedorContador = EstilosYberia.transparente(new GridBagLayout());
        contenedorContador.add(contadorYberia);
        cabecera.add(contenedorContador, BorderLayout.EAST);
        tarjeta.add(cabecera, BorderLayout.NORTH);

        tarjeta.add(EstilosYberia.scroll(filasYberia), BorderLayout.CENTER);

        JButton vaciar = EstilosYberia.botonEnlace("Vaciar lista", IconosYberia.eliminar(14));
        vaciar.addActionListener(e -> {
            itemsYberia.clear();
            noReconocidosYberia.clear();
            refrescar();
        });
        agregarRecordatoriosYberia.setToolTipText("Crea un recordatorio por producto con la frecuencia, duración y "
                + "momento de comida del catálogo. Luego puedes editarlos.");
        agregarRecordatoriosYberia.addActionListener(e -> agregarARecordatorios(
                compatibilidadYberiaService.analizar(List.copyOf(itemsYberia), List.copyOf(noReconocidosYberia))));
        JPanel contenedorRecordatorios = EstilosYberia.transparente(new GridBagLayout());
        contenedorRecordatorios.add(agregarRecordatoriosYberia);
        JPanel pie = EstilosYberia.transparente(new BorderLayout(8, 0));
        pie.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        pie.add(vaciar, BorderLayout.WEST);
        pie.add(contenedorRecordatorios, BorderLayout.EAST);
        tarjeta.add(pie, BorderLayout.SOUTH);
        return tarjeta;
    }

    private static JComponent encabezado(String titulo, String subtitulo) {
        JPanel panel = EstilosYberia.transparente(new GridLayout(0, 1, 0, 2));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 4, 6));
        panel.add(EstilosYberia.etiqueta(titulo, "h2"));
        panel.add(EstilosYberia.etiqueta(subtitulo, null, EstilosYberia::textoSecundario));
        return panel;
    }

    private static JComponent encabezadoTarjeta(String titulo, String subtitulo) {
        JPanel panel = EstilosYberia.transparente(new GridLayout(0, 1, 0, 2));
        panel.add(EstilosYberia.etiqueta(titulo, "h3"));
        panel.add(EstilosYberia.etiqueta(subtitulo, "small", EstilosYberia::textoSecundario));
        return panel;
    }

    private void agregarDesdeTexto() {
        String texto = buscadorYberia.getText().trim();
        if (texto.isEmpty()) {
            buscadorYberia.requestFocusInWindow();
            return;
        }
        medicamentoYberiaService.buscar(texto).ifPresentOrElse(m -> {
            agregar(m);
            buscadorYberia.setTextoSinSugerir("");
        }, () -> mensajeBuscadorYberia.setText("No encontramos \"" + texto + "\". Revisa el nombre o prueba con el componente."));
    }

    private void agregarLista() {
        if (listaYberia.getText().isBlank()) {
            return;
        }
        InterpretacionListaYberia interpretacion = medicamentoYberiaService.interpretarLista(listaYberia.getText());
        interpretacion.encontradosYberia().stream()
                .filter(m -> itemsYberia.stream().noneMatch(i -> i.medicamentoYberia().equals(m)))
                .forEach(m -> itemsYberia.add(ItemCompraYberia.conDosisHabitual(m)));
        interpretacion.noReconocidosYberia().stream()
                .filter(n -> !noReconocidosYberia.contains(n))
                .forEach(noReconocidosYberia::add);
        listaYberia.setText("");
        refrescar();
    }

    private void agregar(MedicamentoYberia medicamentoYberia) {
        if (itemsYberia.stream().anyMatch(i -> i.medicamentoYberia().equals(medicamentoYberia))) {
            Toolkit.getDefaultToolkit().beep();
            mensajeBuscadorYberia.setText(medicamentoYberia.getNombre() + " ya está en tu compra.");
            return;
        }
        itemsYberia.add(ItemCompraYberia.conDosisHabitual(medicamentoYberia));
        refrescar();
    }

    private void agregarARecordatorios(ResultadoAnalisisYberia resultado) {
        if (resultado.nivelGeneralYberia() == NivelRiesgoYberia.PELIGRO) {
            int respuesta = JOptionPane.showConfirmDialog(this,
                    "El análisis encontró alertas importantes en esta lista.\n"
                            + "Revisa las alertas o consulta antes de tomarlos juntos.\n\n"
                            + "¿Crear los recordatorios de todos modos?",
                    "Agregar lista a Recordatorios", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (respuesta != JOptionPane.YES_OPTION) {
                return;
            }
        }
        RecordatorioYberiaService.ResultadoAutomaticoYberia creados =
                recordatorioYberiaService.agregarAutomaticos(List.copyOf(itemsYberia), LocalDateTime.now());

        StringBuilder mensaje = new StringBuilder();
        if (creados.creadosYberia().isEmpty()) {
            mensaje.append("No se creó ningún recordatorio nuevo.\n");
        } else {
            mensaje.append(creados.creadosYberia().size() == 1 ? "Se creó 1 recordatorio:\n"
                    : "Se crearon " + creados.creadosYberia().size() + " recordatorios:\n");
            for (RecordatorioYberia r : creados.creadosYberia()) {
                mensaje.append("  • ").append(r.getMedicamento()).append(": ")
                        .append(r.isSemanal() ? "1 vez por semana"
                                : r.tieneHorasFijas() ? "con cada comida (desayuno, almuerzo y cena)"
                                : r.getFrecuenciaHoras() == 24 ? "1 vez al día" : "cada " + r.getFrecuenciaHoras() + " h")
                        .append(", primera toma ").append(primeraToma(r)).append('\n');
            }
        }
        if (!creados.omitidosYberia().isEmpty()) {
            mensaje.append("\nYa tenían un recordatorio activo: ").append(String.join(", ", creados.omitidosYberia()))
                    .append('\n');
        }
        if (!noReconocidosYberia.isEmpty()) {
            mensaje.append("\nNo están en el catálogo (agrégalos a mano): ").append(String.join(", ", noReconocidosYberia))
                    .append('\n');
        }
        mensaje.append("\nPuedes editarlos cuando quieras en la pestaña Recordatorios.");

        Object[] opciones = {"Ver recordatorios", "Seguir aquí"};
        int eleccion = JOptionPane.showOptionDialog(this, mensaje.toString(), "Agregar lista a Recordatorios",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, opciones, opciones[0]);
        if (eleccion == 0) {
            irARecordatoriosYberia.run();
        }
    }

    private static String primeraToma(RecordatorioYberia r) {
        LocalDate fecha = LocalDate.parse(r.getFechaInicio());
        String hora = LocalTime.parse(r.getHoraInicio()).format(HORA_YBERIA);
        return (fecha.equals(LocalDate.now()) ? "hoy" : fecha.equals(LocalDate.now().plusDays(1)) ? "mañana"
                : fecha.toString()) + " a las " + hora + " (" + r.getMomentoComida().getEtiqueta().toLowerCase() + ")";
    }

    private void cambiarUnidades(MedicamentoYberia medicamentoYberia, int delta) {
        for (int i = 0; i < itemsYberia.size(); i++) {
            ItemCompraYberia item = itemsYberia.get(i);
            if (item.medicamentoYberia().equals(medicamentoYberia)) {
                int unidades = Math.max(1, Math.min(24, item.unidadesDiaYberia() + delta));
                itemsYberia.set(i, new ItemCompraYberia(medicamentoYberia, unidades));
            }
        }
        refrescar();
    }

    private void quitar(MedicamentoYberia medicamentoYberia) {
        itemsYberia.removeIf(i -> i.medicamentoYberia().equals(medicamentoYberia));
        refrescar();
    }

    private void refrescar() {
        refrescarFilas();
        refrescarResultados();
    }

    private void refrescarFilas() {
        filasYberia.removeAll();
        int total = itemsYberia.size() + noReconocidosYberia.size();
        contadorYberia.setText(total + (total == 1 ? " producto" : " productos"));
        agregarRecordatoriosYberia.setVisible(!itemsYberia.isEmpty());

        if (total == 0) {
            JLabel vacio = EstilosYberia.etiqueta("<html><center>Aún no agregaste productos.<br>"
                    + "Búscalos arriba para empezar.</center></html>", null, EstilosYberia::textoSecundario);
            vacio.setHorizontalAlignment(SwingConstants.CENTER);
            JPanel contenedor = EstilosYberia.transparente(new BorderLayout());
            contenedor.setBorder(BorderFactory.createEmptyBorder(28, 0, 28, 0));
            contenedor.add(vacio, BorderLayout.CENTER);
            filasYberia.agregar(contenedor);
        }
        for (ItemCompraYberia item : itemsYberia) {
            filasYberia.agregar(crearFila(item));
        }
        for (String nombre : noReconocidosYberia) {
            filasYberia.agregar(crearFilaNoReconocida(nombre));
        }
        filasYberia.revalidate();
        filasYberia.repaint();
    }

    private JComponent crearFila(ItemCompraYberia item) {
        MedicamentoYberia m = item.medicamentoYberia();
        JButton menos = EstilosYberia.botonIcono(IconosYberia.menos(12), "Una unidad menos al día");
        menos.setEnabled(item.unidadesDiaYberia() > 1);
        menos.addActionListener(e -> cambiarUnidades(m, -1));
        JButton mas = EstilosYberia.botonIcono(IconosYberia.mas(12, EstilosYberia::textoPrincipal), "Una unidad más al día");
        mas.addActionListener(e -> cambiarUnidades(m, 1));
        JLabel unidades = EstilosYberia.etiqueta(item.unidadesDiaYberia() + " / día", "small");
        unidades.setHorizontalAlignment(SwingConstants.CENTER);
        unidades.setPreferredSize(new Dimension(48, unidades.getPreferredSize().height));
        unidades.setToolTipText("Unidades (tabletas, sobres...) que tomarías al día");

        JPanel contadorYberia = new CapsulaYberia();
        contadorYberia.add(menos);
        contadorYberia.add(unidades);
        contadorYberia.add(mas);

        JButton quitar = EstilosYberia.botonIcono(IconosYberia.eliminar(16), "Quitar de la compra");
        quitar.addActionListener(e -> quitar(m));

        return fila(IconosYberia.avatar(m.getNombre(), 36), m.getNombre(),
                m.getPrincipioActivo() + " · " + m.getConcentracion(), contadorYberia, quitar);
    }

    private JComponent crearFilaNoReconocida(String nombre) {
        JButton quitar = EstilosYberia.botonIcono(IconosYberia.eliminar(16), "Quitar de la compra");
        quitar.addActionListener(e -> {
            noReconocidosYberia.remove(nombre);
            refrescar();
        });
        return fila(IconosYberia.nivel(NivelRiesgoYberia.PRECAUCION, 30), nombre, "No está en el catálogo: no se puede evaluar", quitar);
    }

    private static JComponent fila(Icon icono, String titulo, String subtitulo, JComponent... acciones) {
        JPanel fila = new JPanel(new BorderLayout(12, 0)) {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }

            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(EstilosYberia.lineaSuave());
                g.fillRect(0, getHeight() - 1, getWidth(), 1);
            }
        };
        fila.setOpaque(false);
        fila.setBorder(BorderFactory.createEmptyBorder(10, 2, 10, 2));
        fila.add(new JLabel(icono), BorderLayout.WEST);

        JPanel textos = EstilosYberia.transparente(new GridLayout(0, 1, 0, 1));
        textos.add(EstilosYberia.etiqueta(titulo, "h4"));
        textos.add(EstilosYberia.etiqueta(subtitulo, "small", EstilosYberia::textoSecundario));
        fila.add(textos, BorderLayout.CENTER);

        JPanel panelAcciones = EstilosYberia.transparente(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 4, 0, 0);
        for (JComponent accion : acciones) {
            panelAcciones.add(accion, c);
        }
        fila.add(panelAcciones, BorderLayout.EAST);
        return fila;
    }

    private void refrescarResultados() {
        resultadosYberia.removeAll();
        if (itemsYberia.isEmpty() && noReconocidosYberia.isEmpty()) {
            mostrarBienvenida();
        } else {
            mostrarResultado(compatibilidadYberiaService.analizar(List.copyOf(itemsYberia), List.copyOf(noReconocidosYberia)));
        }
        resultadosYberia.revalidate();
        resultadosYberia.repaint();
        SwingUtilities.invokeLater(() -> scrollResultadosYberia.getVerticalScrollBar().setValue(0));
    }

    private void mostrarBienvenida() {
        TarjetaYberia tarjeta = new TarjetaYberia(new BorderLayout(), EstilosYberia::superficie, 24);
        PanelVerticalYberia columna = new PanelVerticalYberia();
        columna.agregar(EstilosYberia.etiqueta("¿Te ofrecen varios medicamentos?", "h2"));
        columna.espacio(6);
        columna.agregar(EstilosYberia.parrafo("Agrégalos a la izquierda. FarmaGuard revisará al instante:", EstilosYberia::textoSecundario));
        columna.espacio(12);
        for (String punto : List.of(
                "Si dos productos tienen el mismo componente con distinto nombre (Paracetamol y Panadol).",
                "Si hay interacciones peligrosas entre ellos.",
                "Si la dosis diaria total supera lo recomendado.",
                "Si te ofrecen productos con receta, muy concentrados o innecesarios.")) {
            columna.agregar(filaIcono(IconosYberia.check(20), EstilosYberia.parrafo(punto, EstilosYberia::textoPrincipal)));
            columna.espacio(8);
        }
        columna.espacio(12);
        columna.agregar(EstilosYberia.etiqueta("Cómo leer el resultado", "h4"));
        columna.espacio(8);
        leyenda(columna, NivelRiesgoYberia.SEGURO, "Seguro", "No encontramos problemas.");
        leyenda(columna, NivelRiesgoYberia.PRECAUCION, "Precaución", "Consulta antes de comprar o tomar.");
        leyenda(columna, NivelRiesgoYberia.PELIGRO, "Incompatible o redundante",
                "No deberían tomarse juntos o estás pagando dos veces por lo mismo.");
        tarjeta.add(columna, BorderLayout.CENTER);
        resultadosYberia.agregar(tarjeta);
    }

    private static void leyenda(PanelVerticalYberia columna, NivelRiesgoYberia nivel, String titulo, String descripcion) {
        PanelVerticalYberia textos = new PanelVerticalYberia();
        textos.agregar(EstilosYberia.etiqueta(titulo, "h4", () -> EstilosYberia.textoNivel(nivel)));
        textos.agregar(EstilosYberia.parrafo(descripcion, EstilosYberia::textoSecundario));
        columna.agregar(filaIcono(IconosYberia.nivel(nivel, 26), textos));
        columna.espacio(10);
    }

    private static JComponent filaIcono(Icon icono, JComponent contenido) {
        JPanel fila = EstilosYberia.transparente(new BorderLayout(12, 0));
        JPanel columnaIcono = EstilosYberia.transparente(new BorderLayout());
        columnaIcono.add(new JLabel(icono), BorderLayout.NORTH);
        fila.add(columnaIcono, BorderLayout.WEST);
        fila.add(contenido, BorderLayout.CENTER);
        return fila;
    }

    private void mostrarResultado(ResultadoAnalisisYberia resultado) {
        TarjetaAlertaYberia veredicto = veredicto(resultado.nivelGeneralYberia());
        int alertas = resultado.alertasYberia().size();
        veredicto.agregarPie(EstilosYberia.etiqueta(resultado.itemsYberia().size() + " productos analizados · "
                + alertas + (alertas == 1 ? " alerta" : " alertas"), "small", EstilosYberia::textoSecundario));
        resultadosYberia.agregar(veredicto);

        if (alertas > 0) {
            resultadosYberia.agregar(seccion("Alertas encontradas"));
            for (AlertaYberia alerta : resultado.alertasYberia()) {
                resultadosYberia.agregar(new TarjetaAlertaYberia(alerta.nivelYberia(), alerta.tituloYberia(), alerta.detalleYberia(), false));
            }
        }
        if (!resultado.itemsYberia().isEmpty()) {
            resultadosYberia.agregar(seccion("¿Para qué sirve cada uno?"));
            for (ItemCompraYberia item : resultado.itemsYberia()) {
                resultadosYberia.agregar(new FichaMedicamentoYberia(item.medicamentoYberia(), false));
            }
        }
        resultadosYberia.espacio(10);
        JTextArea aviso = EstilosYberia.parrafo(EstilosYberia.AVISO_LEGAL_YBERIA, EstilosYberia::textoSecundario);
        aviso.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        resultadosYberia.agregar(aviso);
    }

    private static TarjetaAlertaYberia veredicto(NivelRiesgoYberia nivel) {
        return switch (nivel) {
            case SEGURO -> new TarjetaAlertaYberia(nivel, "Compra sin alertas",
                    "No encontramos interacciones, duplicidades ni dosis excesivas. "
                            + "Igual sigue las indicaciones del empaque o de tu médico.", true);
            case PRECAUCION -> new TarjetaAlertaYberia(nivel, "Revisa antes de comprar",
                    "Hay puntos que conviene consultar con el químico farmacéutico o con tu médico.", true);
            case PELIGRO -> new TarjetaAlertaYberia(nivel, "No compres esta combinación así",
                    "Hay medicamentos que no deberían tomarse juntos, productos que repiten el mismo "
                            + "componente o una dosis diaria excesiva.", true);
        };
    }

    private static JComponent seccion(String titulo) {
        JLabel etiqueta = EstilosYberia.etiqueta(titulo, "h4", EstilosYberia::textoSecundario);
        etiqueta.setBorder(BorderFactory.createEmptyBorder(16, 8, 4, 8));
        return etiqueta;
    }

    private static final class CapsulaYberia extends JPanel {
        CapsulaYberia() {
            super(new GridBagLayout());
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(1, 4, 1, 4));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(EstilosYberia.superficieSuave());
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
            g2.dispose();
        }
    }
}
