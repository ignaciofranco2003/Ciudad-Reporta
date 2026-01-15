# Ciudad Reporta

Proyecto de referencia para reportes ciudadanos con **BackEnd (API Flask + MySQL)** y **FrontEnd (Android)**. Incluye un script SQL de ejemplo para poblar la base de datos.

## Estructura del repositorio

- `BackEnd/`: API en Flask con conexión a MySQL.
- `FrontEnd/Ciudad_Reporta/`: app Android (Android Studio / Gradle).
- `inserts_ciudad_reporta.sql`: script SQL de ejemplo para cargar datos.

## Requisitos

### BackEnd

- Python 3.10+ (recomendado)
- MySQL 8+
- Servicio SMTP para envío de correos (opcional, solo si se usa la funcionalidad de email)

### FrontEnd (Android)

- Android Studio (o Gradle + JDK 17)
- Emulador o dispositivo físico

## Configuración del BackEnd

1. Crear un entorno virtual e instalar dependencias:

   ```bash
   cd BackEnd
   python -m venv .venv
   source .venv/bin/activate
   pip install -r req.txt
   ```

2. Crear un archivo `.env` en `BackEnd/` con estas variables:

   ```dotenv
   DB_HOST=localhost
   DB_USER=tu_usuario
   DB_PASS=tu_password
   SERVER=smtp.example.com
   PORT=587
   SENDER=correo@example.com
   PASS=tu_password_email
   ```

3. Iniciar el servidor:

   ```bash
   python Ciudad_Reporta.py
   ```

   El servicio corre por defecto en `http://0.0.0.0:5000` y en el primer arranque crea la base `ciudad_reporta` con tablas y categorías iniciales.

## Configuración del FrontEnd (Android)

1. Abrir `FrontEnd/Ciudad_Reporta/` desde Android Studio.
2. Sincronizar el proyecto con Gradle.
3. Ajustar la URL del backend en la app (si aplica) para que apunte al servidor donde corre Flask.
4. Ejecutar en emulador o dispositivo.

## Script SQL de ejemplo

El archivo `inserts_ciudad_reporta.sql` contiene inserciones de ejemplo para la base de datos.

Para usarlo:

```bash
mysql -u <usuario> -p ciudad_reporta < inserts_ciudad_reporta.sql
```

## Notas

- Si ejecutas el backend en otra máquina/red, recuerda habilitar CORS y configurar la IP/puerto correctos en la app Android.
- Las variables de correo son necesarias si quieres usar el envío de emails desde el backend.

## Servicios principales que utiliza el proyecto

- **API REST (Flask)**: expone endpoints para login/registro, reportes y administración.
- **Base de datos MySQL**: almacena usuarios, reportes y categorías.
- **Servidor SMTP**: envío de emails desde el backend (opcional).
- **Android (app cliente)**: consume la API y permite reportar incidencias desde el móvil.
