package pe.farmaguard.ui;

import javax.swing.JToggleButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

public class Interruptor extends JToggleButton {

    private static final int ANCHO = 42;
    private static final int ALTO = 24;

    public Interruptor(boolean encendido) {
        setSelected(encendido);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(ANCHO, ALTO);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int y = (getHeight() - ALTO) / 2;
        g2.setColor(isSelected() ? Estilos.acento() : (Estilos.esOscuro() ? new Color(0x5A5E66) : new Color(0xCBD0D6)));
        g2.fill(new RoundRectangle2D.Float(0, y, ANCHO, ALTO, ALTO, ALTO));
        int perilla = ALTO - 6;
        int x = isSelected() ? ANCHO - perilla - 3 : 3;
        g2.setColor(new Color(0, 0, 0, 40));
        g2.fill(new Ellipse2D.Float(x, y + 4f, perilla, perilla));
        g2.setColor(Color.WHITE);
        g2.fill(new Ellipse2D.Float(x, y + 3f, perilla, perilla));
        g2.dispose();
    }
}
