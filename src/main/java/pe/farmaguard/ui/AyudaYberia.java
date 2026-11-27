package pe.farmaguard.ui;

import pe.farmaguard.model.NivelRiesgoYberia;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.prefs.Preferences;

public final class AyudaYberia {
    private static final Preferences PREFERENCIAS_YBERIA = Preferences.userNodeForPackage(AyudaYberia.class);
    private static final String CLAVE_GUIA_VISTA_YBERIA = "guiaVista";

    private record PasoYberia(Icon iconoYberia, String tituloYberia, String textoYberia) {
    }

    private static final List<PasoYberia> PASOS_YBERIA = List.of(
            new PasoYberia(IconosYberia.logo(56), "Bienvenido a FarmaGuard",
                    "FarmaGuard te ayuda a comprar medicamentos de forma informada: te dice si lo que te ofrecen en la "
                            + "botica está repetido con otro nombre, si hay combinaciones peligrosas o si la dosis diaria "
                            + "es demasiado alta.\n\nFunciona sin internet y tus datos se guardan solo en tu computadora.\n\n"
                            + "Esta guía tiene 6 pasos. Puedes volver a verla cuando quieras con el botón «?» de arriba a la derecha."),
            new PasoYberia(IconosYberia.nivel(NivelRiesgoYberia.SEGURO, 56), "1. Analizar compra",
                    "Escribe el nombre de un medicamento (marca, genérico u otro nombre), elígelo de la lista y pulsa "
                            + "«Agregar». Repite con los demás.\n\nTambién puedes pegar una receta o lista completa con "
                            + "«Pegar una lista o receta».\n\nA la derecha verás el resultado:\n"
                            + "•  Verde: no encontramos problemas.\n"
                            + "•  Amarillo: precaución, pregunta antes de comprar o tomar.\n"
                            + "•  Rojo: no deberían tomarse juntos o estás pagando dos veces por lo mismo.\n\n"
                            + "Con los botones + y − indicas cuántas unidades tomarás al día, para revisar la dosis máxima."),
            new PasoYberia(IconosYberia.buscar(56), "2. Medicamentos",
                    "Aquí está el catálogo con la ficha de cada medicamento: para qué sirve, cómo se toma, consejos y "
                            + "con qué no debes combinarlo.\n\nPuedes buscar por marca (Panadol, Dr. Flu, Xanax), por "
                            + "genérico o por otro nombre del mismo componente: «clorfeniramina», «maleato de "
                            + "clorfenamina» y «clorfenamina» muestran lo mismo.\n\nLas marcas con varios componentes "
                            + "(como los antigripales) muestran todos sus componentes de una vez."),
            new PasoYberia(IconosYberia.reloj(56), "3. Recordatorios",
                    "Pulsa «Nuevo recordatorio» para crear un aviso: elige el medicamento, si va antes, durante o después "
                            + "de comer, cada cuánto y por cuántos días.\n\nSi el medicamento va con las comidas, "
                            + "FarmaGuard pone las tomas junto al desayuno, almuerzo y cena.\n\nDesde «Analizar compra» "
                            + "puedes crear todos los recordatorios de tu lista de una vez.\n\nCon el interruptor pausas un "
                            + "recordatorio y con el lápiz lo editas. Los avisos aparecen mientras FarmaGuard esté abierto."),
            new PasoYberia(IconosYberia.info(56), "4. Botones de arriba a la derecha",
                    "•  i  Aviso importante: qué puede y qué no puede hacer FarmaGuard.\n"
                            + "•  ?  Esta guía de uso.\n"
                            + "•  Luna / sol: cambia entre tema claro y oscuro."),
            new PasoYberia(IconosYberia.nivel(NivelRiesgoYberia.PRECAUCION, 56), "5. Importante",
                    "FarmaGuard es una guía de compra informada. No reemplaza la opinión de un médico certificado ni "
                            + "de un químico farmacéutico.\n\nSi tienes dudas, estás embarazada, das de lactar, es para un "
                            + "niño o tienes una enfermedad crónica, consulta siempre antes de tomar un medicamento.\n\n"
                            + "En una emergencia acude al centro de salud más cercano o llama al 106 (SAMU)."));

    private AyudaYberia() {
    }

    public static void mostrarGuiaSiEsPrimeraVez(Window dueno) {
        boolean vista;
        try {
            vista = PREFERENCIAS_YBERIA.getBoolean(CLAVE_GUIA_VISTA_YBERIA, false);
        } catch (RuntimeException e) {
            vista = true;
        }
        if (!vista) {
            mostrarGuia(dueno);
        }
    }

    public static void mostrarGuia(Window dueno) {
        JDialog dialogo = dialogo(dueno, "Guía de uso de FarmaGuard");

        CardLayout tarjetas = new CardLayout();
        JPanel paginas = EstilosYberia.transparente(tarjetas);
        for (int i = 0; i < PASOS_YBERIA.size(); i++) {
            paginas.add(pagina(PASOS_YBERIA.get(i)), String.valueOf(i));
        }

        JLabel progreso = EstilosYberia.etiqueta(" ", "small", EstilosYberia::textoSecundario);
        JButton saltar = EstilosYberia.botonSecundario("Saltar guía");
        JButton anterior = EstilosYberia.botonSecundario("Anterior");
        BotonPrimarioYberia siguiente = new BotonPrimarioYberia("Siguiente", null);
        int[] actual = {0};
        Runnable actualizar = () -> {
            tarjetas.show(paginas, String.valueOf(actual[0]));
            progreso.setText("Paso " + (actual[0] + 1) + " de " + PASOS_YBERIA.size());
            anterior.setEnabled(actual[0] > 0);
            boolean ultimo = actual[0] == PASOS_YBERIA.size() - 1;
            siguiente.setText(ultimo ? "Empezar" : "Siguiente");
            saltar.setVisible(!ultimo);
        };
        Runnable terminar = () -> {
            try {
                PREFERENCIAS_YBERIA.putBoolean(CLAVE_GUIA_VISTA_YBERIA, true);
            } catch (RuntimeException ignorada) {
            }
            dialogo.dispose();
        };
        anterior.addActionListener(e -> {
            actual[0] = Math.max(0, actual[0] - 1);
            actualizar.run();
        });
        siguiente.addActionListener(e -> {
            if (actual[0] == PASOS_YBERIA.size() - 1) {
                terminar.run();
            } else {
                actual[0]++;
                actualizar.run();
            }
        });
        saltar.addActionListener(e -> terminar.run());
        dialogo.getRootPane().registerKeyboardAction(e -> terminar.run(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialogo.getRootPane().setDefaultButton(siguiente);

        JPanel botones = EstilosYberia.transparente(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.add(saltar);
        botones.add(anterior);
        botones.add(siguiente);
        JPanel pie = EstilosYberia.transparente(new BorderLayout());
        pie.setBorder(BorderFactory.createEmptyBorder(12, 4, 0, 0));
        pie.add(progreso, BorderLayout.WEST);
        pie.add(botones, BorderLayout.EAST);

        JPanel contenido = new JPanel(new BorderLayout());
        contenido.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        contenido.add(paginas, BorderLayout.CENTER);
        contenido.add(pie, BorderLayout.SOUTH);
        dialogo.setContentPane(contenido);

        actualizar.run();
        mostrar(dialogo, dueno, 620, 520);
    }

    public static void mostrarAviso(Window dueno) {
        JDialog dialogo = dialogo(dueno, "Aviso importante");

        PanelVerticalYberia columna = new PanelVerticalYberia();
        TarjetaYberia destacado = new TarjetaYberia(new BorderLayout(12, 0), () -> EstilosYberia.fondoNivel(NivelRiesgoYberia.PRECAUCION), 14);
        destacado.setBorde(null);
        JPanel columnaIcono = EstilosYberia.transparente(new BorderLayout());
        columnaIcono.add(new JLabel(IconosYberia.nivel(NivelRiesgoYberia.PRECAUCION, 26)), BorderLayout.NORTH);
        destacado.add(columnaIcono, BorderLayout.WEST);
        PanelVerticalYberia textoDestacado = new PanelVerticalYberia();
        textoDestacado.agregar(EstilosYberia.etiqueta("No reemplaza a tu médico", "h3", () -> EstilosYberia.textoNivel(NivelRiesgoYberia.PRECAUCION)));
        textoDestacado.espacio(4);
        textoDestacado.agregar(EstilosYberia.parrafo("FarmaGuard es una guía de compra informada. No diagnostica, no receta y "
                + "no reemplaza la opinión de un médico certificado ni de un químico farmacéutico.",
                () -> EstilosYberia.textoNivel(NivelRiesgoYberia.PRECAUCION)));
        destacado.add(textoDestacado, BorderLayout.CENTER);
        columna.agregar(destacado);

        seccion(columna, "Lo que debes saber",
                "•  La información es orientativa y pensada para adultos.\n"
                        + "•  Si es para un niño, estás embarazada, das de lactar, eres adulto mayor o tienes una enfermedad "
                        + "del riñón, hígado o corazón, consulta siempre antes de tomar cualquier medicamento.\n"
                        + "•  Que no aparezca una alerta no garantiza que la combinación sea segura: el catálogo no tiene "
                        + "todos los medicamentos ni todas las interacciones.\n"
                        + "•  Las composiciones de las marcas son referenciales. Verifica siempre el empaque y el registro "
                        + "sanitario (DIGEMID).\n"
                        + "•  No cambies, suspendas ni dupliques un tratamiento indicado por tu médico por lo que diga esta "
                        + "aplicación.");
        seccion(columna, "Sobre la aplicación",
                "•  Funciona sin internet. Tus recordatorios se guardan solo en tu computadora (carpeta .farmaguard).\n"
                        + "•  Los recordatorios avisan solo mientras FarmaGuard esté abierto.\n"
                        + "•  Los antibióticos y los productos marcados «Requiere receta» solo deben tomarse con receta médica.");
        seccion(columna, "En una emergencia",
                "Si hay dificultad para respirar, hinchazón de la cara o la lengua, dolor de pecho, desmayo o sospecha de "
                        + "intoxicación, acude de inmediato al centro de salud más cercano o llama al 106 (SAMU).");

        JButton guia = EstilosYberia.botonSecundario("Ver guía de uso");
        guia.addActionListener(e -> {
            dialogo.dispose();
            mostrarGuia(dueno);
        });
        BotonPrimarioYberia entendido = new BotonPrimarioYberia("Entendido", IconosYberia.check(16));
        entendido.addActionListener(e -> dialogo.dispose());
        dialogo.getRootPane().setDefaultButton(entendido);
        dialogo.getRootPane().registerKeyboardAction(e -> dialogo.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        JPanel botones = EstilosYberia.transparente(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.add(guia);
        botones.add(entendido);
        botones.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        JPanel contenido = new JPanel(new BorderLayout());
        contenido.setBorder(BorderFactory.createEmptyBorder(16, 18, 14, 18));
        JPanel tituloYberia = EstilosYberia.transparente(new BorderLayout(10, 0));
        tituloYberia.add(new JLabel(IconosYberia.info(26)), BorderLayout.WEST);
        tituloYberia.add(EstilosYberia.etiqueta("Aviso importante", "h2"), BorderLayout.CENTER);
        tituloYberia.setBorder(BorderFactory.createEmptyBorder(0, 4, 12, 0));
        contenido.add(tituloYberia, BorderLayout.NORTH);
        contenido.add(EstilosYberia.scroll(columna), BorderLayout.CENTER);
        contenido.add(botones, BorderLayout.SOUTH);
        dialogo.setContentPane(contenido);
        mostrar(dialogo, dueno, 600, 600);
    }

    private static JComponent pagina(PasoYberia paso) {
        PanelVerticalYberia columna = new PanelVerticalYberia();
        columna.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        columna.agregar(new JLabel(paso.iconoYberia()));
        columna.espacio(14);
        columna.agregar(EstilosYberia.etiqueta(paso.tituloYberia(), "h1"));
        columna.espacio(10);
        columna.agregar(EstilosYberia.parrafo(paso.textoYberia(), EstilosYberia::textoPrincipal));
        return EstilosYberia.scroll(columna);
    }

    private static void seccion(PanelVerticalYberia columna, String tituloYberia, String textoYberia) {
        columna.espacio(16);
        columna.agregar(EstilosYberia.etiqueta(tituloYberia, "h4"));
        columna.espacio(4);
        columna.agregar(EstilosYberia.parrafo(textoYberia, EstilosYberia::textoPrincipal));
    }

    private static JDialog dialogo(Window dueno, String tituloYberia) {
        JDialog dialogo = new JDialog(dueno, tituloYberia, Dialog.ModalityType.DOCUMENT_MODAL);
        dialogo.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        return dialogo;
    }

    private static void mostrar(JDialog dialogo, Window dueno, int ancho, int alto) {
        int altoMaximo = dueno == null ? alto : Math.max(360, Math.min(alto, dueno.getHeight() - 40));
        dialogo.setSize(ancho, altoMaximo);
        dialogo.setLocationRelativeTo(dueno);
        dialogo.setVisible(true);
    }
}
