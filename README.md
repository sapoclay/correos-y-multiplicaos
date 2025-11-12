# Correos Y Multiplicaos

<img width="425" height="234" alt="logo-correos" src="https://github.com/user-attachments/assets/a6598048-6b9b-4c76-9fb0-40ffae40d646" />

Gestor de correo electrónico completo desarrollado en Java 21.

## Novedades (2025)

- Redacción moderna con JavaFX (HTMLEditor/WebView): corrector ortográfico en vivo (español), soporte de imágenes inline y mejor compatibilidad con acentos/teclas muertas en Linux.
- Imágenes en el editor: previsualización inmediata al insertar, redimensionado con el ratón y conversión automática a contenido inline (cid:) al enviar/guardar.
- Eliminación mejorada: los mensajes se mueven a Papelera local y también en el servidor IMAP; opción para Restaurar y Vaciar Papelera.
- Sincronización robusta: al descargar, no se reintroducen correos que ya estén en Papelera local.
- Ejecución portable: `run.sh` ahora prioriza el JAR ejecutable, lo construye automáticamente si falta y aplica ajustes de entorno (GTK/IME) en Linux.

## Características

### Gestión de correo
- ✉️ Gestión de múltiples cuentas de correo (Gmail, Outlook, Yahoo, personalizado)
- 📁 Organización en carpetas (Bandeja de entrada, Enviados, Borradores, Spam, Papelera)
- 📝 Composición de nuevos mensajes con responder/reenviar
- 📎 Soporte para múltiples archivos adjuntos
- 🔍 **Búsqueda avanzada** de correos (por remitente, asunto, contenido, fecha)
- �️ **Sistema de etiquetas** con colores personalizables
- ✍️ **Firmas de correo** personalizadas por cuenta

### Contactos
- 📇 **Libreta de direcciones** completa
- 🔎 Búsqueda de contactos en tiempo real
- 📊 Seguimiento de frecuencia de uso
- 📤 Importar/Exportar contactos en CSV
- ➕ Agregar contactos manualmente o desde correos

### Interfaz y experiencia
- 🎨 Interfaz gráfica intuitiva con Swing + ventana de redacción en JavaFX
- 🚀 Splash screen personalizado
- 🖥️ **Icono en la bandeja del sistema** con menú contextual
- 🔔 Notificaciones del sistema
- 🪟 Minimizar a la bandeja del sistema
- 📊 Barra de estado con información en tiempo real
- ⌨️ Atajos de teclado (Ctrl+F para buscar)

### Redacción (JavaFX)
- 📝 Editor HTML enriquecido basado en JavaFX HTMLEditor/WebView
- ✓ Corrector ortográfico en vivo (LanguageTool: español)
- 🖼️ Imágenes inline con previsualización inmediata (file:) y redimensionado con el ratón
- 🔄 Conversión automática de imágenes a content-id (cid:) antes de enviar/guardar borradores
- 🎯 Manejo de tildes/ñ/teclas muertas en Linux sin problemas (WebView)

### Seguridad
### Calendario de citas
- 📅 Vista mensual navegable (anterior/siguiente)
- ➕ Crear citas con título, lugar, descripción
- � Horario de inicio y fin o eventos de todo el día
- ⚠️ Aviso de solapado sencillo entre citas del mismo día
- 🖱️ Doble clic en un día para crear rápidamente
- Accesos:
  - Menú "Calendario" → "Abrir calendario…" (atajo Ctrl+Shift+C)
  - Menú de la bandeja del sistema → "Calendario"
 - 🔔 Recordatorios diarios configurables (por defecto 18:00) para las citas del día siguiente

- �🔐 Almacenamiento cifrado de credenciales (AES-256-GCM)
- 🔑 Derivación de claves con PBKDF2
- 🔒 Validación SSL/TLS
- ⏱️ Rate limiting y auto-lock
- 🛡️ Sin almacenamiento de contraseñas en texto plano

## Requisitos

- Java 21 (OpenJDK o Oracle JDK)
- Maven 3.8+

## Compilación

El proyecto usa Maven Assembly Plugin para crear un JAR ejecutable que incluye todas las dependencias necesarias (Gson, Jakarta Mail, etc.):

```bash
mvn clean package
```

Esto genera:
- `target/gestor-correo-1.0-SNAPSHOT.jar` - JAR ejecutable con todas las dependencias

## Ejecución

### Opción 1: Usar el script de lanzamiento (recomendado)

- Linux/macOS:
  ```bash
  ./run.sh
  ```
- Windows (CMD):
  ```bat
  run.bat
  ```
- Windows (PowerShell):
  ```powershell
  ./run.ps1
  ```

Nota: si usas PowerShell Core (pwsh) tanto en Windows como en Linux, puedes emplear únicamente `run.ps1` como script multiplataforma. En algunas distribuciones de Linux puede ser necesario instalar PowerShell previamente.

El script:
- Prioriza ejecutar el JAR ensamblado si existe.
- Si no existe, construye el JAR automáticamente con Maven y lo ejecuta.
- Aplica ajustes de entorno para Linux (GTK/IME/X11) para evitar problemas con acentos/teclas muertas.

### Opción 2: Ejecutar manualmente

Si encuentras problemas con GTK_PATH, ejecuta:

```bash
unset GTK_PATH
java -jar target/gestor-correo-1.0-SNAPSHOT.jar
```

### Opción 3: Ejecución directa

```bash
java -jar target/gestor-correo-1.0-SNAPSHOT.jar
```

## Personalización

### Logo del splash screen

Para personalizar el logo que aparece en el splash screen, coloca tu archivo `logo.png` en:

```
src/main/resources/images/logo.png
```

El logo se redimensionará automáticamente a 250x350 píxeles.

## Funcionalidades implementadas

<img width="991" height="694" alt="interfaz-correos-y-multiplicaos" src="https://github.com/user-attachments/assets/6f7c57c5-2cdd-4d9f-8458-80bc45b98557" />

### Menú archivo
- **Nueva cuenta**: Añadir cuentas de Gmail, Outlook, Yahoo o personalizadas
- **Administrar cuentas**: Gestionar todas las cuentas configuradas
  - Ver listado completo de cuentas
  - Editar cuentas existentes
  - Eliminar cuentas
  - Establecer cuenta predeterminada
  - Ver detalles de configuración
  - Probar conexión
  - Menú contextual con clic derecho
  - ✍️ **Editar firma** de cada cuenta
- **Configuración**: Preferencias generales de la aplicación
  - **Pestaña general**: Comprobación automática de correo, notificaciones, sonidos
  - **Pestaña apariencia**: Tema, tamaño de fuente, opciones de bandeja del sistema
- **Salir**: Cerrar la aplicación (con confirmación)

### Menú correo
- **Nuevo mensaje**: Componer y enviar correos electrónicos reales
  - 🎨 Editor HTML enriquecido (JavaFX HTMLEditor/WebView)
    - Formato: **Negrita**, *Cursiva*, <u>Subrayado</u>, tamaño y color
    - ✓ Corrector ortográfico en vivo (español)
    - 🖼️ Insertar imágenes con previsualización inmediata y redimensionado
    - 🔄 Conversión automática a cid: al enviar/guardar
  - 📧 Múltiples destinatarios (coma o punto y coma)
  - 📇 Selector de contactos
  - 📎 Múltiples adjuntos
  - 🌍 UTF-8 correcto (tildes, ñ, especiales)
  - ✉️ Envío con barra de progreso
  - ✅ Validación de direcciones
  - ✍️ Inserción automática de firma si está habilitada
- **Responder**: Responde al mensaje seleccionado
  - Incluye contenido original con formato HTML preservado
  - Inserta firma automáticamente
  - Editor con formato completo
- **Reenviar**: Reenvía el mensaje seleccionado
  - Mantiene adjuntos originales
  - Preserva formato HTML del mensaje original
  - Incluye firma
- **Eliminar**: Mueve el mensaje a la Papelera (local y en el servidor IMAP)
- **Restaurar** (disponible en la carpeta Papelera): Devuelve el mensaje a la Bandeja de entrada (local y en el servidor)
- **Vaciar papelera**: Elimina definitivamente todos los mensajes de la Papelera local

### Menú buscar
- **Búsqueda avanzada** (Ctrl+F): Buscar correos por múltiples criterios
  - Búsqueda por remitente
  - Búsqueda por asunto
  - Búsqueda por contenido
  - Filtros de fecha (hoy, semana, mes, 3 meses, año, personalizado)
  - Búsqueda en carpetas específicas o todas
  - Resultados en tabla interactiva

### Menú etiquetas
- **Gestionar etiquetas**: CRUD completo de etiquetas
  - Crear etiquetas personalizadas
  - Editar nombre, color y descripción
  - Eliminar etiquetas
  - Restaurar etiquetas predeterminadas
  - Vista previa de colores
  - 6 etiquetas predeterminadas: Importante (rojo), Personal (azul), Trabajo (naranja), Pendiente (amarillo), Completado (verde), Seguimiento (magenta)
- **Asignar etiquetas**: Clic derecho en correo → 🏷️ Etiquetas
  - Checkboxes con colores por etiqueta
  - Múltiples etiquetas por correo
  - Visualización en columna de la tabla

### Menú contactos
- **Libreta de direcciones**: Gestión completa de contactos
  - Búsqueda en tiempo real
  - Agregar/Editar/Eliminar contactos
  - Campos: nombre, email, teléfono, empresa, notas
  - Contador de frecuencia de uso
  - Importar contactos desde CSV
  - Exportar contactos a CSV

### Menú ver
- Mostrar/ocultar barra de herramientas
- Mostrar/ocultar barra de estado

### Menú ayuda
### Menú calendario
- **Abrir calendario** (Ctrl+Shift+C): Abre el calendario mensual.
  - Doble clic en un día para crear una cita.
  - Botón "Nueva cita" para crear rápidamente con valores por defecto.
 - Recordatorios: Configura la hora diaria en Preferencias → General (campo "Recordatorio diario (hora)")

- **Acerca de**: Muestra información sobre la aplicación con enlace al repositorio GitHub

### Menú contextual (Clic Derecho en Correo)
- Abrir
- Responder
- Reenviar
- Eliminar
- Restaurar (cuando se está en la carpeta Papelera)
- 🏷️ **Etiquetas**: Asignar/quitar etiquetas con visualización de colores

## Gestión de cuentas

El gestor incluye un completo sistema de administración de cuentas:

### Añadir cuentas
- Soporte para Gmail, Outlook, Yahoo y proveedores personalizados
- Configuración automática de servidores SMTP/IMAP
- Notas informativas específicas para cada proveedor
- Validación de campos

### Administrar cuentas
El diálogo "Administrar cuentas" permite:
- **Ver todas las cuentas** configuradas en una tabla
- **Editar** cuentas existentes (nombre, contraseña, servidores)
- **Eliminar** cuentas con confirmación
- **Establecer cuenta predeterminada** para envíos
- **Ver detalles** completos de configuración
- **Probar conexión** de cada cuenta
- **Menú contextual** con clic derecho sobre las cuentas

### Almacenamiento
Los datos se guardan de forma persistente en:
```
~/.correosymultiplicaos/
├── accounts.json      # Cuentas de correo configuradas (cifradas)
├── preferences.json   # Preferencias de la aplicación
├── tags.json          # Etiquetas personalizadas
├── contacts.json      # Libreta de direcciones
├── appointments.json  # Citas del calendario
└── emails/            # Correos descargados por cuenta y carpeta
    └── [email]/
        └── [folder]/
            └── emails.json
```

## Icono en la bandeja del sistema

La aplicación incluye un icono en la bandeja del sistema (System Tray) que permite:

- **Doble clic**: Mostrar u ocultar la ventana principal
- **Clic derecho**: Acceder al menú contextual con las siguientes opciones:
  - Mostrar ventana
  - Nuevo mensaje
  - Nueva cuenta
  - Administrar cuentas
  - Configuración
  - Acerca de
  - Salir

Cuando cierras la ventana principal (botón X), la aplicación se minimiza a la bandeja del sistema en lugar de cerrarse completamente. Para salir completamente, usa la opción "Salir" del menú contextual o del menú Archivo.

## Barra de herramientas

La barra de herramientas incluye accesos rápidos para:
- Nuevo mensaje
- Responder
- Reenviar
- Eliminar
- Actualizar bandeja

## Estructura del proyecto

```
gestorCorreo/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── gestorcorreo/
│       │           ├── Main.java                          # Punto de entrada
│       │           ├── model/                             # Modelos de datos
│       │           │   ├── Attachment.java                # Adjuntos de correo
│       │           │   ├── Contact.java                   # Contactos de libreta
│       │           │   ├── EmailConfig.java               # Configuración de cuentas
│       │           │   ├── EmailMessage.java              # Mensajes de correo
│       │           │   └── Tag.java                       # Etiquetas con colores
│       │           ├── security/                          # Seguridad y cifrado
│       │           │   ├── EncryptionService.java         # Cifrado AES-256-GCM
│       │           │   ├── RateLimiter.java               # Limitación de tasa
│       │           │   └── SecurityManager.java           # Gestión de seguridad
│       │           ├── service/                           # Servicios de negocio
│       │           │   ├── ConfigService.java             # Gestión de configuración
│       │           │   ├── ContactService.java            # Gestión de contactos
│       │           │   ├── EmailSendService.java          # Envío de correos
│       │           │   ├── EmailStorageService.java       # Almacenamiento local
│       │           │   ├── PreferencesService.java        # Preferencias de usuario
│       │           │   └── TagService.java                # Gestión de etiquetas
│       │           └── ui/                                # Interfaz gráfica
│       │               ├── AboutDialog.java               # Diálogo "Acerca de"
│       │               ├── AccountManagerDialog.java      # Gestión de cuentas
│       │               ├── AddressBookDialog.java         # Libreta de direcciones
│       │               ├── AdvancedSearchDialog.java      # Búsqueda avanzada
│       │               ├── JavaFXComposeWindow.java       # Redacción HTML (JavaFX)
│       │               ├── ConfigDialog.java              # Configuración general
│       │               ├── ContactEditorDialog.java       # Editor de contactos
│       │               ├── EditAccountDialog.java         # Editar cuenta
│       │               ├── MainWindow.java                # Ventana principal
│       │               ├── NewAccountDialog.java          # Nueva cuenta
│       │               ├── SignatureEditorDialog.java     # Editor de firmas
│       │               ├── SplashScreen.java              # Pantalla de inicio
│       │               ├── SystemTrayManager.java         # Bandeja del sistema
│       │               ├── TagEditorDialog.java           # Editor de etiquetas
│       │               └── TagManagerDialog.java          # Gestión de etiquetas
│       └── resources/
│           └── images/
│               └── logo.png                               # Logo personalizable
├── pom.xml                                                # Configuración Maven
├── run.sh                                                 # Script de lanzamiento
├── .gitignore                                             # Archivos ignorados
├── SECURITY.md                                            # Documentación de seguridad
└── README.md                                              # Este archivo
```

## Las cosas con las que se ha montado esto

- **Java 21**: Última versión LTS con características modernas
- **Swing**: Framework GUI para la interfaz de escritorio
- **JavaFX 21.x**: WebView para renderizado HTML y redacción (HTMLEditor)
- **Maven 3.8+**: Gestión de dependencias y construcción
  - Maven Assembly Plugin para JAR con dependencias incluidas
- **Jakarta Mail API 2.0.1**: Funcionalidades de correo (IMAP/SMTP)
- **Gson 2.11.0**: Serialización/deserialización JSON
- **Seguridad**:
  - AES-256-GCM para cifrado de credenciales
  - PBKDF2 para derivación de claves
  - SSL/TLS para conexiones seguras

## Autor

Creado por **entreunosyceros.net**

## Repositorio

[https://github.com/sapoclay/correos-y-multiplicaos](https://github.com/sapoclay/correos-y-multiplicaos)

## Licencia

Proyecto de código abierto.

## Notas importantes

### Seguridad
- **NUNCA** compartas tu directorio `~/.correosymultiplicaos/` - contiene credenciales cifradas
- Las contraseñas se almacenan cifradas con AES-256-GCM
- El `.gitignore` está configurado para proteger datos sensibles

### Funcionamiento
- El splash screen se muestra durante 4 segundos al iniciar
- La aplicación se centra automáticamente en la pantalla
- Al cerrar la ventana (X), se minimiza a la bandeja del sistema
- Para salir completamente, usa "Salir" del menú o bandeja
- Las descargas de correo muestran progreso en la barra de estado
- Los correos se almacenan localmente para acceso offline

### Papelera y sincronización IMAP
- Eliminar mueve el correo a la carpeta Papelera local y lanza un movimiento en el servidor (IMAP) a Trash/Bin (alias comunes detectados automáticamente).
- Restaurar mueve el correo de Papelera a Bandeja de entrada (local y servidor).
- Si la operación IMAP falla, el estado local se mantiene coherente; en la siguiente descarga no se reintroducen correos que ya estén en Papelera local.

### Imágenes en redacción
- Al insertar imágenes se muestran al instante (file:) y pueden redimensionarse con el ratón directamente en el editor.
- Antes de enviar o guardar, las imágenes se convierten a contenido inline con cid:, manteniendo compatibilidad con la mayoría de clientes de correo.

### Etiquetas
- Asigna etiquetas con clic derecho en cualquier correo
- Las etiquetas son personalizables (nombre, color, descripción)
- Cada correo puede tener múltiples etiquetas
- Las etiquetas se visualizan en una columna de la tabla

### Firmas
- Cada cuenta puede tener su propia firma
- 4 plantillas predefinidas disponibles
- Las firmas se insertan automáticamente al redactar/responder
- Vista previa HTML en tiempo real

## Solución de problemas conocidos

### Error con GTK_PATH

Si encuentras el error relacionado con GTK_PATH, usa el script `run.sh` o ejecuta:

```bash
unset GTK_PATH
java -jar target/gestor-correo-1.0-SNAPSHOT.jar
```

### Teclas muertas/acentos en Linux

La redacción usa JavaFX WebView/HTMLEditor, que evita los problemas de IME/teclas muertas comunes en algunos toolkits. Si notas comportamiento extraño, ejecuta mediante `./run.sh` (Linux/macOS) o los scripts de Windows (`run.bat` / `run.ps1`).

### Error con libpthread

Si aparecen errores relacionados con libpthread, asegúrate de usar el Java del sistema:

```bash
/usr/bin/java -jar target/gestor-correo-1.0-SNAPSHOT.jar
```


