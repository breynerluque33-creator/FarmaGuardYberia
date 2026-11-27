package pe.farmaguard.ui;

import com.formdev.flatlaf.FlatClientProperties;
import pe.farmaguard.model.MedicamentoYberia;
import pe.farmaguard.service.CompatibilidadYberiaService;
import pe.farmaguard.service.InteraccionConocidaYberia;
import pe.farmaguard.service.MedicamentoYberiaService;
import pe.farmaguard.util.SinonimosYberia;

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
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class CatalogoPanelYberia extends JPanel {
    private final CompatibilidadYberiaService compatibilidadYberiaService;
    private final List<MedicamentoYberia> catalogoYberia;
    private final DefaultListModel<MedicamentoYberia> modeloListaYberia = new DefaultListModel<>();
    private final JList<MedicamentoYberia> listaMedicamentosYberia = new JList<>(modeloListaYberia);
    private final JTextField filtroYberia = new JTextField();
    private final PanelVerticalYberia detalleYberia = new PanelVerticalYberia();
    private final JScrollPane scrollDetalleYberia = EstilosYberia.scroll(detalleYberia);

    public CatalogoPanelYberia(MedicamentoYberiaService medicamentoYberiaService, CompatibilidadYberiaService compatibilidadYberiaService) {
        super(new GridBagLayout());
        this.compatibilidadYberiaService = compatibilidadYberiaService;
        this.catalogoYberia = medicamentoYberiaService.listarTodos();
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(4, 20, 16, 20));

        filtroYberia.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Filtrar por nombre, componente o grupo");
        filtroYberia.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, IconosYberia.buscar(16));
        filtroYberia.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
        filtroYberia.setMargin(new Insets(8, 8, 8, 8));
        filtroYberia.getDocument().addDocumentListener(new DocumentListener() {
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

        listaMedicamentosYberia.setCellRenderer(new RenderizadorMedicamentoYberia());
        listaMedicamentosYberia.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaMedicamentosYberia.setOpaque(false);
        listaMedicamentosYberia.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                mostrar(listaMedicamentosYberia.getSelectedValue());
            }
        });

        TarjetaYberia izquierda = new TarjetaYberia(new BorderLayout(0, 12));
        JPanel cabecera = EstilosYberia.transparente(new BorderLayout(0, 12));
        JPanel titulos = EstilosYberia.transparente(new GridLayout(0, 1, 0, 2));
        titulos.add(EstilosYberia.etiqueta("Catálogo", "h3"));
        titulos.add(EstilosYberia.etiqueta(catalogoYberia.size() + " medicamentos comunes en boticas del Perú", "small",
                EstilosYberia::textoSecundario));
        cabecera.add(titulos, BorderLayout.NORTH);
        cabecera.add(filtroYberia, BorderLayout.SOUTH);
        izquierda.add(cabecera, BorderLayout.NORTH);
        izquierda.add(EstilosYberia.scroll(listaMedicamentosYberia), BorderLayout.CENTER);
        izquierda.setPreferredSize(new Dimension(340, 300));
        izquierda.setMinimumSize(new Dimension(300, 300));

        scrollDetalleYberia.setPreferredSize(new Dimension(400, 300));

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
        add(scrollDetalleYberia, c);

        filtrar();
        if (!modeloListaYberia.isEmpty()) {
            listaMedicamentosYberia.setSelectedIndex(0);
        }
    }

    private void filtrar() {
        Set<String> terminos = SinonimosYberia.terminosDeBusqueda(filtroYberia.getText());
        MedicamentoYberia seleccionado = listaMedicamentosYberia.getSelectedValue();
        modeloListaYberia.clear();
        catalogoYberia.stream()
                .filter(m -> terminos.isEmpty()
                        || SinonimosYberia.coincide(m.getNombre(), terminos)
                        || SinonimosYberia.coincide(m.getPrincipioActivo(), terminos)
                        || SinonimosYberia.coincide(m.getCategoria(), terminos))
                .sorted(Comparator.comparing((MedicamentoYberia m) -> !esCoincidenciaDirecta(m, terminos)))
                .forEach(modeloListaYberia::addElement);
        if (!terminos.isEmpty() && !modeloListaYberia.isEmpty()) {
            listaMedicamentosYberia.setSelectedIndex(0);
        } else if (seleccionado != null && modeloListaYberia.contains(seleccionado)) {
            listaMedicamentosYberia.setSelectedValue(seleccionado, true);
        }
    }

    private static boolean esCoincidenciaDirecta(MedicamentoYberia m, Set<String> terminos) {
        String nombre = SinonimosYberia.canonico(m.getNombre());
        String principio = SinonimosYberia.canonico(m.getPrincipioActivo());
        return terminos.stream().anyMatch(t -> nombre.startsWith(t) || principio.equals(t));
    }

    private void mostrar(MedicamentoYberia medicamentoYberia) {
        if (medicamentoYberia == null) {
            return;
        }
        detalleYberia.removeAll();
        detalleYberia.agregar(new FichaMedicamentoYberia(medicamentoYberia, true));

        List<InteraccionConocidaYberia> interacciones = compatibilidadYberiaService.interaccionesConocidas(medicamentoYberia);
        if (!interacciones.isEmpty()) {
            var seccion = EstilosYberia.etiqueta("Precauciones al combinarlo", "h4", EstilosYberia::textoSecundario);
            seccion.setBorder(BorderFactory.createEmptyBorder(16, 8, 4, 8));
            detalleYberia.agregar(seccion);
            for (InteraccionConocidaYberia interaccion : interacciones) {
                detalleYberia.agregar(new TarjetaAlertaYberia(interaccion.nivelYberia(), interaccion.conYberia(), interaccion.explicacionYberia(), false));
            }
        }
        detalleYberia.revalidate();
        detalleYberia.repaint();
        SwingUtilities.invokeLater(() -> scrollDetalleYberia.getVerticalScrollBar().setValue(0));
    }
}
