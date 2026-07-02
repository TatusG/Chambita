# Especificación Técnica de Proyecto: Chambita

Este es un marketplace móvil hiperlocal de servicios técnicos en Lima Metropolitana y el Callao. Desarrollado en Android Studio con Kotlin.

**Información del Proyecto:**
- **Package Name:** `com.chambita.app`
- **Lenguaje:** Kotlin
- **Base de Datos Remota:** Cloud Firestore
- **Base de Datos Local:** Room (SQLite)
- **Google Maps API Key:** `AIzaSyC5LKsQXOQXMVoasT0BVhjekmjmPSzw_5o` (Guardada de forma segura en `local.properties` como `MAPS_API_KEY`)

---

## PARTE 1: ARQUITECTURA DE DATOS EN LA NUBE (Cloud Firestore)

Todas las colecciones siguen la nomenclatura *camelCase*.

### 1. Colección Raíz: `usuarios` (Polimórfica)
- **Document ID:** UID de Firebase Authentication (`usuarios/{uid}`).
- **Campos Compartidos:**
    - `nombreCompleto` *(string)*, `correo` *(string)*, `dni` *(string, 8 dígitos)*, `telefono` *(string)*, `rol` *(string: "cliente" o "tecnico")*, `fotoPerfil` *(string, URL)*, `fcmToken` *(string)*, `notificacionesHabilitadas` *(boolean)*, `fechaRegistro` *(timestamp)*.
- **Campos Exclusivos del Cliente:**
    - `distritoResidencia` *(string)*, `fechaNacimiento` *(timestamp)*.
- **Campos Exclusivos del Técnico:**
    - `disponible` *(boolean)*, `distritoActivoHoy` *(string)*, `especialidad` *(string)*, `tarifaPorHora` *(number - Double)*, `descripcion` *(string)*, `experienciaAnos` *(number - Integer)*, `promedioEstrellas` *(number - Double)*, `numeroResenas` *(number - Integer)*, `conteoTrabajos` *(number - Integer)*, `servicios` *(array)*, `distritos` *(array)*.

#### Subcolecciones de `usuarios`
- **`direcciones`** (`usuarios/{uid}/direcciones/{autoId}`) [Máx. 5 por cliente]:
    - `alias` *(string)*, `direccion` *(string)*, `distrito` *(string)*, `referencia` *(string)*, `esPrincipal` *(boolean)*, `fechaRegistro` *(timestamp)*.
- **`metodos_pago`** (`usuarios/{uid}/metodos_pago/{autoId}`):
    - `tipo` *(string)*, `numeroAsociado` *(string)*, `esPredeterminado` *(boolean)*, `fechaRegistro` *(timestamp)*.
- **`resenas`** (`usuarios/{uid}/resenas/{autoId}`):
    - `clienteId` *(string)*, `nombreCliente` *(string)*, `calificacion` *(number - Integer)*
    - `recomienda` *(boolean)*, `solicitudId` *(string)*, `fechaRegistro` *(timestamp)*.

### 2. Colección Raíz: `solicitudes`
- **Document ID:** Auto-ID (`solicitudes/{autoId}`).
- **Campos:**
  - `clienteId` *(string)*: UID del cliente solicitante.
  - `nombreCliente` *(string)*: Nombre desnormalizado para evitar lecturas extras en la Vista 18.
  - `fotoCliente` *(string)*: URL desnormalizada para la Vista 18.
  - `tecnicoId` *(string o null)*: UID del técnico o `null` si está pendiente de asignación.
  - `descripcionAveria` *(string)*: Detalle del problema.
  - `especialidadRequerida` *(string)*: Rubro solicitado (Ej. "Electricista").
  - `direccionServicio` *(string)*: Dirección física.
  - `distritoServicio` *(string)*: Distrito del servicio (coincide con el catálogo maestro).
  - `estado` *(string)*: Estados: `"pendiente"`, `"aceptada"`, `"en_curso"`, `"finalizada"`, `"cancelada"`.
  - `montoFinal` *(number - Double)*: Tarifa acordada transaccionada (inicialmente `0.00`).
  - `resenaDejada` *(boolean)*: `true` si el cliente ya calificó (bloquea segundas reseñas).
  - `fechaCreacion` *(timestamp)*: Fecha de creación.
  - `fechaServicioProgramado` *(timestamp)*: Fecha programada de la visita.

### 3. Colección Raíz: `pagos`
- **Document ID:** Auto-ID (`pagos/{autoId}`).
- **Campos:**
  - `clienteId` *(string)*: UID del cliente emisor [1].
  - `tecnicoId` *(string)*: UID del técnico receptor [1].
  - `solicitudId` *(string)*: ID de la solicitud vinculada.
  - `monto` *(number - Double)*: Total pagado por el servicio.
  - `metodoUsado` *(string)*: Ej. `"Yape"`, `"Plin"`, `"Efectivo"`.
  - `estado` *(string)*: Transacción (`"exitoso"`, `"pendiente"`, `"fallido"`).
  - `fechaRegistro` *(timestamp)*: Fecha y hora exacta de la transacción.

### 4. Colección Raíz: `chats`
- **Document ID:** Combinación única `{clienteId}_{tecnicoId}`.
- **Campos:**
  - `clienteId` *(string)*: UID del cliente participante.
  - `tecnicoId` *(string)*: UID del técnico participante.
  - `ultimoMensaje` *(string)*: Resumen del último mensaje para la lista de chats de la bandeja de entrada (Vista 21).
  - `fechaUltimoMensaje` *(timestamp)*: Fecha para ordenar los chats por antigüedad.

#### Subcolección de `chats`
- **`mensajes`** (`chats/{chatId}/mensajes/{autoId}`):
  - `remitenteId` *(string)*: UID del emisor del mensaje (sea cliente o técnico).
  - `texto` *(string)*: Contenido del mensaje de texto o URL de la imagen de Firebase Storage.
  - `leido` *(boolean)*: `true` si fue leído, `false` en caso contrario.
  - `fechaRegistro` *(timestamp)*: Fecha y hora exacta de envío.

### 5. Colección Raíz: `distritos` (Catálogo Maestro)
- **Document ID:** Nombre del distrito (Ej: `"Ventanilla"`, `"Surco"`).
- **Campos:**
  - `nombre` *(string)*: Nombre del distrito.
  - `codigoPostal` *(string)*: Código postal asignado.
  - `coordenadas` *(geopoint)*: Latitud y longitud de referencia (para el mapa estático de la Vista 4).
  - `distritosVecinos` *(array of strings)*: Distritos colindantes para sugerencias en la Vista 5.

---

## PARTE 2: BASE DE DATOS LOCAL (Room / SQLite)
- Persistencia de sesión e historial local para trabajo fuera de línea y velocidad instantánea en Splash Screen (Vista 1) y listados frecuentes.

### 1. Entidad: `UserSessionEntity` (Tabla `user_session`)
- `@PrimaryKey val uid: String`: UID de Firebase Auth.
- `val nombreCompleto: String`
- `val correo: String`
- `val rol: String`: `"cliente"` o `"tecnico"`.
- `val fotoPerfil: String?`
- `val fcmToken: String?`
- `val estaActivo: Boolean`: `true` si la sesión de Android está vigente.

### 2. Entidad: `LocalAddressEntity` (Tabla `local_addresses`)
- `@PrimaryKey val direccionId: String`
- `val clienteId: String`
- `val alias: String`: Ej: `"Casa"`, `"Trabajo"`.
- `val direccion: String`
- `val distrito: String`
- `val esPrincipal: Boolean`

---

## PARTE 3: MAPA DE LAS 22 VISTAS DEL SISTEMA (Lógica y UI)

### BLOQUE 1: AUTENTICACIÓN Y ACCESO

#### Vista 1: Splash Screen
- **UI:** Fondo azul, logotipo del martillo en escudo de geolocalización, eslogan.
- **Lógica:** Valida sesión activa localmente en `UserSessionEntity` (Room) o SharedPreferences. Si el campo `estaActivo` es `true`, enruta automáticamente a la pantalla de Inicio (Home Cliente o Home Técnico) según el `rol` del usuario sin llamar a la red. Si no hay sesión, redirige a la Vista 2 tras 2-3 segundos.

#### Vista 2: Login Chambita
- **UI:** Inputs de correo y contraseña (enmascarable), enlace de recuperación y botones de ingreso y registro.
- **Lógica:** Valida formato de correo localmente. Ejecuta inicio de sesión con Firebase Auth. Tras el éxito, lee el documento correspondiente en `usuarios/{uid}` para verificar el campo `rol` y enrutar el flujo a la Vista 4 o Vista 15.

#### Vista 3: Registro de Usuario
- **UI:** Inputs de Nombre completo, DNI (8 dígitos), Correo, Teléfono, Contraseña y Toggle exclusivo de Rol ("Cliente" o "Técnico").
- **Lógica:** Valida en tiempo real (con `TextWatcher`) la longitud del DNI y formato telefónico. Registra la cuenta en Firebase Auth y, tras crearse con éxito, genera el documento físico en `usuarios/{uid}` inicializando los campos de perfil correspondientes e insertando la sesión local en Room.

---
#### BLOQUE 2: MÓDULO DEL CLIENTE
#### Vista 4: Home Cliente

- **UI:** Menú hamburguesa, barra de búsqueda predictiva "Buscar en tu distrito...", chips de categorías ("Todos", "Electr.", "Gasfíter"), mapa estático, y RecyclerView vertical de técnicos destacados (foto, nombre, promedio de estrellas, distrito).
- **Lógica:** Al ingresar texto o seleccionar un distrito en la barra predictiva, ejecuta una consulta en la colección usuarios con filtro rol == "tecnico", disponible == true y utilizando la consulta .whereArrayContains("distritos", distritoBuscado) para actualizar la lista. Al hacer clic en un técnico, abre la Vista 6 pasando el UID del técnico seleccionado.

#### Vista 5: Sin Resultados
- **UI:** Cara triste, texto de contingencia, y RecyclerView de "Distritos vecinos" con cantidad de técnicos disponibles en cada uno.
- **Lógica:** Se activa cambiando la visibilidad del contenedor si la consulta en la Vista 4 devuelve 0 registros. Realiza una lectura del documento del distrito buscado en la colección distritos para leer el array distritosVecinos. Luego, realiza una consulta rápida por cada distrito vecino en la colección usuarios para mostrar la cantidad de técnicos que cubren esas zonas.

#### Vista 6: Vista del Cliente al Perfil Técnico

- **UI:** Ficha del técnico, tres tarjetas de resumen (trabajos realizados, estado "LIBRE"/ocupado, tarifa por hora), botón "CONTRATAR AHORA", botones de acción rápida de "LLAMAR" (llamada local) y "CHAT".
- **Lógica:** Lee en tiempo real (addSnapshotListener) el documento de usuarios/{tecnicoId}. Si el usuario presiona "CHAT", valida/crea el ID del canal persistente chats/{clienteId}_{tecnicoId} y redirige a la Vista 22. Si presiona "CONTRATAR AHORA", abre la Vista 7 pasando los datos del técnico.

#### Vista 7: Nueva Solicitud

- **UI:** Formulario de contratación (descripción del problema, Spinner de direcciones preguardadas, selector de fecha/hora, selector exclusivo de categoría, cuadro resumen de tarifa).
- **Lógica:** Carga las direcciones del cliente directamente desde la subcolección usuarios/{uid}/direcciones (o caché local de Room). Obliga a llenar la descripción y seleccionar una dirección antes de habilitar "CONFIRMAR SERVICIO". Al confirmar, inserta un documento en la colección raíz solicitudes con estado "pendiente" y tecnicoId correspondiente.

#### Vista 8: Contratos Cliente

- **UI:** TabLayout ("Activos" e "Historial"). RecyclerView de tarjetas de órdenes de servicio.
- **Lógica:** Consulta en tiempo real la colección raíz `solicitudes` filtrando por `clienteId == currentUid` de forma ordenada por `fechaCreacion` descendente [2]. En Kotlin, separa los datos: solicitudes con estado `"pendiente"`, `"aceptada"` o `"en_curso"` se muestran en la pestaña "Activos"; y las de estado `"finalizada"` o `"cancelada"` se muestran en "Historial".

#### Vista 9: Perfil Cliente
- **UI:** Avatar circular editable (lápiz), nombre, correo, rol. SwitchMaterial para "Notificaciones" y "Permitir GPS puntual". Lista de navegación: Editar perfil, Métodos de pago, Mis direcciones, Historial de pagos. Botón rojo "CERRAR SESIÓN".
- **Lógica:** Lee los datos de `UserSessionEntity` (Room) para mostrarlos al instante. Al presionar "CERRAR SESIÓN", borra la sesión local en Room (`estaActivo = false`), borra las credenciales en Firebase Auth y redirige al Login (Vista 2).

#### Vista 10: Editar Perfil Cliente
- **UI:** Avatar circular editable, inputs de Nombre completo, DNI (bloqueado), Correo, Teléfono, DatePicker para "Fecha de nacimiento", Spinner de "Distrito" residencial. Botón "GUARDAR CAMBIOS".
- **Lógica:** Carga datos de Room/Firestore. Al guardar, valida que todos los campos sean consistentes y actualiza en paralelo la colección `usuarios/{uid}` (cambiando `fechaNacimiento` como timestamp) y la caché local de Room.

#### Vista 11: Mis Direcciones
- **UI:** Lista vertical de direcciones grabadas (Casa, Trabajo, Otro), tag "PRINCIPAL", botón "Editar" y "+ Agregar dirección (máx. 5)". Mapa de previsualización.
- **Lógica:** Lee de la subcolección `usuarios/{uid}/direcciones` [3]. Mediante código en Kotlin, valida que el número de documentos existentes sea menor a 5 para habilitar el botón de adición. Guarda en Firestore y en la tabla `local_addresses` de Room.

#### Vista 12: Historial de Pagos
- **UI:** Contenedor superior oscuro con "Total gastado" monetario. Fila de meses de filtrado ("Junio", "Mayo", "Abril"). Lista vertical de transacciones.
- **Lógica:** Hace una consulta parametrizada a la colección raíz `pagos` donde `clienteId == currentUid` para acumular el total y listar los movimientos filtrados por mes [1].

#### Vista 13: Métodos de Pago
- **UI:** Tarjetas de logotipos vinculados (Yape, Plin, Efectivo), tag "PRINCIPAL" e icono de candado de seguridad. Opción "+ AGREGAR MÉTODO DE PAGO".
- **Lógica:** Lee y gestiona la subcolección `usuarios/{uid}/metodos_pago` [3]. Permite al cliente seleccionar cuál es el método predeterminado (`esPredeterminado = true` y cambia el anterior a `false`).

#### Vista 14: Dejar Reseña
- **UI:** Avatar e información del técnico evaluado, fecha de finalización. Barra de calificación (RatingBar) de 5 estrellas, caja de texto libre opcional para el "Comentario", y selector de pulgar arriba ("Sí") o pulgar abajo ("No"). Botón "ENVIAR RESEÑA".
- **Lógica:** Captura las estrellas, comentario y recomendación. Al enviar, escribe un documento en la subcolección `usuarios/{tecnicoId}/resenas` y en paralelo actualiza el campo `resenaDejada = true` en la colección raíz `solicitudes`. También actualiza los campos `promedioEstrellas` y `numeroResenas` del perfil del técnico en `usuarios/{tecnicoId}` mediante una transacción segura de Firestore para evitar condiciones de carrera.

---

### BLOQUE 3: MÓDULO DEL TÉCNICO

#### Vista 15: Home Técnico
- **UI:** Cabecera con foto e indicador de conexión. Switch "Estoy disponible", campo "ZONA ACTIVA HOY", tres métricas consolidadas (trabajos del mes, ingresos acumulados, promedio de estrellas). RecyclerView de "Solicitudes nuevas" con botones "ACEPTAR" (verde) o "RECHAZAR" (blanco).
- **Lógica:** El switch modifica el campo `disponible` en `usuarios/{tecnicoId}`. Si está activo (`true`), escucha en tiempo real (`addSnapshotListener`) la colección raíz `solicitudes` donde `distritoServicio` esté en su array `distritos` del técnico y el `estado == "pendiente"`. Al pulsar "ACEPTAR", cambia el estado a `"aceptada"`, asigna su UID a `tecnicoId` en la solicitud, y se abre la Vista 17.

#### Vista 16: Cobertura Laboral
- **UI:** Pestañas "Distritos" o "Radio km". Lista de 5 Spinners independientes ("DISTRITO 1", etc.) y botón "GUARDAR COBERTURA".
- **Lógica:** Captura los distritos elegidos en los Spinners, elimina duplicados o nulos mediante lógica en Kotlin, y actualiza el array `distritos` en `usuarios/{tecnicoId}`.

#### Vista 17: Gestión de Solicitudes / Mis Solicitudes
- **UI:** TabLayout de tres estados fijos ("PENDIENTES", "EN CURSO" y "FINALIZADOS"). RecyclerView de tarjetas de órdenes (foto del cliente, nombre, fecha, hora, tarifa).
- **Lógica:** Realiza una consulta en tiempo real a la colección raíz `solicitudes` filtrando por `tecnicoId == currentUid` ordenada por fecha de creación desc [2]. En Kotlin, los documentos son clasificados según el campo `estado`: las solicitudes con estado `"pendiente"` se muestran en la pestaña "PENDIENTES"; las de estado `"aceptada"` o `"en_curso"` en "EN CURSO"; y las de estado `"finalizada"` o `"cancelada"` en "FINALIZADOS".

#### Vista 18: Perfil Técnico / Mi Cuenta
- **UI:** Cabecera azul con avatar circular grande del profesional, nombre, correo electrónico y tag de verificación azul ("Técnico Verificado"). Lista de opciones clickables: Editar perfil técnico, Mis métodos de pago, Mis zonas de cobertura, "Mis ganancias". Botón "CERRAR SESIÓN".
- **Lógica:** Gestiona la navegación interna a los subformularios. Lee los datos de `UserSessionEntity` (Room) para mostrarlos de forma instantánea. Al pulsar "CERRAR SESIÓN", borra la sesión local en Room (`estaActivo = false`), desconecta en Firebase Auth y redirige al Login (Vista 2).

#### Vista 19: Editar Perfil Técnico / Mi Perfil Profesional
- **UI:** Cabecera con avatar, nombre, especialidad y barra de progreso de completitud del perfil. Inputs: Descripción profesional, años de experiencia, chips interactivos de subcategorías de servicios y inputs de tarifa por hora (Mínimo / Máximo). Botón "GUARDAR CAMBIOS".
- **Lógica:** Carga datos de Room/Firestore. Valida en Kotlin que el rango de tarifa horaria ingresado sea consistente (Mínimo <= Máximo) antes de actualizar el perfil. Al pulsar "GUARDAR CAMBIOS", actualiza en paralelo el documento del técnico en `usuarios/{uid}` y la caché local de Room.

#### Vista 20: Dashboard de Ganancias / Mis Ganancias
- **UI:** Contenedor superior oscuro con métrica "GANANCIA DEL DÍA". Tarjetas consolidadas "Ganado este mes" y "Servicios". Gráfico de barras de "Tendencia Semanal" e historial de "Movimientos Recientes".
- **Lógica:** Realiza una consulta con filtros de rango de fechas a la colección raíz `pagos` donde `tecnicoId == currentUid`. En Kotlin, se calcula dinámicamente el dinero cobrado en el día y en el mes de corte. Alimenta el gráfico de barras calculando los montos totales de los pagos recibidos en los últimos 7 días.

---

### BLOQUE 4: MÓDULO DE COMUNICACIÓN

#### Vista 21: Vista Chat / Bandeja de Mensajes
- **UI:** Buscador superior de chats por nombre o servicio. RecyclerView vertical de hilos de conversación activos (avatar, indicador de conexión, nombre, especialidad, previsualización, hora/fecha, check de lectura, globo de mensajes no leídos).
- **Lógica:** Escucha de manera activa (`addSnapshotListener`) las actualizaciones en la colección raíz `chats` donde `clienteId == currentUid` o `tecnicoId == currentUid`, ordenando las conversaciones de forma descendente basándose en el timestamp del campo `fechaUltimoMensaje`. En Kotlin, para cada elemento de la lista, consulta el perfil del interlocutor desde la colección `usuarios/{contactoId}` para renderizar su nombre y foto de perfil en tiempo real.

#### Vista 22: Chat Bilateral / Ventana de Chat Activo
- **UI:** Barra superior con botón de retorno, foto de perfil, nombre, especialidad, indicador de estado ("EN LÍNEA" en verde). Cuerpo central con scroll de conversación compuesto por burbujas de mensajes alineadas a la izquierda para el remitente externo (fondo blanco) y a la derecha para el usuario propio (fondo azul), marcador de tiempo (timestamp) y doble check de lectura. Barra de entrada inferior con botones para adjuntar archivos (+), enviar ubicación, campo de texto "Escribe un mensaje..." y botón circular azul para "Enviar".
- **Lógica:** Al pulsar el botón "Enviar", añade inmediatamente el mensaje con su timestamp y estado `leido = false` en la subcolección `mensajes` dentro del canal de chat correspondiente (`chats/{chatId}/mensajes/{autoId}`). En paralelo, actualiza los campos `ultimoMensaje` y `fechaUltimoMensaje` en el documento principal del chat para que la bandeja de entrada se ordene de forma instantánea. Las imágenes se procesan con Glide o Picasso, se suben a Firebase Storage, y se guarda su referencia pública en el campo `texto` del mensaje.