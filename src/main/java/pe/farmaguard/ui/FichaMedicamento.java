package pe.farmaguard.ui;

import pe.farmaguard.model.MedicamentoYberia;
import pe.farmaguard.model.NivelRiesgo;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

class FichaMedicamento extends Tarjeta {

    FichaMedicamento(MedicamentoYberia m, boolean grande) {
        super(new BorderLayout(14, 0), Estilos::superficie, grande ? 24 : 18);

        if (!grande) {
            JPanel columnaAvatar = Estilos.transparente(new BorderLayout());
            columnaAvatar.add(new JLabel(Iconos.avatar(m.getNombre(), 38)), BorderLayout.NORTH);
            add(columnaAvatar, BorderLayout.WEST);
        }

        PanelVertical columna = new PanelVertical();
        columna.agregar(Estilos.etiqueta(m.getNombre(), grande ? "h1" : "h3"));
        columna.espacio(2);
        columna.agregar(Estilos.parrafo(m.getPrincipioActivo() + " · " + m.getConcentracion(), Estilos::textoSecundario));
        columna.espacio(10);

        JPanel insignias = Estilos.transparente(new FlowLayout(FlowLayout.LEFT, 0, 0));
        insignias.add(new Insignia(m.getCategoria(), Estilos::acentoSuave, Estilos::acento));
        insignias.add(Box.createHorizontalStrut(6));
        insignias.add(new Insignia(m.getMomentoComida().getEtiqueta(), Iconos.reloj(12),
                Estilos::superficieSuave, Estilos::textoPrincipal));
        if (m.isRequiereReceta()) {
            insignias.add(Box.createHorizontalStrut(6));
            insignias.add(new Insignia("Requiere receta",
                    () -> Estilos.fondoNivel(NivelRiesgo.PELIGRO), () -> Estilos.textoNivel(NivelRiesgo.PELIGRO)));
        }
        columna.agregar(insignias);

        bloque(columna, "¿Para qué sirve?", m.getDescripcion());
        bloque(columna, "Cómo se toma", m.getDosisSugerida());
        bloque(columna, "Consejos", m.getConsejos());
        add(columna, BorderLayout.CENTER);
    }

    private static void bloque(PanelVertical columna, String titulo, String contenido) {
        if (contenido == null || contenido.isBlank()) {
            return;
        }
        columna.espacio(14);
        columna.agregar(Estilos.etiqueta(titulo, "h4"));
        columna.espacio(3);
        columna.agregar(Estilos.parrafo(contenido, Estilos::textoPrincipal));
    }
}
