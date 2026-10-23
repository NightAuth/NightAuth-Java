# NightAuth Java SDK — Donnez-nous une étoile sur GitHub ⭐

SDK officiel de **NightAuth** pour Java et suite de clients de référence (interface graphique moderne en **Java Swing** et CLI interactive pour terminal). Protégez votre logiciel Java / JVM contre le piratage, l'ingénierie inverse et la falsification avec gestion des licences cloud, verrouillage HWID, chat chiffré en temps réel et exécution sécurisée en mémoire vive en quelques minutes.

Portail Officiel: [https://nightauth.org](https://nightauth.org)

---

**Languages / Idiomas / Langues / Sprachen:**  
[English](../README.md) • [Español](es.md) • [Français](fr.md) • [Deutsch](de.md) • [Italiano](it.md) • [Português](pt.md) • [Русский](ru.md) • [Türkçe](tr.md) • [Polski](pl.md) • [中文](zh.md)

---

> [!NOTE]
> ### 📺 Tutoriels Vidéo et Guides de Configuration
> Les tutoriels vidéo officiels et les guides pas à pas expliquant la configuration des applications, abonnements, variables cloud, webhooks et licences sur le tableau de bord NightAuth **seront bientôt publiés** sur notre portail et chaîne YouTube officielle.

---

## 📸 Captures d'Écran

### Application de Bureau (Thème Sombre Moderne Java Swing Obsidian)

| Écran d'Accueil / Authentification | Tableau de Bord & Gestion des Abonnements |
| :---: | :---: |
| ![NightAuth Desktop Login](../image/desktop.png) | ![NightAuth Desktop Dashboard](../image/desktop-dashboard.png) |

### Application Console (CLI Interactive de Terminal)

| Écran de Connexion Terminal | Tableau de Bord Interactif Console |
| :---: | :---: |
| ![NightAuth Console Login](../image/console.png) | ![NightAuth Console Dashboard](../image/console-dashboard.png) |

---

## 🛡️ Pratiques de Sécurité Recommandées

* **Obfuscation & Virtualisation du Bytecode:** Le bytecode Java (.class) peut être décompilé facilement avec CFR, Procyon, Fernflower ou JD-GUI. Pour la production, protégez vos classes et JARs avec des outils comme **Zelix KlassMaster (ZKM)**, **ProGuard** ou **DexGuard** pour chiffrer les chaînes littérales, renommer les symboles et aplatir le flux d'exécution.
* **Compilation Native Ahead-Of-Time (AOT):** Compilez votre code Java en exécutables natifs autonomes (.exe sur Windows, ELF sur Linux) avec **GraalVM Native Image** (`native-image -jar MyApp.jar`). Cela supprime entièrement le bytecode JVM et désarme les décompilateurs traditionnels.
* **Purge des Tampons en Mémoire:** Nettoyez immédiatement les identifiants sensibles, tokens déchiffrés et clés cryptographiques après usage en remplissant les tableaux avec des zéros (`Arrays.fill(bytes, (byte) 0)`).
* **Exécution Sécurisée de Fichiers en Mémoire:** Lors du téléchargement de modules protégés avec `NightAuthApp.downloadFile()`, **n'écrivez jamais les octets sur disque**. Chargez le bytecode directement en mémoire via un `ClassLoader` personnalisé (`defineClass`) et purgez immédiatement les tampons temporaires.

---

## 📦 Prérequis & Compilation

Requiert **JDK 17** ou ultérieur (testé sur OpenJDK 17, 21 et 25 LTS). Zéro dépendance externe — développé exclusivement avec les bibliothèques standard du JDK (`java.net.http`, `javax.crypto`, `java.security`).

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

## ⚙️ Configuration & Initialisation

Définissez les identifiants de votre application depuis l'onglet **Manage Apps** du tableau de bord NightAuth :

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

### Initialiser le Handshake Cryptographique (`init`)

Vous devez appeler `init()` avant d'exécuter toute autre fonction du SDK. Cela établit un échange de clés éphémère Diffie-Hellman (X25519) et une session chiffrée :

```java
NightAuthApp.init();

if (!NightAuthApp.response.success) {
    System.err.printf("Initialization failed: %s\n", NightAuthApp.response.message);
    System.exit(1);
}
```

---

## 🚀 Guide d'Utilisation du SDK

### 1. Validation de Session et Vérification de Liste Noire

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

### 2. Connexion avec Nom d'Utilisateur et Mot de Passe (et 2FA)

```java
if (NightAuthApp.login(username, password, totpCode)) {
    System.out.printf("Welcome back, %s!\n", NightAuthApp.user.username);
} else {
    System.err.printf("Login failed: %s\n", NightAuthApp.response.message);
}
```

### 3. Inscription avec Clé de Licence

```java
if (NightAuthApp.register(username, password, licenseKey, email)) {
    System.out.printf("Registration successful: %s!\n", NightAuthApp.user.username);
} else {
    System.err.printf("Registration failed: %s\n", NightAuthApp.response.message);
}
```

### 4. Authentification Uniquement par Clé de Licence (et 2FA)

```java
// Authenticate using license key alone or with 2FA TOTP code
if (NightAuthApp.license(licenseKey, totpCode)) {
    System.out.printf("License valid! Logged in as: %s\n", NightAuthApp.user.username);
} else {
    System.err.printf("License authentication failed: %s\n", NightAuthApp.response.message);
}
```

### 5. Mettre à Niveau le Compte / Réclamer une Clé de Licence

```java
if (NightAuthApp.upgrade(username, licenseKey)) {
    System.out.printf("Account upgraded: %s\n", NightAuthApp.response.message);
} else {
    System.err.printf("Upgrade failed: %s\n", NightAuthApp.response.message);
}
```

### 6. Réinitialiser l'Identifiant Matériel (Verrouillage HWID)

```java
if (NightAuthApp.resetHwid(username, password, totpCode)) {
    System.out.println("Hardware fingerprint reset successfully.");
} else {
    System.err.printf("HWID reset failed: %s\n", NightAuthApp.response.message);
}
```

### 7. Récupération de Compte / Mot de Passe Oublié

```java
// Step 1: Request recovery email link
NightAuthApp.forgotPassword("user@example.com");

// Step 2: Complete reset using token received via email
if (NightAuthApp.resetPassword(resetToken, newPassword)) {
    System.out.println("Password updated successfully.");
}
```

### 8. Données du Profil Utilisateur (`NightAuthApp.user`)

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

### 9. Variables Cloud (Globales)

```java
String motd = NightAuthApp.getGlobalVar("server_motd");
System.out.printf("Announcement: %s\n", motd);
```

### 10. Variables Personnalisées Utilisateur

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

### 11. Journaux d'Audit de l'Application

```java
NightAuthApp.log("User initialized workflow #42");
```

### 12. Bannir l'Utilisateur et Bloquer la Machine

```java
NightAuthApp.ban("Security tampering detected");
```

### 13. Webhooks Côté Serveur et Envoi de Fichiers

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

### 14. Téléchargement Sécurisé de Fichiers en Mémoire

```java
byte[] fileData = NightAuthApp.downloadFile("asset_file_id");
if (fileData != null) {
    // Process or load payload directly into memory (e.g. ClassLoader defineClass)
    // Wipe buffer from memory immediately after execution:
    java.util.Arrays.fill(fileData, (byte) 0);
}
```

### 15. Salons de Discussion en Temps Réel

```java
// Fetch recent message history
List<NightAuthApi.ChatMessage> messages = NightAuthApp.getChatMessages("general", 0L, true);
for (NightAuthApi.ChatMessage msg : messages) {
    System.out.printf("[%s] %s: %s\n", msg.timestamp, msg.author, msg.message);
}

// Send a message to a channel
NightAuthApp.sendChatMessage("Hello from Java SDK!", "general");
```

### 16. Flux d'Événements en Temps Réel (Server-Sent Events)

```java
NightAuthApi.EventSubscription sub = NightAuthApp.subscribeEvents((payload) -> {
    System.out.printf("[EVENT] %s\n", payload);
});

// Close event stream when finished
sub.close();
```

### 17. Authentification à Deux Facteurs (2FA)

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

### 18. Cycle de Vie de Session et Déconnexion

```java
// Keep session active with periodic ping
NightAuthApp.heartbeat();

// Revoke session on cloud and close client
NightAuthApp.logout();
NightAuthApp.close();
```

### 19. Modifier le Nom d'Utilisateur du Compte

```java
if (NightAuthApp.changeUsername("new_alias", "CurrentPassword123!")) {
    System.out.printf("Username changed successfully to: %s\n", NightAuthApp.user.username);
} else {
    System.err.printf("Change username failed: %s\n", NightAuthApp.response.message);
}
```

### 20. Télémétrie de l'Application et Utilisateurs en Ligne

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

### 21. Intégrité des Fichiers et Vérification du Hash

```java
String fileSha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
if (NightAuthApp.verifyFileHash(fileSha256)) {
    System.out.println("Binary matches registered server checksum.");
} else {
    System.err.println("Integrity verification failed!");
}
```

### 22. Empreinte Matérielle (`getHardwareId`)

```java
// Retrieve stable hardware fingerprint identifier
String hwid = NightAuthApi.getHardwareId();
System.out.printf("Device HWID: %s\n", hwid);
```

### 23. Options Avancées, Délai d'Attente et Clés Serveur

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

### 24. Mises à Jour Signées Cryptographiquement

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

## 🖥️ Exécuter les Clients Console et Bureau

```bash
# Interactive Terminal Console Client
javac -d build sdk/src/main/java/com/nightauth/sdk/NightAuthApi.java console/src/main/java/com/nightauth/console/Main.java
java -cp build com.nightauth.console.Main

# Modern Java Swing Desktop Client
javac -d build sdk/src/main/java/com/nightauth/sdk/NightAuthApi.java desktop/Main.java
java -cp build com.nightauth.desktop.Main
```

---

## 📄 Licence

Ce projet est sous licence **Elastic License 2.0**.

* Vous ne pouvez pas fournir le logiciel à des tiers en tant que service géré ou hébergé.
* Vous ne pouvez pas déplacer, modifier, désactiver ou contourner la fonctionnalité de clé de licence.
* Vous ne pouvez pas modifier, supprimer ou masquer les mentions de licence ou de droits d'auteur.

Consultez le fichier [LICENSE](../LICENSE) pour le texte légal complet.
