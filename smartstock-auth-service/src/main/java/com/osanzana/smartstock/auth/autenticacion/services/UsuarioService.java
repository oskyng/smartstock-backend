package com.osanzana.smartstock.auth.autenticacion.services;

import com.osanzana.smartstock.auth.shared.dto.request.UsuarioRequestDTO;
import com.osanzana.smartstock.auth.shared.dto.response.UsuarioResponseDTO;

public interface UsuarioService {
    UsuarioResponseDTO crearUsuario(UsuarioRequestDTO request, Long idComercioContexto, String rolSolicitante);
}
