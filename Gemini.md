# Contexto de Proyecto: Chambita

Este es un marketplace móvil hiperlocal de servicios técnicos que utiliza Cloud Firestore.

## Colecciones y Subcolecciones Raíz

1. **usuarios** (Colección raíz polimórfica)
   - ID Documento: UID del usuario (`usuarios/{uid}`).
   - Campos Clientes: nombreCompleto, correo, dni, telefono, rol ("cliente"), distritoResidencia, fechaNacimiento (timestamp), fotoPerfil, fechaRegistro (timestamp).
     * Subcolección `direcciones` (usuarios/{uid}/direcciones/{autoId}): alias, direccion, distrito, referencia, esPrincipal (boolean), fechaRegistro (timestamp).
     * Subcolección `metodos_pago` (usuarios/{uid}/metodos_pago/{autoId}): tipo, numeroAsociado, esPredeterminado (boolean), fechaRegistro (timestamp).
   - Campos Técnicos: nombreCompleto, correo, dni, telefono, rol ("tecnico"), disponible (boolean), distritoActivoHoy, especialidad, tarifaPorHora (double), descripcion, experienciaAnos (integer), promedioEstrellas (double), numeroResenas (integer), conteoTrabajos (integer), servicios (array), distritos (array), fotoPerfil, fechaRegistro (timestamp).
     * Subcolección `resenas` (usuarios/{uid}/resenas/{autoId}): clienteId, nombreCliente, calificacion (integer), comentario, recomienda (boolean), solicitudId, fechaRegistro (timestamp).

2. **solicitudes** (Colección raíz)
   - ID Documento: Auto-ID (`solicitudes/{autoId}`).
   - Campos: clienteId, nombreCliente, fotoCliente, tecnicoId (string o null), descripcionAveria, especialidadRequerida, direccionServicio, distritoServicio, estado, montoFinal (double), resenaDejada (boolean), fechaCreacion (timestamp), fechaServicioProgramado (timestamp).

3. **pagos** (Colección raíz)
   - ID Documento: Auto-ID (`pagos/{autoId}`).
   - Campos: clienteId, tecnicoId, solicitudId, monto (double), metodoUsado, estado, fechaRegistro (timestamp).

4. **chats** (Colección raíz)
   - ID Documento: `{clienteId}_{tecnicoId}`.
   - Campos: clienteId, tecnicoId, ultimoMensaje, fechaUltimoMensaje (timestamp).
   - Subcolección `mensajes` (chats/{chatId}/mensajes/{autoId}): remitenteId, texto, leido (boolean), fechaRegistro (timestamp).

5. **distritos** (Colección raíz)
   - ID Documento: Nombre del distrito (Ej: "Ventanilla").
   - Campos: nombre, codigoPostal, coordenadas (geopoint), distritosVecinos (array).
