package pe.farmaguard.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import pe.farmaguard.util.TextoYberia;

@Entity
@Table(name = "interacciones")
public class InteraccionYberia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long idYberia;

    @Column(name = "principio_a", nullable = false)
    private String principioAYberia;

    @Column(name = "principio_b", nullable = false)
    private String principioBYberia;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_riesgo", nullable = false)
    private NivelRiesgoYberia nivelRiesgoYberia;

    @Column(name = "explicacion", nullable = false)
    private String explicacionYberia;

    protected InteraccionYberia() {
    }

    public boolean involucra(String principio1, String principio2) {
        return (TextoYberia.iguales(principioAYberia, principio1) && TextoYberia.iguales(principioBYberia, principio2))
                || (TextoYberia.iguales(principioAYberia, principio2) && TextoYberia.iguales(principioBYberia, principio1));
    }

    public Long getId() {
        return idYberia;
    }

    public String getPrincipioA() {
        return principioAYberia;
    }

    public String getPrincipioB() {
        return principioBYberia;
    }

    public NivelRiesgoYberia getNivelRiesgo() {
        return nivelRiesgoYberia;
    }

    public String getExplicacion() {
        return explicacionYberia;
    }
}
