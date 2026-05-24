# 📚 El Libro del Cuco

Proyecto final de 2º DAM — Aplicación Android nativa para gestión de lectura personal.

---

## ¿Qué es?

El Libro del Cuco es una app para llevar el control de tus lecturas, descubrir libros escaneando su ISBN, mantener una racha lectora diaria y compartir reseñas con otros usuarios.

---

## Funcionalidades

- **Biblioteca personal** — Añade libros buscando por título o escaneando el código de barras ISBN con la cámara. Organiza tus lecturas en cuatro estados: Pendiente, Leyendo, Leído y Quiero leer.
- **Progreso de lectura** — Registra en qué página vas y visualiza tu avance con una barra de progreso.
- **Gamificación** — Racha lectora diaria con notificaciones de recordatorio. Sistema de logros desbloqueables según libros leídos, géneros explorados y días consecutivos.
- **Estadísticas** — Gráficas de libros leídos por mes y distribución por géneros.
- **Comunidad** — Publica reseñas públicas con valoración. Ranking global de los libros mejor puntuados.
- **Noticias** — Actualidad literaria en tiempo real mediante The Guardian API.
- **Panel de administrador** — Gestión de usuarios, ranking global y sistema de baneo en tiempo real.

---

## Stack técnico

| Capa | Tecnología |
|---|---|
| Lenguaje | Java |
| Autenticación | Firebase Auth |
| Base de datos | Cloud Firestore |
| Networking | Volley |
| Imágenes | Picasso |
| UI | Material Design 3 |
| Gráficas | MPAndroidChart |
| Background | WorkManager |
| Escaneo ISBN | ML Kit Code Scanner |

---

## Estructura del proyecto

```
com.example.ellibrodelcuco
├── data
│   ├── callback        # Interfaces Callback y SnapshotCallback
│   └── repository      # AuthRepository, BibliotecaRepository, ComunidadRepository...
├── interfaz_usuario
│   ├── activities      # Login, Register, MainActivity, DetalleLibro, Admin, Onboarding
│   ├── adapters        # LibroAdapter, ResenaAdapter, NoticiasAdapter
│   └── fragments       # Perfil, BuscarLibros, Estadisticas, Noticias, Comunidad
├── modelo              # Libro, Resena, NoticiaExterna
├── utils               # RachaHelper, LogrosHelper, GoogleBooksParser
└── workers             # RachaWorker (notificaciones diarias)
```

---

## Requisitos

- Android 7.0 (API 24) o superior
- Conexión a Internet
- Cuenta de Google para Firebase

---

## Configuración

1. Clona el repositorio
2. Abre el proyecto en Android Studio
3. Añade tu propio `google-services.json` en `/app` (requiere proyecto Firebase propio)
4. Sustituye la API key de The Guardian en `strings.xml` (`guardian_api_key`)
5. Compila y ejecuta

---

## APIs externas

- [Google Books API](https://developers.google.com/books) — búsqueda de libros
- [The Guardian API](https://open-platform.theguardian.com) — noticias literarias

---

## Autor

José Manuel Palomo Zambrano — 2º DAM
