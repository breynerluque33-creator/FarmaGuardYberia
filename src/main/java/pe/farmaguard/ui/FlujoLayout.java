package pe.farmaguard.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

class FlujoLayout extends FlowLayout {

    FlujoLayout(int espacioHorizontal, int espacioVertical) {
        super(LEFT, espacioHorizontal, espacioVertical);
    }

    @Override
    public Dimension preferredLayoutSize(Container contenedor) {
        return calcular(contenedor, true);
    }

    @Override
    public Dimension minimumLayoutSize(Container contenedor) {
        return calcular(contenedor, false);
    }

    private Dimension calcular(Container contenedor, boolean preferido) {
        synchronized (contenedor.getTreeLock()) {
            int anchoDisponible = contenedor.getWidth() > 0 ? contenedor.getWidth() : Integer.MAX_VALUE;
            Insets margen = contenedor.getInsets();
            int limite = anchoDisponible - margen.left - margen.right - getHgap() * 2;
            int ancho = 0;
            int alto = 0;
            int anchoFila = 0;
            int altoFila = 0;
            for (Component c : contenedor.getComponents()) {
                if (!c.isVisible()) {
                    continue;
                }
                Dimension d = preferido ? c.getPreferredSize() : c.getMinimumSize();
                if (anchoFila > 0 && anchoFila + getHgap() + d.width > limite) {
                    ancho = Math.max(ancho, anchoFila);
                    alto += altoFila + getVgap();
                    anchoFila = 0;
                    altoFila = 0;
                }
                anchoFila += (anchoFila > 0 ? getHgap() : 0) + d.width;
                altoFila = Math.max(altoFila, d.height);
            }
            ancho = Math.max(ancho, anchoFila);
            alto += altoFila;
            return new Dimension(Math.min(ancho, 240) + margen.left + margen.right,
                    alto + getVgap() * 2 + margen.top + margen.bottom);
        }
    }
}
