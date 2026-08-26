# MultiSport Manager 🏆

MultiSport Manager es una aplicación Android moderna diseñada para la gestión y seguimiento de torneos deportivos en tiempo real (Fútbol, Básquetbol, Vóley). La aplicación permite a administradores, árbitros e hinchas interactuar con los eventos deportivos de manera eficiente.

## 🚀 Características Principales

### Para Hinchas (Público)
*   **Arena Live**: Visualización de partidos en vivo con marcadores actualizados al instante.
*   **Calendario**: Consulta de próximos encuentros programados.
*   **Estadísticas**: Tablas de posiciones detalladas y ranking de goleadores por campeonato y categoría.
*   **Blog de Novedades**: Noticias y avisos oficiales de los torneos.
*   **Transmisiones**: Enlaces directos a streaming de video cuando estén disponibles.

### Para Árbitros y Administradores
*   **Control en Vivo**: Interfaz dedicada para registrar goles, tarjetas y sucesos minuto a minuto.
*   **Gestión de Plantillas**: Administración de equipos y jugadores globales.
*   **Panel de Publicación**: Herramientas para lanzar noticias y banners publicitarios con subida de imágenes a Firebase Storage.
*   **Configuración de Torneos**: Creación de campeonatos, categorías y programación de fechas.

## 🛠️ Arquitectura Técnica

El proyecto ha sido refactorizado siguiendo una arquitectura limpia y modular para mejorar la mantenibilidad:

*   **UI (Jetpack Compose)**: Interfaz 100% declarativa y moderna.
    *   `ui/screens/`: Pantallas principales de la aplicación.
    *   `ui/components/`: Componentes visuales reutilizables.
*   **Data**:
    *   `data/models/`: Estructuras de datos (POJOs) para Firebase.
    *   `data/repository/`: Lógica de acceso a datos y gestión de Firebase Firestore.
*   **Services**:
    *   `service/`: Integración con Firebase Cloud Messaging (FCM) para notificaciones push.

## 🏗️ Stack Tecnológico

*   **Lenguaje**: Kotlin 2.2.10
*   **Framework UI**: Jetpack Compose con Material Design 3.
*   **Backend**: Firebase Suite.
    *   **Firestore**: Base de datos NoSQL en tiempo real.
    *   **Authentication**: Gestión de accesos para administradores y árbitros.
    *   **Storage**: Almacenamiento de banners publicitarios.
    *   **Cloud Messaging**: Notificaciones de inicio de partidos y noticias.
*   **Carga de Imágenes**: Coil-compose.

## 📦 Instalación y Configuración

1.  **Clonar el repositorio**:
    ```bash
    git clone [url-del-repositorio]
    ```
2.  **Configurar Firebase**:
    *   Descargar el archivo `google-services.json` desde la consola de Firebase.
    *   Colocarlo en la carpeta `app/` del proyecto.
3.  **Habilitar Servicios en Firebase Console**:
    *   **Firestore**: Crear base de datos en modo producción o prueba.
    *   **Authentication**: Habilitar método de Correo/Contraseña.
    *   **Storage**: Inicializar el bucket de almacenamiento (obligatorio para la subida de imágenes).
4.  **Compilar**: Abrir en Android Studio y ejecutar en un dispositivo o emulador (API 24+).

## 🔥 Puesta en marcha de la base de datos

Sin este paso la app parece funcionar pero **no guarda nada**: Firestore acepta la
escritura en la caché local del teléfono, la pantalla se actualiza, y el servidor la
rechaza sin que nadie se entere.

### 1. Desplegar reglas e índices

El repositorio incluye `firestore.rules`, `firestore.indexes.json` y `storage.rules`.
Con [Firebase CLI](https://firebase.google.com/docs/cli) instalado:

```bash
firebase login
firebase deploy --only firestore:rules,firestore:indexes,storage
```

Sin reglas desplegadas, una base creada en **modo producción** deniega toda escritura
(`allow read, write: if false`). Sin los índices, las consultas del home devuelven
`FAILED_PRECONDITION` y la pantalla sale vacía.

### 2. Habilitar Authentication

Consola de Firebase → Authentication → Sign-in method → **Correo/contraseña**.

Las reglas exigen sesión iniciada para escribir. Crea al menos un usuario y su
documento en `usuarios/{uid}` con el campo `rol` en `"admin"`.

### 3. Comprobar que efectivamente guarda

Con el dispositivo conectado:

```bash
adb logcat -s GUARDADO LECTURA
```

Toda escritura fallida y toda lectura denegada aparecen ahí con el motivo exacto
(`PERMISSION_DENIED`, `UNAVAILABLE`, `FAILED_PRECONDITION`…). Si el Logcat está limpio
y la consola de Firebase muestra el documento, quedó guardado de verdad.

### Colecciones que usa la app

| Colección | La escribe | La lee |
|---|---|---|
| `campeonatos` | Panel admin | Admin, tabla de posiciones, lista de árbitro |
| `equipos_globales` | Panel admin, registro de usuario | Casi todas las pantallas |
| `jugadores_globales` | Panel admin, arbitraje (sanciones), plantillas (baja) | Arbitraje, plantillas |
| `partidos_en_vivo` | Panel admin, arbitraje | Home, lista de árbitro |
| `goleadores` | Arbitraje (transacción del gol) | Tabla de goleadores |
| `tabla_posiciones` | Arbitraje (al cerrar el partido) | Tabla de posiciones |
| `noticias` | Gestor de contenido | Home |
| `publicidades` | Gestor de contenido | Home, posiciones, lista de árbitro |
| `usuarios` | Registro y panel admin | Login, panel admin |

## 📄 Notas de Versión
*   **v1.0**: Implementación de arquitectura limpia, corrección de errores de subida de imágenes y sistema de navegación optimizado.

---
Desarrollado por **Sofia** - *Tournament Management Solutions*
