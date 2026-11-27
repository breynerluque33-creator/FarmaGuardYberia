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
    @Column(name = "id")
    private Long idYberia;

    @Column(name = "medicamento", nullable = false)
    private String medicamentoYberia;

    @Column(name = "frecuencia_horas", nullable = false)
    private int frecuenciaHorasYberia;

    @Column(name = "duracion_dias", nullable = false)
    private int duracionDiasYberia;

    @Column(name = "intervalo_dias", nullable = false)
    private int intervaloDiasYberia = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "momento_comida", nullable = false)
    private MomentoComidaYberia momentoComidaYberia;

    @Column(name = "hora_inicio", nullable = false)
    private String horaInicioYberia;

    @Column(name = "fecha_inicio", nullable = false)
    private String fechaInicioYberia;

    @Column(name = "notas")
    private String notasYberia;

    @Column(name = "horas_fijas")
    private String horasFijasYberia;

    @Column(name = "activo", nullable = false)
    private boolean activoYberia = true;

    protected RecordatorioYberia() {
    }

    public RecordatorioYberia(String medicamentoYberia, int frecuenciaHorasYberia, int duracionDiasYberia, MomentoComidaYberia momentoComidaYberia,
                        String horaInicioYberia, String fechaInicioYberia, String notasYberia) {
        this.medicamentoYberia = medicamentoYberia;
        this.frecuenciaHorasYberia = frecuenciaHorasYberia;
        this.duracionDiasYberia = duracionDiasYberia;
        this.momentoComidaYberia = momentoComidaYberia;
        this.horaInicioYberia = horaInicioYberia;
        this.fechaInicioYberia = fechaInicioYberia;
        this.notasYberia = notasYberia;
    }

    public static RecordatorioYberia cadaDias(String medicamentoYberia, int intervaloDiasYberia, int duracionDiasYberia,
                                        MomentoComidaYberia momentoComidaYberia, String horaInicioYberia, String fechaInicioYberia,
                                        String notasYberia) {
        RecordatorioYberia recordatorioYberia = new RecordatorioYberia(medicamentoYberia, 24, duracionDiasYberia, momentoComidaYberia,
                horaInicioYberia, fechaInicioYberia, notasYberia);
        recordatorioYberia.intervaloDiasYberia = intervaloDiasYberia;
        return recordatorioYberia;
    }

    public void actualizarDesde(RecordatorioYberia datos) {
        this.medicamentoYberia = datos.medicamentoYberia;
        this.frecuenciaHorasYberia = datos.frecuenciaHorasYberia;
        this.duracionDiasYberia = datos.duracionDiasYberia;
        this.intervaloDiasYberia = datos.intervaloDiasYberia;
        this.momentoComidaYberia = datos.momentoComidaYberia;
        this.horaInicioYberia = datos.horaInicioYberia;
        this.fechaInicioYberia = datos.fechaInicioYberia;
        this.notasYberia = datos.notasYberia;
        this.horasFijasYberia = datos.horasFijasYberia;
    }

    public RecordatorioYberia conHorasFijas(String horasFijasYberia) {
        this.horasFijasYberia = horasFijasYberia == null || horasFijasYberia.isBlank() ? null : horasFijasYberia;
        return this;
    }

    public String getHorasFijas() {
        return horasFijasYberia;
    }

    public boolean tieneHorasFijas() {
        return horasFijasYberia != null && !horasFijasYberia.isBlank();
    }

    public Long getId() {
        return idYberia;
    }

    public String getMedicamento() {
        return medicamentoYberia;
    }

    public int getFrecuenciaHoras() {
        return frecuenciaHorasYberia;
    }

    public int getDuracionDias() {
        return duracionDiasYberia;
    }

    public int getIntervaloDias() {
        return intervaloDiasYberia;
    }

    public boolean isSemanal() {
        return intervaloDiasYberia == 7;
    }

    public MomentoComidaYberia getMomentoComida() {
        return momentoComidaYberia;
    }

    public String getHoraInicio() {
        return horaInicioYberia;
    }

    public String getFechaInicio() {
        return fechaInicioYberia;
    }

    public String getNotas() {
        return notasYberia;
    }

    public boolean isActivo() {
        return activoYberia;
    }

    public void setActivo(boolean activoYberia) {
        this.activoYberia = activoYberia;
    }
}
