package pe.farmaguard.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@DependsOnDatabaseInitialization
public class MigracionBaseDatos {

    private static final Logger LOG = LoggerFactory.getLogger(MigracionBaseDatos.class);

    public MigracionBaseDatos(JdbcTemplate jdbc) {
        agregarColumnaSiFalta(jdbc, "recordatorios", "intervalo_dias",
                "INTEGER NOT NULL DEFAULT 1 CHECK (intervalo_dias BETWEEN 1 AND 30)");
    }

    private static void agregarColumnaSiFalta(JdbcTemplate jdbc, String tabla, String columna, String definicion) {
        List<String> columnas = jdbc.query("PRAGMA table_info(" + tabla + ")", (fila, i) -> fila.getString("name"));
        if (!columnas.contains(columna)) {
            jdbc.execute("ALTER TABLE " + tabla + " ADD COLUMN " + columna + " " + definicion);
            LOG.info("Base de datos actualizada: columna {}.{} agregada", tabla, columna);
        }
    }
}
