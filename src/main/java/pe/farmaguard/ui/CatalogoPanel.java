package pe.farmaguard.ui;

import com.formdev.flatlaf.FlatClientProperties;
import pe.farmaguard.model.MedicamentoYberia;
import pe.farmaguard.service.CompatibilidadYberiaService;
import pe.farmaguard.service.InteraccionConocida;
import pe.farmaguard.service.MedicamentoYberiaService;
import pe.farmaguard.util.Texto;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.List;

public class CatalogoPanel extends JPanel {

    private final CompatibilidadYberiaService compatibilidadYberiaService;
    private final List<MedicamentoYberia> catalogo;
    private final DefaultListModel<MedicamentoYberia> modeloLista = new DefaultListModel<>();
    private final JList<MedicamentoYberia> listaMedicamentos = new JList<>(modeloLista);
    private final JTextField filtro = new JTextField();
    private final PanelVertical detalle = new PanelVertical();
    private final JScrollPane scrollDetalle = Estilos.scroll(detalle);

    public CatalogoPanel(MedicamentoYberiaService medicamentoYberiaService, CompatibilidadYberiaService compatibilidadYberiaService) {
        super(new GridBagLayout());
        this.compatibilidadYberiaService = compatibilidadYberiaService;
        this.catalogo = medicamentoYberiaService.listarTodos();
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(4, 20, 16, 20));

        filtro.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Filtrar por nombre, componente o grupo");
        filtro.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, Iconos.buscar(16));
        filtro.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
        filtro.setMargin(new Insets(8, 8, 8, 8));
        filtro.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filtrar();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filtrar();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filtrar();
            }
        });

        listaMedicamentos.setCellRenderer(new RenderizadorMedicamento());
        listaMedicamentos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaMedicamentos.setOpaque(false);
        listaMedicamentos.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                mostrar(listaMedicamentos.getSelectedValue());
            }
        });

        Tarjeta izquierda = new Tarjeta(new BorderLayout(0, 12));
        JPanel cabecera = Estilos.transparente(new BorderLayout(0, 12));
        JPanel titulos = Estilos.transparente(new GridLayout(0, 1, 0, 2));
        titulos.add(Estilos.etiqueta("Catálogo", "h3"));
        titulos.add(Estilos.etiqueta(catalogo.size() + " medicamentos comunes en boticas del Perú", "small",
                Estilos::textoSecundario));
        cabecera.add(titulos, BorderLayout.NORTH);
        cabecera.add(filtro, BorderLayout.SOUTH);
        izquierda.add(cabecera, BorderLayout.NORTH);
        izquierda.add(Estilos.scroll(listaMedicamentos), BorderLayout.CENTER);
        izquierda.setPreferredSize(new Dimension(340, 300));
        izquierda.setMinimumSize(new Dimension(300, 300));

        scrollDetalle.setPreferredSize(new Dimension(400, 300));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridx = 0;
        c.weightx = 0;
        c.insets = new Insets(0, 0, 0, 10);
        add(izquierda, c);
        c.gridx = 1;
        c.weightx = 1;
        c.insets = new Insets(0, 10, 0, 0);
        add(scrollDetalle, c);

        filtrar();
        if (!modeloLista.isEmpty()) {
            listaMedicamentos.setSelectedIndex(0);
        }
    }

    private void filtrar() {
        String consulta = Texto.normalizar(filtro.getText());
        MedicamentoYberia seleccionado = listaMedicamentos.getSelectedValue();
        modeloLista.clear();
        catalogo.stream()
                .filter(m -> consulta.isEmpty()
                        || Texto.normalizar(m.getNombre()).contains(consulta)
                        || Texto.normalizar(m.getPrincipioActivo()).contains(consulta)
                        || Texto.normalizar(m.getCategoria()).contains(consulta))
                .forEach(modeloLista::addElement);
        if (seleccionado != null && modeloLista.contains(seleccionado)) {
            listaMedicamentos.setSelectedValue(seleccionado, true);
        }
    }

    private void mostrar(MedicamentoYberia medicamentoYberia) {
        if (medicamentoYberia == null) {
            return;
        }
        detalle.removeAll();
        detalle.agregar(new FichaMedicamento(medicamentoYberia, true));

        List<InteraccionConocida> interacciones = compatibilidadYberiaService.interaccionesConocidas(medicamentoYberia);
        if (!interacciones.isEmpty()) {
            var seccion = Estilos.etiqueta("Precauciones al combinarlo", "h4", Estilos::textoSecundario);
            seccion.setBorder(BorderFactory.createEmptyBorder(16, 8, 4, 8));
            detalle.agregar(seccion);
            for (InteraccionConocida interaccion : interacciones) {
                detalle.agregar(new TarjetaAlerta(interaccion.nivel(), interaccion.con(), interaccion.explicacion(), false));
            }
        }
        detalle.revalidate();
        detalle.repaint();
        SwingUtilities.invokeLater(() -> scrollDetalle.getVerticalScrollBar().setValue(0));
    }
}
