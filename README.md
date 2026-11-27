# FarmaGuard

**Asistente de compras seguras en farmacia** para computadoras con Windows.

¿Te ofrecen varios medicamentos en la botica? Escríbelos en FarmaGuard y te dirá al instante si hay algún problema:

- **Medicamentos repetidos con otro nombre** (por ejemplo, Paracetamol y Panadol son lo mismo).
- **Interacciones peligrosas** entre medicamentos que no deberían tomarse juntos.
- **Dosis diaria excesiva** al sumar varios productos.
- **Productos que requieren receta**, muy concentrados o innecesarios.

Además tiene un **catálogo** de más de 200 medicamentos comunes en boticas del Perú (para qué sirve cada uno, cómo se toma y consejos), más de **100 marcas comerciales** reconocidas y **recordatorios** que te avisan a la hora de cada toma.

### Novedades de la versión 1.1.0
- **Sinónimos:** *clorfenamina*, *clorfeniramina* y *maleato de clorfeniramina* se reconocen como el mismo principio activo (igual con acetaminofén = paracetamol, dipirona = metamizol y muchos más).
- **Más de 100 marcas del Perú:** las marcas con varios componentes (Dr. Flu, Abubron BR, Comtrex, Tapsin, Midol, Dolomax Plus…) muestran todos sus componentes de una vez.
- **Guía de uso** que aparece la primera vez que abres el programa y **aviso importante** (botón **i**).
- **Recordatorios más limpios:** la pestaña muestra solo tu agenda y el formulario se abre con **Nuevo recordatorio**. Las tomas se ponen junto a las comidas cuando el medicamento va antes, durante o después de comer.
- **Análisis más preciso** en recetas grandes: menos falsos positivos y una sola alerta por componente repetido.

> 📱 ¿Lo quieres en el celular? Mira la versión para Android: [FarmaGuardYberia-Android](https://github.com/breynerluque33-creator/FarmaGuardYberia-Android)

---

## ✅ Requisitos

| Para... | Necesitas |
|---|---|
| **Usar el programa** (instalador `.exe`) | Windows 10 u 11 de 64 bits. **Nada más**: el instalador ya trae Java incluido. |
| **Ejecutarlo desde el código** | Windows, [Java JDK 17 o superior](https://adoptium.net/es/temurin/releases/) |
| **Crear el instalador `.exe`** | Java JDK 17 o superior + [WiX Toolset](https://wixtoolset.org/) |

No necesita internet para funcionar.

---

## 💻 Instalar en Windows (la forma fácil)

1. Entra a la sección **[Releases](https://github.com/breynerluque33-creator/FarmaGuardYberia/releases)** de este repositorio y descarga el archivo **`FarmaGuard-1.1.0.exe`**.
2. Haz doble clic en el archivo descargado.
3. Si aparece una ventana azul que dice **"Windows protegió su PC"**:
   - Haz clic en **"Más información"**.
   - Luego en **"Ejecutar de todas formas"**.

   *(Este aviso sale porque el programa no está firmado por una empresa. Es normal en programas hechos por estudiantes o desarrolladores independientes.)*
4. Elige la carpeta donde quieres instalarlo (o deja la que aparece) y haz clic en **Instalar**.
   **No pide permisos de administrador.**
5. ¡Listo! Abre **FarmaGuard** desde el **acceso directo del Escritorio** o desde el **menú Inicio**.

### Desinstalar
Ve a **Configuración → Aplicaciones → Aplicaciones instaladas**, busca **FarmaGuard** y haz clic en **Desinstalar**.

### ¿Dónde se guardan mis datos?
Tus recordatorios se guardan en tu computadora, en la carpeta `C:\Users\TU_USUARIO\.farmaguard`. Si instalas una versión nueva, se conservan.

---

## 🧭 Cómo se usa

El programa tiene tres pestañas arriba:

1. **Analizar compra:** escribe el nombre de un medicamento (ej. *Panadol*), elígelo de la lista y pulsa **Agregar**. Repite con los demás. A la derecha verás el resultado:
   - 🟢 **Seguro:** no hay problemas.
   - 🟡 **Precaución:** consulta antes de comprar o tomar.
   - 🔴 **Incompatible o redundante:** no deberían tomarse juntos o estás pagando dos veces por lo mismo.

   También puedes pulsar **"Pegar una lista o receta"** para agregar varios a la vez.
2. **Medicamentos:** busca cualquier medicamento del catálogo para ver para qué sirve y con qué no debes combinarlo. Puedes buscar por marca, genérico u otro nombre del mismo componente: *clorfeniramina*, *maleato de clorfenamina* y *clorfenamina* muestran la misma ficha. Las marcas con varios componentes (ej. *Dr. Flu*) muestran todos sus componentes de una vez.
3. **Recordatorios:** muestra tu agenda. Pulsa **Nuevo recordatorio** para crear un aviso (antes, durante o después de comer). Si el medicamento va con las comidas, las tomas se ponen junto al desayuno, almuerzo y cena. FarmaGuard te avisará mientras el programa esté abierto.

Botones de arriba a la derecha:
- **?** abre la guía de uso (se muestra sola la primera vez que abres el programa).
- 🌙 **luna** cambia entre tema claro y oscuro.
- **i** muestra el aviso importante: FarmaGuard no reemplaza la opinión de un médico.

> Los sinónimos y marcas de un solo componente se configuran en `src/main/resources/sinonimos.txt` (una línea por principio activo: `Clorfenamina = Clorfeniramina, Maleato de clorfeniramina`).

---

## 🛠️ Ejecutar desde el código (para programadores)

1. Instala **Java JDK 17 o superior**. Para comprobarlo, abre una terminal y escribe:
   ```bash
   java -version
   ```
2. Descarga este repositorio: botón verde **Code → Download ZIP** (y descomprímelo), o con Git:
   ```bash
   git clone https://github.com/breynerluque33-creator/FarmaGuardYberia.git
   cd FarmaGuardYberia
   ```
3. Ejecuta el programa (la primera vez descarga lo necesario y tarda unos minutos):
   ```bash
   mvnw.cmd spring-boot:run
   ```

No necesitas instalar Maven: el proyecto ya trae `mvnw.cmd`, que lo descarga solo.

### Correr las pruebas
```bash
mvnw.cmd test
```

### Crear el instalador `.exe`
Con Java JDK 17+ y WiX Toolset instalados, ejecuta en la carpeta del proyecto:
```bash
powershell -ExecutionPolicy Bypass -File .\empaquetar.ps1
```
El instalador aparecerá en `target\dist\FarmaGuard-1.1.0.exe` y también se copiará a tu Escritorio.

---

## 🧩 Tecnologías

Java 17 · Spring Boot · SQLite · Swing con FlatLaf · Maven

---

⚠️ **Aviso:** FarmaGuard es una guía de compra informada. **No diagnostica ni reemplaza** la consulta con un médico o químico farmacéutico.
