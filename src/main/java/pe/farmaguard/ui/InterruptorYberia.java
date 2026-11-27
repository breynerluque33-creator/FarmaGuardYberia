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

public class InterruptorYberia extends JToggleButton {
    private static final int ANCHO_YBERIA = 42;
    private static final int ALTO_YBERIA = 24;

    public InterruptorYberia(boolean encendido) {
        setSelected(encendido);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(ANCHO_YBERIA, ALTO_YBERIA);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int y = (getHeight() - ALTO_YBERIA) / 2;
        g2.setColor(isSelected() ? EstilosYberia.acento() : (EstilosYberia.esOscuro() ? new Color(0x5A5E66) : new Color(0xCBD0D6)));
        g2.fill(new RoundRectangle2D.Float(0, y, ANCHO_YBERIA, ALTO_YBERIA, ALTO_YBERIA, ALTO_YBERIA));
        int perilla = ALTO_YBERIA - 6;
        int x = isSelected() ? ANCHO_YBERIA - perilla - 3 : 3;
        g2.setColor(new Color(0, 0, 0, 40));
        g2.fill(new Ellipse2D.Float(x, y + 4f, perilla, perilla));
        g2.setColor(Color.WHITE);
        g2.fill(new Ellipse2D.Float(x, y + 3f, perilla, perilla));
        g2.dispose();
    }
}
