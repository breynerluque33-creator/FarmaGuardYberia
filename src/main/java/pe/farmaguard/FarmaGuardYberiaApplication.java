package pe.farmaguard;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import pe.farmaguard.service.CompatibilidadYberiaService;
import pe.farmaguard.service.MedicamentoYberiaService;
import pe.farmaguard.service.RecordatorioYberiaService;
import pe.farmaguard.ui.Estilos;
import pe.farmaguard.ui.MainFrameYberia;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
public class FarmaGuardYberiaApplication {

    public static void main(String[] args) {
        Estilos.instalarTema();

        ConfigurableApplicationContext contextoYberia;
        try {
            prepararCarpetaDeDatos();
            contextoYberia = new SpringApplicationBuilder(FarmaGuardYberiaApplication.class)
                    .headless(false)
                    .web(WebApplicationType.NONE)
                    .run(args);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "No se pudo iniciar FarmaGuard:\n" + e.getMessage(),
                    "FarmaGuard", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
            return;
        }

        SwingUtilities.invokeLater(() -> new MainFrameYberia(
                contextoYberia.getBean(MedicamentoYberiaService.class),
                contextoYberia.getBean(CompatibilidadYberiaService.class),
                contextoYberia.getBean(RecordatorioYberiaService.class)).setVisible(true));
    }

    private static void prepararCarpetaDeDatos() throws IOException {
        Path carpeta = Path.of(System.getProperty("user.home"), ".farmaguard");
        Files.createDirectories(carpeta);
        System.setProperty("farmaguard.home", carpeta.toString().replace('\\', '/'));
    }
}
