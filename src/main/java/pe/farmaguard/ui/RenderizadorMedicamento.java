package pe.farmaguard.ui;

import pe.farmaguard.model.MedicamentoYberia;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

class RenderizadorMedicamento extends JPanel implements ListCellRenderer<MedicamentoYberia> {

    private final JLabel nombre = Estilos.etiqueta("", "h4");
    private final JLabel detalle = Estilos.etiqueta("", "small");
    private boolean seleccionado;

    RenderizadorMedicamento() {
        super(new GridLayout(0, 1, 0, 1));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(7, 12, 7, 12));
        add(nombre);
        add(detalle);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends MedicamentoYberia> lista, MedicamentoYberia m, int indice,
                                                  boolean seleccionado, boolean conFoco) {
        this.seleccionado = seleccionado;
        nombre.setText(m.getNombre());
        detalle.setText(m.getPrincipioActivo() + " · " + m.getConcentracion());
        nombre.setForeground(seleccionado ? Color.WHITE : Estilos.textoPrincipal());
        detalle.setForeground(seleccionado ? new Color(255, 255, 255, 210) : Estilos.textoSecundario());
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (seleccionado) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Estilos.acento());
            g2.fill(new RoundRectangle2D.Float(2, 1, getWidth() - 4f, getHeight() - 2f, Estilos.ARC, Estilos.ARC));
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
