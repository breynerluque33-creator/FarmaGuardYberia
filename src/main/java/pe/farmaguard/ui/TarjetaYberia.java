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

public class TarjetaYberia extends JPanel {
    private static final int SOMBRA_YBERIA = 5;
    private static final int ARCO_YBERIA = 18;

    private final Supplier<Color> fondoYberia;
    private Supplier<Color> bordeYberia = EstilosYberia::lineaSuave;
    private boolean atenuadaYberia;

    public TarjetaYberia(LayoutManager layout) {
        this(layout, EstilosYberia::superficie, 18);
    }

    public TarjetaYberia(LayoutManager layout, Supplier<Color> fondoYberia, int relleno) {
        super(layout);
        this.fondoYberia = fondoYberia;
        setOpaque(false);
        int margen = SOMBRA_YBERIA + relleno;
        setBorder(BorderFactory.createEmptyBorder(margen, margen, margen, margen));
    }

    public void setBorde(Supplier<Color> bordeYberia) {
        this.bordeYberia = bordeYberia;
        repaint();
    }

    public void setAtenuada(boolean atenuadaYberia) {
        this.atenuadaYberia = atenuadaYberia;
        repaint();
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    @Override
    public void paint(Graphics g) {
        if (!atenuadaYberia) {
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
        float x = SOMBRA_YBERIA;
        float y = SOMBRA_YBERIA - 1f;
        float ancho = getWidth() - 2f * SOMBRA_YBERIA;
        float alto = getHeight() - 2f * SOMBRA_YBERIA;

        if (!EstilosYberia.esOscuro()) {
            for (int i = SOMBRA_YBERIA; i >= 1; i--) {
                g2.setColor(new Color(15, 23, 42, 2 + (SOMBRA_YBERIA - i)));
                g2.fill(new RoundRectangle2D.Float(x - i, y - i + 2, ancho + 2f * i, alto + 2f * i,
                        ARCO_YBERIA + 2f * i, ARCO_YBERIA + 2f * i));
            }
        }
        g2.setColor(fondoYberia.get());
        g2.fill(new RoundRectangle2D.Float(x, y, ancho, alto, ARCO_YBERIA, ARCO_YBERIA));
        if (bordeYberia != null) {
            g2.setColor(bordeYberia.get());
            g2.draw(new RoundRectangle2D.Float(x, y, ancho - 1, alto - 1, ARCO_YBERIA, ARCO_YBERIA));
        }
        g2.dispose();
    }
}
