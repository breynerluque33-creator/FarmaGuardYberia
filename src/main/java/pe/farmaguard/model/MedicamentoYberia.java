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
    @Column(name = "id")
    private Long idYberia;

    @Column(name = "nombre", nullable = false, unique = true)
    private String nombreYberia;

    @Column(name = "principio_activo", nullable = false)
    private String principioActivoYberia;

    @Column(name = "categoria", nullable = false)
    private String categoriaYberia;

    @Column(name = "concentracion")
    private String concentracionYberia;

    @Column(name = "dosis_mg")
    private Double dosisMgYberia;

    @Column(name = "dosis_max_unidad_mg")
    private Double dosisMaxUnidadMgYberia;

    @Column(name = "dosis_max_diaria_mg")
    private Double dosisMaxDiariaMgYberia;

    @Column(name = "unidades_dia_habitual", nullable = false)
    private Integer unidadesDiaHabitualYberia;

    @Column(name = "dosis_sugerida")
    private String dosisSugeridaYberia;

    @Enumerated(EnumType.STRING)
    @Column(name = "momento_comida", nullable = false)
    private MomentoComidaYberia momentoComidaYberia;

    @Column(name = "requiere_receta", nullable = false)
    private boolean requiereRecetaYberia;

    @Column(name = "descripcion", nullable = false)
    private String descripcionYberia;

    @Column(name = "consejos")
    private String consejosYberia;

    protected MedicamentoYberia() {
    }

    public List<String> getPrincipios() {
        return Arrays.stream(principioActivoYberia.split("\\+"))
                .map(String::trim)
                .filter(p -> !p.isEmpty())
                .toList();
    }

    public String getPrincipioPrincipal() {
        return getPrincipios().get(0);
    }

    public Long getId() {
        return idYberia;
    }

    public String getNombre() {
        return nombreYberia;
    }

    public String getPrincipioActivo() {
        return principioActivoYberia;
    }

    public String getCategoria() {
        return categoriaYberia;
    }

    public String getConcentracion() {
        return concentracionYberia;
    }

    public Double getDosisMg() {
        return dosisMgYberia;
    }

    public Double getDosisMaxUnidadMg() {
        return dosisMaxUnidadMgYberia;
    }

    public Double getDosisMaxDiariaMg() {
        return dosisMaxDiariaMgYberia;
    }

    public int getUnidadesDiaHabitual() {
        return unidadesDiaHabitualYberia == null ? 1 : unidadesDiaHabitualYberia;
    }

    public String getDosisSugerida() {
        return dosisSugeridaYberia;
    }

    public MomentoComidaYberia getMomentoComida() {
        return momentoComidaYberia == null ? MomentoComidaYberia.INDIFERENTE : momentoComidaYberia;
    }

    public boolean isRequiereReceta() {
        return requiereRecetaYberia;
    }

    public String getDescripcion() {
        return descripcionYberia;
    }

    public String getConsejos() {
        return consejosYberia;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof MedicamentoYberia otro && idYberia != null && Objects.equals(idYberia, otro.idYberia));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(idYberia);
    }

    @Override
    public String toString() {
        return nombreYberia;
    }
}
