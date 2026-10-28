package pe.farmaguard.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "medicamentos")
public class MedicamentoYberia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    @Column(name = "principio_activo", nullable = false)
    private String principioActivo;

    @Column(nullable = false)
    private String categoria;

    private String concentracion;

    @Column(name = "dosis_mg")
    private Double dosisMg;

    @Column(name = "dosis_max_unidad_mg")
    private Double dosisMaxUnidadMg;

    @Column(name = "dosis_max_diaria_mg")
    private Double dosisMaxDiariaMg;

    @Column(name = "unidades_dia_habitual", nullable = false)
    private Integer unidadesDiaHabitual;

    @Column(name = "dosis_sugerida")
    private String dosisSugerida;

    @Enumerated(EnumType.STRING)
    @Column(name = "momento_comida", nullable = false)
    private MomentoComida momentoComida;

    @Column(name = "requiere_receta", nullable = false)
    private boolean requiereReceta;

    @Column(nullable = false)
    private String descripcion;

    private String consejos;

    protected MedicamentoYberia() {
    }

    public List<String> getPrincipios() {
        return Arrays.stream(principioActivo.split("\\+"))
                .map(String::trim)
                .filter(p -> !p.isEmpty())
                .toList();
    }

    public String getPrincipioPrincipal() {
        return getPrincipios().get(0);
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getPrincipioActivo() {
        return principioActivo;
    }

    public String getCategoria() {
        return categoria;
    }

    public String getConcentracion() {
        return concentracion;
    }

    public Double getDosisMg() {
        return dosisMg;
    }

    public Double getDosisMaxUnidadMg() {
        return dosisMaxUnidadMg;
    }

    public Double getDosisMaxDiariaMg() {
        return dosisMaxDiariaMg;
    }

    public int getUnidadesDiaHabitual() {
        return unidadesDiaHabitual == null ? 1 : unidadesDiaHabitual;
    }

    public String getDosisSugerida() {
        return dosisSugerida;
    }

    public MomentoComida getMomentoComida() {
        return momentoComida == null ? MomentoComida.INDIFERENTE : momentoComida;
    }

    public boolean isRequiereReceta() {
        return requiereReceta;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getConsejos() {
        return consejos;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof MedicamentoYberia otro && id != null && Objects.equals(id, otro.id));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return nombre;
    }
}
