# NightAuth Java SDK — Gib uns einen Stern auf GitHub ⭐

Offizielles **NightAuth** Java SDK & Referenz-Client-Suite (Moderne Desktop-Benutzeroberfläche in **Java Swing** und interaktive Terminal-CLI). Schütze deine Java / JVM-Software in wenigen Minuten vor Piraterie, Reverse Engineering und Manipulation durch Cloud-Lizenzierung, HWID-Sperre, verschlüsselten Echtzeit-Chat und sicheres In-Memory-Streaming.

Offizielles Portal: [https://nightauth.org](https://nightauth.org)

---

**Languages / Idiomas / Langues / Sprachen:**  
[English](../README.md) • [Español](es.md) • [Français](fr.md) • [Deutsch](de.md) • [Italiano](it.md) • [Português](pt.md) • [Русский](ru.md) • [Türkçe](tr.md) • [Polski](pl.md) • [中文](zh.md)

---

> [!NOTE]
> ### 📺 Video-Tutorials & System-Einrichtungsanleitungen
> Offizielle Video-Tutorials und detaillierte Schritt-für-Schritt-Anleitungen zur Konfiguration von Anwendungen, Plänen, Abonnements, Cloud-Variablen, Webhooks und Lizenzen im NightAuth-Dashboard **werden bald** auf unserem offiziellen Portal und YouTube-Kanal veröffentlicht.

---

## 📸 Screenshots

### Desktop-Anwendung (Java Swing Obsidian Dark Theme)

| Start- / Anmeldebildschirm | Dashboard & Abonnementverwaltung |
| :---: | :---: |
| ![NightAuth Desktop Login](../image/desktop.png) | ![NightAuth Desktop Dashboard](../image/desktop-dashboard.png) |

### Konsolenanwendung (Interaktive Terminal-CLI)

| Terminal-Anmeldebildschirm | Interaktives Konsolen-Dashboard |
| :---: | :---: |
| ![NightAuth Console Login](../image/console.png) | ![NightAuth Console Dashboard](../image/console-dashboard.png) |

---

## 🛡️ Empfohlene Sicherheitspraktiken

* **Bytecode-Obfuskation & Virtualisierung:** Java-Bytecode (.class) kann mit Tools wie CFR, Procyon, Fernflower oder JD-GUI leicht dekompiliert werden. Schütze kompilierte Klassen und JARs in Produktionsumgebungen mit kommerziellen Obfuskatoren wie **Zelix KlassMaster (ZKM)**, **ProGuard** oder **DexGuard**, um Zeichenketten zu verschlüsseln, Bezeichner umzubenennen und den Kontrollfluss zu verschleiern.
* **Ahead-Of-Time (AOT) Native Kompilierung:** Kompiliere Java-Code mit **GraalVM Native Image** (`native-image -jar MyApp.jar`) zu eigenständigen nativen Binärdateien (.exe unter Windows, ELF unter Linux). Dies entfernt den JVM-Bytecode vollständig und neutralisiert traditionelle Dekompiler.
* **Bereinigung von Puffern im Arbeitsspeicher:** Lösche sensible Anmeldedaten, entschlüsselte Token und kryptografische Schlüssel direkt nach der Verwendung durch Überschreiben mit Nullen (`Arrays.fill(bytes, (byte) 0)`).
* **Sichere Dateiausführung im Arbeitsspeicher:** Beim Herunterladen geschützter Module mit `NightAuthApp.downloadFile()` **niemals Bytes auf Datenträger oder Festplatten schreiben**. Lade Bytecode direkt über einen benutzerdefinierten `ClassLoader` (`defineClass`) in den RAM und zerstöre temporäre Puffer unverzüglich.

---

## 📦 Anforderungen & Erstellung

Erfordert **JDK 17** oder höher (getestet auf OpenJDK 17, 21 und 25 LTS). Null externe Abhängigkeiten — ausschließlich mit JDK-Standardbibliotheken erstellt (`java.net.http`, `javax.crypto`, `java.security`).

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

## ⚙️ Konfiguration & Initialisierung

Definiere deine Anwendungs-Anmeldedaten auf der Registerkarte **Manage Apps** des NightAuth-Dashboards:

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

### Kryptografischen Handshake initialisieren (`init`)

Du musst `init()` vor dem Ausführen anderer SDK-Funktionen aufrufen. Dadurch wird ein kurzlebiger Diffie-Hellman-Schlüsselaustausch (X25519) und eine verschlüsselte Sitzung eingerichtet:

```java
NightAuthApp.init();

if (!NightAuthApp.response.success) {
    System.err.printf("Initialization failed: %s\n", NightAuthApp.response.message);
    System.exit(1);
}
```

---

## 🚀 SDK-Nutzungsanleitung

### 1. Sitzungsvalidierung und Blacklist-Prüfung

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

### 2. Anmeldung mit Benutzername & Passwort (und 2FA)

```java
if (NightAuthApp.login(username, password, totpCode)) {
    System.out.printf("Welcome back, %s!\n", NightAuthApp.user.username);
} else {
    System.err.printf("Login failed: %s\n", NightAuthApp.response.message);
}
```

### 3. Registrierung mit Lizenzschlüssel

```java
if (NightAuthApp.register(username, password, licenseKey, email)) {
    System.out.printf("Registration successful: %s!\n", NightAuthApp.user.username);
} else {
    System.err.printf("Registration failed: %s\n", NightAuthApp.response.message);
}
```

### 4. Nur mit Lizenzschlüssel authentifizieren (und 2FA)

```java
// Authenticate using license key alone or with 2FA TOTP code
if (NightAuthApp.license(licenseKey, totpCode)) {
    System.out.printf("License valid! Logged in as: %s\n", NightAuthApp.user.username);
} else {
    System.err.printf("License authentication failed: %s\n", NightAuthApp.response.message);
}
```

### 5. Konto upgraden / Lizenzschlüssel einlösen

```java
if (NightAuthApp.upgrade(username, licenseKey)) {
    System.out.printf("Account upgraded: %s\n", NightAuthApp.response.message);
} else {
    System.err.printf("Upgrade failed: %s\n", NightAuthApp.response.message);
}
```

### 6. Hardware-ID zurücksetzen (HWID-Sperre)

```java
if (NightAuthApp.resetHwid(username, password, totpCode)) {
    System.out.println("Hardware fingerprint reset successfully.");
} else {
    System.err.printf("HWID reset failed: %s\n", NightAuthApp.response.message);
}
```

### 7. Passwort vergessen / Kontowiederherstellung

```java
// Step 1: Request recovery email link
NightAuthApp.forgotPassword("user@example.com");

// Step 2: Complete reset using token received via email
if (NightAuthApp.resetPassword(resetToken, newPassword)) {
    System.out.println("Password updated successfully.");
}
```

### 8. Benutzerprofildaten (`NightAuthApp.user`)

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

### 9. Cloud-Variablen (Global)

```java
String motd = NightAuthApp.getGlobalVar("server_motd");
System.out.printf("Announcement: %s\n", motd);
```

### 10. Benutzer-Variablen

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

### 11. Anwendungs-Audit-Protokolle

```java
NightAuthApp.log("User initialized workflow #42");
```

### 12. Benutzer sperren & Gerät blockieren

```java
NightAuthApp.ban("Security tampering detected");
```

### 13. Server-Webhooks & Datei-Upload

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

### 14. Sicherer Datei-Download im Arbeitsspeicher

```java
byte[] fileData = NightAuthApp.downloadFile("asset_file_id");
if (fileData != null) {
    // Process or load payload directly into memory (e.g. ClassLoader defineClass)
    // Wipe buffer from memory immediately after execution:
    java.util.Arrays.fill(fileData, (byte) 0);
}
```

### 15. Echtzeit-Chat-Kanäle

```java
// Fetch recent message history
List<NightAuthApi.ChatMessage> messages = NightAuthApp.getChatMessages("general", 0L, true);
for (NightAuthApi.ChatMessage msg : messages) {
    System.out.printf("[%s] %s: %s\n", msg.timestamp, msg.author, msg.message);
}

// Send a message to a channel
NightAuthApp.sendChatMessage("Hello from Java SDK!", "general");
```

### 16. Echtzeit-Event-Streaming (Server-Sent Events)

```java
NightAuthApi.EventSubscription sub = NightAuthApp.subscribeEvents((payload) -> {
    System.out.printf("[EVENT] %s\n", payload);
});

// Close event stream when finished
sub.close();
```

### 17. Zwei-Faktor-Authentifizierung (2FA)

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

### 18. Sitzungslebenszyklus und Abmeldung

```java
// Keep session active with periodic ping
NightAuthApp.heartbeat();

// Revoke session on cloud and close client
NightAuthApp.logout();
NightAuthApp.close();
```

### 19. Kontobenutzernamen ändern

```java
if (NightAuthApp.changeUsername("new_alias", "CurrentPassword123!")) {
    System.out.printf("Username changed successfully to: %s\n", NightAuthApp.user.username);
} else {
    System.err.printf("Change username failed: %s\n", NightAuthApp.response.message);
}
```

### 20. Anwendungs-Telemetrie & Online-Benutzer

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

### 21. Dateiintegrität und Hash-Verifizierung

```java
String fileSha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
if (NightAuthApp.verifyFileHash(fileSha256)) {
    System.out.println("Binary matches registered server checksum.");
} else {
    System.err.println("Integrity verification failed!");
}
```

### 22. Hardware-Fingerabdruck (`getHardwareId`)

```java
// Retrieve stable hardware fingerprint identifier
String hwid = NightAuthApi.getHardwareId();
System.out.printf("Device HWID: %s\n", hwid);
```

### 23. Erweiterte Optionen, Timeout & Server-Schlüssel

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

### 24. Kryptografisch signierte Selbst-Updates

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

## 🖥️ Konsolen- und Desktop-Clients ausführen

```bash
# Interactive Terminal Console Client
javac -d build sdk/src/main/java/com/nightauth/sdk/NightAuthApi.java console/src/main/java/com/nightauth/console/Main.java
java -cp build com.nightauth.console.Main

# Modern Java Swing Desktop Client
javac -d build sdk/src/main/java/com/nightauth/sdk/NightAuthApi.java desktop/Main.java
java -cp build com.nightauth.desktop.Main
```

---

## 📄 Lizenz

Dieses Projekt ist unter der **Elastic License 2.0** lizenziert.

* Die Software darf Dritten nicht als gehosteter oder verwalteter Dienst bereitgestellt werden.
* Lizenzschlüsselfunktionen dürfen weder umgangen, verschoben, verändert noch deaktiviert werden.
* Lizenz- oder Urheberrechtshinweise dürfen nicht geändert, entfernt oder unkenntlich gemacht werden.

Den vollständigen Text findest du in der Datei [LICENSE](../LICENSE).
