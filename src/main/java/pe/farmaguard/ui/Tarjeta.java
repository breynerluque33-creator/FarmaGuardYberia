package pe.farmaguard.ui;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Supplier;

public class Tarjeta extends JPanel {

    private static final int SOMBRA = 5;
    private static final int ARCO = 18;

    private final Supplier<Color> fondo;
    private Supplier<Color> borde = Estilos::lineaSuave;
    private boolean atenuada;

    public Tarjeta(LayoutManager layout) {
        this(layout, Estilos::superficie, 18);
    }

    public Tarjeta(LayoutManager layout, Supplier<Color> fondo, int relleno) {
        super(layout);
        this.fondo = fondo;
        setOpaque(false);
        int margen = SOMBRA + relleno;
        setBorder(BorderFactory.createEmptyBorder(margen, margen, margen, margen));
    }

    public void setBorde(Supplier<Color> borde) {
        this.borde = borde;
        repaint();
    }

    public void setAtenuada(boolean atenuada) {
        this.atenuada = atenuada;
        repaint();
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    @Override
    public void paint(Graphics g) {
        if (!atenuada) {
            super.paint(g);
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.SrcOver.derive(0.55f));
        super.paint(g2);
        g2.dispose();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float x = SOMBRA;
        float y = SOMBRA - 1f;
        float ancho = getWidth() - 2f * SOMBRA;
        float alto = getHeight() - 2f * SOMBRA;

        if (!Estilos.esOscuro()) {
            for (int i = SOMBRA; i >= 1; i--) {
                g2.setColor(new Color(15, 23, 42, 2 + (SOMBRA - i)));
                g2.fill(new RoundRectangle2D.Float(x - i, y - i + 2, ancho + 2f * i, alto + 2f * i,
                        ARCO + 2f * i, ARCO + 2f * i));
            }
        }
        g2.setColor(fondo.get());
        g2.fill(new RoundRectangle2D.Float(x, y, ancho, alto, ARCO, ARCO));
        if (borde != null) {
            g2.setColor(borde.get());
            g2.draw(new RoundRectangle2D.Float(x, y, ancho - 1, alto - 1, ARCO, ARCO));
        }
        g2.dispose();
    }
}
