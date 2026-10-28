package pe.farmaguard.service;

import pe.farmaguard.model.NivelRiesgo;

import java.util.List;

public record ResultadoAnalisis(List<ItemCompra> items, List<Alerta> alertas, NivelRiesgo nivelGeneral) {
}
