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

public class Insignia extends JLabel {

    private final Supplier<Color> fondo;
    private final Supplier<Color> texto;

    public Insignia(String contenido, Supplier<Color> fondo, Supplier<Color> texto) {
        this(contenido, null, fondo, texto);
    }

    public Insignia(String contenido, Icon icono, Supplier<Color> fondo, Supplier<Color> texto) {
        super(contenido, icono, LEADING);
        this.fondo = fondo;
        this.texto = texto;
        setIconTextGap(5);
        setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        putClientProperty(FlatClientProperties.STYLE_CLASS, "small");
    }

    @Override
    public Color getForeground() {
        return texto != null ? texto.get() : super.getForeground();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(fondo.get());
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
        g2.dispose();
        super.paintComponent(g);
    }
}
