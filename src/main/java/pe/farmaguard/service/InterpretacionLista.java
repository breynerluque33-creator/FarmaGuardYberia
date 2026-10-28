package pe.farmaguard.service;

import pe.farmaguard.model.MedicamentoYberia;

import java.util.List;

public record InterpretacionLista(List<MedicamentoYberia> encontrados, List<String> noReconocidos) {
}
