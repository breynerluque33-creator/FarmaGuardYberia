package pe.farmaguard.model;

public enum MomentoComida {
    ANTES("Antes de comer"),
    DURANTE("Durante la comida"),
    DESPUES("Después de comer"),
    INDIFERENTE("Con o sin comida");

    private final String etiqueta;

    MomentoComida(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
