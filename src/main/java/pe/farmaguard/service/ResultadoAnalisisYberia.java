package pe.farmaguard.service;

import pe.farmaguard.model.NivelRiesgoYberia;

import java.util.List;

public record ResultadoAnalisisYberia(List<ItemCompraYberia> itemsYberia, List<AlertaYberia> alertasYberia, NivelRiesgoYberia nivelGeneralYberia) {
}
