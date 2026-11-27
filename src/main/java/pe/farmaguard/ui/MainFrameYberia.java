package pe.farmaguard.ui;

import pe.farmaguard.service.CompatibilidadYberiaService;
import pe.farmaguard.service.MedicamentoYberiaService;
import pe.farmaguard.service.RecordatorioYberiaService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.List;

public class MainFrameYberia extends JFrame {
    private static final String ANALISIS_YBERIA = "Analizar compra";
    private static final String CATALOGO_YBERIA = "Medicamentos";
    private static final String RECORDATORIOS_YBERIA = "Recordatorios";

    public MainFrameYberia(MedicamentoYberiaService medicamentoYberiaService,
                     CompatibilidadYberiaService compatibilidadYberiaService,
                     RecordatorioYberiaService recordatorioYberiaService) {
        super("FarmaGuard");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setIconImages(iconosVentana());

        CardLayout paginas = new CardLayout();
        JPanel contenido = EstilosYberia.transparente(paginas);
        SegmentadoYberia<String> navegacion = new SegmentadoYberia<>(List.of(ANALISIS_YBERIA, CATALOGO_YBERIA, RECORDATORIOS_YBERIA), s -> s,
                new Insets(7, 20, 7, 20));
        navegacion.alCambiar(pagina -> paginas.show(contenido, pagina));
        navegacion.setSeleccion(ANALISIS_YBERIA);
        Runnable irARecordatorios = () -> {
            navegacion.setSeleccion(RECORDATORIOS_YBERIA);
            paginas.show(contenido, RECORDATORIOS_YBERIA);
        };

        AnalisisPanelYberia analisis = new AnalisisPanelYberia(medicamentoYberiaService, compatibilidadYberiaService,
                recordatorioYberiaService, irARecordatorios);
        contenido.add(analisis, ANALISIS_YBERIA);
        contenido.add(new CatalogoPanelYberia(medicamentoYberiaService, compatibilidadYberiaService), CATALOGO_YBERIA);
        contenido.add(new RecordatoriosPanelYberia(recordatorioYberiaService, medicamentoYberiaService), RECORDATORIOS_YBERIA);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(crearCabecera(navegacion, this), BorderLayout.NORTH);
        raiz.add(contenido, BorderLayout.CENTER);
        raiz.add(crearPie(), BorderLayout.SOUTH);
        setContentPane(raiz);

        setSize(1280, 840);
        setMinimumSize(new Dimension(1000, 680));
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                analisis.enfocarBuscador();
                SwingUtilities.invokeLater(() -> AyudaYberia.mostrarGuiaSiEsPrimeraVez(MainFrameYberia.this));
            }
        });
    }

    private static JPanel crearCabecera(SegmentadoYberia<String> navegacion, JFrame ventana) {
        JLabel logo = new JLabel(IconosYberia.logo(38));
        JPanel textos = EstilosYberia.transparente(new GridLayout(0, 1, 0, 0));
        textos.add(EstilosYberia.etiqueta("FarmaGuard", "h2"));
        textos.add(EstilosYberia.etiqueta("Compras seguras en farmacia", "small", EstilosYberia::textoSecundario));
        JPanel marca = EstilosYberia.transparente(new BorderLayout(10, 0));
        marca.add(logo, BorderLayout.WEST);
        marca.add(textos, BorderLayout.CENTER);

        JPanel centroNavegacion = EstilosYberia.transparente(new GridBagLayout());
        centroNavegacion.add(navegacion);

        JButton tema = EstilosYberia.botonIcono(IconosYberia.tema(20), "Cambiar entre tema claro y oscuro");
        tema.addActionListener(e -> EstilosYberia.alternarTema());
        JButton guia = EstilosYberia.botonIcono(IconosYberia.ayuda(20), "Guía de uso");
        guia.addActionListener(e -> AyudaYberia.mostrarGuia(ventana));
        JButton info = EstilosYberia.botonIcono(IconosYberia.info(20), "Aviso importante: FarmaGuard no reemplaza a un médico");
        info.addActionListener(e -> AyudaYberia.mostrarAviso(ventana));
        JPanel derecha = EstilosYberia.transparente(new GridBagLayout());
        derecha.add(guia);
        derecha.add(tema);
        derecha.add(info);
        JPanel contenedorDerecha = EstilosYberia.transparente(new BorderLayout());
        contenedorDerecha.add(derecha, BorderLayout.EAST);
        contenedorDerecha.setPreferredSize(marca.getPreferredSize());

        JPanel cabecera = EstilosYberia.transparente(new BorderLayout(16, 0));
        cabecera.setBorder(BorderFactory.createEmptyBorder(14, 26, 14, 26));
        cabecera.add(marca, BorderLayout.WEST);
        cabecera.add(centroNavegacion, BorderLayout.CENTER);
        cabecera.add(contenedorDerecha, BorderLayout.EAST);
        return cabecera;
    }

    private static JPanel crearPie() {
        JLabel aviso = EstilosYberia.etiqueta(EstilosYberia.AVISO_LEGAL_YBERIA, "small", EstilosYberia::textoSecundario);
        aviso.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel pie = EstilosYberia.transparente(new BorderLayout());
        pie.setBorder(BorderFactory.createEmptyBorder(0, 24, 10, 24));
        pie.add(aviso, BorderLayout.CENTER);
        return pie;
    }

    private static List<Image> iconosVentana() {
        return List.of(16, 24, 32, 48, 64, 128).stream()
                .map(tam -> {
                    BufferedImage imagen = new BufferedImage(tam, tam, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = imagen.createGraphics();
                    IconosYberia.logo(tam).paintIcon(null, g, 0, 0);
                    g.dispose();
                    return (Image) imagen;
                })
                .toList();
    }
}
