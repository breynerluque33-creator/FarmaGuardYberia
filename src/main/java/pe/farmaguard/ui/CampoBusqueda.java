package pe.farmaguard.ui;

import com.formdev.flatlaf.FlatClientProperties;
import pe.farmaguard.model.MedicamentoYberia;
import pe.farmaguard.util.Texto;

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
import java.util.function.Consumer;

public class CampoBusqueda extends JTextField {

    private static final int MAX_SUGERENCIAS = 6;

    private final List<MedicamentoYberia> catalogo;
    private final DefaultListModel<MedicamentoYberia> sugerencias = new DefaultListModel<>();
    private final JList<MedicamentoYberia> lista = new JList<>(sugerencias);
    private final JPopupMenu popup = new JPopupMenu();
    private Consumer<MedicamentoYberia> alElegir = m -> setTextoSinSugerir(m.getNombre());
    private boolean ajustando;

    public CampoBusqueda(List<MedicamentoYberia> catalogo, String ayuda) {
        this.catalogo = catalogo;
        putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, ayuda);
        putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, Iconos.buscar(16));
        putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
        setMargin(new Insets(8, 8, 8, 8));

        lista.setCellRenderer(new RenderizadorMedicamento());
        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.setFocusable(false);
        lista.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                int indice = lista.locationToIndex(e.getPoint());
                if (indice >= 0) {
                    elegir(sugerencias.get(indice));
                }
            }
        });
        lista.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int indice = lista.locationToIndex(e.getPoint());
                if (indice >= 0) {
                    lista.setSelectedIndex(indice);
                }
            }
        });

        JScrollPane scroll = new JScrollPane(lista);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        popup.add(scroll);
        popup.setFocusable(false);
        popup.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

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
                if (!popup.isVisible()) {
                    return;
                }
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_DOWN -> mover(1, e);
                    case KeyEvent.VK_UP -> mover(-1, e);
                    case KeyEvent.VK_ENTER -> {
                        if (lista.getSelectedValue() != null) {
                            elegir(lista.getSelectedValue());
                            e.consume();
                        }
                    }
                    case KeyEvent.VK_ESCAPE -> {
                        popup.setVisible(false);
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
                popup.setVisible(false);
            }
        });
    }

    public void alElegir(Consumer<MedicamentoYberia> oyente) {
        this.alElegir = oyente;
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
        ajustando = true;
        setText(texto);
        ajustando = false;
        popup.setVisible(false);
    }

    @Override
    public void updateUI() {
        super.updateUI();
        if (popup != null) {
            SwingUtilities.updateComponentTreeUI(popup);
        }
    }

    private void programarSugerencias() {
        if (!ajustando) {
            SwingUtilities.invokeLater(this::actualizarSugerencias);
        }
    }

    private void actualizarSugerencias() {
        String consulta = Texto.normalizar(getText());
        if (consulta.isEmpty() || !isShowing() || !isFocusOwner()) {
            popup.setVisible(false);
            return;
        }
        sugerencias.clear();
        catalogo.stream()
                .filter(m -> Texto.normalizar(m.getNombre()).contains(consulta)
                        || Texto.normalizar(m.getPrincipioActivo()).contains(consulta))
                .sorted(Comparator.comparing((MedicamentoYberia m) -> !Texto.normalizar(m.getNombre()).startsWith(consulta))
                        .thenComparing(MedicamentoYberia::getNombre))
                .limit(MAX_SUGERENCIAS)
                .forEach(sugerencias::addElement);
        if (sugerencias.isEmpty()) {
            popup.setVisible(false);
            return;
        }
        lista.setSelectedIndex(0);
        lista.setVisibleRowCount(sugerencias.size());
        Dimension tam = lista.getPreferredScrollableViewportSize();
        lista.getParent().getParent().setPreferredSize(new Dimension(getWidth() - 8, tam.height));
        if (popup.isVisible()) {
            popup.pack();
        } else {
            popup.show(this, 0, getHeight() + 4);
        }
    }

    private void mover(int delta, KeyEvent e) {
        int indice = Math.floorMod(lista.getSelectedIndex() + delta, sugerencias.size());
        lista.setSelectedIndex(indice);
        lista.ensureIndexIsVisible(indice);
        e.consume();
    }

    private void elegir(MedicamentoYberia medicamentoYberia) {
        popup.setVisible(false);
        alElegir.accept(medicamentoYberia);
    }
}
