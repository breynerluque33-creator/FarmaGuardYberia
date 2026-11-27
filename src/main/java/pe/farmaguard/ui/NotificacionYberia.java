package pe.farmaguard.ui;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;

final class NotificacionYberia {
    private static final int DURACION_MS_YBERIA = 120_000;

    private NotificacionYberia() {
    }

    static void mostrar(Window duenio, String encabezado, String titulo, String detalle) {
        Toolkit.getDefaultToolkit().beep();

        JDialog dialogo = new JDialog(duenio);
        dialogo.setUndecorated(true);
        dialogo.setAlwaysOnTop(true);
        dialogo.setFocusableWindowState(false);
        GraphicsEnvironment entorno = GraphicsEnvironment.getLocalGraphicsEnvironment();
        if (entorno.getDefaultScreenDevice()
                .isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT)) {
            dialogo.setBackground(new Color(0, 0, 0, 0));
        }

        TarjetaYberia tarjeta = new TarjetaYberia(new BorderLayout(14, 0));
        JPanel columnaIcono = EstilosYberia.transparente(new BorderLayout());
        columnaIcono.add(new JLabel(IconosYberia.reloj(30)), BorderLayout.NORTH);
        tarjeta.add(columnaIcono, BorderLayout.WEST);

        PanelVerticalYberia columna = new PanelVerticalYberia();
        columna.agregar(EstilosYberia.etiqueta(encabezado, "small", EstilosYberia::textoSecundario));
        columna.espacio(2);
        columna.agregar(EstilosYberia.etiqueta(titulo, "h3"));
        columna.espacio(2);
        columna.agregar(EstilosYberia.parrafo(detalle, EstilosYberia::textoPrincipal));
        columna.espacio(12);
        BotonPrimarioYberia entendido = new BotonPrimarioYberia("Entendido", null);
        entendido.addActionListener(e -> dialogo.dispose());
        JPanel filaBoton = EstilosYberia.transparente(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        filaBoton.add(entendido);
        columna.agregar(filaBoton);
        tarjeta.add(columna, BorderLayout.CENTER);

        JPanel raiz = EstilosYberia.transparente(new BorderLayout());
        raiz.add(tarjeta);
        dialogo.setContentPane(raiz);
        dialogo.pack();
        int ancho = 380;
        for (int i = 0; i < 2; i++) {
            dialogo.setSize(new Dimension(ancho, Math.max(dialogo.getHeight(), 150)));
            dialogo.validate();
            dialogo.setSize(new Dimension(ancho, Math.max(raiz.getPreferredSize().height, 150)));
            dialogo.validate();
        }

        Rectangle area = entorno.getMaximumWindowBounds();
        dialogo.setLocation(area.x + area.width - dialogo.getWidth() - 16,
                area.y + area.height - dialogo.getHeight() - 16);
        dialogo.setVisible(true);

        Timer cierre = new Timer(DURACION_MS_YBERIA, e -> dialogo.dispose());
        cierre.setRepeats(false);
        cierre.start();
    }
}
