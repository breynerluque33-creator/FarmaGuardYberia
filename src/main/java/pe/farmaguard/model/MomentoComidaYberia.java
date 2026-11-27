package pe.farmaguard.model;

public enum MomentoComidaYberia {
    ANTES("Antes de comer"),
    DURANTE("Durante la comida"),
    DESPUES("Después de comer"),
    INDIFERENTE("Con o sin comida");

    private final String etiquetaYberia;

    MomentoComidaYberia(String etiquetaYberia) {
        this.etiquetaYberia = etiquetaYberia;
    }

    public String getEtiqueta() {
        return etiquetaYberia;
    }

    @Override
    public String toString() {
        return etiquetaYberia;
    }
}
