# NightAuth Java SDK — GitHub'da Bize Yıldız Verin ⭐

Resmi **NightAuth** Java SDK ve Referans İstemci Paketi (**Java Swing** ile modern masaüstü arayüzü ve etkileşimli terminal CLI). Bulut lisanslama, HWID kilitleme, gerçek zamanlı şifreli sohbet ve bellek içi güvenli yük akışı ile Java / JVM yazılımınızı dakikalar içinde korsanlığa, tersine mühendisliğe ve kurcalamaya karşı koruyun.

Resmi Portal: [https://nightauth.org](https://nightauth.org)

---

**Languages / Idiomas / Langues / Sprachen:**  
[English](../README.md) • [Español](es.md) • [Français](fr.md) • [Deutsch](de.md) • [Italiano](it.md) • [Português](pt.md) • [Русский](ru.md) • [Türkçe](tr.md) • [Polski](pl.md) • [中文](zh.md)

---

> [!NOTE]
> ### 📺 Video Eğitimleri ve Sistem Kurulum Kılavuzları
> NightAuth Kontrol Panelinde uygulamaların, planların, aboneliklerin, bulut değişkenlerinin, webhook'ların ve lisansların nasıl yapılandırılacağını açıklayan resmi video eğitimleri ve ayrıntılı adım adım kılavuzlar **yakında** resmi portalımızda ve YouTube kanalımızda yayınlanacaktır.

---

## 📸 Ekran Görüntüleri

### Masaüstü Uygulaması (Java Swing Obsidian Dark Tema)

| Başlangıç / Kimlik Doğrulama Ekranı | Kontrol Paneli ve Abonelik Yönetimi |
| :---: | :---: |
| ![NightAuth Desktop Login](../image/desktop.png) | ![NightAuth Desktop Dashboard](../image/desktop-dashboard.png) |

### Konsol Uygulaması (Etkileşimli Terminal CLI)

| Terminal Giriş Ekranı | Etkileşimli Konsol Kontrol Paneli |
| :---: | :---: |
| ![NightAuth Console Login](../image/console.png) | ![NightAuth Console Dashboard](../image/console-dashboard.png) |

---

## 🛡️ Önerilen Güvenlik Uygulamaları

* **Bayt Kodu Karartma & Sanallaştırma:** Java bayt kodları (.class), CFR, Procyon, Fernflower veya JD-GUI gibi araçlarla kolayca kaynak koda dönüştürülebilir. Üretim sürümleri için, dizgi sabitlerini şifrelemek, sembolleri yeniden adlandırmak ve kontrol akışını düzleştirmek amacıyla **Zelix KlassMaster (ZKM)**, **ProGuard** veya **DexGuard** gibi ticari karartıcılar kullanın.
* **Ahead-Of-Time (AOT) Yerel Derleme:** Java kodunu **GraalVM Native Image** (`native-image -jar MyApp.jar`) kullanarak bağımsız yerel çalıştırılabilir dosyalara (Windows'ta .exe, Linux'ta ELF) derleyin. Bu, JVM bayt kodunu tamamen ortadan kaldırır ve geleneksel dekompiler'ları etkisiz hale getirir.
* **Bellek Arabelleklerini Temizleme:** Hassas kimlik bilgilerini, şifresi çözülmüş belirteçleri ve kriptografik anahtarları kullanımdan hemen sonra dizileri sıfırlayarak silin (`Arrays.fill(bytes, (byte) 0)`).
* **Bellek İçi Güvenli Dosya Yürütme:** `NightAuthApp.downloadFile()` ile korunan modülleri indirirken **baytları asla diske veya fiziksel depolamaya yazmayın**. Bayt kodunu özel bir `ClassLoader` (`defineClass`) aracılığıyla doğrudan belleğe yükleyin ve geçici arabellekleri anında temizleyin.

---

## 📦 Gereksinimler & Derleme

**JDK 17** veya üstünü gerektirir (OpenJDK 17, 21 ve 25 LTS sürümlerinde test edilmiştir). Sıfır harici bağımlılık — tamamen standart JDK kütüphaneleriyle oluşturulmuştur (`java.net.http`, `javax.crypto`, `java.security`).

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

## ⚙️ Yapılandırma & Başlatma

Uygulama kimlik bilgilerinizi NightAuth kontrol panelinin **Manage Apps** sekmesinden tanımlayın:

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

### Kriptografik El Sıkışmayı Başlatma (`init`)

Diğer SDK işlevlerini yürütmeden önce `init()` işlevini çağırmalısınız. Bu, kısa ömürlü bir Diffie-Hellman (X25519) anahtar değişimi ve şifrelenmiş bir oturum kurar:

```java
NightAuthApp.init();

if (!NightAuthApp.response.success) {
    System.err.printf("Initialization failed: %s\n", NightAuthApp.response.message);
    System.exit(1);
}
```

---

## 🚀 SDK Kullanım Kılavuzu

### 1. Oturum Doğrulama ve Kara Liste Durumu Kontrolü

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

### 2. Kullanıcı Adı ve Şifre ile Giriş (ve 2FA)

```java
if (NightAuthApp.login(username, password, totpCode)) {
    System.out.printf("Welcome back, %s!\n", NightAuthApp.user.username);
} else {
    System.err.printf("Login failed: %s\n", NightAuthApp.response.message);
}
```

### 3. Lisans Anahtarı ile Kayıt Olma

```java
if (NightAuthApp.register(username, password, licenseKey, email)) {
    System.out.printf("Registration successful: %s!\n", NightAuthApp.user.username);
} else {
    System.err.printf("Registration failed: %s\n", NightAuthApp.response.message);
}
```

### 4. Yalnızca Lisans Anahtarı ile Kimlik Doğrulama (ve 2FA)

```java
// Authenticate using license key alone or with 2FA TOTP code
if (NightAuthApp.license(licenseKey, totpCode)) {
    System.out.printf("License valid! Logged in as: %s\n", NightAuthApp.user.username);
} else {
    System.err.printf("License authentication failed: %s\n", NightAuthApp.response.message);
}
```

### 5. Hesabı Yükselt / Lisans Anahtarı Kullan

```java
if (NightAuthApp.upgrade(username, licenseKey)) {
    System.out.printf("Account upgraded: %s\n", NightAuthApp.response.message);
} else {
    System.err.printf("Upgrade failed: %s\n", NightAuthApp.response.message);
}
```

### 6. Donanım Kimliğini Sıfırla (HWID Kilidi)

```java
if (NightAuthApp.resetHwid(username, password, totpCode)) {
    System.out.println("Hardware fingerprint reset successfully.");
} else {
    System.err.printf("HWID reset failed: %s\n", NightAuthApp.response.message);
}
```

### 7. Şifremi Unuttum / Hesap Kurtarma

```java
// Step 1: Request recovery email link
NightAuthApp.forgotPassword("user@example.com");

// Step 2: Complete reset using token received via email
if (NightAuthApp.resetPassword(resetToken, newPassword)) {
    System.out.println("Password updated successfully.");
}
```

### 8. Kullanıcı Profil Verileri (`NightAuthApp.user`)

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

### 9. Bulut Değişkenleri (Genel)

```java
String motd = NightAuthApp.getGlobalVar("server_motd");
System.out.printf("Announcement: %s\n", motd);
```

### 10. Kullanıcı Değişkenleri

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

### 11. Uygulama Denetim Günlükleri

```java
NightAuthApp.log("User initialized workflow #42");
```

### 12. Kullanıcıyı Yasakla ve Cihazı Kara Listeye Al

```java
NightAuthApp.ban("Security tampering detected");
```

### 13. Sunucu Taraflı Webhook'lar ve Dosya Yükleme

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

### 14. Bellek İçi Güvenli Dosya İndirme

```java
byte[] fileData = NightAuthApp.downloadFile("asset_file_id");
if (fileData != null) {
    // Process or load payload directly into memory (e.g. ClassLoader defineClass)
    // Wipe buffer from memory immediately after execution:
    java.util.Arrays.fill(fileData, (byte) 0);
}
```

### 15. Gerçek Zamanlı Sohbet Kanalları

```java
// Fetch recent message history
List<NightAuthApi.ChatMessage> messages = NightAuthApp.getChatMessages("general", 0L, true);
for (NightAuthApi.ChatMessage msg : messages) {
    System.out.printf("[%s] %s: %s\n", msg.timestamp, msg.author, msg.message);
}

// Send a message to a channel
NightAuthApp.sendChatMessage("Hello from Java SDK!", "general");
```

### 16. Gerçek Zamanlı Olay Akışı (Server-Sent Events)

```java
NightAuthApi.EventSubscription sub = NightAuthApp.subscribeEvents((payload) -> {
    System.out.printf("[EVENT] %s\n", payload);
});

// Close event stream when finished
sub.close();
```

### 17. İki Faktörlü Kimlik Doğrulama (2FA)

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

### 18. Oturum Yaşam Döngüsü ve Güvenli Çıkış

```java
// Keep session active with periodic ping
NightAuthApp.heartbeat();

// Revoke session on cloud and close client
NightAuthApp.logout();
NightAuthApp.close();
```

### 19. Hesap Kullanıcı Adını Değiştirme

```java
if (NightAuthApp.changeUsername("new_alias", "CurrentPassword123!")) {
    System.out.printf("Username changed successfully to: %s\n", NightAuthApp.user.username);
} else {
    System.err.printf("Change username failed: %s\n", NightAuthApp.response.message);
}
```

### 20. Uygulama Telemetrisi ve Çevrimiçi Kullanıcılar

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

### 21. Dosya Bütünlüğü ve Sağlama (Hash) Doğrulaması

```java
String fileSha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
if (NightAuthApp.verifyFileHash(fileSha256)) {
    System.out.println("Binary matches registered server checksum.");
} else {
    System.err.println("Integrity verification failed!");
}
```

### 22. Donanım Parmak İzi (`getHardwareId`)

```java
// Retrieve stable hardware fingerprint identifier
String hwid = NightAuthApi.getHardwareId();
System.out.printf("Device HWID: %s\n", hwid);
```

### 23. Bağımsız Seçenekler, Zaman Aşımı ve Özel Sunucu Anahtarları

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

### 24. İmzalı Otomatik Güncellemeler

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

## 🖥️ Konsol ve Masaüstü İstemcilerini Çalıştırma

```bash
# Interactive Terminal Console Client
javac -d build sdk/src/main/java/com/nightauth/sdk/NightAuthApi.java console/src/main/java/com/nightauth/console/Main.java
java -cp build com.nightauth.console.Main

# Modern Java Swing Desktop Client
javac -d build sdk/src/main/java/com/nightauth/sdk/NightAuthApi.java desktop/Main.java
java -cp build com.nightauth.desktop.Main
```

---

## 📄 Lisans

Bu proje **Elastic License 2.0** kapsamında lisanslanmıştır.

* Yazılımı üçüncü taraflara barındırılan veya yönetilen bir hizmet olarak sağlayamazsınız.
* Lisans anahtarı işlevselliğini taşıyamaz, değiştiremez, devre dışı bırakamaz veya atlatamazsınız.
* Hiçbir lisans veya telif hakkı bildirimini değiştiremez, kaldıramaz veya gizleyemezsiniz.

Tam yasal metin için [LICENSE](../LICENSE) dosyasına bakın.
