package pe.farmaguard.service;

import pe.farmaguard.model.MedicamentoYberia;

public record ItemCompraYberia(MedicamentoYberia medicamentoYberia, int unidadesDiaYberia) {
    public static ItemCompraYberia conDosisHabitual(MedicamentoYberia medicamentoYberia) {
        return new ItemCompraYberia(medicamentoYberia, medicamentoYberia.getUnidadesDiaHabitual());
    }
}
