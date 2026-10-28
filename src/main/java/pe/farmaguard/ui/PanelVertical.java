package pe.farmaguard.ui;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.Scrollable;
import java.awt.Dimension;
import java.awt.Rectangle;

public class PanelVertical extends JPanel implements Scrollable {

    public PanelVertical() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
    }

    public <C extends JComponent> C agregar(C componente) {
        componente.setAlignmentX(LEFT_ALIGNMENT);
        add(componente);
        return componente;
    }

    public void espacio(int alto) {
        agregar((JComponent) Box.createVerticalStrut(alto));
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visible, int orientacion, int direccion) {
        return 16;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visible, int orientacion, int direccion) {
        return Math.max(16, visible.height - 32);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}
