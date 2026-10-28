package pe.farmaguard.model;

public enum NivelRiesgo {
    SEGURO("Seguro"),
    PRECAUCION("Precaución"),
    PELIGRO("Incompatible / Venta redundante");

    private final String etiqueta;

    NivelRiesgo(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
