package pe.farmaguard.service;

import pe.farmaguard.model.MedicamentoYberia;

import java.util.List;

public record InterpretacionListaYberia(List<MedicamentoYberia> encontradosYberia, List<String> noReconocidosYberia) {
}
