package pe.farmaguard.ui;

import pe.farmaguard.model.NivelRiesgo;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Font;

class TarjetaAlerta extends Tarjeta {

    private final PanelVertical texto = new PanelVertical();

    TarjetaAlerta(NivelRiesgo nivel, String titulo, String detalle, boolean grande) {
        super(new BorderLayout(grande ? 18 : 14, 0), () -> Estilos.fondoNivel(nivel), grande ? 22 : 16);
        setBorde(null);

        JPanel columnaIcono = Estilos.transparente(new BorderLayout());
        columnaIcono.add(new JLabel(Iconos.nivel(nivel, grande ? 44 : 26)), BorderLayout.NORTH);
        add(columnaIcono, BorderLayout.WEST);

        JTextArea encabezado = Estilos.parrafo(titulo, () -> Estilos.textoNivel(nivel));
        Font fuente = encabezado.getFont();
        encabezado.setFont(fuente.deriveFont(Font.BOLD, fuente.getSize2D() + (grande ? 7f : 1f)));
        texto.agregar(encabezado);
        texto.espacio(grande ? 6 : 4);
        texto.agregar(Estilos.parrafo(detalle, Estilos::textoPrincipal));
        add(texto, BorderLayout.CENTER);
    }

    void agregarPie(JComponent componente) {
        texto.espacio(10);
        texto.agregar(componente);
    }
}
