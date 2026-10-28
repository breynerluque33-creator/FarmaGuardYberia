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

    private static final String ANALISIS = "Analizar compra";
    private static final String CATALOGO = "Medicamentos";
    private static final String RECORDATORIOS = "Recordatorios";

    public MainFrameYberia(MedicamentoYberiaService medicamentoYberiaService,
                     CompatibilidadYberiaService compatibilidadYberiaService,
                     RecordatorioYberiaService recordatorioYberiaService) {
        super("FarmaGuard");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setIconImages(iconosVentana());

        CardLayout paginas = new CardLayout();
        JPanel contenido = Estilos.transparente(paginas);
        Segmentado<String> navegacion = new Segmentado<>(List.of(ANALISIS, CATALOGO, RECORDATORIOS), s -> s,
                new Insets(7, 20, 7, 20));
        navegacion.alCambiar(pagina -> paginas.show(contenido, pagina));
        navegacion.setSeleccion(ANALISIS);
        Runnable irARecordatorios = () -> {
            navegacion.setSeleccion(RECORDATORIOS);
            paginas.show(contenido, RECORDATORIOS);
        };

        AnalisisPanel analisis = new AnalisisPanel(medicamentoYberiaService, compatibilidadYberiaService,
                recordatorioYberiaService, irARecordatorios);
        contenido.add(analisis, ANALISIS);
        contenido.add(new CatalogoPanel(medicamentoYberiaService, compatibilidadYberiaService), CATALOGO);
        contenido.add(new RecordatoriosPanel(recordatorioYberiaService, medicamentoYberiaService), RECORDATORIOS);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(crearCabecera(navegacion), BorderLayout.NORTH);
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
            }
        });
    }

    private static JPanel crearCabecera(Segmentado<String> navegacion) {
        JLabel logo = new JLabel(Iconos.logo(38));
        JPanel textos = Estilos.transparente(new GridLayout(0, 1, 0, 0));
        textos.add(Estilos.etiqueta("FarmaGuard", "h2"));
        textos.add(Estilos.etiqueta("Compras seguras en farmacia", "small", Estilos::textoSecundario));
        JPanel marca = Estilos.transparente(new BorderLayout(10, 0));
        marca.add(logo, BorderLayout.WEST);
        marca.add(textos, BorderLayout.CENTER);

        JPanel centroNavegacion = Estilos.transparente(new GridBagLayout());
        centroNavegacion.add(navegacion);

        JButton tema = Estilos.botonIcono(Iconos.tema(20), "Cambiar entre tema claro y oscuro");
        tema.addActionListener(e -> Estilos.alternarTema());
        JPanel derecha = Estilos.transparente(new GridBagLayout());
        derecha.add(tema);
        JPanel contenedorDerecha = Estilos.transparente(new BorderLayout());
        contenedorDerecha.add(derecha, BorderLayout.EAST);
        contenedorDerecha.setPreferredSize(marca.getPreferredSize());

        JPanel cabecera = Estilos.transparente(new BorderLayout(16, 0));
        cabecera.setBorder(BorderFactory.createEmptyBorder(14, 26, 14, 26));
        cabecera.add(marca, BorderLayout.WEST);
        cabecera.add(centroNavegacion, BorderLayout.CENTER);
        cabecera.add(contenedorDerecha, BorderLayout.EAST);
        return cabecera;
    }

    private static JPanel crearPie() {
        JLabel aviso = Estilos.etiqueta(Estilos.AVISO_LEGAL, "small", Estilos::textoSecundario);
        aviso.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel pie = Estilos.transparente(new BorderLayout());
        pie.setBorder(BorderFactory.createEmptyBorder(0, 24, 10, 24));
        pie.add(aviso, BorderLayout.CENTER);
        return pie;
    }

    private static List<Image> iconosVentana() {
        return List.of(16, 24, 32, 48, 64, 128).stream()
                .map(tam -> {
                    BufferedImage imagen = new BufferedImage(tam, tam, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = imagen.createGraphics();
                    Iconos.logo(tam).paintIcon(null, g, 0, 0);
                    g.dispose();
                    return (Image) imagen;
                })
                .toList();
    }
}
