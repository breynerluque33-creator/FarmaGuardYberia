package pe.farmaguard.ui;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class SegmentadoYberia<T> extends JPanel {
    private final ButtonGroup grupoYberia = new ButtonGroup();
    private final Map<T, PildoraYberia> opcionesYberia = new LinkedHashMap<>();
    private Consumer<T> alCambiar = valor -> {
    };

    public SegmentadoYberia(List<T> valores, Function<T, String> texto) {
        this(valores, texto, new Insets(6, 10, 6, 10));
    }

    public SegmentadoYberia(List<T> valores, Function<T, String> texto, Insets margen) {
        super(new GridLayout(1, valores.size(), 4, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        for (T valor : valores) {
            PildoraYberia pildora = new PildoraYberia(texto.apply(valor));
            pildora.setMargin(margen);
            pildora.addActionListener(e -> alCambiar.accept(valor));
            grupoYberia.add(pildora);
            opcionesYberia.put(valor, pildora);
            add(pildora);
        }
    }

    public void alCambiar(Consumer<T> oyente) {
        this.alCambiar = oyente;
    }

    public void setSeleccion(T valor) {
        PildoraYberia pildora = opcionesYberia.get(valor);
        if (pildora != null) {
            pildora.setSelected(true);
        }
    }

    public T getSeleccion() {
        return opcionesYberia.entrySet().stream()
                .filter(e -> e.getValue().isSelected())
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        RoundRectangle2D capsula = new RoundRectangle2D.Float(0, 0, getWidth() - 1f, getHeight() - 1f,
                getHeight(), getHeight());
        g2.setColor(EstilosYberia.superficieSuave());
        g2.fill(capsula);
        g2.setColor(EstilosYberia.lineaSuave());
        g2.draw(capsula);
        g2.dispose();
    }

    private static final class PildoraYberia extends JToggleButton {
        PildoraYberia(String texto) {
            super(texto);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setFocusable(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        public Color getForeground() {
            ButtonModel modelo = getModel();
            if (modelo == null) {
                return super.getForeground();
            }
            if (modelo.isSelected()) {
                return Color.WHITE;
            }
            return modelo.isRollover() ? EstilosYberia.textoPrincipal() : EstilosYberia.textoSecundario();
        }

        @Override
        protected void paintComponent(Graphics g) {
            ButtonModel modelo = getModel();
            if (modelo.isSelected() || modelo.isRollover()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(modelo.isSelected() ? EstilosYberia.acento() : EstilosYberia.lineaSuave());
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}
