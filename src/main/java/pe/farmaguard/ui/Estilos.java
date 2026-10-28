package pe.farmaguard.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import pe.farmaguard.model.NivelRiesgo;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.util.Map;
import java.util.function.Supplier;
import java.util.prefs.Preferences;

public final class Estilos {

    public static final int ARC = 12;
    public static final String AVISO_LEGAL = "FarmaGuard es una guía de compra informada. No diagnostica ni reemplaza "
            + "la consulta con un médico o químico farmacéutico.";

    private static final Preferences PREFERENCIAS = Preferences.userNodeForPackage(Estilos.class);
    private static final String CLAVE_TEMA_OSCURO = "temaOscuro";

    private Estilos() {
    }

    public static void instalarTema() {
        boolean oscuro;
        try {
            oscuro = PREFERENCIAS.getBoolean(CLAVE_TEMA_OSCURO, false);
        } catch (RuntimeException e) {
            oscuro = false;
        }
        aplicarTema(oscuro);
    }

    public static void alternarTema() {
        boolean oscuro = !esOscuro();
        aplicarTema(oscuro);
        try {
            PREFERENCIAS.putBoolean(CLAVE_TEMA_OSCURO, oscuro);
        } catch (RuntimeException ignorada) {
        }
        FlatLaf.updateUI();
    }

    public static boolean esOscuro() {
        return FlatLaf.isLafDark();
    }

    private static void aplicarTema(boolean oscuro) {
        FlatLaf.setGlobalExtraDefaults(Map.of("@accentColor", oscuro ? "#2BB3A3" : "#0F766E"));
        UIManager.put("defaultFont", null);
        if (oscuro) {
            FlatMacDarkLaf.setup();
        } else {
            FlatMacLightLaf.setup();
        }
        Font base = UIManager.getFont("defaultFont");
        if (base != null) {
            UIManager.put("defaultFont", base.deriveFont(base.getSize2D() + 1f));
        }
        UIManager.put("Component.arc", ARC);
        UIManager.put("Button.arc", ARC);
        UIManager.put("TextComponent.arc", ARC);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("TitlePane.unifiedBackground", true);
    }

    private static Color color(int claro, int oscuro) {
        return new Color(esOscuro() ? oscuro : claro);
    }

    public static Color superficie() {
        return color(0xFFFFFF, 0x2A2C30);
    }

    public static Color superficieSuave() {
        return color(0xF1F3F5, 0x34373C);
    }

    public static Color lineaSuave() {
        return color(0xE3E6EA, 0x43464C);
    }

    public static Color textoPrincipal() {
        return color(0x1F2933, 0xE6E8EB);
    }

    public static Color textoSecundario() {
        return color(0x6B7280, 0x9CA3AF);
    }

    public static Color acento() {
        return color(0x0F766E, 0x2BB3A3);
    }

    public static Color acentoSuave() {
        return color(0xE3F3F1, 0x1E3B39);
    }

    public static Color fondoNivel(NivelRiesgo nivel) {
        return switch (nivel) {
            case SEGURO -> color(0xEAF7EF, 0x1F3A2C);
            case PRECAUCION -> color(0xFFF7E1, 0x3B3220);
            case PELIGRO -> color(0xFDEDED, 0x432727);
        };
    }

    public static Color colorNivel(NivelRiesgo nivel) {
        return switch (nivel) {
            case SEGURO -> color(0x2F9E62, 0x4CC38A);
            case PRECAUCION -> color(0xE3A008, 0xF2C14E);
            case PELIGRO -> color(0xD64545, 0xF07171);
        };
    }

    public static Color textoNivel(NivelRiesgo nivel) {
        return switch (nivel) {
            case SEGURO -> color(0x1E6B42, 0xA7E8C6);
            case PRECAUCION -> color(0x8A5A00, 0xF7D98B);
            case PELIGRO -> color(0xA12C2C, 0xF8B4B4);
        };
    }

    public static JLabel etiqueta(String texto, String claseEstilo) {
        return etiqueta(texto, claseEstilo, null);
    }

    public static JLabel etiqueta(String texto, String claseEstilo, Supplier<Color> color) {
        JLabel label = new JLabel(texto) {
            @Override
            public Color getForeground() {
                return color != null ? color.get() : super.getForeground();
            }
        };
        if (claseEstilo != null) {
            label.putClientProperty(FlatClientProperties.STYLE_CLASS, claseEstilo);
        }
        return label;
    }

    public static JTextArea parrafo(String texto, Supplier<Color> color) {
        JTextArea area = new JTextArea(texto) {
            @Override
            public Color getForeground() {
                return color != null ? color.get() : super.getForeground();
            }

            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                return new Dimension(Math.min(d.width, 240), d.height);
            }
        };
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setBorder(BorderFactory.createEmptyBorder());
        area.setFont(UIManager.getFont("Label.font"));
        return area;
    }

    public static JPanel transparente(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    public static JScrollPane scroll(JComponent vista) {
        JScrollPane scroll = new JScrollPane(vista);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setViewportBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    public static JButton botonSecundario(String texto) {
        JButton boton = new JButton(texto);
        boton.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        boton.setMargin(new Insets(6, 14, 6, 14));
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return boton;
    }

    public static JButton botonIcono(Icon icono, String ayuda) {
        JButton boton = new JButton(icono);
        boton.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS);
        boton.setToolTipText(ayuda);
        boton.setFocusable(false);
        boton.setMargin(new Insets(4, 4, 4, 4));
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return boton;
    }

    public static JButton botonEnlace(String texto, Icon icono) {
        JButton boton = new JButton(texto, icono) {
            @Override
            public Color getForeground() {
                return acento();
            }
        };
        boton.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS);
        boton.setFocusable(false);
        boton.setMargin(new Insets(4, 2, 4, 6));
        boton.setIconTextGap(6);
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return boton;
    }
}
