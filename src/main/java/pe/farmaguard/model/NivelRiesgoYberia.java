package pe.farmaguard.model;

public enum NivelRiesgoYberia {
    SEGURO("Seguro"),
    PRECAUCION("Precaución"),
    PELIGRO("Incompatible / Venta redundante");

    private final String etiquetaYberia;

    NivelRiesgoYberia(String etiquetaYberia) {
        this.etiquetaYberia = etiquetaYberia;
    }

    public String getEtiqueta() {
        return etiquetaYberia;
    }
}
