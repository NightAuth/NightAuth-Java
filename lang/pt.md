# NightAuth Java SDK — Dê-nos uma estrela no GitHub ⭐

SDK oficial da **NightAuth** para Java e conjunto de clientes de referência (interface gráfica moderna em **Java Swing** e CLI interativa para terminal). Proteja seu software Java / JVM contra pirataria, engenharia reversa e adulterações com licenciamento em nuvem, bloqueio por HWID, chat criptografado em tempo real e download seguro de módulos na memória em minutos.

Portal Oficial: [https://nightauth.org](https://nightauth.org)

---

**Languages / Idiomas / Langues / Sprachen:**  
[English](../README.md) • [Español](es.md) • [Français](fr.md) • [Deutsch](de.md) • [Italiano](it.md) • [Português](pt.md) • [Русский](ru.md) • [Türkçe](tr.md) • [Polski](pl.md) • [中文](zh.md)

---

> [!NOTE]
> ### 📺 Tutoriais em Vídeo e Guias de Configuração
> Tutoriais em vídeo oficiais e guias detalhados passo a passo sobre como configurar aplicativos, planos, assinaturas, variáveis na nuvem, webhooks e licenças no Painel NightAuth **serão publicados em breve** em nosso portal e canal oficial do YouTube.

---

## 📸 Capturas de Tela

### Aplicativo Desktop (Tema Escuro Moderno Java Swing Obsidian)

| Tela Inicial / Autenticação | Painel de Controle & Gestão de Assinaturas |
| :---: | :---: |
| ![NightAuth Desktop Login](../image/desktop.png) | ![NightAuth Desktop Dashboard](../image/desktop-dashboard.png) |

### Aplicativo de Console (CLI Interativa de Terminal)

| Tela de Login no Terminal | Painel Interativo de Console |
| :---: | :---: |
| ![NightAuth Console Login](../image/console.png) | ![NightAuth Console Dashboard](../image/console-dashboard.png) |

---

## 🛡️ Práticas de Segurança Recomendadas

* **Ofuscação e Virtualização de Bytecode:** O bytecode Java (.class) pode ser facilmente descompilado com ferramentas como CFR, Procyon, Fernflower ou JD-GUI. Para ambientes de produção, proteja suas classes e JARs com ofuscadores comerciais como **Zelix KlassMaster (ZKM)**, **ProGuard** ou **DexGuard** para criptografar strings, renomear símbolos e achatar o fluxo de controle.
* **Compilação Nativa Ahead-Of-Time (AOT):** Compile o código Java em executáveis nativos independentes (.exe no Windows, ELF no Linux) com o **GraalVM Native Image** (`native-image -jar MyApp.jar`). Isso remove completamente o bytecode JVM e neutraliza descompiladores convencionais.
* **Limpeza de Buffers na Memória:** Limpe credenciais confidenciais, tokens descriptografados e chaves criptográficas imediatamente após o uso preenchendo arrays com zeros (`Arrays.fill(bytes, (byte) 0)`).
* **Execução Segura de Arquivos em Memória:** Ao baixar módulos protegidos ou JARs com `NightAuthApp.downloadFile()`, **nunca salve os bytes no armazenamento físico ou disco**. Carregue o bytecode diretamente na memória através de um `ClassLoader` personalizado (`defineClass`) e descarte os buffers temporários imediatamente.

---

## 📦 Requisitos & Compilação

Requer **JDK 17** ou superior (testado no OpenJDK 17, 21 e 25 LTS). Zero dependências externas — construído exclusivamente com bibliotecas padrão do JDK (`java.net.http`, `javax.crypto`, `java.security`).

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

## ⚙️ Configuração & Inicialização

Defina as credenciais do seu aplicativo na guia **Manage Apps** do painel do NightAuth:

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

### Inicializar o Handshake Criptográfico (`init`)

Você deve chamar `init()` antes de executar qualquer outra função do SDK. Isso estabelece uma troca efêmera de chaves Diffie-Hellman (X25519) e uma sessão criptografada:

```java
NightAuthApp.init();

if (!NightAuthApp.response.success) {
    System.err.printf("Initialization failed: %s\n", NightAuthApp.response.message);
    System.exit(1);
}
```

---

## 🚀 Guia de Uso do SDK

### 1. Validação de Sessão e Verificação de Lista Negra

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

### 2. Login com Nome de Usuário e Senha (e 2FA)

```java
if (NightAuthApp.login(username, password, totpCode)) {
    System.out.printf("Welcome back, %s!\n", NightAuthApp.user.username);
} else {
    System.err.printf("Login failed: %s\n", NightAuthApp.response.message);
}
```

### 3. Registro com Chave de Licença

```java
if (NightAuthApp.register(username, password, licenseKey, email)) {
    System.out.printf("Registration successful: %s!\n", NightAuthApp.user.username);
} else {
    System.err.printf("Registration failed: %s\n", NightAuthApp.response.message);
}
```

### 4. Autenticação Apenas com Chave de Licença (e 2FA)

```java
// Authenticate using license key alone or with 2FA TOTP code
if (NightAuthApp.license(licenseKey, totpCode)) {
    System.out.printf("License valid! Logged in as: %s\n", NightAuthApp.user.username);
} else {
    System.err.printf("License authentication failed: %s\n", NightAuthApp.response.message);
}
```

### 5. Atualizar Conta / Resgatar Chave de Licença

```java
if (NightAuthApp.upgrade(username, licenseKey)) {
    System.out.printf("Account upgraded: %s\n", NightAuthApp.response.message);
} else {
    System.err.printf("Upgrade failed: %s\n", NightAuthApp.response.message);
}
```

### 6. Redefinir ID de Hardware (Bloqueio de HWID)

```java
if (NightAuthApp.resetHwid(username, password, totpCode)) {
    System.out.println("Hardware fingerprint reset successfully.");
} else {
    System.err.printf("HWID reset failed: %s\n", NightAuthApp.response.message);
}
```

### 7. Recuperação de Conta / Esqueceu a Senha

```java
// Step 1: Request recovery email link
NightAuthApp.forgotPassword("user@example.com");

// Step 2: Complete reset using token received via email
if (NightAuthApp.resetPassword(resetToken, newPassword)) {
    System.out.println("Password updated successfully.");
}
```

### 8. Dados do Perfil de Usuário (`NightAuthApp.user`)

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

### 9. Variáveis na Nuvem (Globais)

```java
String motd = NightAuthApp.getGlobalVar("server_motd");
System.out.printf("Announcement: %s\n", motd);
```

### 10. Variáveis do Usuário

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

### 11. Logs de Auditoria do Aplicativo

```java
NightAuthApp.log("User initialized workflow #42");
```

### 12. Banir Usuário e Bloquear Dispositivo

```java
NightAuthApp.ban("Security tampering detected");
```

### 13. Webhooks no Servidor e Envio de Arquivos

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

### 14. Download Seguro de Arquivos na Memória RAM

```java
byte[] fileData = NightAuthApp.downloadFile("asset_file_id");
if (fileData != null) {
    // Process or load payload directly into memory (e.g. ClassLoader defineClass)
    // Wipe buffer from memory immediately after execution:
    java.util.Arrays.fill(fileData, (byte) 0);
}
```

### 15. Canais de Chat em Tempo Real

```java
// Fetch recent message history
List<NightAuthApi.ChatMessage> messages = NightAuthApp.getChatMessages("general", 0L, true);
for (NightAuthApi.ChatMessage msg : messages) {
    System.out.printf("[%s] %s: %s\n", msg.timestamp, msg.author, msg.message);
}

// Send a message to a channel
NightAuthApp.sendChatMessage("Hello from Java SDK!", "general");
```

### 16. Transmissão de Eventos em Tempo Real (Server-Sent Events)

```java
NightAuthApi.EventSubscription sub = NightAuthApp.subscribeEvents((payload) -> {
    System.out.printf("[EVENT] %s\n", payload);
});

// Close event stream when finished
sub.close();
```

### 17. Autenticação de Dois Fatores (2FA)

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

### 18. Ciclo de Vida da Sessão e Logout

```java
// Keep session active with periodic ping
NightAuthApp.heartbeat();

// Revoke session on cloud and close client
NightAuthApp.logout();
NightAuthApp.close();
```

### 19. Alterar Nome de Usuário da Conta

```java
if (NightAuthApp.changeUsername("new_alias", "CurrentPassword123!")) {
    System.out.printf("Username changed successfully to: %s\n", NightAuthApp.user.username);
} else {
    System.err.printf("Change username failed: %s\n", NightAuthApp.response.message);
}
```

### 20. Telemetria do Aplicativo e Usuários Online

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

### 21. Integridade de Arquivos e Verificação de Hash

```java
String fileSha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
if (NightAuthApp.verifyFileHash(fileSha256)) {
    System.out.println("Binary matches registered server checksum.");
} else {
    System.err.println("Integrity verification failed!");
}
```

### 22. Identificador de Hardware (`getHardwareId`)

```java
// Retrieve stable hardware fingerprint identifier
String hwid = NightAuthApi.getHardwareId();
System.out.printf("Device HWID: %s\n", hwid);
```

### 23. Opções Avançadas, Timeout e Chaves de Servidor Personalizadas

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

### 24. Atualizações com Assinatura Criptográfica

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

## 🖥️ Executar os Clientes de Console e Desktop

```bash
# Interactive Terminal Console Client
javac -d build sdk/src/main/java/com/nightauth/sdk/NightAuthApi.java console/src/main/java/com/nightauth/console/Main.java
java -cp build com.nightauth.console.Main

# Modern Java Swing Desktop Client
javac -d build sdk/src/main/java/com/nightauth/sdk/NightAuthApi.java desktop/Main.java
java -cp build com.nightauth.desktop.Main
```

---

## 📄 Licença

Este projeto está licenciado sob a **Elastic License 2.0**.

* Você não pode fornecer o software a terceiros como um serviço hospedado ou gerenciado.
* Você não pode mover, alterar, desativar ou contornar a funcionalidade de chave de licença.
* Você não pode alterar, remover ou ocultar quaisquer avisos de licença ou direitos autorais.

Consulte o arquivo [LICENSE](../LICENSE) para o texto legal completo.
