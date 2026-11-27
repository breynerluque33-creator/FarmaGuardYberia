package pe.farmaguard.ui;

import pe.farmaguard.model.MedicamentoYberia;
import pe.farmaguard.model.NivelRiesgoYberia;
import pe.farmaguard.util.SinonimosYberia;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

class FichaMedicamentoYberia extends TarjetaYberia {
    FichaMedicamentoYberia(MedicamentoYberia m, boolean grande) {
        super(new BorderLayout(14, 0), EstilosYberia::superficie, grande ? 24 : 18);

        if (!grande) {
            JPanel columnaAvatar = EstilosYberia.transparente(new BorderLayout());
            columnaAvatar.add(new JLabel(IconosYberia.avatar(m.getNombre(), 38)), BorderLayout.NORTH);
            add(columnaAvatar, BorderLayout.WEST);
        }

        PanelVerticalYberia columna = new PanelVerticalYberia();
        columna.agregar(EstilosYberia.etiqueta(m.getNombre(), grande ? "h1" : "h3"));
        columna.espacio(2);
        columna.agregar(EstilosYberia.parrafo(m.getPrincipioActivo() + " · " + m.getConcentracion(), EstilosYberia::textoSecundario));
        columna.espacio(10);

        JPanel insignias = EstilosYberia.transparente(new FlowLayout(FlowLayout.LEFT, 0, 0));
        insignias.add(new InsigniaYberia(m.getCategoria(), EstilosYberia::acentoSuave, EstilosYberia::acento));
        insignias.add(Box.createHorizontalStrut(6));
        insignias.add(new InsigniaYberia(m.getMomentoComida().getEtiqueta(), IconosYberia.reloj(12),
                EstilosYberia::superficieSuave, EstilosYberia::textoPrincipal));
        if (m.isRequiereReceta()) {
            insignias.add(Box.createHorizontalStrut(6));
            insignias.add(new InsigniaYberia("Requiere receta",
                    () -> EstilosYberia.fondoNivel(NivelRiesgoYberia.PELIGRO), () -> EstilosYberia.textoNivel(NivelRiesgoYberia.PELIGRO)));
        }
        columna.agregar(insignias);

        bloque(columna, "¿Para qué sirve?", m.getDescripcion());
        bloque(columna, "Cómo se toma", m.getDosisSugerida());
        bloque(columna, "Consejos", m.getConsejos());
        if (grande) {
            bloque(columna, "También se conoce como", nombresAlternativos(m, SinonimosYberia::otrosNombres));
            bloque(columna, "Otras marcas con el mismo componente", nombresAlternativos(m, SinonimosYberia::marcas));
        }
        add(columna, BorderLayout.CENTER);
    }

    private static String nombresAlternativos(MedicamentoYberia m, Function<String, List<String>> fuente) {
        boolean compuesto = m.getPrincipios().size() > 1;
        return m.getPrincipios().stream()
                .filter(p -> !fuente.apply(p).isEmpty())
                .map(p -> (compuesto ? p + ": " : "") + String.join(", ", fuente.apply(p)))
                .collect(Collectors.joining(compuesto ? "\n" : ""));
    }

    private static void bloque(PanelVerticalYberia columna, String titulo, String contenido) {
        if (contenido == null || contenido.isBlank()) {
            return;
        }
        columna.espacio(14);
        columna.agregar(EstilosYberia.etiqueta(titulo, "h4"));
        columna.espacio(3);
        columna.agregar(EstilosYberia.parrafo(contenido, EstilosYberia::textoPrincipal));
    }
}
