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

class RenderizadorMedicamentoYberia extends JPanel implements ListCellRenderer<MedicamentoYberia> {
    private final JLabel nombreYberia = EstilosYberia.etiqueta("", "h4");
    private final JLabel detalleYberia = EstilosYberia.etiqueta("", "small");
    private boolean seleccionadoYberia;

    RenderizadorMedicamentoYberia() {
        super(new GridLayout(0, 1, 0, 1));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(7, 12, 7, 12));
        add(nombreYberia);
        add(detalleYberia);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends MedicamentoYberia> lista, MedicamentoYberia m, int indice,
                                                  boolean seleccionadoYberia, boolean conFoco) {
        this.seleccionadoYberia = seleccionadoYberia;
        nombreYberia.setText(m.getNombre());
        detalleYberia.setText(m.getPrincipioActivo() + " · " + m.getConcentracion());
        nombreYberia.setForeground(seleccionadoYberia ? Color.WHITE : EstilosYberia.textoPrincipal());
        detalleYberia.setForeground(seleccionadoYberia ? new Color(255, 255, 255, 210) : EstilosYberia.textoSecundario());
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (seleccionadoYberia) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(EstilosYberia.acento());
            g2.fill(new RoundRectangle2D.Float(2, 1, getWidth() - 4f, getHeight() - 2f, EstilosYberia.ARC_YBERIA, EstilosYberia.ARC_YBERIA));
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
