# NightAuth Java SDK — Danos una estrella en GitHub ⭐

SDK oficial de **NightAuth** para Java y suite de clientes de referencia (interfaz gráfica moderna en **Java Swing** y CLI interactiva para terminal). Protege tu software en Java / JVM contra ingeniería inversa, piratería y manipulaciones con licenciamiento en la nube, bloqueo por HWID, chat cifrado en tiempo real y descarga segura de módulos en memoria en minutos.

Portal Oficial: [https://nightauth.org](https://nightauth.org)

---

**Languages / Idiomas / Langues / Sprachen:**  
[English](../README.md) • [Español](es.md) • [Français](fr.md) • [Deutsch](de.md) • [Italiano](it.md) • [Português](pt.md) • [Русский](ru.md) • [Türkçe](tr.md) • [Polski](pl.md) • [中文](zh.md)

---

> [!NOTE]
> ### 📺 Tutoriales en Video y Guías de Configuración
> Los tutoriales en video oficiales y las guías detalladas paso a paso para configurar aplicaciones, planes, suscripciones, variables en la nube, webhooks y licencias en el Panel de Control de NightAuth **serán publicados próximamente** en nuestro portal y canal oficial de YouTube.

---

## 📸 Capturas de Pantalla

### Aplicación de Escritorio (Tema Oscuro Moderno Java Swing Obsidian)

| Pantalla de Inicio / Autenticación | Panel de Control & Gestión de Suscripciones |
| :---: | :---: |
| ![NightAuth Desktop Login](../image/desktop.png) | ![NightAuth Desktop Dashboard](../image/desktop-dashboard.png) |

### Aplicación de Consola (CLI Interactiva de Terminal)

| Inicio de Sesión en Terminal | Panel Interactivo de Consola |
| :---: | :---: |
| ![NightAuth Console Login](../image/console.png) | ![NightAuth Console Dashboard](../image/console-dashboard.png) |

---

## 🛡️ Prácticas de Seguridad Recomendadas

* **Ofuscación y Virtualización de Bytecode:** El bytecode de Java (.class) se puede descompilar con herramientas como CFR, Procyon, Fernflower o JD-GUI. Para versiones de producción, protege tus clases y JARs con ofuscadores comerciales como **Zelix KlassMaster (ZKM)**, **ProGuard** o **DexGuard** para cifrar cadenas de texto, renombrar símbolos y aplanar el flujo de control.
* **Compilación Nativa Ahead-Of-Time (AOT):** Compila tu código Java en binarios ejecutables nativos independientes (.exe en Windows, ELF en Linux) utilizando **GraalVM Native Image** (`native-image -jar MyApp.jar`). Esto elimina el bytecode JVM por completo y neutraliza los descompiladores tradicionales.
* **Sanitización de Búferes en Memoria:** Limpia credenciales sensibles, tokens descifrados y claves criptográficas inmediatamente después de usarlos rellenando arrays con ceros (`Arrays.fill(bytes, (byte) 0)`).
* **Ejecución Segura de Archivos en Memoria:** Al descargar módulos protegidos o JARs con `NightAuthApp.downloadFile()`, **nunca escribas los bytes en el almacenamiento físico o disco**. Carga el bytecode directamente en memoria mediante un `ClassLoader` personalizado (`defineClass`) y destruye los búferes temporales de inmediato.

---

## 📦 Requisitos y Compilación

Requiere **JDK 17** o posterior (probado en OpenJDK 17, 21 y 25 LTS). Cero dependencias externas — construido exclusivamente con bibliotecas estándar de JDK (`java.net.http`, `javax.crypto`, `java.security`).

### Build & Run Console Client
```bash
# Compile SDK and Console
javac -d build sdk/src/main/java/com/nightauth/sdk/NightAuthApi.java console/src/main/java/com/nightauth/console/Main.java

# Run Console
java -cp build com.nightauth.console.Main
```

### Build & Run Desktop Client (Swing GUI)
```bash
# Compile SDK and Desktop
javac -d build sdk/src/main/java/com/nightauth/sdk/NightAuthApi.java desktop/Main.java

# Run Desktop
java -cp build com.nightauth.desktop.Main
```

### Package Standalone Executable JARs
```bash
# Build Console JAR
jar cfe NightAuth-Console.jar com.nightauth.console.Main -C build .

# Build Desktop JAR
jar cfe NightAuth-Desktop.jar com.nightauth.desktop.Main -C build .
```

---

## ⚙️ Configuración e Inicialización

Define las credenciales de tu aplicación desde la pestaña **Manage Apps** del panel de control de NightAuth:

```java
import com.nightauth.sdk.NightAuthApi;

public static final String APP_NAME = "";
public static final String APP_API_KEY = "";
public static final String APP_VERSION = "1.0";
public static final String APP_TOKEN = "";

public static NightAuthApi NightAuthApp = new NightAuthApi(
    APP_NAME,
    APP_API_KEY,
    APP_VERSION,
    APP_TOKEN
);
```

### Inicializar el Handshake Criptográfico (`init`)

Debes invocar `init()` antes de ejecutar cualquier otra función del SDK. Esto establece un intercambio de claves efímero Diffie-Hellman (X25519) y una sesión cifrada:

```java
NightAuthApp.init();

if (!NightAuthApp.response.success) {
    System.err.printf("Initialization failed: %s\n", NightAuthApp.response.message);
    System.exit(1);
}
```

---

## 🚀 Guía de Uso del SDK

### 1. Validación de Sesión y Verificación de Lista Negra

```java
// Validate active session status with the server
if (!NightAuthApp.check()) {
    System.out.printf("Session invalid: %s\n", NightAuthApp.response.message);
}

// Check if client HWID or IP address is blacklisted
String hwid = NightAuthApi.getHardwareId();
if (NightAuthApp.checkBlacklist(hwid)) {
    System.err.println("Access Denied: Machine or IP is blacklisted.");
    System.exit(1);
}
```

### 2. Inicio de Sesión con Usuario y Contraseña (y 2FA)

```java
if (NightAuthApp.login(username, password, totpCode)) {
    System.out.printf("Welcome back, %s!\n", NightAuthApp.user.username);
} else {
    System.err.printf("Login failed: %s\n", NightAuthApp.response.message);
}
```

### 3. Registro con Clave de Licencia

```java
if (NightAuthApp.register(username, password, licenseKey, email)) {
    System.out.printf("Registration successful: %s!\n", NightAuthApp.user.username);
} else {
    System.err.printf("Registration failed: %s\n", NightAuthApp.response.message);
}
```

### 4. Autenticación Solo con Clave de Licencia (y 2FA)

```java
// Authenticate using license key alone or with 2FA TOTP code
if (NightAuthApp.license(licenseKey, totpCode)) {
    System.out.printf("License valid! Logged in as: %s\n", NightAuthApp.user.username);
} else {
    System.err.printf("License authentication failed: %s\n", NightAuthApp.response.message);
}
```

### 5. Mejorar Cuenta / Canjear Clave de Licencia

```java
if (NightAuthApp.upgrade(username, licenseKey)) {
    System.out.printf("Account upgraded: %s\n", NightAuthApp.response.message);
} else {
    System.err.printf("Upgrade failed: %s\n", NightAuthApp.response.message);
}
```

### 6. Restablecer ID de Hardware (Bloqueo HWID)

```java
if (NightAuthApp.resetHwid(username, password, totpCode)) {
    System.out.println("Hardware fingerprint reset successfully.");
} else {
    System.err.printf("HWID reset failed: %s\n", NightAuthApp.response.message);
}
```

### 7. Recuperación de Cuenta / Olvido de Contraseña

```java
// Step 1: Request recovery email link
NightAuthApp.forgotPassword("user@example.com");

// Step 2: Complete reset using token received via email
if (NightAuthApp.resetPassword(resetToken, newPassword)) {
    System.out.println("Password updated successfully.");
}
```

### 8. Datos del Perfil de Usuario (`NightAuthApp.user`)

```java
NightAuthApi.UserData u = NightAuthApp.user;
System.out.printf("Username: %s\n", u.username);
System.out.printf("Email: %s\n", u.email);
System.out.printf("HWID: %s\n", u.hwid);
System.out.printf("Client IP: %s\n", u.ip);
System.out.printf("Account Status: %s\n", u.status);
System.out.printf("Created At: %s\n", u.createdAt);
System.out.printf("Last Login: %s\n", u.lastLoginAt);
System.out.printf("2FA Enabled: %b\n", u.twoFactorEnabled);

for (NightAuthApi.SubscriptionData sub : u.subscriptions) {
    System.out.printf("Tier: %s | Key: %s | Expires: %s | Seconds Left: %d\n",
        sub.subscription, sub.key, sub.expiry, sub.timeLeft);
}
```

### 9. Variables en la Nube (Globales)

```java
String motd = NightAuthApp.getGlobalVar("server_motd");
System.out.printf("Announcement: %s\n", motd);
```

### 10. Variables de Usuario

```java
// Retrieve a per-user encrypted variable
String theme = NightAuthApp.getUserVar("custom_theme");

// Set or update a per-user variable
NightAuthApp.setUserVar("custom_theme", "dark_obsidian");

// Fetch all per-user variables
if (NightAuthApp.getUserVariables()) {
    Object vars = NightAuthApp.response.payload != null ? NightAuthApp.response.payload.get("variables") : null;
}
```

### 11. Registros de Auditoría de la Aplicación

```java
NightAuthApp.log("User initialized workflow #42");
```

### 12. Suspender Usuario y Bloquear Dispositivo

```java
NightAuthApp.ban("Security tampering detected");
```

### 13. Webhooks del Servidor y Subida de Archivos

```java
// 1. Trigger JSON payload webhook
Map<String, Object> payload = Map.of("event", "user_action", "score", 42);
NightAuthApi.WebhookResult result = NightAuthApp.triggerWebhook("webhook-id", payload);
if (result != null && result.success) {
    System.out.printf("Webhook response (HTTP %d): %s\n", result.status, result.data);
}

// 2. Upload file securely via server-side webhook proxy
byte[] reportBytes = "Audit dump content...".getBytes(java.nio.charset.StandardCharsets.UTF_8);
NightAuthApi.WebhookResult fileResult = NightAuthApp.triggerWebhookFile("webhook-id", reportBytes, "audit.log", payload);
```

### 14. Descarga Segura de Archivos en Memoria

```java
byte[] fileData = NightAuthApp.downloadFile("asset_file_id");
if (fileData != null) {
    // Process or load payload directly into memory (e.g. ClassLoader defineClass)
    // Wipe buffer from memory immediately after execution:
    java.util.Arrays.fill(fileData, (byte) 0);
}
```

### 15. Canales de Chat en Tiempo Real

```java
// Fetch recent message history
List<NightAuthApi.ChatMessage> messages = NightAuthApp.getChatMessages("general", 0L, true);
for (NightAuthApi.ChatMessage msg : messages) {
    System.out.printf("[%s] %s: %s\n", msg.timestamp, msg.author, msg.message);
}

// Send a message to a channel
NightAuthApp.sendChatMessage("Hello from Java SDK!", "general");
```

### 16. Transmisión de Eventos en Tiempo Real (Server-Sent Events)

```java
NightAuthApi.EventSubscription sub = NightAuthApp.subscribeEvents((payload) -> {
    System.out.printf("[EVENT] %s\n", payload);
});

// Close event stream when finished
sub.close();
```

### 17. Autenticación de Dos Factores (2FA)

```java
// Step 1: Initialize TOTP setup
NightAuthApi.TwoFactorSetupData setup = NightAuthApp.setup2FA();
if (setup != null) {
    System.out.printf("Secret: %s | URI: %s\n", setup.secret, setup.uri);
}

// Step 2: Confirm with 6-digit TOTP code
NightAuthApi.TwoFactorConfirmData conf = NightAuthApp.confirm2FA("123456");
if (conf != null) {
    System.out.println("2FA Activated! Backup Codes:");
    for (String code : conf.backupCodes) {
        System.out.printf(" - %s\n", code);
    }
}

// Step 3: Disable 2FA if needed
boolean disabled = NightAuthApp.disable2FA(password, "123456");
```

### 18. Ciclo de Vida de la Sesión y Cierre de Sesión

```java
// Keep session active with periodic ping
NightAuthApp.heartbeat();

// Revoke session on cloud and close client
NightAuthApp.logout();
NightAuthApp.close();
```

### 19. Cambiar Nombre de Usuario de la Cuenta

```java
if (NightAuthApp.changeUsername("new_alias", "CurrentPassword123!")) {
    System.out.printf("Username changed successfully to: %s\n", NightAuthApp.user.username);
} else {
    System.err.printf("Change username failed: %s\n", NightAuthApp.response.message);
}
```

### 20. Telemetría de la Aplicación y Usuarios en Línea

```java
// Retrieve application usage statistics
NightAuthApi.AppStats stats = NightAuthApp.getStats();
if (stats != null) {
    System.out.printf("Users: %d active / %d total | Licenses: %d used / %d total\n",
        stats.activeUsers, stats.totalUsers, stats.usedLicenses, stats.totalLicenses);
}

// Fetch list of currently online usernames
List<String> onlineUsers = NightAuthApp.getOnlineUsers();
System.out.printf("Online users (%d): %s\n", onlineUsers.size(), String.join(", ", onlineUsers));
```

### 21. Integridad de Archivos y Verificación de Hash

```java
String fileSha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
if (NightAuthApp.verifyFileHash(fileSha256)) {
    System.out.println("Binary matches registered server checksum.");
} else {
    System.err.println("Integrity verification failed!");
}
```

### 22. Huella Digital de Hardware (`getHardwareId`)

```java
// Retrieve stable hardware fingerprint identifier
String hwid = NightAuthApi.getHardwareId();
System.out.printf("Device HWID: %s\n", hwid);
```

### 23. Opciones Personalizadas, Timeout y Claves de Servidor

```java
// Pin server signing (Ed25519) and sealed-box (X25519) public keys
NightAuthApi.PinnedKeys pinned = new NightAuthApi.PinnedKeys(
    "cdb2285db481666ce40003aadb874c8751dbeb9a0672a37f740dfe881ecdc742",
    "b773fc4f6cf1a0938e78e714c3d84cb386584bea24728923008471a6cd203213"
);

// Initialize with custom API endpoint and pinned keys
NightAuthApi customAuth = new NightAuthApi(
    "Custom App",
    "na_custom_key",
    "1.0",
    null,
    "https://nightauth.org/api/v1",
    false,
    pinned
);

// Configure HTTP request timeout
customAuth.setRequestTimeoutMillis(15000);
```

### 24. Actualizaciones Firmadas Criptográficamente

```java
// Download and verify Ed25519 digitally signed update manifest with monotonic sequence protection
String downloadedJar = NightAuthApi.SignedUpdateInstaller.download(
    "https://releases.example.com/manifest.json",
    "cdb2285db481666ce40003aadb874c8751dbeb9a0672a37f740dfe881ecdc742",
    "SDK Example App",
    "updates/app-v2.0.jar"
);
System.out.printf("Verified update downloaded to: %s\n", downloadedJar);
```

---

## 🖥️ Ejecutar los Clientes de Consola y Escritorio

```bash
# Interactive Terminal Console Client
javac -d build sdk/src/main/java/com/nightauth/sdk/NightAuthApi.java console/src/main/java/com/nightauth/console/Main.java
java -cp build com.nightauth.console.Main

# Modern Java Swing Desktop Client
javac -d build sdk/src/main/java/com/nightauth/sdk/NightAuthApi.java desktop/Main.java
java -cp build com.nightauth.desktop.Main
```

---

## 📄 Licencia

Este proyecto está bajo la licencia **Elastic License 2.0**.

* No puedes proporcionar el software a terceros como un servicio alojado o administrado.
* No puedes mover, modificar, deshabilitar o eludir la funcionalidad de clave de licencia.
* No puedes alterar, eliminar ni ocultar ningún aviso de licencia o derechos de autor.

Consulta el archivo [LICENSE](../LICENSE) para el texto legal completo.
