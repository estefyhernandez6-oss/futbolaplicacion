package com.sofia.multisport.data.models

import java.util.UUID

// 1. EL FILTRO MAESTRO: Estructura base de cada Deporte
data class Deporte(
    val id: String = "",       // "futbol", "basquet", "ecuavoley"
    val nombre: String = "",   // "Fútbol", "Básquetbol"
    val icono: String = ""     // "⚽", "🏀"
)

// 2. EL CONTENEDOR ANUAL: El campeonato en curso o histórico
data class Campeonato(
    val id: String = UUID.randomUUID().toString(),
    val nombre: String = "",
    val anio: Int = 2026,
    val categoriaId: String = "sub_12", // "sub_8", "sub_10", "sub_12", "libre"
    val estado: String = "Inscripción"
)

// 3. EL NÚCLEO DE LA PLANTILLA: Jugadores globales (Reutilizables entre torneos)
data class Jugador(
    val id: String = UUID.randomUUID().toString(),
    val nombre: String = "",
    val cedula: String = "",
    val edad: Int = 0,
    val dorsal: Int = 0,
    val equipoId: String = "",
    // Añadimos campos de control de sanciones para el torneo actual
    val tarjetasAmarillasActivas: Int = 0,
    val tarjetasRojasActivas: Int = 0,
    val estaSuspendido: Boolean = false, // 🟥 El interruptor maestro de seguridad
    // Baja logica. `estaSuspendido` es una sancion temporal del arbitro; `activo = false`
    // es la baja definitiva de la plantilla. No se borra el documento para no dejar
    // huerfanas las filas de `goleadores`, que referencian jugadorId.
    val activo: Boolean = true
)

// 4. EL CLUB: Identidad del equipo
data class Equipo(
    val id: String = UUID.randomUUID().toString(),
    val nombre: String = "",
    val logotipoUrl: String = "",
    val colorPrincipal: String = "#FFFFFF",
    val categoriaId: String = "sub_12", // Ahora los equipos pertenecen a una categorí
    val representante: String = "",
    val estado: String = ""
)

// 5. EL RADAR EN VIVO: Estructura de un partido con su Línea de Tiempo y Links
data class Partido(
    val id: String = UUID.randomUUID().toString(),
    val deporteId: String = "futbol",
    val campeonatoId: String = "copa_2024", // Campo para agrupar por torneo
    val categoriaId: String = "sub_15",
    val equipoLocalId: String = "",
    val equipoLocalNombre: String = "",
    val equipoVisitanteId: String = "",
    val equipoVisitanteNombre: String = "",
    val puntuacionLocal: Int = 0,
    val puntuacionVisitante: Int = 0,
    val estado: String = "Programado",
    val fechaHora: Long = System.currentTimeMillis(),
    val fechaString: String = "", // Para mostrar dd/mm
    val horaString: String = "",  // Para mostrar hh:mm
    val cancha: String = "Cancha Principal",
    val linkTransmision: String = "",
    val eventoReciente: String = "Partido por jugarse",
    val notaOficial: String = "", // Campo para retrasos, avisos o cambios especiales
    val lineaDeTimeline: List<EventoPartido> = emptyList()
)

// 6. LOS SUCESOS DEL MINUTO A MINUTO: Goles, triples, faltas o tarjetas
data class EventoPartido(
    val id: String = UUID.randomUUID().toString(),
    val minuto: Int = 0,
    val tipo: String = "",       // "GOL", "TRIPLE", "TARJETA_AMARILLA", "FALTA_PERSONAL"
    val jugadorNombre: String = "",
    val equipoId: String = ""    // Para saber a quién sumarle el suceso
)

// 7. EL HISTORIAL DE COMPETICIÓN: Tabla de posiciones por Categoría
data class FilaPosiciones(
    val equipoId: String = "",
    val equipoNombre: String = "",
    val partidosJugados: Int = 0,
    val partidosGanados: Int = 0,
    val partidosEmpatados: Int = 0, // En Básquet este se quedará en 0 siempre
    val partidosPerdidos: Int = 0,
    val puntosAFavor: Int = 0,      // Goles a favor o puntos anotados
    val puntosEnContra: Int = 0,    // Goles en contra o puntos recibidos
    val puntosTotales: Int = 0      // 3 por ganar en fútbol, 2 en básquet, etc.
)

// 8. EL CONTENIDO COMUNITARIO: El módulo del Blog / Noticias

data class Noticia(
    val id: String = "",
    val titulo: String = "",
    val contenido: String = "",
    val deporteId: String = "",
    val fecha: Long = 0L
)

data class Publicidad(
    val id: String = "",
    val titulo: String = "",    // Nombre del negocio (ej: "Pizzería Ballenita")
    val imagenUrl: String = "", // Enlace directo del archivo guardado en Firebase Storage
    val descripcion: String = "", // Campo adicional detectado en Firestore
    val linkEnlace: String = ""
)

data class FilaPosicion(
    val equipoId: String = "",
    val equipoNombre: String = "",
    val deporteId: String = "",
    val campeonatoId: String = "",
    val partidosJugados: Int = 0,
    val partidosGanados: Int = 0,
    val partidosEmpatados: Int = 0,
    val partidosPerdidos: Int = 0,
    val golesFavor: Int = 0,
    val golesContra: Int = 0,
    val golDiferencia: Int = 0,
    val puntos: Int = 0,
    val historial: List<String> = emptyList() // "G" (Ganado), "E" (Empatado), "P" (Perdido)
)

data class FilaGoleador(
    val jugadorId: String = "",
    val jugadorNombre: String = "",
    val equipoId: String = "",
    val equipoNombre: String = "",
    val deporteId: String = "",
    val campeonatoId: String = "",
    val goles: Int = 0
)


data class UsuarioApp(
    val id: String = "",
    val nombre: String = "",
    val correo: String = "",
    val equipoFavoritoId: String = "",
    val equipoFavoritoNombre: String = "",
    val esRepresentanteEquipo: Boolean = false // Si quiere registrar o administrar un equipo
)