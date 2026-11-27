package pe.farmaguard.ui;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Supplier;

public class InsigniaYberia extends JLabel {
    private final Supplier<Color> fondoYberia;
    private final Supplier<Color> textoYberia;

    public InsigniaYberia(String contenido, Supplier<Color> fondoYberia, Supplier<Color> textoYberia) {
        this(contenido, null, fondoYberia, textoYberia);
    }

    public InsigniaYberia(String contenido, Icon icono, Supplier<Color> fondoYberia, Supplier<Color> textoYberia) {
        super(contenido, icono, LEADING);
        this.fondoYberia = fondoYberia;
        this.textoYberia = textoYberia;
        setIconTextGap(5);
        setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        putClientProperty(FlatClientProperties.STYLE_CLASS, "small");
    }

    @Override
    public Color getForeground() {
        return textoYberia != null ? textoYberia.get() : super.getForeground();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(fondoYberia.get());
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
        g2.dispose();
        super.paintComponent(g);
    }
}
