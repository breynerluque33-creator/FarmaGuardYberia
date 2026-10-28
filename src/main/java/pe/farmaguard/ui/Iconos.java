package pe.farmaguard.ui;

import pe.farmaguard.model.NivelRiesgo;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Supplier;

public final class Iconos {

    @FunctionalInterface
    private interface Pintor {
        void pintar(Graphics2D g, float t);
    }

    private record Vectorial(int tam, Pintor pintor) implements Icon {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.translate(x, y);
            pintor.pintar(g2, tam);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return tam;
        }

        @Override
        public int getIconHeight() {
            return tam;
        }
    }

    private Iconos() {
    }

    private static BasicStroke trazo(float ancho) {
        return new BasicStroke(ancho, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }

    public static Icon nivel(NivelRiesgo nivel, int tam) {
        return new Vectorial(tam, (g, t) -> {
            g.setColor(Estilos.colorNivel(nivel));
            if (nivel == NivelRiesgo.PRECAUCION) {
                Path2D triangulo = new Path2D.Float();
                triangulo.moveTo(t * 0.5, t * 0.1);
                triangulo.lineTo(t * 0.93, t * 0.86);
                triangulo.lineTo(t * 0.07, t * 0.86);
                triangulo.closePath();
                g.setStroke(trazo(t * 0.1f));
                g.fill(triangulo);
                g.draw(triangulo);
            } else {
                g.fill(new Ellipse2D.Float(0, 0, t, t));
            }
            g.setColor(Color.WHITE);
            g.setStroke(trazo(t * 0.1f));
            switch (nivel) {
                case SEGURO -> {
                    Path2D check = new Path2D.Float();
                    check.moveTo(t * 0.29, t * 0.52);
                    check.lineTo(t * 0.44, t * 0.67);
                    check.lineTo(t * 0.72, t * 0.37);
                    g.draw(check);
                }
                case PRECAUCION -> {
                    g.draw(new Line2D.Float(t * 0.5f, t * 0.4f, t * 0.5f, t * 0.6f));
                    float punto = t * 0.11f;
                    g.fill(new Ellipse2D.Float(t * 0.5f - punto / 2, t * 0.7f, punto, punto));
                }
                case PELIGRO -> {
                    g.draw(new Line2D.Float(t * 0.35f, t * 0.35f, t * 0.65f, t * 0.65f));
                    g.draw(new Line2D.Float(t * 0.65f, t * 0.35f, t * 0.35f, t * 0.65f));
                }
            }
        });
    }

    public static Icon buscar(int tam) {
        return new Vectorial(tam, (g, t) -> {
            g.setColor(Estilos.textoSecundario());
            g.setStroke(trazo(Math.max(1.5f, t * 0.1f)));
            g.draw(new Ellipse2D.Float(t * 0.12f, t * 0.12f, t * 0.54f, t * 0.54f));
            g.draw(new Line2D.Float(t * 0.6f, t * 0.6f, t * 0.86f, t * 0.86f));
        });
    }

    public static Icon eliminar(int tam) {
        return new Vectorial(tam, (g, t) -> {
            g.setColor(Estilos.textoSecundario());
            g.setStroke(trazo(Math.max(1.4f, t * 0.08f)));
            g.draw(new Line2D.Float(t * 0.16f, t * 0.27f, t * 0.84f, t * 0.27f));
            Path2D asa = new Path2D.Float();
            asa.moveTo(t * 0.38, t * 0.27);
            asa.lineTo(t * 0.38, t * 0.14);
            asa.lineTo(t * 0.62, t * 0.14);
            asa.lineTo(t * 0.62, t * 0.27);
            g.draw(asa);
            Path2D cuerpo = new Path2D.Float();
            cuerpo.moveTo(t * 0.25, t * 0.27);
            cuerpo.lineTo(t * 0.3, t * 0.87);
            cuerpo.lineTo(t * 0.7, t * 0.87);
            cuerpo.lineTo(t * 0.75, t * 0.27);
            g.draw(cuerpo);
            g.draw(new Line2D.Float(t * 0.44f, t * 0.42f, t * 0.44f, t * 0.72f));
            g.draw(new Line2D.Float(t * 0.56f, t * 0.42f, t * 0.56f, t * 0.72f));
        });
    }

    public static Icon mas(int tam, Supplier<Color> color) {
        return new Vectorial(tam, (g, t) -> {
            g.setColor(color.get());
            g.setStroke(trazo(Math.max(1.6f, t * 0.12f)));
            g.draw(new Line2D.Float(t * 0.5f, t * 0.2f, t * 0.5f, t * 0.8f));
            g.draw(new Line2D.Float(t * 0.2f, t * 0.5f, t * 0.8f, t * 0.5f));
        });
    }

    public static Icon menos(int tam) {
        return new Vectorial(tam, (g, t) -> {
            g.setColor(Estilos.textoPrincipal());
            g.setStroke(trazo(Math.max(1.6f, t * 0.12f)));
            g.draw(new Line2D.Float(t * 0.2f, t * 0.5f, t * 0.8f, t * 0.5f));
        });
    }

    public static Icon check(int tam) {
        return new Vectorial(tam, (g, t) -> {
            g.setColor(Estilos.acentoSuave());
            g.fill(new Ellipse2D.Float(0, 0, t, t));
            g.setColor(Estilos.acento());
            g.setStroke(trazo(Math.max(1.5f, t * 0.11f)));
            Path2D check = new Path2D.Float();
            check.moveTo(t * 0.29, t * 0.52);
            check.lineTo(t * 0.44, t * 0.66);
            check.lineTo(t * 0.71, t * 0.37);
            g.draw(check);
        });
    }

    public static Icon reloj(int tam) {
        return new Vectorial(tam, (g, t) -> {
            g.setColor(Estilos.acento());
            g.setStroke(trazo(Math.max(1.6f, t * 0.09f)));
            g.draw(new Ellipse2D.Float(t * 0.08f, t * 0.08f, t * 0.84f, t * 0.84f));
            Path2D agujas = new Path2D.Float();
            agujas.moveTo(t * 0.5, t * 0.27);
            agujas.lineTo(t * 0.5, t * 0.5);
            agujas.lineTo(t * 0.67, t * 0.62);
            g.draw(agujas);
        });
    }

    public static Icon logo(int tam) {
        return new Vectorial(tam, (g, t) -> {
            g.setColor(Estilos.acento());
            g.fill(new RoundRectangle2D.Float(0, 0, t, t, t * 0.38f, t * 0.38f));
            g.setColor(Color.WHITE);
            float grosor = t * 0.2f;
            float largo = t * 0.56f;
            float inicio = (t - largo) / 2;
            float centro = (t - grosor) / 2;
            g.fill(new RoundRectangle2D.Float(centro, inicio, grosor, largo, grosor * 0.6f, grosor * 0.6f));
            g.fill(new RoundRectangle2D.Float(inicio, centro, largo, grosor, grosor * 0.6f, grosor * 0.6f));
        });
    }

    public static Icon tema(int tam) {
        return new Vectorial(tam, (g, t) -> {
            g.setColor(Estilos.textoSecundario());
            if (Estilos.esOscuro()) {
                g.fill(new Ellipse2D.Float(t * 0.3f, t * 0.3f, t * 0.4f, t * 0.4f));
                g.setStroke(trazo(Math.max(1.5f, t * 0.08f)));
                for (int i = 0; i < 8; i++) {
                    double angulo = Math.PI / 4 * i;
                    float x1 = (float) (t * 0.5 + Math.cos(angulo) * t * 0.33);
                    float y1 = (float) (t * 0.5 + Math.sin(angulo) * t * 0.33);
                    float x2 = (float) (t * 0.5 + Math.cos(angulo) * t * 0.44);
                    float y2 = (float) (t * 0.5 + Math.sin(angulo) * t * 0.44);
                    g.draw(new Line2D.Float(x1, y1, x2, y2));
                }
            } else {
                Area luna = new Area(new Ellipse2D.Float(t * 0.12f, t * 0.12f, t * 0.76f, t * 0.76f));
                luna.subtract(new Area(new Ellipse2D.Float(t * 0.36f, t * 0.02f, t * 0.62f, t * 0.62f)));
                g.fill(luna);
            }
        });
    }

    public static Icon editar(int tam) {
        return new Vectorial(tam, (g, t) -> {
            g.setColor(Estilos.textoSecundario());
            g.setStroke(trazo(Math.max(1.4f, t * 0.08f)));
            Path2D lapiz = new Path2D.Float();
            lapiz.moveTo(t * 0.16, t * 0.84);
            lapiz.lineTo(t * 0.2, t * 0.64);
            lapiz.lineTo(t * 0.66, t * 0.18);
            lapiz.lineTo(t * 0.82, t * 0.34);
            lapiz.lineTo(t * 0.36, t * 0.8);
            lapiz.closePath();
            g.draw(lapiz);
            g.draw(new Line2D.Float(t * 0.56f, t * 0.28f, t * 0.72f, t * 0.44f));
        });
    }

    public static Icon avatar(String texto, int tam) {
        String inicial = texto == null || texto.isBlank() ? "?" : texto.substring(0, 1).toUpperCase();
        return new Vectorial(tam, (g, t) -> {
            g.setColor(Estilos.acentoSuave());
            g.fill(new Ellipse2D.Float(0, 0, t, t));
            g.setColor(Estilos.acento());
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.round(t * 0.44f)));
            FontMetrics fm = g.getFontMetrics();
            float x = (t - fm.stringWidth(inicial)) / 2f;
            float y = (t - fm.getHeight()) / 2f + fm.getAscent();
            g.drawString(inicial, x, y);
        });
    }
}
