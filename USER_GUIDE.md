# Guía de Usuario y Arquitectura Offline-First — PuceMatch 📱✈️

PuceMatch es una aplicación móvil diseñada para conectar a los estudiantes de la PUCE bajo la filosofía **Offline-First**. Esto significa que la aplicación prioriza el funcionamiento local (sin conexión) para garantizar una experiencia de usuario rápida y resiliente, y sincroniza los datos en segundo plano cuando la conexión a internet está disponible.

---

## 🛠️ Stack Tecnológico e Integración: Room + Retrofit

La arquitectura del proyecto separa estrictamente la fuente de datos local de la remota, utilizando un **Repositorio Unificado** (`PuceMatchRepository.kt`) como mediador.

```
       ┌────────────────────────────────────────────────────────┐
       │                Capa de Presentación (UI)               │
       │        HomeScreen.kt / ChatDetailScreen.kt (Compose)   │
       └───────────────────────────┬────────────────────────────┘
                                   │ (Observa flujos de datos reactivos)
       ┌───────────────────────────▼────────────────────────────┐
       │                Capa de Negocio (ViewModels)            │
       │    HomeViewModel.kt / ChatDetailViewModel.kt (Flows)   │
       └───────────────────────────┬────────────────────────────┘
                                   │ (Invoca casos de uso / comandos)
       ┌───────────────────────────▼────────────────────────────┐
       │               Repositorio Unificado (Repository)       │
       │                  PuceMatchRepository.kt                │
       └───────────────────────────┬────────────────────────────┘
                                   │
                  ┌────────────────┴────────────────┐
                  │ (Lectura principal / Cache)     │ (Escritura y consulta asíncrona)
       ┌──────────▼──────────┐           ┌──────────▼──────────┐
       │  Base de Datos Room │           │   Cliente Retrofit  │
       │  (SQLite Local)     │           │   (Servidor Remoto) │
       └─────────────────────┘           └─────────────────────┘
```

1. **Room (Base de Datos Local)**:
   * Actúa como la **Única Fuente de Verdad (Single Source of Truth - SSOT)**.
   * La interfaz de usuario (Compose) **nunca** lee directamente de Retrofit; siempre observa flujos de datos reactivos (`Flow<List<T>>`) expuestos por Room.
   * Si la base de datos cambia, la UI se actualiza de manera reactiva instantáneamente (UDF).
2. **Retrofit (Servidor Remoto / API)**:
   * Se encarga del envío y recepción de datos con el servidor web externo.
   * Cuando el usuario realiza una acción (dar Like, enviar mensaje, registrarse), la app guarda los cambios localmente en Room primero, e intenta sincronizarlos con Retrofit.
   * Si la llamada a Retrofit falla, la app retiene los datos locales y la UI continúa funcionando con normalidad.

---

## 📶 Modos de Funcionamiento: Con y Sin Conexión

### 🟢 1. Funcionamiento Con Internet (Online)
Cuando el dispositivo tiene acceso a internet:
* **Registro de Usuario**: Al registrarse, el perfil se crea localmente y se sube de inmediato al servidor remoto (`POST /api/v1/students`). El ID de usuario se genera de forma determinista a partir del email (`java.util.UUID.nameUUIDFromBytes`), permitiendo iniciar sesión desde otros dispositivos.
* **Descarga del Catálogo**: Al abrir la pantalla de inicio, la app consulta el catálogo global remoto (`GET /api/v1/students`), fusiona los perfiles descargados con los locales (preservando el estado de matches) y los inserta en Room.
* **Likes y Coincidencias (Matches)**: Al deslizar a la derecha, se envía la acción al servidor remoto (`POST /api/v1/matches/like`). Si ambos usuarios se gustan mutuamente, el servidor responde confirmando la coincidencia (`isMatch = true`). La app actualiza localmente el perfil a `isMatched = true` y muestra un diálogo flotante modal instantáneo ("¡Es un Match! 🎉") con un acceso directo al chat.
* **Chats Reales**: La app realiza sondeos periódicos en segundo plano (cada 3 segundos) al endpoint `GET /api/v1/matches/chat/{matchId}` para descargar nuevos mensajes de la otra persona y guardarlos en Room.

### 🔴 2. Funcionamiento Sin Internet (Offline-First)
Cuando el dispositivo se queda sin señal o está en Modo Avión:
* **Persistencia de Datos**: El usuario puede seguir abriendo la aplicación, viendo el catálogo previamente sincronizado, y revisando el historial de sus chats guardados en la base de datos de Room.
* **Likes Offline**: Si el usuario da like a un perfil sin conexión, la app registra la acción. El match se procesará y consolidará en el servidor una vez se recupere la conexión de red.
* **Envío de Mensajes Resiliente**: Al enviar un mensaje, este se guarda inmediatamente en Room con una llave primaria UUID única, mostrándose de inmediato en la burbuja del chat. El envío a través de Retrofit falla silenciosamente en segundo plano, pero la UI retiene el mensaje.
* **Carga de Fotos Offline**: Para evitar que las URIs de fotos tomadas con la cámara expiren al reiniciar el celular, PuceMatch copia físicamente la foto al directorio de almacenamiento interno privado de la aplicación (`copyUriToInternalStorage()`), permitiendo visualizar la imagen del perfil de manera persistente sin internet.

---

## 👥 Cómo Simular el Comportamiento Multiusuario (Entre 2 Dispositivos)

Para probar la lógica real de registros, likes cruzados, matches instantáneos y mensajería en tiempo real bidireccional entre dos teléfonos celulares o emuladores:

### Opción A: Usando el Servidor Mock en Memoria (Local en un dispositivo)
El proyecto incluye un interceptor de OkHttp virtualizado (`MockInterceptor.kt`) que simula el servidor en memoria.
* **Ventaja**: Permite probar toda la lógica de manera rápida dentro de un mismo simulador cerrando sesión e ingresando con otra cuenta.
* **Cómo probar**:
  1. Asegúrate de que `USE_MOCK_API = true` esté configurado en [PuceMatchApplication.kt](file:///app/src/main/java/com/example/pucematch/PuceMatchApplication.kt).
  2. Registra el **Usuario A** (`usuarioA@puce.edu.ec`) con su foto y categoría de interés.
  3. Cierra la aplicación, borra la caché/datos si deseas limpiar el estado de la sesión actual, e ingresa de nuevo para registrar al **Usuario B** (`usuarioB@puce.edu.ec`).
  4. Desliza al **Usuario A** a la derecha (Like). Como el Usuario A aún no ha dado like al Usuario B, no habrá match instantáneo.
  5. Cierra sesión e ingresa con el **Usuario A**. Verás al **Usuario B** en el catálogo. Deslízalo a la derecha.
  6. **¡Coincidencia!** Aparecerá el diálogo flotante modal informando del Match y preguntándote si deseas chatear. Haz clic en "Chatear ahora" y envía un mensaje.

### Opción B: Conexión Real entre 2 Dispositivos Físicos / Emuladores Distintos
Si deseas probar la sincronización de datos física entre dos teléfonos móviles reales conectados en la misma red local (Wi-Fi):
1. **Configurar el Backend**: Asegúrate de tener levantado el backend web real del proyecto en tu computadora en el puerto `8080`.
2. **Obtener tu IP local**: Busca la dirección IPv4 local de tu computadora (ej: `192.168.1.15` en Windows ejecutando `ipconfig`).
3. **Modificar el archivo** [PuceMatchApplication.kt](file:///app/src/main/java/com/example/pucematch/PuceMatchApplication.kt):
   * Cambia `const val USE_MOCK_API = false` para desactivar el servidor virtual.
   * Cambia `const val BASE_URL = "http://<TU_IP_LOCAL>:8080/"` (reemplazando por tu IPv4).
4. **Instalar la app**: Instala la aplicación en ambos teléfonos.
5. **Ejecutar la prueba**:
   * En el **Teléfono 1**, registra al **Usuario 1** y selecciona categoría "Sentimental".
   * En el **Teléfono 2**, abre la app. Verás al **Usuario 1** en el catálogo. Regístrate como **Usuario 2** con categoría "Sentimental".
   * En el **Teléfono 1**, la app se actualizará automáticamente y mostrará al **Usuario 2** en la baraja.
   * Ambos se dan Like (Swipe right) en sus respectivos celulares.
   * En el momento en que se procese el segundo Like, ambos dispositivos verán la ventana flotante de **"¡Es un Match!"** simultáneamente en sus pantallas.
   * Abran el chat y empiecen a escribir. Los mensajes se reflejarán instantáneamente gracias al sondeo en segundo plano (polling).
