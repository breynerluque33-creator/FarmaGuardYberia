package pe.farmaguard.ui;

import com.formdev.flatlaf.FlatClientProperties;
import pe.farmaguard.model.MedicamentoYberia;
import pe.farmaguard.util.SinonimosYberia;
import pe.farmaguard.util.TextoYberia;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class CampoBusquedaYberia extends JTextField {
    private static final int MAX_SUGERENCIAS_YBERIA = 6;

    private final List<MedicamentoYberia> catalogoYberia;
    private final DefaultListModel<MedicamentoYberia> sugerenciasYberia = new DefaultListModel<>();
    private final JList<MedicamentoYberia> listaYberia = new JList<>(sugerenciasYberia);
    private final JPopupMenu popupYberia = new JPopupMenu();
    private Consumer<MedicamentoYberia> alElegirYberia = m -> setTextoSinSugerir(m.getNombre());
    private boolean ajustandoYberia;

    public CampoBusquedaYberia(List<MedicamentoYberia> catalogoYberia, String ayuda) {
        this.catalogoYberia = catalogoYberia;
        putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, ayuda);
        putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, IconosYberia.buscar(16));
        putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
        setMargin(new Insets(8, 8, 8, 8));

        listaYberia.setCellRenderer(new RenderizadorMedicamentoYberia());
        listaYberia.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaYberia.setFocusable(false);
        listaYberia.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                int indice = listaYberia.locationToIndex(e.getPoint());
                if (indice >= 0) {
                    elegir(sugerenciasYberia.get(indice));
                }
            }
        });
        listaYberia.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int indice = listaYberia.locationToIndex(e.getPoint());
                if (indice >= 0) {
                    listaYberia.setSelectedIndex(indice);
                }
            }
        });

        JScrollPane scroll = new JScrollPane(listaYberia);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        popupYberia.add(scroll);
        popupYberia.setFocusable(false);
        popupYberia.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                programarSugerencias();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                programarSugerencias();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                programarSugerencias();
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!popupYberia.isVisible()) {
                    return;
                }
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_DOWN -> mover(1, e);
                    case KeyEvent.VK_UP -> mover(-1, e);
                    case KeyEvent.VK_ENTER -> {
                        if (listaYberia.getSelectedValue() != null) {
                            elegir(listaYberia.getSelectedValue());
                            e.consume();
                        }
                    }
                    case KeyEvent.VK_ESCAPE -> {
                        popupYberia.setVisible(false);
                        e.consume();
                    }
                    default -> {
                    }
                }
            }
        });
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                popupYberia.setVisible(false);
            }
        });
    }

    public void alElegir(Consumer<MedicamentoYberia> oyente) {
        this.alElegirYberia = oyente;
    }

    public void setAccionesFinales(JComponent... acciones) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        panel.setOpaque(false);
        for (JComponent accion : acciones) {
            panel.add(accion);
        }
        putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, panel);
    }

    public void setTextoSinSugerir(String texto) {
        ajustandoYberia = true;
        setText(texto);
        ajustandoYberia = false;
        popupYberia.setVisible(false);
    }

    @Override
    public void updateUI() {
        super.updateUI();
        if (popupYberia != null) {
            SwingUtilities.updateComponentTreeUI(popupYberia);
        }
    }

    private void programarSugerencias() {
        if (!ajustandoYberia) {
            SwingUtilities.invokeLater(this::actualizarSugerencias);
        }
    }

    private void actualizarSugerencias() {
        String consulta = TextoYberia.normalizar(getText());
        if (consulta.isEmpty() || !isShowing() || !isFocusOwner()) {
            popupYberia.setVisible(false);
            return;
        }
        sugerenciasYberia.clear();
        Set<String> terminos = SinonimosYberia.terminosDeBusqueda(consulta);
        catalogoYberia.stream()
                .filter(m -> SinonimosYberia.coincide(m.getNombre(), terminos)
                        || SinonimosYberia.coincide(m.getPrincipioActivo(), terminos))
                .sorted(Comparator.comparing((MedicamentoYberia m) -> terminos.stream()
                                .noneMatch(SinonimosYberia.canonico(m.getNombre())::startsWith))
                        .thenComparing(MedicamentoYberia::getNombre))
                .limit(MAX_SUGERENCIAS_YBERIA)
                .forEach(sugerenciasYberia::addElement);
        if (sugerenciasYberia.isEmpty()) {
            popupYberia.setVisible(false);
            return;
        }
        listaYberia.setSelectedIndex(0);
        listaYberia.setVisibleRowCount(sugerenciasYberia.size());
        Dimension tam = listaYberia.getPreferredScrollableViewportSize();
        listaYberia.getParent().getParent().setPreferredSize(new Dimension(getWidth() - 8, tam.height));
        if (popupYberia.isVisible()) {
            popupYberia.pack();
        } else {
            popupYberia.show(this, 0, getHeight() + 4);
        }
    }

    private void mover(int delta, KeyEvent e) {
        int indice = Math.floorMod(listaYberia.getSelectedIndex() + delta, sugerenciasYberia.size());
        listaYberia.setSelectedIndex(indice);
        listaYberia.ensureIndexIsVisible(indice);
        e.consume();
    }

    private void elegir(MedicamentoYberia medicamentoYberia) {
        popupYberia.setVisible(false);
        alElegirYberia.accept(medicamentoYberia);
    }
}
