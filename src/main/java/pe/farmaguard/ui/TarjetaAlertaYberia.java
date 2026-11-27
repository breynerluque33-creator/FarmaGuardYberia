package pe.farmaguard.ui;

import pe.farmaguard.model.NivelRiesgoYberia;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Font;

class TarjetaAlertaYberia extends TarjetaYberia {
    private final PanelVerticalYberia textoYberia = new PanelVerticalYberia();

    TarjetaAlertaYberia(NivelRiesgoYberia nivel, String titulo, String detalle, boolean grande) {
        super(new BorderLayout(grande ? 18 : 14, 0), () -> EstilosYberia.fondoNivel(nivel), grande ? 22 : 16);
        setBorde(null);

        JPanel columnaIcono = EstilosYberia.transparente(new BorderLayout());
        columnaIcono.add(new JLabel(IconosYberia.nivel(nivel, grande ? 44 : 26)), BorderLayout.NORTH);
        add(columnaIcono, BorderLayout.WEST);

        JTextArea encabezado = EstilosYberia.parrafo(titulo, () -> EstilosYberia.textoNivel(nivel));
        Font fuente = encabezado.getFont();
        encabezado.setFont(fuente.deriveFont(Font.BOLD, fuente.getSize2D() + (grande ? 7f : 1f)));
        textoYberia.agregar(encabezado);
        textoYberia.espacio(grande ? 6 : 4);
        textoYberia.agregar(EstilosYberia.parrafo(detalle, EstilosYberia::textoPrincipal));
        add(textoYberia, BorderLayout.CENTER);
    }

    void agregarPie(JComponent componente) {
        textoYberia.espacio(10);
        textoYberia.agregar(componente);
    }
}
