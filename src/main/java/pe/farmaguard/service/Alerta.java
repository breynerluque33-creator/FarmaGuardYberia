package pe.farmaguard.service;

import pe.farmaguard.model.NivelRiesgo;

public record Alerta(NivelRiesgo nivel, String titulo, String detalle) {
}
