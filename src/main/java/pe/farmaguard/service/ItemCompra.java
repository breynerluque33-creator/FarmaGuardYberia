package pe.farmaguard.service;

import pe.farmaguard.model.MedicamentoYberia;

public record ItemCompra(MedicamentoYberia medicamento, int unidadesDia) {

    public static ItemCompra conDosisHabitual(MedicamentoYberia medicamentoYberia) {
        return new ItemCompra(medicamentoYberia, medicamentoYberia.getUnidadesDiaHabitual());
    }
}
