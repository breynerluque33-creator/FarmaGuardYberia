package pe.farmaguard.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "recordatorios")
public class RecordatorioYberia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String medicamento;

    @Column(name = "frecuencia_horas", nullable = false)
    private int frecuenciaHoras;

    @Column(name = "duracion_dias", nullable = false)
    private int duracionDias;

    @Column(name = "intervalo_dias", nullable = false)
    private int intervaloDias = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "momento_comida", nullable = false)
    private MomentoComida momentoComida;

    @Column(name = "hora_inicio", nullable = false)
    private String horaInicio;

    @Column(name = "fecha_inicio", nullable = false)
    private String fechaInicio;

    private String notas;

    @Column(nullable = false)
    private boolean activo = true;

    protected RecordatorioYberia() {
    }

    public RecordatorioYberia(String medicamento, int frecuenciaHoras, int duracionDias, MomentoComida momentoComida,
                        String horaInicio, String fechaInicio, String notas) {
        this.medicamento = medicamento;
        this.frecuenciaHoras = frecuenciaHoras;
        this.duracionDias = duracionDias;
        this.momentoComida = momentoComida;
        this.horaInicio = horaInicio;
        this.fechaInicio = fechaInicio;
        this.notas = notas;
    }

    public static RecordatorioYberia cadaDias(String medicamento, int intervaloDias, int duracionDias,
                                        MomentoComida momentoComida, String horaInicio, String fechaInicio,
                                        String notas) {
        RecordatorioYberia recordatorioYberia = new RecordatorioYberia(medicamento, 24, duracionDias, momentoComida,
                horaInicio, fechaInicio, notas);
        recordatorioYberia.intervaloDias = intervaloDias;
        return recordatorioYberia;
    }

    public void actualizarDesde(RecordatorioYberia datos) {
        this.medicamento = datos.medicamento;
        this.frecuenciaHoras = datos.frecuenciaHoras;
        this.duracionDias = datos.duracionDias;
        this.intervaloDias = datos.intervaloDias;
        this.momentoComida = datos.momentoComida;
        this.horaInicio = datos.horaInicio;
        this.fechaInicio = datos.fechaInicio;
        this.notas = datos.notas;
    }

    public Long getId() {
        return id;
    }

    public String getMedicamento() {
        return medicamento;
    }

    public int getFrecuenciaHoras() {
        return frecuenciaHoras;
    }

    public int getDuracionDias() {
        return duracionDias;
    }

    public int getIntervaloDias() {
        return intervaloDias;
    }

    public boolean isSemanal() {
        return intervaloDias == 7;
    }

    public MomentoComida getMomentoComida() {
        return momentoComida;
    }

    public String getHoraInicio() {
        return horaInicio;
    }

    public String getFechaInicio() {
        return fechaInicio;
    }

    public String getNotas() {
        return notas;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
