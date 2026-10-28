package pe.farmaguard.ui;

import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

public class BotonPrimario extends JButton {

    public BotonPrimario(String texto, Icon icono) {
        super(texto, icono);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(getFont().deriveFont(Font.BOLD));
        setMargin(new Insets(10, 20, 10, 20));
        setIconTextGap(8);
    }

    @Override
    public Color getForeground() {
        return Color.WHITE;
    }

    @Override
    protected void paintComponent(Graphics g) {
        ButtonModel modelo = getModel();
        Color base = Estilos.acento();
        Color relleno;
        if (!isEnabled()) {
            relleno = Estilos.textoSecundario();
        } else if (modelo.isPressed()) {
            relleno = base.darker();
        } else if (modelo.isRollover()) {
            relleno = mezclar(base, Color.WHITE, 0.12f);
        } else {
            relleno = base;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(relleno);
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), Estilos.ARC, Estilos.ARC));
        g2.dispose();
        super.paintComponent(g);
    }

    private static Color mezclar(Color a, Color b, float proporcion) {
        return new Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * proporcion),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * proporcion),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * proporcion));
    }
}
