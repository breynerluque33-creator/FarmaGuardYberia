package pe.farmaguard.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import pe.farmaguard.util.Texto;

@Entity
@Table(name = "interacciones")
public class InteraccionYberia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "principio_a", nullable = false)
    private String principioA;

    @Column(name = "principio_b", nullable = false)
    private String principioB;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_riesgo", nullable = false)
    private NivelRiesgo nivelRiesgo;

    @Column(nullable = false)
    private String explicacion;

    protected InteraccionYberia() {
    }

    public boolean involucra(String principio1, String principio2) {
        return (Texto.iguales(principioA, principio1) && Texto.iguales(principioB, principio2))
                || (Texto.iguales(principioA, principio2) && Texto.iguales(principioB, principio1));
    }

    public Long getId() {
        return id;
    }

    public String getPrincipioA() {
        return principioA;
    }

    public String getPrincipioB() {
        return principioB;
    }

    public NivelRiesgo getNivelRiesgo() {
        return nivelRiesgo;
    }

    public String getExplicacion() {
        return explicacion;
    }
}
