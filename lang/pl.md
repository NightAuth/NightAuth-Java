# NightAuth Java SDK — Daj nam gwiazdkę na GitHubie ⭐

Oficjalny pakiet SDK **NightAuth** dla języka Java oraz zestaw referencyjnych klientów (nowoczesny interfejs graficzny w **Java Swing** i interaktywny wiersz poleceń terminala). Chroń swoje oprogramowanie Java / JVM przed piractwem, inżynierią wsteczną i manipulacjami dzięki licencjonowaniu w chmurze, blokadzie HWID, szyfrowanemu czatowi w czasie rzeczywistym i bezpiecznemu ładowaniu w pamięci RAM w kilka minut.

Oficjalny Portal: [https://nightauth.org](https://nightauth.org)

---

**Languages / Idiomas / Langues / Sprachen:**  
[English](../README.md) • [Español](es.md) • [Français](fr.md) • [Deutsch](de.md) • [Italiano](it.md) • [Português](pt.md) • [Русский](ru.md) • [Türkçe](tr.md) • [Polski](pl.md) • [中文](zh.md)

---

> [!NOTE]
> ### 📺 Samouczki Wideo i Poradniki Konfiguracji
> Oficjalne poradniki wideo i szczegółowe instrukcje krok po kroku dotyczące konfiguracji aplikacji, planów, subskrypcji, zmiennych chmurowych, webhooków i licencji w panelu NightAuth **zostaną wkrótce opublikowane** na naszym portalu i kanale YouTube.

---

## 📸 Zrzuty Ekranu

### Aplikacja Okienkowa (Ciemny Motyw Java Swing Obsidian)

| Ekran Powitalny / Logowanie | Panel Główny & Zarządzanie Subskrypcją |
| :---: | :---: |
| ![NightAuth Desktop Login](../image/desktop.png) | ![NightAuth Desktop Dashboard](../image/desktop-dashboard.png) |

### Aplikacja Konsolowa (Interaktywne CLI Terminala)

| Ekran Logowania w Terminalu | Interaktywny Panel Konsolowy |
| :---: | :---: |
| ![NightAuth Console Login](../image/console.png) | ![NightAuth Console Dashboard](../image/console-dashboard.png) |

---

## 🛡️ Zalecane Praktyki Bezpieczeństwa

* **Zaciemnianie i Wirtualizacja Kodu Bajtowego:** Kod bajtowy Java (.class) można łatwo odtworzyć za pomocą dekompilatorów CFR, Procyon, Fernflower czy JD-GUI. W środowiskach produkcyjnych zabezpieczaj skompilowane klasy i pliki JAR za pomocą komercyjnych obfuskatorów, takich jak **Zelix KlassMaster (ZKM)**, **ProGuard** lub **DexGuard**, aby szyfrować ciągi znaków, zmieniać nazwy symboli i spłaszczać przepływ sterowania.
* **Kompilacja Natywna Ahead-Of-Time (AOT):** Kompiluj kod Java do samodzielnych binariów natywnych (.exe w Windows, ELF w Linux) za pomocą **GraalVM Native Image** (`native-image -jar MyApp.jar`). Pozwala to całkowicie wyeliminować kod bajtowy JVM i zneutralizować konwencjonalne dekompilatory.
* **Czyszczenie Buforów w Pamięci:** Natychmiast po użyciu usuwaj poufne dane uwierzytelniające, odszyfrowane tokeny i klucze kryptograficzne, zerując tablice (`Arrays.fill(bytes, (byte) 0)`).
* **Bezpieczne Wykonywanie Plików w Pamięci RAM:** Pobierając zabezpieczone moduły lub archiwa JAR za pomocą `NightAuthApp.downloadFile()`, **nigdy nie zapisuj bajtów na dysku fizycznym**. Ładuj kod bajtowy bezpośrednio do pamięci za pomocą niestandardowego `ClassLoader` (`defineClass`) i natychmiast czyść tymczasowe bufory.

---

## 📦 Wymagania & Budowanie

Wymaga **JDK 17** lub nowszego (testowano na OpenJDK 17, 21 i 25 LTS). Zero zewnętrznych zależności — zbudowano wyłącznie przy użyciu standardowych bibliotek JDK (`java.net.http`, `javax.crypto`, `java.security`).

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

## ⚙️ Konfiguracja & Inicjalizacja

Zdefiniuj dane uwierzytelniające aplikacji w zakładce **Manage Apps** panelu kontrolnego NightAuth:

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

### Inicjalizacja Uzgadniania Kryptograficznego (`init`)

Musisz wywołać `init()` przed wykonaniem jakichkolwiek innych funkcji SDK. Ustanawia to ulotną wymianę kluczy Diffiego-Hellmana (X25519) i zaszyfrowaną sesję:

```java
NightAuthApp.init();

if (!NightAuthApp.response.success) {
    System.err.printf("Initialization failed: %s\n", NightAuthApp.response.message);
    System.exit(1);
}
```

---

## 🚀 Przewodnik po SDK

### 1. Walidacja Sesji i Sprawdzanie Czarnej Listy

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

### 2. Logowanie za pomocą Nazwy Użytkownika i Hasła (oraz 2FA)

```java
if (NightAuthApp.login(username, password, totpCode)) {
    System.out.printf("Welcome back, %s!\n", NightAuthApp.user.username);
} else {
    System.err.printf("Login failed: %s\n", NightAuthApp.response.message);
}
```

### 3. Rejestracja z Kluczem Licencyjnym

```java
if (NightAuthApp.register(username, password, licenseKey, email)) {
    System.out.printf("Registration successful: %s!\n", NightAuthApp.user.username);
} else {
    System.err.printf("Registration failed: %s\n", NightAuthApp.response.message);
}
```

### 4. Uwierzytelnianie Tylko za Pomocą Klucza Licencji (oraz 2FA)

```java
// Authenticate using license key alone or with 2FA TOTP code
if (NightAuthApp.license(licenseKey, totpCode)) {
    System.out.printf("License valid! Logged in as: %s\n", NightAuthApp.user.username);
} else {
    System.err.printf("License authentication failed: %s\n", NightAuthApp.response.message);
}
```

### 5. Uaktualnij Konto / Odbierz Klucz Licencyjny

```java
if (NightAuthApp.upgrade(username, licenseKey)) {
    System.out.printf("Account upgraded: %s\n", NightAuthApp.response.message);
} else {
    System.err.printf("Upgrade failed: %s\n", NightAuthApp.response.message);
}
```

### 6. Zresetuj ID Sprzętu (Blokada HWID)

```java
if (NightAuthApp.resetHwid(username, password, totpCode)) {
    System.out.println("Hardware fingerprint reset successfully.");
} else {
    System.err.printf("HWID reset failed: %s\n", NightAuthApp.response.message);
}
```

### 7. Odzyskiwanie Konta / Zapomniane Hasło

```java
// Step 1: Request recovery email link
NightAuthApp.forgotPassword("user@example.com");

// Step 2: Complete reset using token received via email
if (NightAuthApp.resetPassword(resetToken, newPassword)) {
    System.out.println("Password updated successfully.");
}
```

### 8. Dane Profilu Użytkownika (`NightAuthApp.user`)

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

### 9. Zmienne Chmurowe (Globalne)

```java
String motd = NightAuthApp.getGlobalVar("server_motd");
System.out.printf("Announcement: %s\n", motd);
```

### 10. Zmienne Użytkownika

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

### 11. Dzienniki Audytu Aplikacji

```java
NightAuthApp.log("User initialized workflow #42");
```

### 12. Zablokuj Użytkownika i Maszynę na Czarnej Liście

```java
NightAuthApp.ban("Security tampering detected");
```

### 13. Webhooki Serwerowe i Przesyłanie Plików

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

### 14. Bezpieczne Pobieranie Plików do Pamięci RAM

```java
byte[] fileData = NightAuthApp.downloadFile("asset_file_id");
if (fileData != null) {
    // Process or load payload directly into memory (e.g. ClassLoader defineClass)
    // Wipe buffer from memory immediately after execution:
    java.util.Arrays.fill(fileData, (byte) 0);
}
```

### 15. Kanały Czatu w Czasie Rzeczywistym

```java
// Fetch recent message history
List<NightAuthApi.ChatMessage> messages = NightAuthApp.getChatMessages("general", 0L, true);
for (NightAuthApi.ChatMessage msg : messages) {
    System.out.printf("[%s] %s: %s\n", msg.timestamp, msg.author, msg.message);
}

// Send a message to a channel
NightAuthApp.sendChatMessage("Hello from Java SDK!", "general");
```

### 16. Strumieniowanie Zdarzeń w Czasie Rzeczywistym (SSE)

```java
NightAuthApi.EventSubscription sub = NightAuthApp.subscribeEvents((payload) -> {
    System.out.printf("[EVENT] %s\n", payload);
});

// Close event stream when finished
sub.close();
```

### 17. Uwierzytelnianie Dwuetapowe (2FA)

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

### 18. Cykl Życia Sesji i Wylogowanie

```java
// Keep session active with periodic ping
NightAuthApp.heartbeat();

// Revoke session on cloud and close client
NightAuthApp.logout();
NightAuthApp.close();
```

### 19. Zmiana Nazwy Użytkownika Konta

```java
if (NightAuthApp.changeUsername("new_alias", "CurrentPassword123!")) {
    System.out.printf("Username changed successfully to: %s\n", NightAuthApp.user.username);
} else {
    System.err.printf("Change username failed: %s\n", NightAuthApp.response.message);
}
```

### 20. Telemetria Aplikacji i Użytkownicy Online

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

### 21. Integralność Plików i Weryfikacja Skrótu Hash

```java
String fileSha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
if (NightAuthApp.verifyFileHash(fileSha256)) {
    System.out.println("Binary matches registered server checksum.");
} else {
    System.err.println("Integrity verification failed!");
}
```

### 22. Identyfikator Sprzętowy (`getHardwareId`)

```java
// Retrieve stable hardware fingerprint identifier
String hwid = NightAuthApi.getHardwareId();
System.out.printf("Device HWID: %s\n", hwid);
```

### 23. Zaawansowane Opcje, Limit Czasu i Własne Klucze Serwera

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

### 24. Kryptograficznie Podpisane Aktualizacje

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

## 🖥️ Uruchamianie Klientów Konsolowych i Graficznych

```bash
# Interactive Terminal Console Client
javac -d build sdk/src/main/java/com/nightauth/sdk/NightAuthApi.java console/src/main/java/com/nightauth/console/Main.java
java -cp build com.nightauth.console.Main

# Modern Java Swing Desktop Client
javac -d build sdk/src/main/java/com/nightauth/sdk/NightAuthApi.java desktop/Main.java
java -cp build com.nightauth.desktop.Main
```

---

## 📄 Licencja

Ten projekt jest licencjonowany na warunkach **Elastic License 2.0**.

* Nie możesz udostępniać oprogramowania stronom trzecim jako usługi hostowanej lub zarządzanej.
* Nie możesz modyfikować, przenosić, wyłączać ani omijać funkcjonalności kluczy licencyjnych.
* Nie możesz zmieniać, usuwać ani ukrywać jakichkolwiek informacji o licencji lub prawach autorskich.

Pełny tekst prawny znajduje się w pliku [LICENSE](../LICENSE).
