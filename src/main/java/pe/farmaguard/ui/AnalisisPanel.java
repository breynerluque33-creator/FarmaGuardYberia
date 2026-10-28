package pe.farmaguard.ui;

import com.formdev.flatlaf.FlatClientProperties;
import pe.farmaguard.model.MedicamentoYberia;
import pe.farmaguard.model.NivelRiesgo;
import pe.farmaguard.model.RecordatorioYberia;
import pe.farmaguard.service.Alerta;
import pe.farmaguard.service.CompatibilidadYberiaService;
import pe.farmaguard.service.InterpretacionLista;
import pe.farmaguard.service.ItemCompra;
import pe.farmaguard.service.MedicamentoYberiaService;
import pe.farmaguard.service.RecordatorioYberiaService;
import pe.farmaguard.service.ResultadoAnalisis;

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

public class AnalisisPanel extends JPanel {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("h:mm a", Locale.US);

    private final MedicamentoYberiaService medicamentoYberiaService;
    private final CompatibilidadYberiaService compatibilidadYberiaService;
    private final RecordatorioYberiaService recordatorioYberiaService;
    private final Runnable irARecordatorios;

    private final List<ItemCompra> items = new ArrayList<>();
    private final List<String> noReconocidos = new ArrayList<>();

    private final CampoBusqueda buscador;
    private final JLabel mensajeBuscador = Estilos.etiqueta(" ", "small", () -> Estilos.textoNivel(NivelRiesgo.PELIGRO));
    private final JTextArea lista = new JTextArea(4, 20);
    private final JPanel panelLista = Estilos.transparente(new BorderLayout(0, 8));
    private final PanelVertical filas = new PanelVertical();
    private final Insignia contador = new Insignia("0 productos", Estilos::superficieSuave, Estilos::textoSecundario);
    private final BotonPrimario agregarRecordatorios = new BotonPrimario("Agregar lista a Recordatorios",
            Iconos.mas(14, () -> Color.WHITE));
    private final PanelVertical resultados = new PanelVertical();
    private final JScrollPane scrollResultados = Estilos.scroll(resultados);

    public AnalisisPanel(MedicamentoYberiaService medicamentoYberiaService, CompatibilidadYberiaService compatibilidadYberiaService,
                         RecordatorioYberiaService recordatorioYberiaService, Runnable irARecordatorios) {
        super(new GridBagLayout());
        this.medicamentoYberiaService = medicamentoYberiaService;
        this.compatibilidadYberiaService = compatibilidadYberiaService;
        this.recordatorioYberiaService = recordatorioYberiaService;
        this.irARecordatorios = irARecordatorios;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(4, 20, 16, 20));

        buscador = new CampoBusqueda(medicamentoYberiaService.listarTodos(), "Busca: Panadol, Apronax, amoxicilina...");
        buscador.alElegir(m -> {
            agregar(m);
            buscador.setTextoSinSugerir("");
        });
        buscador.addActionListener(e -> agregarDesdeTexto());

        buscador.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                mensajeBuscador.setText(" ");
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                mensajeBuscador.setText(" ");
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });

        JPanel izquierda = Estilos.transparente(new BorderLayout(0, 6));
        izquierda.add(crearTarjetaBuscador(), BorderLayout.NORTH);
        izquierda.add(crearTarjetaCompra(), BorderLayout.CENTER);
        izquierda.setPreferredSize(new Dimension(430, 300));
        izquierda.setMinimumSize(new Dimension(380, 300));

        JPanel derecha = Estilos.transparente(new BorderLayout(0, 4));
        derecha.add(encabezado("Resultado del análisis", "Se actualiza al instante al agregar o quitar productos."),
                BorderLayout.NORTH);
        scrollResultados.setPreferredSize(new Dimension(300, 300));
        derecha.add(scrollResultados, BorderLayout.CENTER);
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
        buscador.requestFocusInWindow();
    }

    private JComponent crearTarjetaBuscador() {
        Tarjeta tarjeta = new Tarjeta(new BorderLayout(0, 12));
        tarjeta.add(encabezadoTarjeta("¿Qué te están ofreciendo?",
                "Escribe el nombre comercial o genérico y elige de la lista."), BorderLayout.NORTH);

        BotonPrimario agregar = new BotonPrimario("Agregar", Iconos.mas(14, () -> Color.WHITE));
        agregar.addActionListener(e -> agregarDesdeTexto());
        JPanel filaBuscador = Estilos.transparente(new BorderLayout(10, 0));
        filaBuscador.add(buscador, BorderLayout.CENTER);
        filaBuscador.add(agregar, BorderLayout.EAST);

        lista.setLineWrap(true);
        lista.setWrapStyleWord(true);
        lista.setMargin(new Insets(8, 10, 8, 10));
        lista.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                "Un producto por línea o separados por comas.\nEj.: Panadol x2, Apronax 550, amoxicilina");
        JButton agregarLista = Estilos.botonSecundario("Agregar todos");
        agregarLista.addActionListener(e -> agregarLista());
        JPanel filaAgregarLista = Estilos.transparente(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        filaAgregarLista.add(agregarLista);
        panelLista.add(new JScrollPane(lista), BorderLayout.CENTER);
        panelLista.add(filaAgregarLista, BorderLayout.SOUTH);
        panelLista.setVisible(false);

        JButton alternarLista = Estilos.botonEnlace("Pegar una lista o receta", Iconos.mas(12, Estilos::acento));
        alternarLista.addActionListener(e -> {
            panelLista.setVisible(!panelLista.isVisible());
            alternarLista.setText(panelLista.isVisible() ? "Ocultar lista" : "Pegar una lista o receta");
            if (panelLista.isVisible()) {
                lista.requestFocusInWindow();
            }
            revalidate();
        });

        PanelVertical cuerpo = new PanelVertical();
        cuerpo.agregar(filaBuscador);
        cuerpo.espacio(2);
        cuerpo.agregar(mensajeBuscador);
        cuerpo.agregar(alternarLista);
        cuerpo.agregar(panelLista);
        tarjeta.add(cuerpo, BorderLayout.CENTER);
        return tarjeta;
    }

    private JComponent crearTarjetaCompra() {
        Tarjeta tarjeta = new Tarjeta(new BorderLayout(0, 8));
        JPanel cabecera = Estilos.transparente(new BorderLayout());
        cabecera.add(Estilos.etiqueta("Tu compra", "h3"), BorderLayout.WEST);
        JPanel contenedorContador = Estilos.transparente(new GridBagLayout());
        contenedorContador.add(contador);
        cabecera.add(contenedorContador, BorderLayout.EAST);
        tarjeta.add(cabecera, BorderLayout.NORTH);

        tarjeta.add(Estilos.scroll(filas), BorderLayout.CENTER);

        JButton vaciar = Estilos.botonEnlace("Vaciar lista", Iconos.eliminar(14));
        vaciar.addActionListener(e -> {
            items.clear();
            noReconocidos.clear();
            refrescar();
        });
        agregarRecordatorios.setToolTipText("Crea un recordatorio por producto con la frecuencia, duración y "
                + "momento de comida del catálogo. Luego puedes editarlos.");
        agregarRecordatorios.addActionListener(e -> agregarARecordatorios(
                compatibilidadYberiaService.analizar(List.copyOf(items), List.copyOf(noReconocidos))));
        JPanel contenedorRecordatorios = Estilos.transparente(new GridBagLayout());
        contenedorRecordatorios.add(agregarRecordatorios);
        JPanel pie = Estilos.transparente(new BorderLayout(8, 0));
        pie.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        pie.add(vaciar, BorderLayout.WEST);
        pie.add(contenedorRecordatorios, BorderLayout.EAST);
        tarjeta.add(pie, BorderLayout.SOUTH);
        return tarjeta;
    }

    private static JComponent encabezado(String titulo, String subtitulo) {
        JPanel panel = Estilos.transparente(new GridLayout(0, 1, 0, 2));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 4, 6));
        panel.add(Estilos.etiqueta(titulo, "h2"));
        panel.add(Estilos.etiqueta(subtitulo, null, Estilos::textoSecundario));
        return panel;
    }

    private static JComponent encabezadoTarjeta(String titulo, String subtitulo) {
        JPanel panel = Estilos.transparente(new GridLayout(0, 1, 0, 2));
        panel.add(Estilos.etiqueta(titulo, "h3"));
        panel.add(Estilos.etiqueta(subtitulo, "small", Estilos::textoSecundario));
        return panel;
    }

    private void agregarDesdeTexto() {
        String texto = buscador.getText().trim();
        if (texto.isEmpty()) {
            buscador.requestFocusInWindow();
            return;
        }
        medicamentoYberiaService.buscar(texto).ifPresentOrElse(m -> {
            agregar(m);
            buscador.setTextoSinSugerir("");
        }, () -> mensajeBuscador.setText("No encontramos \"" + texto + "\". Revisa el nombre o prueba con el componente."));
    }

    private void agregarLista() {
        if (lista.getText().isBlank()) {
            return;
        }
        InterpretacionLista interpretacion = medicamentoYberiaService.interpretarLista(lista.getText());
        interpretacion.encontrados().stream()
                .filter(m -> items.stream().noneMatch(i -> i.medicamento().equals(m)))
                .forEach(m -> items.add(ItemCompra.conDosisHabitual(m)));
        interpretacion.noReconocidos().stream()
                .filter(n -> !noReconocidos.contains(n))
                .forEach(noReconocidos::add);
        lista.setText("");
        refrescar();
    }

    private void agregar(MedicamentoYberia medicamentoYberia) {
        if (items.stream().anyMatch(i -> i.medicamento().equals(medicamentoYberia))) {
            Toolkit.getDefaultToolkit().beep();
            mensajeBuscador.setText(medicamentoYberia.getNombre() + " ya está en tu compra.");
            return;
        }
        items.add(ItemCompra.conDosisHabitual(medicamentoYberia));
        refrescar();
    }

    private void agregarARecordatorios(ResultadoAnalisis resultado) {
        if (resultado.nivelGeneral() == NivelRiesgo.PELIGRO) {
            int respuesta = JOptionPane.showConfirmDialog(this,
                    "El análisis encontró alertas importantes en esta lista.\n"
                            + "Revisa las alertas o consulta antes de tomarlos juntos.\n\n"
                            + "¿Crear los recordatorios de todos modos?",
                    "Agregar lista a Recordatorios", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (respuesta != JOptionPane.YES_OPTION) {
                return;
            }
        }
        RecordatorioYberiaService.ResultadoAutomatico creados =
                recordatorioYberiaService.agregarAutomaticos(List.copyOf(items), LocalDateTime.now());

        StringBuilder mensaje = new StringBuilder();
        if (creados.creados().isEmpty()) {
            mensaje.append("No se creó ningún recordatorio nuevo.\n");
        } else {
            mensaje.append(creados.creados().size() == 1 ? "Se creó 1 recordatorio:\n"
                    : "Se crearon " + creados.creados().size() + " recordatorios:\n");
            for (RecordatorioYberia r : creados.creados()) {
                mensaje.append("  • ").append(r.getMedicamento()).append(": ")
                        .append(r.isSemanal() ? "1 vez por semana"
                                : r.getFrecuenciaHoras() == 24 ? "1 vez al día" : "cada " + r.getFrecuenciaHoras() + " h")
                        .append(", primera toma ").append(primeraToma(r)).append('\n');
            }
        }
        if (!creados.omitidos().isEmpty()) {
            mensaje.append("\nYa tenían un recordatorio activo: ").append(String.join(", ", creados.omitidos()))
                    .append('\n');
        }
        if (!noReconocidos.isEmpty()) {
            mensaje.append("\nNo están en el catálogo (agrégalos a mano): ").append(String.join(", ", noReconocidos))
                    .append('\n');
        }
        mensaje.append("\nPuedes editarlos cuando quieras en la pestaña Recordatorios.");

        Object[] opciones = {"Ver recordatorios", "Seguir aquí"};
        int eleccion = JOptionPane.showOptionDialog(this, mensaje.toString(), "Agregar lista a Recordatorios",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, opciones, opciones[0]);
        if (eleccion == 0) {
            irARecordatorios.run();
        }
    }

    private static String primeraToma(RecordatorioYberia r) {
        LocalDate fecha = LocalDate.parse(r.getFechaInicio());
        String hora = LocalTime.parse(r.getHoraInicio()).format(HORA);
        return (fecha.equals(LocalDate.now()) ? "hoy" : fecha.equals(LocalDate.now().plusDays(1)) ? "mañana"
                : fecha.toString()) + " a las " + hora + " (" + r.getMomentoComida().getEtiqueta().toLowerCase() + ")";
    }

    private void cambiarUnidades(MedicamentoYberia medicamentoYberia, int delta) {
        for (int i = 0; i < items.size(); i++) {
            ItemCompra item = items.get(i);
            if (item.medicamento().equals(medicamentoYberia)) {
                int unidades = Math.max(1, Math.min(24, item.unidadesDia() + delta));
                items.set(i, new ItemCompra(medicamentoYberia, unidades));
            }
        }
        refrescar();
    }

    private void quitar(MedicamentoYberia medicamentoYberia) {
        items.removeIf(i -> i.medicamento().equals(medicamentoYberia));
        refrescar();
    }

    private void refrescar() {
        refrescarFilas();
        refrescarResultados();
    }

    private void refrescarFilas() {
        filas.removeAll();
        int total = items.size() + noReconocidos.size();
        contador.setText(total + (total == 1 ? " producto" : " productos"));
        agregarRecordatorios.setVisible(!items.isEmpty());

        if (total == 0) {
            JLabel vacio = Estilos.etiqueta("<html><center>Aún no agregaste productos.<br>"
                    + "Búscalos arriba para empezar.</center></html>", null, Estilos::textoSecundario);
            vacio.setHorizontalAlignment(SwingConstants.CENTER);
            JPanel contenedor = Estilos.transparente(new BorderLayout());
            contenedor.setBorder(BorderFactory.createEmptyBorder(28, 0, 28, 0));
            contenedor.add(vacio, BorderLayout.CENTER);
            filas.agregar(contenedor);
        }
        for (ItemCompra item : items) {
            filas.agregar(crearFila(item));
        }
        for (String nombre : noReconocidos) {
            filas.agregar(crearFilaNoReconocida(nombre));
        }
        filas.revalidate();
        filas.repaint();
    }

    private JComponent crearFila(ItemCompra item) {
        MedicamentoYberia m = item.medicamento();
        JButton menos = Estilos.botonIcono(Iconos.menos(12), "Una unidad menos al día");
        menos.setEnabled(item.unidadesDia() > 1);
        menos.addActionListener(e -> cambiarUnidades(m, -1));
        JButton mas = Estilos.botonIcono(Iconos.mas(12, Estilos::textoPrincipal), "Una unidad más al día");
        mas.addActionListener(e -> cambiarUnidades(m, 1));
        JLabel unidades = Estilos.etiqueta(item.unidadesDia() + " / día", "small");
        unidades.setHorizontalAlignment(SwingConstants.CENTER);
        unidades.setPreferredSize(new Dimension(48, unidades.getPreferredSize().height));
        unidades.setToolTipText("Unidades (tabletas, sobres...) que tomarías al día");

        JPanel contador = new Capsula();
        contador.add(menos);
        contador.add(unidades);
        contador.add(mas);

        JButton quitar = Estilos.botonIcono(Iconos.eliminar(16), "Quitar de la compra");
        quitar.addActionListener(e -> quitar(m));

        return fila(Iconos.avatar(m.getNombre(), 36), m.getNombre(),
                m.getPrincipioActivo() + " · " + m.getConcentracion(), contador, quitar);
    }

    private JComponent crearFilaNoReconocida(String nombre) {
        JButton quitar = Estilos.botonIcono(Iconos.eliminar(16), "Quitar de la compra");
        quitar.addActionListener(e -> {
            noReconocidos.remove(nombre);
            refrescar();
        });
        return fila(Iconos.nivel(NivelRiesgo.PRECAUCION, 30), nombre, "No está en el catálogo: no se puede evaluar", quitar);
    }

    private static JComponent fila(Icon icono, String titulo, String subtitulo, JComponent... acciones) {
        JPanel fila = new JPanel(new BorderLayout(12, 0)) {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }

            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(Estilos.lineaSuave());
                g.fillRect(0, getHeight() - 1, getWidth(), 1);
            }
        };
        fila.setOpaque(false);
        fila.setBorder(BorderFactory.createEmptyBorder(10, 2, 10, 2));
        fila.add(new JLabel(icono), BorderLayout.WEST);

        JPanel textos = Estilos.transparente(new GridLayout(0, 1, 0, 1));
        textos.add(Estilos.etiqueta(titulo, "h4"));
        textos.add(Estilos.etiqueta(subtitulo, "small", Estilos::textoSecundario));
        fila.add(textos, BorderLayout.CENTER);

        JPanel panelAcciones = Estilos.transparente(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 4, 0, 0);
        for (JComponent accion : acciones) {
            panelAcciones.add(accion, c);
        }
        fila.add(panelAcciones, BorderLayout.EAST);
        return fila;
    }

    private void refrescarResultados() {
        resultados.removeAll();
        if (items.isEmpty() && noReconocidos.isEmpty()) {
            mostrarBienvenida();
        } else {
            mostrarResultado(compatibilidadYberiaService.analizar(List.copyOf(items), List.copyOf(noReconocidos)));
        }
        resultados.revalidate();
        resultados.repaint();
        SwingUtilities.invokeLater(() -> scrollResultados.getVerticalScrollBar().setValue(0));
    }

    private void mostrarBienvenida() {
        Tarjeta tarjeta = new Tarjeta(new BorderLayout(), Estilos::superficie, 24);
        PanelVertical columna = new PanelVertical();
        columna.agregar(Estilos.etiqueta("¿Te ofrecen varios medicamentos?", "h2"));
        columna.espacio(6);
        columna.agregar(Estilos.parrafo("Agrégalos a la izquierda. FarmaGuard revisará al instante:", Estilos::textoSecundario));
        columna.espacio(12);
        for (String punto : List.of(
                "Si dos productos tienen el mismo componente con distinto nombre (Paracetamol y Panadol).",
                "Si hay interacciones peligrosas entre ellos.",
                "Si la dosis diaria total supera lo recomendado.",
                "Si te ofrecen productos con receta, muy concentrados o innecesarios.")) {
            columna.agregar(filaIcono(Iconos.check(20), Estilos.parrafo(punto, Estilos::textoPrincipal)));
            columna.espacio(8);
        }
        columna.espacio(12);
        columna.agregar(Estilos.etiqueta("Cómo leer el resultado", "h4"));
        columna.espacio(8);
        leyenda(columna, NivelRiesgo.SEGURO, "Seguro", "No encontramos problemas.");
        leyenda(columna, NivelRiesgo.PRECAUCION, "Precaución", "Consulta antes de comprar o tomar.");
        leyenda(columna, NivelRiesgo.PELIGRO, "Incompatible o redundante",
                "No deberían tomarse juntos o estás pagando dos veces por lo mismo.");
        tarjeta.add(columna, BorderLayout.CENTER);
        resultados.agregar(tarjeta);
    }

    private static void leyenda(PanelVertical columna, NivelRiesgo nivel, String titulo, String descripcion) {
        PanelVertical textos = new PanelVertical();
        textos.agregar(Estilos.etiqueta(titulo, "h4", () -> Estilos.textoNivel(nivel)));
        textos.agregar(Estilos.parrafo(descripcion, Estilos::textoSecundario));
        columna.agregar(filaIcono(Iconos.nivel(nivel, 26), textos));
        columna.espacio(10);
    }

    private static JComponent filaIcono(Icon icono, JComponent contenido) {
        JPanel fila = Estilos.transparente(new BorderLayout(12, 0));
        JPanel columnaIcono = Estilos.transparente(new BorderLayout());
        columnaIcono.add(new JLabel(icono), BorderLayout.NORTH);
        fila.add(columnaIcono, BorderLayout.WEST);
        fila.add(contenido, BorderLayout.CENTER);
        return fila;
    }

    private void mostrarResultado(ResultadoAnalisis resultado) {
        TarjetaAlerta veredicto = veredicto(resultado.nivelGeneral());
        int alertas = resultado.alertas().size();
        veredicto.agregarPie(Estilos.etiqueta(resultado.items().size() + " productos analizados · "
                + alertas + (alertas == 1 ? " alerta" : " alertas"), "small", Estilos::textoSecundario));
        resultados.agregar(veredicto);

        if (alertas > 0) {
            resultados.agregar(seccion("Alertas encontradas"));
            for (Alerta alerta : resultado.alertas()) {
                resultados.agregar(new TarjetaAlerta(alerta.nivel(), alerta.titulo(), alerta.detalle(), false));
            }
        }
        if (!resultado.items().isEmpty()) {
            resultados.agregar(seccion("¿Para qué sirve cada uno?"));
            for (ItemCompra item : resultado.items()) {
                resultados.agregar(new FichaMedicamento(item.medicamento(), false));
            }
        }
        resultados.espacio(10);
        JTextArea aviso = Estilos.parrafo(Estilos.AVISO_LEGAL, Estilos::textoSecundario);
        aviso.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        resultados.agregar(aviso);
    }

    private static TarjetaAlerta veredicto(NivelRiesgo nivel) {
        return switch (nivel) {
            case SEGURO -> new TarjetaAlerta(nivel, "Compra sin alertas",
                    "No encontramos interacciones, duplicidades ni dosis excesivas. "
                            + "Igual sigue las indicaciones del empaque o de tu médico.", true);
            case PRECAUCION -> new TarjetaAlerta(nivel, "Revisa antes de comprar",
                    "Hay puntos que conviene consultar con el químico farmacéutico o con tu médico.", true);
            case PELIGRO -> new TarjetaAlerta(nivel, "No compres esta combinación así",
                    "Hay medicamentos que no deberían tomarse juntos, productos que repiten el mismo "
                            + "componente o una dosis diaria excesiva.", true);
        };
    }

    private static JComponent seccion(String titulo) {
        JLabel etiqueta = Estilos.etiqueta(titulo, "h4", Estilos::textoSecundario);
        etiqueta.setBorder(BorderFactory.createEmptyBorder(16, 8, 4, 8));
        return etiqueta;
    }

    private static final class Capsula extends JPanel {

        Capsula() {
            super(new GridBagLayout());
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(1, 4, 1, 4));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Estilos.superficieSuave());
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
            g2.dispose();
        }
    }
}
