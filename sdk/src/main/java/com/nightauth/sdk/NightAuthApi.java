
package com.nightauth.sdk;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.EdECPublicKey;
import java.security.interfaces.XECPublicKey;
import java.security.spec.EdECPoint;
import java.security.spec.EdECPublicKeySpec;
import java.security.spec.NamedParameterSpec;
import java.security.spec.XECPublicKeySpec;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.zip.GZIPInputStream;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class NightAuthApi {

    public static final String DEFAULT_API_URL = "https://nightauth.org/api/v1";
    public static final String DEFAULT_SIGNING_PUBLIC_KEY = "cdb2285db481666ce40003aadb874c8751dbeb9a0672a37f740dfe881ecdc742";
    public static final String DEFAULT_SEALED_BOX_PUBLIC_KEY = "b773fc4f6cf1a0938e78e714c3d84cb386584bea24728923008471a6cd203213";
    public static final String SERVER_SIGNING_PUBLIC_KEY = DEFAULT_SIGNING_PUBLIC_KEY;
    public static final String SERVER_SEALED_BOX_PUBLIC_KEY = DEFAULT_SEALED_BOX_PUBLIC_KEY;

    public static final byte[] SEAL_INFO = "nightauth-v1-init-seal".getBytes(StandardCharsets.UTF_8);
    public static final byte[] E2E_INFO = "nightauth-v1-e2e".getBytes(StandardCharsets.UTF_8);
    public static final byte[] HKDF_INFO = E2E_INFO;

    private static final long MAX_REQUEST_BYTES = 64L * 1024 * 1024;
    private static final long MAX_RESPONSE_BYTES = 5L * 1024 * 1024;

    private static final long DOWNLOAD_TIMEOUT_MS = 300_000;

    private static final long MAX_FILE_BYTES = 64L * 1024 * 1024;

    private static final long MAX_FILE_RESPONSE_BYTES = 96L * 1024 * 1024;
    private static final long TS_SKEW_MS = 90 * 1000L;

    public static class Response {
        public boolean success;
        public String message = "";
        public String error = "";

        public int statusCode;

        public double retryAfter;

        public Map<String, Object> payload;
    }

    public static class SubscriptionData {
        public String subscription = "";
        public String key = "";
        public String expiry = "";
        public long timeLeft;
    }

    public static class UserData {
        public String username = "";
        public String email = "";
        public String hwid = "";
        public String ip = "";
        public String status = "";
        public String createdAt = "";
        public String lastLoginAt = "";
        public boolean twoFactorEnabled;
        public List<SubscriptionData> subscriptions = new ArrayList<>();
    }

    public static class AppStats {
        public long totalUsers;
        public long activeUsers;
        public long bannedUsers;
        public long totalLicenses;
        public long usedLicenses;
        public long unusedLicenses;
        public String version = "";
    }

    public static class ChatMessage {
        public String id = "";
        public String author = "";
        public String message = "";
        public String timestamp = "";
    }

    public static class TwoFactorSetupData {
        public String secret = "";
        public String qrDataUrl = "";
        public String uri = "";
    }

    public static class TwoFactorConfirmData {
        public List<String> backupCodes = new ArrayList<>();
    }

    public static class ProtocolException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public ProtocolException(String message) { super(message); }
    }

    public static class CryptographicException extends ProtocolException {
        private static final long serialVersionUID = 1L;
        public CryptographicException(String message) { super(message); }
    }

    public static final class PinnedKeys {
        public final String signingPublicKeyHex;
        public final String sealedBoxPublicKeyHex;

        public PinnedKeys(String signingPublicKeyHex, String sealedBoxPublicKeyHex) {
            if (signingPublicKeyHex == null || signingPublicKeyHex.isEmpty()) throw new IllegalArgumentException("signingPublicKeyHex cannot be empty");
            if (sealedBoxPublicKeyHex == null || sealedBoxPublicKeyHex.isEmpty()) throw new IllegalArgumentException("sealedBoxPublicKeyHex cannot be empty");
            this.signingPublicKeyHex = signingPublicKeyHex;
            this.sealedBoxPublicKeyHex = sealedBoxPublicKeyHex;
        }

        public static PinnedKeys defaults() {
            return new PinnedKeys(SERVER_SIGNING_PUBLIC_KEY, SERVER_SEALED_BOX_PUBLIC_KEY);
        }
    }

    public String name;
    public String apiKey;
    public String version;
    public String token;

    public Response response = new Response();
    public UserData user = new UserData();
    public volatile String pendingDownloadUrl = "";
    public volatile String requiredVersion = "";

    private final boolean allowInsecureLoopback;
    private final HttpClient httpClient;

    private final String apiUrl;

    private final String pinnedSigningKey;

    private final String pinnedSealedBoxKey;

    private final AtomicLong generation = new AtomicLong(0);

    private final ReentrantLock stateLock = new ReentrantLock();

    private volatile String sessionId;
    private volatile byte[] sharedKey;
    private volatile boolean initialized;

    public NightAuthApi(String name, String apiKey, String version, String token) {
        this(name, apiKey, version, token, DEFAULT_API_URL, false);
    }

    public NightAuthApi(String name, String apiKey, String version, String token, String apiUrl) {
        this(name, apiKey, version, token, apiUrl, false);
    }

    public NightAuthApi(String name, String apiKey, String version, String token, String apiUrl, boolean allowInsecureLoopback) {
        this(name, apiKey, version, token, apiUrl, allowInsecureLoopback, PinnedKeys.defaults());
    }

    public NightAuthApi(String name, String apiKey, String version, String token, String apiUrl, boolean allowInsecureLoopback,
                         PinnedKeys pinnedKeys) {
        this(name, apiKey, version, token, apiUrl, allowInsecureLoopback,
                (pinnedKeys != null ? pinnedKeys : PinnedKeys.defaults()).signingPublicKeyHex,
                (pinnedKeys != null ? pinnedKeys : PinnedKeys.defaults()).sealedBoxPublicKeyHex);
    }

    NightAuthApi(String name, String apiKey, String version, String token, String apiUrl, boolean allowInsecureLoopback,
                 String pinnedSigningKeyOverride, String pinnedSealedBoxKeyOverride) {
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("name cannot be empty");
        if (apiKey == null || apiKey.isEmpty()) throw new IllegalArgumentException("apiKey cannot be empty");
        this.name = name;
        this.apiKey = apiKey;
        this.version = (version != null && !version.isEmpty()) ? version : "1.0";
        this.token = token != null ? token : "";
        String normalizedUrl = (apiUrl != null && !apiUrl.isEmpty()) ? apiUrl : DEFAULT_API_URL;
        if (normalizedUrl.endsWith("/")) normalizedUrl = normalizedUrl.substring(0, normalizedUrl.length() - 1);
        this.allowInsecureLoopback = allowInsecureLoopback;
        this.pinnedSigningKey = pinnedSigningKeyOverride;
        this.pinnedSealedBoxKey = pinnedSealedBoxKeyOverride;

        URI base = URI.create(normalizedUrl);
        String host = base.getHost();
        boolean loopback = "localhost".equals(host) || "127.0.0.1".equals(host) || "::1".equals(host) || "[::1]".equals(host);
        boolean https = "https".equalsIgnoreCase(base.getScheme());
        boolean httpLoopbackOptIn = "http".equalsIgnoreCase(base.getScheme()) && loopback && allowInsecureLoopback;
        if (!https && !httpLoopbackOptIn) {
            throw new IllegalArgumentException("HTTPS is required; insecure loopback HTTP requires explicit allowInsecureLoopback=true");
        }
        this.apiUrl = normalizedUrl;

        HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NEVER);
        if ("http".equalsIgnoreCase(base.getScheme())) {
            clientBuilder.version(HttpClient.Version.HTTP_1_1);
        }
        this.httpClient = clientBuilder.build();
    }

    long requestTimeoutMs = 20_000;

    public void setRequestTimeoutMillis(long timeoutMs) {
        if (timeoutMs < 1) throw new IllegalArgumentException("timeoutMs must be at least 1");
        requestTimeoutMs = timeoutMs;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public java.util.function.Supplier<String> hardwareIdProvider;

    private String hardwareId() {
        return hardwareIdProvider != null ? hardwareIdProvider.get() : getHardwareId();
    }

    public static String getHardwareId() {
        try {
            String hostname = System.getenv("COMPUTERNAME");
            if (hostname == null || hostname.isEmpty()) hostname = System.getenv("HOSTNAME");
            if (hostname == null || hostname.isEmpty()) hostname = "unknown_host";
            hostname = hostname.trim().toUpperCase(java.util.Locale.ROOT);
            String username = System.getenv("USERNAME");
            if (username == null || username.isEmpty()) username = System.getenv("USER");
            if (username == null || username.isEmpty()) username = "unknown_user";
            username = username.trim().toLowerCase(java.util.Locale.ROOT);
            String raw = hostname + "-" + username + "-nightauth-hwid";
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "default_hwid_hash";
        }
    }

    public void init() {
        long myGeneration;
        stateLock.lock();
        try {
            myGeneration = generation.incrementAndGet();
            clearSessionFieldsLocked();
        } finally {
            stateLock.unlock();
        }

        String newSessionId = UUID.randomUUID().toString().replace("-", "");
        try {
            KeyPair clientKp = generateX25519KeyPair();
            byte[] clientPubRaw = x25519PublicRaw((XECPublicKey) clientKp.getPublic());
            String k1Hex = hex(clientPubRaw);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("apikey", apiKey);
            payload.put("name", name);
            payload.put("version", version);
            payload.put("id", newSessionId);
            payload.put("k1", k1Hex);

            String sealedB64 = sealInit(payload, pinnedSealedBoxKey);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("p", sealedB64);

            HttpResp raw;
            try {
                raw = postRaw("/init", Json.write(body));
            } catch (Exception transport) {
                publishInitOutcomeIfCurrent(myGeneration, null, null, false);
                applyResponse(transportFailure(transport));
                return;
            }
            Object parsedObj = parseObjectOrNull(raw.body);
            if (!(parsedObj instanceof Map)) {
                publishInitOutcomeIfCurrent(myGeneration, null, null, false);
                applyResponse(clientFailure("invalid_response", "Response body is not a JSON object.", raw.status));
                return;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = (Map<String, Object>) parsedObj;
            resp.put("_status", (long) raw.status);
            if (raw.retryAfter > 0) resp.put("_retryAfter", raw.retryAfter);

            if (!applyResponse(resp) || raw.status != 200) {

                Response refusal = copyOf(response);
                publishInitOutcomeIfCurrent(myGeneration, null, null, false);
                restoreInto(response, refusal);
                return;
            }
            Object k2Obj = resp.get("k2"), vkObj = resp.get("vk"), tsObj = resp.get("ts"), sigObj = resp.get("sig");
            if (!(k2Obj instanceof String) || !(vkObj instanceof String) || !(tsObj instanceof Number) || !(sigObj instanceof String)) {
                throw new CryptographicException("init response missing signature binding fields (k2/vk/ts/sig)");
            }
            String k2 = (String) k2Obj;
            String vk = (String) vkObj;
            long ts = ((Number) tsObj).longValue();
            String sig = (String) sigObj;

            if (!pinnedSigningKey.equalsIgnoreCase(vk)) {
                throw new CryptographicException("init response signing key does not match pinned key — refusing handshake");
            }
            if (!verifyInitSignature(k2, pinnedSigningKey, ts, sig)) {
                throw new CryptographicException("init response signature verification failed — refusing to trust this handshake");
            }
            long now = System.currentTimeMillis();
            if (ts < now - TS_SKEW_MS || ts > now + TS_SKEW_MS) {
                throw new CryptographicException("init response timestamp is stale/future — possible replay");
            }

            byte[] serverPubRaw = fromHex(k2);
            if (serverPubRaw.length != 32) throw new CryptographicException("invalid server ephemeral key length");
            XECPublicKey serverPub = x25519PublicFromRaw(serverPubRaw);
            byte[] secret = x25519Dh(clientKp.getPrivate(), serverPub);
            byte[] newSharedKey;
            try {
                newSharedKey = hkdfSha256(secret, E2E_INFO, 32);
            } finally {
                Arrays.fill(secret, (byte) 0);
            }

            boolean published = publishInitOutcomeIfCurrent(myGeneration, newSessionId, newSharedKey, true);
            if (!published) {

                Arrays.fill(newSharedKey, (byte) 0);
                throw new ProtocolException("init request superseded by newer session generation");
            }
            setSuccess("Initialized successfully.");
        } catch (ProtocolException pe) {
            publishInitOutcomeIfCurrent(myGeneration, null, null, false);
            throw pe;
        } catch (Exception e) {
            publishInitOutcomeIfCurrent(myGeneration, null, null, false);
            throw new ProtocolException("init failed: " + e.getMessage());
        }
    }

    private static Response copyOf(Response source) {
        Response copy = new Response();
        restoreInto(copy, source);
        return copy;
    }

    private static void restoreInto(Response target, Response source) {
        target.success = source.success;
        target.message = source.message;
        target.error = source.error;
        target.statusCode = source.statusCode;
        target.retryAfter = source.retryAfter;
        target.payload = source.payload;
    }

    private void clearSessionFieldsLocked() {
        if (sharedKey != null) Arrays.fill(sharedKey, (byte) 0);
        sharedKey = null;
        sessionId = null;
        initialized = false;
        user = new UserData();

        response.success = false;
        response.message = "";
        response.error = null;
    }

    private boolean publishInitOutcomeIfCurrent(long myGeneration, String newSessionId, byte[] newSharedKey, boolean success) {
        stateLock.lock();
        try {
            if (generation.get() != myGeneration) {
                if (newSharedKey != null) Arrays.fill(newSharedKey, (byte) 0);
                return false;
            }
            if (success) {
                this.sessionId = newSessionId;
                this.sharedKey = newSharedKey;
                this.initialized = true;
            } else {
                clearSessionFieldsLocked();
            }
            return true;
        } finally {
            stateLock.unlock();
        }
    }

    public boolean login(String username, String password, String totpCode) {
        Map<String, Object> body = basePayload();
        body.put("version", version);
        body.put("username", username);
        body.put("password", password);
        body.put("hwid", hardwareId());
        if (totpCode != null && !totpCode.isEmpty()) body.put("totpCode", totpCode);
        Map<String, Object> resp = sendEncrypted("/login", body);
        if (!applyResponse(resp)) { return false; }
        populateUserData(resp);
        setSuccess("Logged in successfully.");
        return true;
    }

    public boolean register(String username, String password, String key, String email) {
        Map<String, Object> body = basePayload();
        body.put("version", version);
        body.put("username", username);
        body.put("password", password);
        body.put("key", key);
        if (email != null && !email.isEmpty()) body.put("email", email);
        body.put("hwid", hardwareId());
        Map<String, Object> resp = sendEncrypted("/register", body);
        if (!applyResponse(resp)) { return false; }
        populateUserData(resp);
        setSuccess("Registered successfully.");
        return true;
    }

    public boolean license(String key) {
        return license(key, null);
    }

    public boolean license(String key, String totpCode) {
        Map<String, Object> body = basePayload();
        body.put("version", version);
        body.put("key", key);
        body.put("hwid", hardwareId());
        if (totpCode != null && !totpCode.isEmpty()) body.put("totpCode", totpCode);
        Map<String, Object> resp = sendEncrypted("/license", body);
        if (!applyResponse(resp)) { return false; }
        populateUserData(resp);
        setSuccess("License verified successfully.");
        return true;
    }

    public boolean upgrade(String username, String key) {
        Map<String, Object> body = basePayload();

        if (username != null && !username.isBlank()) body.put("username", username);
        body.put("key", key);
        Map<String, Object> resp = sendEncrypted("/upgrade", body);
        if (!applyResponse(resp)) { return false; }
        setSuccess("Subscription upgraded successfully.");
        return true;
    }

    public boolean heartbeat() {
        Map<String, Object> body = basePayload();
        body.put("version", version);
        Map<String, Object> resp = sendEncrypted("/heartbeat", body);
        if (!applyResponse(resp)) { return false; }
        setSuccess("Session alive.");
        return true;
    }

    public boolean check() {
        Map<String, Object> body = basePayload();
        body.put("version", version);
        Map<String, Object> resp = sendEncrypted("/check", body);
        if (!applyResponse(resp)) { return false; }
        populateUserData(resp);
        Object subs = resp.get("subscriptions");
        if (subs instanceof List && user.subscriptions.isEmpty()) {
            for (Object o : (List<?>) subs) {
                if (o instanceof Map) user.subscriptions.add(toSubscription((Map<?, ?>) o));
            }
        }
        setSuccess("Session valid.");
        return true;
    }

    public boolean logout() {

        if (!initialized || sharedKey == null || sessionId == null) {
            init();
            if (!initialized) return false;
        }
        long logoutGeneration;
        stateLock.lock();
        try {
            checkInit();
            logoutGeneration = generation.get();
        } finally {
            stateLock.unlock();
        }
        Map<String, Object> resp = logoutOnce(logoutGeneration);

        long now = generation.get();
        boolean ownClear = resp.get("_clearedGeneration") instanceof Number g && g.longValue() == now;
        if (isHandshakeExpired(resp) && (now == logoutGeneration || ownClear)) {
            boolean reinit;
            try {
                init();
                reinit = initialized;
            } catch (RuntimeException e) {
                reinit = false;
            }
            if (reinit) {
                logoutGeneration = generation.get();
                resp = logoutOnce(logoutGeneration);
            }
        }
        boolean confirmed = isSuccess(resp) && resp.get("_clientFailure") == null;
        stateLock.lock();
        try {

            long current = generation.get();
            boolean clearedByThisLogout = resp.get("_clearedGeneration") instanceof Number g && g.longValue() == current;
            if (current != logoutGeneration && !clearedByThisLogout) return false;
            generation.incrementAndGet();
            clearSessionFieldsLocked();
            if (confirmed || resp.get("_clientFailure") != null || "handshake_required".equals(resp.get("error"))) {

                applyResponse(resp);
                if (confirmed) setSuccess("Logged out successfully.");
            } else {
                int status = resp.get("_status") instanceof Number n ? n.intValue() : 0;
                setError("Local session cleared; server revocation unconfirmed.", "logout_unconfirmed");
                response.statusCode = status;
            }
        } finally {
            stateLock.unlock();
        }
        return confirmed;
    }

    private Map<String, Object> logoutOnce(long logoutGeneration) {
        try {
            return sendEncrypted("/logout", basePayload(), logoutGeneration);
        } catch (RuntimeException e) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("success", false);
            m.put("message", String.valueOf(e.getMessage()));
            return m;
        }
    }

    public boolean changeUsername(String newUsername, String password) {
        Map<String, Object> body = basePayload();
        body.put("newUsername", newUsername);
        body.put("password", password);
        Map<String, Object> resp = sendEncrypted("/change-username", body);
        if (!applyResponse(resp)) { return false; }
        user.username = newUsername;
        setSuccess("Username changed successfully.");
        return true;
    }

    public boolean resetHwid(String username, String password, String totpCode) {
        Map<String, Object> body = basePayload();
        body.put("username", username);
        body.put("password", password);
        if (totpCode != null && !totpCode.isEmpty()) body.put("totpCode", totpCode);
        Map<String, Object> resp = sendEncrypted("/hwid-reset", body);
        if (!applyResponse(resp)) { return false; }
        setSuccess(str(resp.get("message"), "HWID reset successfully."));
        return true;
    }

    public boolean ban(String reason) {
        Map<String, Object> body = basePayload();
        if (reason != null && !reason.isEmpty()) body.put("reason", reason);
        Map<String, Object> resp = sendEncrypted("/ban", body);
        if (!applyResponse(resp)) { return false; }
        setSuccess("Account banned.");
        return true;
    }

    public boolean checkBlacklist() {
        return checkBlacklist(null);
    }

    public boolean checkBlacklist(String hwid) {
        Map<String, Object> body = basePayload();
        body.put("hwid", (hwid != null && !hwid.isEmpty()) ? hwid : hardwareId());
        Map<String, Object> resp = sendEncrypted("/check-blacklist", body);
        if (!applyResponse(resp)) { return false; }
        setSuccess("Blacklist check completed.");
        return Boolean.TRUE.equals(resp.get("blacklisted"));
    }

    public boolean verifyFileHash(String hash) {
        Map<String, Object> body = basePayload();
        body.put("hash", hash);
        Map<String, Object> resp = sendEncrypted("/verify-hash", body);
        if (!applyResponse(resp)) { return false; }
        return Boolean.TRUE.equals(resp.get("verified"));
    }

    public boolean forgotPassword(String email) {
        Map<String, Object> body = basePayload();
        body.put("email", email);

        return applyResponse(sendEncrypted("/forgot-password", body));
    }

    public boolean resetPassword(String token, String newPassword) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("token", token);
        body.put("newPassword", newPassword);

        HttpResp raw;
        try {
            raw = postRaw("/reset-password", Json.write(body));
        } catch (Exception e) {
            applyResponse(transportFailure(e));
            return false;
        }
        Map<String, Object> resp = parseObjectOrNull(raw.body);
        if (resp == null) {
            applyResponse(clientFailure("invalid_response", "Response body is not a JSON object.", raw.status));
            return false;
        }
        resp.put("_status", (long) raw.status);
        if (raw.retryAfter > 0) resp.put("_retryAfter", raw.retryAfter);
        if (!applyResponse(resp)) return false;
        setSuccess("Password has been reset.");
        return true;
    }

    public TwoFactorSetupData setup2FA() {
        Map<String, Object> resp = sendEncrypted("/2fa/setup", basePayload());
        if (!applyResponse(resp)) { return null; }
        TwoFactorSetupData s = new TwoFactorSetupData();
        s.secret = str(resp.get("secret"), "");
        s.qrDataUrl = str(resp.get("qr"), "");
        s.uri = str(resp.get("uri"), "");
        return s;
    }

    public TwoFactorConfirmData confirm2FA(String code) {
        Map<String, Object> body = basePayload();
        body.put("code", code);
        Map<String, Object> resp = sendEncrypted("/2fa/confirm", body);
        if (!applyResponse(resp)) { return null; }
        TwoFactorConfirmData c = new TwoFactorConfirmData();
        Object arr = resp.get("backupCodes");
        if (arr instanceof List) for (Object o : (List<?>) arr) if (o != null) c.backupCodes.add(String.valueOf(o));
        return c;
    }

    public boolean disable2FA(String password, String code) {
        Map<String, Object> body = basePayload();
        body.put("password", password);
        body.put("code", code);
        Map<String, Object> resp = sendEncrypted("/2fa/disable", body);
        if (!applyResponse(resp)) { return false; }
        setSuccess("2FA disabled.");
        return true;
    }

    public String getGlobalVar(String key) {
        Map<String, Object> body = basePayload();
        body.put("varName", key);
        Map<String, Object> resp = sendEncrypted("/global-var", body);
        if (!applyResponse(resp)) { return ""; }
        return str(resp.get("value"), "");
    }

    public String getUserVar(String key) {
        Map<String, Object> body = basePayload();
        body.put("key", key);
        Map<String, Object> resp = sendEncrypted("/user-vars/get", body);
        if (!applyResponse(resp)) { return ""; }
        if (resp.get("value") != null) return str(resp.get("value"), "");
        Object vars = resp.get("variables");
        if (vars instanceof List) {
            for (Object o : (List<?>) vars) {
                if (o instanceof Map && key.equals(((Map<?, ?>) o).get("key"))) return str(((Map<?, ?>) o).get("value"), "");
            }
        }
        return "";
    }

    public boolean setUserVar(String key, String value) {
        Map<String, Object> body = basePayload();
        body.put("key", key);
        body.put("value", value);
        Map<String, Object> resp = sendEncrypted("/user-vars/set", body);
        if (!applyResponse(resp)) { return false; }
        setSuccess("User variable saved.");
        return true;
    }

    public boolean log(String message) {
        Map<String, Object> body = basePayload();
        body.put("message", message);
        Map<String, Object> resp = sendEncrypted("/log", body);
        return applyResponse(resp);
    }

    public AppStats getStats() {
        Map<String, Object> resp = sendEncrypted("/stats", basePayload());
        if (!applyResponse(resp)) { return null; }
        AppStats s = new AppStats();
        s.version = str(resp.get("version"), version);
        Object usersObj = resp.get("users");
        Object licObj = resp.get("licenses");
        Map<?, ?> u = usersObj instanceof Map ? (Map<?, ?>) usersObj : Collections.emptyMap();
        Map<?, ?> l = licObj instanceof Map ? (Map<?, ?>) licObj : Collections.emptyMap();
        s.totalUsers = num(u.get("total"));
        s.activeUsers = num(u.get("active"));
        s.bannedUsers = num(u.get("banned"));
        s.totalLicenses = num(l.get("total"));
        s.usedLicenses = num(l.get("used"));
        s.unusedLicenses = num(l.get("notUsed"));
        return s;
    }

    public List<String> getOnlineUsers() {
        Map<String, Object> resp = sendEncrypted("/online-users", basePayload());
        if (!applyResponse(resp)) { return Collections.emptyList(); }
        List<String> out = new ArrayList<>();
        Object arr = resp.get("users");
        if (arr instanceof List) for (Object o : (List<?>) arr) if (o != null) out.add(String.valueOf(o));
        return out;
    }

    public List<ChatMessage> getChatMessages(String channel) {
        Map<String, Object> body = basePayload();
        body.put("room", channel != null ? channel : "general");
        Map<String, Object> resp = sendEncrypted("/chat/get", body);
        if (!applyResponse(resp)) { return Collections.emptyList(); }
        List<ChatMessage> out = new ArrayList<>();
        Object arr = resp.get("messages");
        if (arr instanceof List) {
            for (Object o : (List<?>) arr) {
                if (!(o instanceof Map)) continue;
                Map<?, ?> m = (Map<?, ?>) o;
                ChatMessage cm = new ChatMessage();
                cm.id = str(m.get("id"), "");
                cm.author = str(m.get("author"), "");
                cm.message = str(m.get("content") != null ? m.get("content") : m.get("message"), "");
                cm.timestamp = str(m.get("timestamp") != null ? m.get("timestamp") : m.get("createdAt"), "");
                out.add(cm);
            }
        }
        return out;
    }

    public boolean sendChatMessage(String message, String channel) {
        Map<String, Object> body = basePayload();
        body.put("room", channel != null ? channel : "general");
        body.put("content", message);
        Map<String, Object> resp = sendEncrypted("/chat/send", body);
        if (!applyResponse(resp)) { return false; }
        setSuccess("Message sent.");
        return true;
    }

    public static final class WebhookResult {
        public boolean success;
        public int status;

        public String data = "";
        public boolean truncated;
    }

    public byte[] downloadFile(String fileId) {
        if (fileId == null || fileId.isEmpty() || fileId.length() > 128) {
            setError("File id is required (max 128 characters).", "invalid_request");
            return null;
        }
        String ticket = requestTicket("file", fileId);
        if (ticket == null) return null;

        HttpResp raw;
        try {
            raw = getRaw("/files/download?ticket=" + URLEncoder.encode(ticket, StandardCharsets.UTF_8), MAX_FILE_RESPONSE_BYTES);
        } catch (Exception e) {
            applyResponse(transportFailure(e));
            return null;
        }
        if (raw.status >= 300 && raw.status < 400) {

            setError("File download redirected to an external host; not followed.", "external_download");
            response.statusCode = raw.status;

            response.payload = new LinkedHashMap<>();
            if (raw.location != null) response.payload.put("url", raw.location);
            return null;
        }
        Map<String, Object> json = parseObjectOrNull(raw.body);
        if (raw.status != 200 || !isSuccess(json)) {
            if (json != null) setErrorFrom(json, "File download failed (HTTP " + raw.status + ")", "download_http_error");
            else setError("File download failed (HTTP " + raw.status + ")", "download_http_error");
            return null;
        }
        Object data = json.get("data");
        if (!(data instanceof String) || ((String) data).isEmpty()) {
            setError("File response is missing content", "download_missing_field");
            return null;
        }
        try {
            byte[] content = gunzipBounded(Base64.getDecoder().decode((String) data), MAX_FILE_BYTES);
            setSuccess("File downloaded.");
            return content;
        } catch (OutputLimitException e) {
            setError("File content exceeds the " + (MAX_FILE_BYTES / (1024 * 1024)) + " MiB limit", "download_too_large");
            return null;
        } catch (Exception e) {
            setError("File content could not be decompressed", "download_decode_failed");
            return null;
        }
    }

    public WebhookResult triggerWebhook(String webId, Map<String, Object> payload) {

        Map<String, Object> body = basePayload();
        body.put("webId", webId == null ? "" : webId);
        if (payload != null) body.put("payload", payload);
        Map<String, Object> resp = sendEncrypted("/webhook", body);
        Object status = resp.get("status");
        if (!(status instanceof Number)) {
            setErrorFrom(resp, "Webhook trigger failed", "webhook_failed");
            return null;
        }
        WebhookResult out = new WebhookResult();
        out.status = ((Number) status).intValue();
        out.success = Boolean.TRUE.equals(resp.get("success")) && out.status >= 200 && out.status < 300;
        Object data = resp.get("data");
        out.data = data == null ? "" : (data instanceof String ? (String) data : Json.write(data));
        out.truncated = Boolean.TRUE.equals(resp.get("truncated"));
        if (out.success) setSuccess("Webhook delivered.");
        else setError("Webhook endpoint returned HTTP " + out.status, "webhook_failed");
        return out;
    }

    public EventSubscription subscribeEvents(Consumer<String> onEvent) {
        if (onEvent == null) throw new IllegalArgumentException("onEvent is required");
        return subscribeEvents(null, (data, id) -> onEvent.accept(data));
    }

    public EventSubscription subscribeEvents(String lastEventId, BiConsumer<String, String> onEvent) {
        if (onEvent == null) throw new IllegalArgumentException("onEvent is required");
        String ticket = requestTicket("events", null);
        if (ticket == null) return null;

        HttpResponse<InputStream> resp;
        try {

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "/events?ticket=" + URLEncoder.encode(ticket, StandardCharsets.UTF_8)))
                    .timeout(Duration.ofSeconds(90))
                    .header("Accept", "text/event-stream")
                    .header("User-Agent", "NightAuth-Java-SDK/1.0");
            if (isEventId(lastEventId)) builder.header("Last-Event-ID", lastEventId);
            HttpRequest req = builder.GET().build();
            resp = httpClient.send(req, HttpResponse.BodyHandlers.ofInputStream());
        } catch (Exception e) {
            setError("Failed to open event stream: " + e.getMessage(), "events_connect_failed");
            return null;
        }
        if (resp.statusCode() >= 300 && resp.statusCode() < 400) {
            closeQuietly(resp.body());
            setError("Event stream redirected; redirects are refused.", "events_redirect_refused");
            return null;
        }
        if (resp.statusCode() != 200) {
            String text;
            try {
                text = new String(readBounded(resp.body(), MAX_RESPONSE_BYTES), StandardCharsets.UTF_8);
            } catch (Exception e) {
                text = "";
            }
            Map<String, Object> json = parseObjectOrNull(text);
            if (json != null) setErrorFrom(json, "Event stream refused (HTTP " + resp.statusCode() + ")", "events_http_error");
            else setError("Event stream refused (HTTP " + resp.statusCode() + ")", "events_http_error");
            return null;
        }
        setSuccess("Event stream connected.");
        return new EventSubscription(resp.body(), onEvent, lastEventId);
    }

    private static boolean isEventId(String id) {
        if (id == null || id.isEmpty() || id.length() > 15) return false;
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (c < '0' || c > '9') return false;
        }
        return true;
    }

    public static final class EventSubscription implements AutoCloseable {
        private final InputStream body;
        private final BiConsumer<String, String> listener;
        private final ReentrantLock dispatchLock = new ReentrantLock();
        private final AtomicReference<String> endReason = new AtomicReference<>();
        private final AtomicReference<String> lastEventId = new AtomicReference<>();
        private final Thread reader;
        static final int MAX_EVENT_CHARS = 65536;

        EventSubscription(InputStream body, BiConsumer<String, String> listener, String resumedFrom) {
            this.body = body;
            this.listener = listener;
            if (isEventId(resumedFrom)) lastEventId.set(resumedFrom);
            this.reader = new Thread(this::run, "nightauth-events");
            this.reader.setDaemon(true);
            this.reader.start();
        }

        public String getLastEventId() {
            return lastEventId.get();
        }

        public boolean isOpen() {
            return endReason.get() == null;
        }

        public String getEndReason() {
            return endReason.get();
        }

        public boolean awaitEnd(long millis) throws InterruptedException {
            reader.join(millis);
            return !reader.isAlive();
        }

        @Override
        public void close() {
            finish("closed");
            closeQuietly(body);
            dispatchLock.lock();
            dispatchLock.unlock();
        }

        private void run() {
            String reason = "stream_ended";
            try (BufferedReader in = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8))) {
                StringBuilder data = null;
                String frameId = null;
                String line;
                while (endReason.get() == null && (line = readBoundedLine(in)) != null) {
                    if (line.length() + (data == null ? 0 : data.length()) > MAX_EVENT_CHARS) {
                        reason = "protocol_error";
                        break;
                    }
                    if (line.isEmpty()) {
                        String id = frameId;
                        frameId = null;
                        if (data != null) {
                            String frame = data.toString();
                            data = null;
                            if (!dispatch(frame, id)) {
                                reason = "listener_error";
                                break;
                            }
                            if (id != null) lastEventId.set(id);
                        }
                        continue;
                    }
                    if (line.charAt(0) == ':') continue;
                    int colon = line.indexOf(':');
                    String field = colon < 0 ? line : line.substring(0, colon);
                    String value = colon < 0 ? "" : line.substring(colon + 1);
                    if (value.startsWith(" ")) value = value.substring(1);
                    if ("data".equals(field)) {
                        if (data == null) data = new StringBuilder(value);
                        else data.append('\n').append(value);
                    } else if ("id".equals(field) && isEventId(value)) {
                        frameId = value;
                    }
                }
            } catch (LineTooLongException e) {
                reason = "protocol_error";
            } catch (IOException e) {
                reason = "stream_error";
            } finally {
                finish(reason);
                closeQuietly(body);
            }
        }

        private static final class LineTooLongException extends IOException {
            LineTooLongException() { super("Event exceeds size limit."); }
        }

        private static String readBoundedLine(BufferedReader in) throws IOException {
            StringBuilder sb = new StringBuilder();
            int c;
            while ((c = in.read()) >= 0) {
                if (c == '\n') return sb.toString();
                if (c == '\r') {
                    in.mark(1);
                    int next = in.read();
                    if (next != '\n' && next >= 0) in.reset();
                    return sb.toString();
                }
                if (sb.length() >= MAX_EVENT_CHARS) throw new LineTooLongException();
                sb.append((char) c);
            }
            return sb.length() == 0 ? null : sb.toString();
        }

        private boolean dispatch(String frame, String id) {
            dispatchLock.lock();
            try {
                if (endReason.get() != null) return false;
                listener.accept(frame, id);
                return true;
            } catch (Throwable t) {
                return false;
            } finally {
                dispatchLock.unlock();
            }
        }

        private void finish(String reason) {
            endReason.compareAndSet(null, reason);
        }
    }

    public boolean getUserVariables() {
        Map<String, Object> resp = sendEncrypted("/user-vars/get", basePayload());
        return applyResponse(resp);
    }

    public List<ChatMessage> getChatMessages(String channel, Long before, Boolean markRead) {
        Map<String, Object> body = basePayload();
        body.put("room", channel != null ? channel : "general");
        if (before != null) body.put("before", before);
        if (markRead != null) body.put("markRead", markRead);
        Map<String, Object> resp = sendEncrypted("/chat/get", body);
        if (!applyResponse(resp)) { return Collections.emptyList(); }
        List<ChatMessage> out = new ArrayList<>();
        Object arr = resp.get("messages");
        if (arr instanceof List) {
            for (Object o : (List<?>) arr) {
                if (!(o instanceof Map)) continue;
                Map<?, ?> m = (Map<?, ?>) o;
                ChatMessage cm = new ChatMessage();
                cm.id = str(m.get("id"), "");
                cm.author = str(m.get("author"), "");
                cm.message = str(m.get("content") != null ? m.get("content") : m.get("message"), "");
                cm.timestamp = str(m.get("timestamp") != null ? m.get("timestamp") : m.get("createdAt"), "");
                out.add(cm);
            }
        }
        return out;
    }

    public WebhookResult triggerWebhook(String webId, Map<String, Object> payload, String content, String params) {
        return triggerWebhookWith(webId, payload, content, params, null, null);
    }

    public WebhookResult triggerWebhookFile(String webId, byte[] file, String fileName, Map<String, Object> payload) {
        if (fileName == null || fileName.trim().isEmpty()) throw new IllegalArgumentException("fileName is required");
        return triggerWebhookWith(webId, payload, null, null, file, fileName);
    }

    private WebhookResult triggerWebhookWith(String webId, Map<String, Object> payload, String content, String params,
                                             byte[] file, String fileName) {
        Map<String, Object> body = basePayload();
        body.put("webId", webId);
        if (payload != null) body.put("payload", payload);
        if (content != null) body.put("content", content);
        if (params != null) body.put("params", params);
        EnvelopeTransport transport = file == null ? null : envelope -> multipartEnvelope(envelope, file, fileName);
        Map<String, Object> resp = sendEncrypted("/webhook", body, null, transport);
        applyResponse(resp);
        Object status = resp.get("status");
        if (!(status instanceof Number)) return null;
        WebhookResult out = new WebhookResult();
        out.status = ((Number) status).intValue();
        out.success = Boolean.TRUE.equals(resp.get("success")) && out.status >= 200 && out.status < 300;
        Object data = resp.get("data");
        out.data = data == null ? "" : (data instanceof String ? (String) data : Json.write(data));
        out.truncated = Boolean.TRUE.equals(resp.get("truncated"));
        if (out.success) response.message = "Webhook delivered.";
        else if (response.error.isEmpty()) response.error = "webhook_failed";
        return out;
    }

    private static Object[] multipartEnvelope(String envelopeJson, byte[] file, String fileName) {
        String boundary = "NightAuthBoundary" + Long.toHexString(ThreadLocalRandom.current().nextLong() & Long.MAX_VALUE);
        String safeName = fileName.replace("\"", "%22").replace("\r", "%0D").replace("\n", "%0A");
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] crlf = "\r\n".getBytes(StandardCharsets.UTF_8);
        out.writeBytes(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"envelope\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.writeBytes(envelopeJson.getBytes(StandardCharsets.UTF_8));
        out.writeBytes(crlf);
        out.writeBytes(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"" + safeName
                + "\"\r\nContent-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.writeBytes(file);
        out.writeBytes(crlf);
        out.writeBytes(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return new Object[] {"multipart/form-data; boundary=" + boundary, out.toByteArray()};
    }

    public void close() {
        destroySession();
    }

    private void destroySession() {
        stateLock.lock();
        try {
            generation.incrementAndGet();
            clearSessionFieldsLocked();
        } finally {
            stateLock.unlock();
        }
    }

    private void checkInit() {
        if (!initialized || sharedKey == null || sessionId == null) {
            throw new IllegalStateException("NightAuth must be initialized before calling API methods. Call app.init() first.");
        }
    }

    private Map<String, Object> basePayload() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("apikey", apiKey);
        body.put("name", name);
        if (token != null && !token.isEmpty()) body.put("token", token);
        return body;
    }

    private static boolean isSuccess(Map<String, Object> resp) {
        return resp != null && Boolean.TRUE.equals(resp.get("success"));
    }

    private void setErrorFrom(Map<String, Object> resp, String defaultMsg, String defaultErr) {
        String msg = resp != null ? str(resp.get("message"), defaultMsg) : defaultMsg;
        String err = resp != null ? str(resp.get("error"), defaultErr) : defaultErr;
        setError(msg, err);
    }

    private void setError(String msg, String err) {

        response.success = false;
        response.message = (msg != null && !msg.isEmpty()) ? msg : "Operation failed";
        response.error = err != null ? err : "";
    }

    private boolean applyResponse(Map<String, Object> resp) {
        int status = resp != null && resp.get("_status") instanceof Number ? ((Number) resp.get("_status")).intValue() : 0;
        double retry = resp != null && resp.get("_retryAfter") instanceof Number ? ((Number) resp.get("_retryAfter")).doubleValue() : 0;
        boolean clientFailure = resp != null && resp.get("_clientFailure") != null;
        Map<String, Object> payload = new LinkedHashMap<>();
        if (resp != null) for (Map.Entry<String, Object> e : resp.entrySet()) if (!e.getKey().startsWith("_")) payload.put(e.getKey(), e.getValue());
        boolean ok = resp != null && Boolean.TRUE.equals(resp.get("success")) && !clientFailure && status >= 200 && status < 300;

        response.success = ok;
        response.statusCode = status;
        response.retryAfter = retry;
        response.payload = payload;
        response.message = resp != null ? str(resp.get("message"), "") : "";
        response.error = resp != null ? str(resp.get("error"), "") : "";
        if (resp != null) {
            Object dl = resp.get("downloadUrl");
            if (dl instanceof String && !((String) dl).isEmpty()) {
                pendingDownloadUrl = (String) dl;
            }
            Object reqVer = resp.get("requiredVersion");
            if (reqVer instanceof String && !((String) reqVer).isEmpty()) {
                requiredVersion = (String) reqVer;
            }
        }
        if (!ok && response.message.isEmpty()) response.message = "Operation failed";
        return ok;
    }

    private void setSuccess(String msg) {
        response.success = true;
        response.message = msg != null ? msg : "";
        response.error = "";
    }

    private void populateUserData(Map<String, Object> resp) {
        Object u = resp.get("user") != null ? resp.get("user") : resp.get("info");
        if (!(u instanceof Map)) return;
        Map<?, ?> um = (Map<?, ?>) u;
        user.username = str(um.get("username"), "");
        user.email = str(um.get("email"), "");
        user.hwid = str(um.get("hwid"), "");
        user.ip = str(um.get("ip"), "");
        user.status = str(um.get("status"), "active");
        user.createdAt = str(um.get("createdAt") != null ? um.get("createdAt") : um.get("createdate"), "");
        user.lastLoginAt = str(um.get("lastLoginAt") != null ? um.get("lastLoginAt") : um.get("lastlogin"), "");
        Object tfa = um.get("twoFactorEnabled");
        if (tfa instanceof Boolean) {
            user.twoFactorEnabled = (Boolean) tfa;
        } else if (tfa instanceof String) {
            user.twoFactorEnabled = Boolean.parseBoolean((String) tfa);
        }
        user.subscriptions.clear();
        Object subs = um.get("subscriptions");
        if (subs instanceof List) for (Object o : (List<?>) subs) if (o instanceof Map) user.subscriptions.add(toSubscription((Map<?, ?>) o));
    }

    private SubscriptionData toSubscription(Map<?, ?> item) {
        SubscriptionData sub = new SubscriptionData();
        sub.subscription = str(item.get("subscription"), "");
        sub.key = str(item.get("key"), "");
        sub.expiry = str(item.get("expiry") != null ? item.get("expiry") : item.get("expiresAt"), "");
        Object tl = item.get("timeleft") != null ? item.get("timeleft") : item.get("timeLeft");
        sub.timeLeft = tl != null ? num(tl) : 0;
        return sub;
    }

    private static long num(Object o) {
        if (o instanceof Number) return ((Number) o).longValue();
        if (o instanceof String) { try { return Long.parseLong((String) o); } catch (Exception e) { return 0; } }
        return 0;
    }

    private static String str(Object o, String dflt) {
        return o != null ? String.valueOf(o) : dflt;
    }

    private Map<String, Object> sendEncrypted(String path, Map<String, Object> payload) {
        return sendEncrypted(path, payload, null);
    }

    private interface EnvelopeTransport {
        Object[] build(String envelopeJson) throws Exception;
    }

    private Map<String, Object> sendEncrypted(String path, Map<String, Object> payload, Long expectedGeneration) {
        return sendEncrypted(path, payload, expectedGeneration, null);
    }

    private Map<String, Object> sendEncrypted(String path, Map<String, Object> payload, Long expectedGeneration, EnvelopeTransport transport) {

        if (!initialized || sharedKey == null || sessionId == null) {
            init();
            if (!initialized) return currentFailureMap();
            if (expectedGeneration != null) expectedGeneration = generation.get();
        }
        Map<String, Object> result = sendEncryptedOnce(path, payload, expectedGeneration, transport);

        if (expectedGeneration == null && isHandshakeExpired(result)) {
            boolean reinit;
            try {
                init();
                reinit = initialized;
            } catch (RuntimeException e) {
                reinit = false;
            }
            if (reinit) result = sendEncryptedOnce(path, payload, null, transport);
        }
        return result;
    }

    private static Map<String, Object> clientFailure(String error, String message, int status) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("success", false);
        m.put("error", error);
        m.put("message", message);
        m.put("_clientFailure", Boolean.TRUE);
        m.put("_status", (long) status);
        return m;
    }

    private static Map<String, Object> transportFailure(Exception e) {
        String msg = String.valueOf(e.getMessage());
        if (e instanceof java.net.http.HttpTimeoutException) return clientFailure("timeout", msg, 0);
        if (msg.startsWith("request too large")) return clientFailure("request_too_large", msg, 0);
        if (msg.contains("exceeds") || msg.contains("too large") || msg.contains("size limit")) return clientFailure("response_too_large", msg, 0);
        if (msg.contains("redirect refused")) return clientFailure("redirect_refused", msg, 0);
        return clientFailure("network_error", msg, 0);
    }

    private Map<String, Object> currentFailureMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("success", false);
        m.put("error", response.error);
        m.put("message", response.message);
        m.put("_status", (long) response.statusCode);
        if (response.statusCode == 0) m.put("_clientFailure", Boolean.TRUE);
        return m;
    }

    private static boolean isHandshakeExpired(Map<String, Object> resp) {
        if (resp == null || Boolean.TRUE.equals(resp.get("success"))) return false;
        if (resp.get("_clientFailure") != null) return false;
        String err = str(resp.get("error"), "");
        String msg = str(resp.get("message"), "").toLowerCase(java.util.Locale.ROOT);
        return err.equals("handshake_required") || err.equals("session_invalid") || err.equals("unauthenticated")
                || msg.contains("handshake") || msg.contains("initialize app");
    }

    private Map<String, Object> sendEncryptedOnce(String path, Map<String, Object> payload, Long expectedGeneration, EnvelopeTransport transport) {
        long myGeneration;
        String snapSessionId;
        byte[] snapSharedKey;
        stateLock.lock();
        try {
            checkInit();
            myGeneration = generation.get();
            if (expectedGeneration != null && myGeneration != expectedGeneration)
                throw new ProtocolException("request superseded by newer session generation");
            snapSessionId = sessionId;
            snapSharedKey = sharedKey;
            if (snapSessionId == null || snapSharedKey == null)
                throw new IllegalStateException("NightAuth must be initialized before calling API methods. Call app.init() first.");
        } finally {
            stateLock.unlock();
        }
        String rid = "rid-" + System.currentTimeMillis() + "-" + Long.toHexString(ThreadLocalRandom.current().nextLong() & Long.MAX_VALUE);
        long ts = System.currentTimeMillis();
        String targetPath = URI.create(apiUrl + path).getPath();
        if (targetPath == null || targetPath.isEmpty()) {
            targetPath = path;
        }
        String aad = "POST:" + targetPath + ":" + snapSessionId + ":" + rid + ":" + ts;
        try {
            byte[] plaintext = Json.write(payload).getBytes(StandardCharsets.UTF_8);
            Enc enc = aeadEncrypt(snapSharedKey, plaintext, aad.getBytes(StandardCharsets.UTF_8));

            Map<String, Object> envelope = new LinkedHashMap<>();
            envelope.put("id", snapSessionId);
            envelope.put("ct", enc.ctB64);
            envelope.put("n", enc.nonceB64);
            envelope.put("rid", rid);
            envelope.put("ts", ts);

            HttpResp raw;
            try {
                if (transport == null) {
                    raw = postRaw(path, Json.write(envelope));
                } else {
                    Object[] built = transport.build(Json.write(envelope));
                    raw = postRawBytes(path, (String) built[0], (byte[]) built[1]);
                }
            } catch (Exception failure) {
                return transportFailure(failure);
            }
            Object parsed;
            try {
                parsed = Json.parse(raw.body);
            } catch (RuntimeException notJson) {
                parsed = null;
            }
            if (!(parsed instanceof Map)) {

                return clientFailure("invalid_response", "Response body is not a JSON object.", raw.status);
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = (Map<String, Object>) parsed;

            Object ct = resp.get("ct"), n = resp.get("n");
            if (!(ct instanceof String) || !(n instanceof String)) {

                if (raw.status < 200 || raw.status >= 300) {
                    Map<String, Object> refusal = new LinkedHashMap<>(resp);
                    refusal.put("success", false);
                    refusal.put("_status", (long) raw.status);
                    if (raw.retryAfter > 0) refusal.put("_retryAfter", raw.retryAfter);
                    refusal.putIfAbsent("error", raw.status == 428 ? "handshake_required" : "http_" + raw.status);
                    if (raw.status == 428 || "handshake_required".equals(refusal.get("error"))) {
                        stateLock.lock();
                        try {
                            if (generation.get() == myGeneration) {

                                refusal.put("_clearedGeneration", generation.incrementAndGet());
                                clearSessionFieldsLocked();
                            }
                        } finally {
                            stateLock.unlock();
                        }
                    }
                    return refusal;
                }

                throw new CryptographicException("response was not encrypted (plaintext success is never valid)");
            }
            Object idResp = resp.get("id"), ridResp = resp.get("rid"), tsResp = resp.get("ts");
            long tsRespVal = (tsResp instanceof Number) ? ((Number) tsResp).longValue() : Long.MIN_VALUE;
            if (!Objects.equals(idResp, snapSessionId) || !Objects.equals(ridResp, rid) || tsRespVal != ts) {
                throw new CryptographicException("response id/rid/ts does not match the request - binding violation, discarding");
            }
            String responseAad = "v1:res:" + idResp + ":" + ridResp + ":" + tsRespVal;
            byte[] decrypted = aeadDecrypt(snapSharedKey, (String) ct, (String) n, responseAad.getBytes(StandardCharsets.UTF_8));
            Object body = Json.parse(new String(decrypted, StandardCharsets.UTF_8));
            if (!(body instanceof Map)) return clientFailure("invalid_response", "Decrypted response is not a JSON object.", raw.status);
            @SuppressWarnings("unchecked")
            Map<String, Object> bodyMap = (Map<String, Object>) body;
            bodyMap.put("_status", (long) raw.status);
            if (raw.retryAfter > 0) bodyMap.put("_retryAfter", raw.retryAfter);

            stateLock.lock();
            try {
                if (generation.get() != myGeneration) {
                    throw new ProtocolException("request superseded by newer session generation");
                }
            } finally {
                stateLock.unlock();
            }
            return bodyMap;
        } catch (ProtocolException pe) {
            throw pe;
        } catch (java.security.GeneralSecurityException e) {

            throw new CryptographicException("encrypted response from " + path + " does not decrypt: " + e.getMessage());
        } catch (Exception e) {
            throw new ProtocolException("encrypted request to " + path + " failed: " + e.getMessage());
        }
    }

    private static class HttpResp {
        int status;
        String body;
        double retryAfter;
        String location;
    }

    private static double parseRetryAfter(String v) {
        if (v == null || v.trim().isEmpty()) return 0;
        try { return Math.max(0, Double.parseDouble(v.trim())); } catch (NumberFormatException ignored) { }
        try {
            java.time.ZonedDateTime t = java.time.ZonedDateTime.parse(v.trim(), java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME);
            return Math.max(0, java.time.Duration.between(java.time.Instant.now(), t.toInstant()).toMillis() / 1000.0);
        } catch (RuntimeException ignored) { return 0; }
    }

    private HttpResp postRaw(String path, String jsonBody) throws Exception {
        return postRawBytes(path, "application/json", jsonBody.getBytes(StandardCharsets.UTF_8));
    }

    private HttpResp postRawBytes(String path, String contentType, byte[] bodyBytes) throws Exception {
        if (!path.matches("^/[a-zA-Z0-9/_-]+$")) throw new IllegalArgumentException("invalid API path");
        if (bodyBytes.length > MAX_REQUEST_BYTES) throw new ProtocolException("request too large");
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + path))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", contentType)
                .header("User-Agent", "NightAuth-Java-SDK/1.0")
                .POST(HttpRequest.BodyPublishers.ofByteArray(bodyBytes))
                .build();

        HttpResponse<byte[]> resp = sendBounded(req, MAX_RESPONSE_BYTES, requestTimeoutMs);
        if (resp.statusCode() >= 300 && resp.statusCode() < 400) {
            throw new ProtocolException("redirect refused (status " + resp.statusCode() + ")");
        }
        HttpResp out = new HttpResp();
        out.status = resp.statusCode();
        out.body = new String(resp.body(), StandardCharsets.UTF_8);
        out.retryAfter = parseRetryAfter(resp.headers().firstValue("Retry-After").orElse(null));
        return out;
    }

    private HttpResponse<byte[]> sendBounded(HttpRequest req, long maxBytes, long timeoutMs) throws Exception {
        java.util.concurrent.CompletableFuture<HttpResponse<byte[]>> pending = httpClient.sendAsync(req, info ->
                info.statusCode() >= 300 && info.statusCode() < 400
                        ? HttpResponse.BodySubscribers.replacing(new byte[0])
                        : new BoundedBody(maxBytes));
        try {
            return pending.get(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            pending.cancel(true);
            throw new java.net.http.HttpTimeoutException("request timed out after " + timeoutMs + " ms");
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable cause = e.getCause();
            while (cause instanceof java.util.concurrent.CompletionException && cause.getCause() != null) cause = cause.getCause();
            if (cause instanceof Exception ex) throw ex;
            throw e;
        } catch (InterruptedException e) {
            pending.cancel(true);
            throw e;
        }
    }

    private static final class BoundedBody implements HttpResponse.BodySubscriber<byte[]> {
        private final long max;
        private final java.util.concurrent.CompletableFuture<byte[]> result = new java.util.concurrent.CompletableFuture<>();
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private java.util.concurrent.Flow.Subscription subscription;
        private long total;

        BoundedBody(long max) { this.max = max; }

        @Override public java.util.concurrent.CompletionStage<byte[]> getBody() { return result; }

        @Override public void onSubscribe(java.util.concurrent.Flow.Subscription s) {
            subscription = s;
            s.request(Long.MAX_VALUE);
        }

        @Override public void onNext(List<java.nio.ByteBuffer> items) {
            if (result.isDone()) return;
            for (java.nio.ByteBuffer b : items) {
                int n = b.remaining();
                total += n;
                if (total > max) {
                    subscription.cancel();
                    result.completeExceptionally(new ProtocolException("response too large"));
                    return;
                }
                byte[] chunk = new byte[n];
                b.get(chunk);
                buffer.write(chunk, 0, n);
            }
        }

        @Override public void onError(Throwable t) { result.completeExceptionally(t); }

        @Override public void onComplete() { result.complete(buffer.toByteArray()); }
    }

    private static byte[] readBounded(InputStream in, long maxBytes) throws Exception {
        try (InputStream stream = in) {
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            long total = 0;
            int n;
            while ((n = stream.read(chunk)) != -1) {
                total += n;
                if (total > maxBytes) {
                    throw new ProtocolException("response too large");
                }
                buffer.write(chunk, 0, n);
            }
            return buffer.toByteArray();
        }
    }

    private String requestTicket(String scope, String resourceId) {
        Map<String, Object> body = basePayload();
        body.put("scope", scope);
        if (resourceId != null) body.put("resourceId", resourceId);
        Map<String, Object> resp = sendEncrypted("/access-ticket", body);

        if (!applyResponse(resp)) return null;
        if (!(resp.get("ticket") instanceof String)) {
            applyResponse(clientFailure("invalid_response", "Access ticket response has no ticket.", response.statusCode));
            return null;
        }
        return (String) resp.get("ticket");
    }

    private HttpResp getRaw(String pathAndQuery, long maxBytes) throws Exception {
        int q = pathAndQuery.indexOf('?');
        String path = q < 0 ? pathAndQuery : pathAndQuery.substring(0, q);
        if (!path.matches("^/[a-zA-Z0-9/_-]+$")) throw new IllegalArgumentException("invalid API path");
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + pathAndQuery))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "NightAuth-Java-SDK/1.0")
                .GET()
                .build();
        HttpResponse<byte[]> resp = sendBounded(req, maxBytes, DOWNLOAD_TIMEOUT_MS);
        HttpResp out = new HttpResp();
        out.status = resp.statusCode();
        out.body = out.status >= 300 && out.status < 400 ? "" : new String(resp.body(), StandardCharsets.UTF_8);
        out.location = resp.headers().firstValue("Location").orElse(null);
        return out;
    }

    private static Map<String, Object> parseObjectOrNull(String text) {
        if (text == null || text.isEmpty()) return null;
        try {
            Object parsed = Json.parse(text);
            if (!(parsed instanceof Map)) return null;
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) parsed;
            return map;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static final class OutputLimitException extends IOException {
        private static final long serialVersionUID = 1L;
        OutputLimitException() { super("output limit exceeded"); }
    }

    private static byte[] gunzipBounded(byte[] gz, long maxBytes) throws IOException {
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(gz))) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            long total = 0;
            int n;
            while ((n = in.read(chunk)) != -1) {
                total += n;
                if (total > maxBytes) throw new OutputLimitException();
                out.write(chunk, 0, n);
            }
            return out.toByteArray();
        }
    }

    private static void closeQuietly(InputStream in) {
        if (in == null) return;
        try { in.close(); } catch (IOException ignored) { }
    }

    private static KeyPair generateX25519KeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("X25519");
        return kpg.generateKeyPair();
    }

    private static byte[] x25519PublicRaw(XECPublicKey pub) {
        return bigIntToLittleEndian(pub.getU(), 32);
    }

    private static XECPublicKey x25519PublicFromRaw(byte[] raw32) throws Exception {
        byte[] masked = raw32.clone();
        masked[31] &= 0x7f;
        byte[] be = reverse(masked);
        java.math.BigInteger u = new java.math.BigInteger(1, be);
        KeyFactory kf = KeyFactory.getInstance("X25519");
        XECPublicKeySpec spec = new XECPublicKeySpec(NamedParameterSpec.X25519, u);
        return (XECPublicKey) kf.generatePublic(spec);
    }

    private static byte[] x25519Dh(PrivateKey priv, PublicKey pub) throws Exception {
        KeyAgreement ka = KeyAgreement.getInstance("X25519");
        ka.init(priv);
        ka.doPhase(pub, true);
        return ka.generateSecret();
    }

    private static boolean verifyInitSignature(String k2Hex, String vkHex, long ts, String sigB64) {
        try {
            byte[] vkRaw = fromHex(vkHex);
            if (vkRaw.length != 32) return false;
            EdECPublicKey pub = ed25519PublicFromRaw(vkRaw);
            byte[] data = (k2Hex + "." + vkHex + "." + ts).getBytes(StandardCharsets.UTF_8);
            byte[] sig = Base64.getDecoder().decode(sigB64);
            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(pub);
            verifier.update(data);
            return verifier.verify(sig);
        } catch (Exception e) {
            return false;
        }
    }

    private static EdECPublicKey ed25519PublicFromRaw(byte[] raw32) throws Exception {
        byte[] enc = raw32.clone();
        boolean xOdd = (enc[31] & 0x80) != 0;
        enc[31] &= 0x7f;
        byte[] be = reverse(enc);
        java.math.BigInteger y = new java.math.BigInteger(1, be);
        EdECPoint point = new EdECPoint(xOdd, y);
        KeyFactory kf = KeyFactory.getInstance("Ed25519");
        EdECPublicKeySpec spec = new EdECPublicKeySpec(NamedParameterSpec.ED25519, point);
        return (EdECPublicKey) kf.generatePublic(spec);
    }

    private static byte[] hkdfSha256(byte[] ikm, byte[] info, int length) throws Exception {
        if (length > 32) throw new IllegalArgumentException("length beyond single HMAC block not implemented (unused by this protocol)");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(new byte[32], "HmacSHA256"));
        byte[] prk = mac.doFinal(ikm);
        try {
            mac.init(new SecretKeySpec(prk, "HmacSHA256"));
            mac.update(info);
            mac.update((byte) 1);
            byte[] t1 = mac.doFinal();
            return Arrays.copyOf(t1, length);
        } finally {
            Arrays.fill(prk, (byte) 0);
        }
    }

    private static class Enc {
        String ctB64;
        String nonceB64;
    }

    private static Enc aeadEncrypt(byte[] key, byte[] plaintext, byte[] aad) throws Exception {
        byte[] nonce = new byte[12];
        new java.security.SecureRandom().nextBytes(nonce);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
        if (aad != null) cipher.updateAAD(aad);
        byte[] ctWithTag = cipher.doFinal(plaintext);
        Enc e = new Enc();
        e.ctB64 = Base64.getEncoder().encodeToString(ctWithTag);
        e.nonceB64 = Base64.getEncoder().encodeToString(nonce);
        return e;
    }

    private static byte[] aeadDecrypt(byte[] key, String ctB64, String nB64, byte[] aad) throws Exception {
        byte[] nonce = Base64.getDecoder().decode(nB64);
        byte[] combined = Base64.getDecoder().decode(ctB64);
        if (nonce.length != 12 || combined.length < 16 || combined.length > MAX_RESPONSE_BYTES) {
            throw new ProtocolException("invalid encrypted response size");
        }
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
        if (aad != null) cipher.updateAAD(aad);
        return cipher.doFinal(combined);
    }

    private static String sealInit(Map<String, Object> payload, String recipientPubKeyHex) throws Exception {
        byte[] recipientRaw = fromHex(recipientPubKeyHex);
        if (recipientRaw.length != 32) throw new IllegalArgumentException("invalid recipient public key length");
        XECPublicKey recipientPub = x25519PublicFromRaw(recipientRaw);

        KeyPair ephemeral = generateX25519KeyPair();
        byte[] ephemeralPubRaw = x25519PublicRaw((XECPublicKey) ephemeral.getPublic());

        byte[] secret = x25519Dh(ephemeral.getPrivate(), recipientPub);
        byte[] key;
        try {
            key = hkdfSha256(secret, SEAL_INFO, 32);
        } finally {
            Arrays.fill(secret, (byte) 0);
        }

        byte[] nonce = new byte[12];
        new java.security.SecureRandom().nextBytes(nonce);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
        cipher.updateAAD(ephemeralPubRaw);
        byte[] plaintext = Json.write(payload).getBytes(StandardCharsets.UTF_8);
        byte[] ctWithTag = cipher.doFinal(plaintext);
        Arrays.fill(key, (byte) 0);

        int ctLen = ctWithTag.length - 16;
        byte[] ct = Arrays.copyOfRange(ctWithTag, 0, ctLen);
        byte[] tag = Arrays.copyOfRange(ctWithTag, ctLen, ctWithTag.length);

        byte[] out = new byte[32 + 12 + 16 + ctLen];
        System.arraycopy(ephemeralPubRaw, 0, out, 0, 32);
        System.arraycopy(nonce, 0, out, 32, 12);
        System.arraycopy(tag, 0, out, 44, 16);
        System.arraycopy(ct, 0, out, 60, ctLen);
        return Base64.getEncoder().encodeToString(out);
    }

    private static String hex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static byte[] fromHex(String s) {
        if (s == null || (s.length() % 2) != 0) throw new IllegalArgumentException("invalid hex string");
        int n = s.length() / 2;
        byte[] out = new byte[n];
        for (int i = 0; i < n; i++) out[i] = (byte) Integer.parseInt(s.substring(2 * i, 2 * i + 2), 16);
        return out;
    }

    private static byte[] reverse(byte[] in) {
        byte[] out = new byte[in.length];
        for (int i = 0; i < in.length; i++) out[i] = in[in.length - 1 - i];
        return out;
    }

    private static byte[] bigIntToLittleEndian(java.math.BigInteger v, int len) {
        byte[] be = v.toByteArray();
        int off = (be.length > 0 && be[0] == 0) ? 1 : 0;
        int n = be.length - off;
        byte[] out = new byte[len];
        for (int i = 0; i < n && i < len; i++) out[i] = be[be.length - 1 - i];
        return out;
    }

    static final class Json {
        static String write(Object o) {
            StringBuilder sb = new StringBuilder();
            writeValue(o, sb);
            return sb.toString();
        }

        private static void writeValue(Object o, StringBuilder sb) {
            if (o == null) { sb.append("null"); return; }
            if (o instanceof String) { writeString((String) o, sb); return; }
            if (o instanceof Boolean) { sb.append(o.toString()); return; }
            if (o instanceof Number) { sb.append(o.toString()); return; }
            if (o instanceof Map) {
                sb.append('{');
                boolean first = true;
                for (Map.Entry<?, ?> e : ((Map<?, ?>) o).entrySet()) {
                    if (!first) sb.append(',');
                    first = false;
                    writeString(String.valueOf(e.getKey()), sb);
                    sb.append(':');
                    writeValue(e.getValue(), sb);
                }
                sb.append('}');
                return;
            }
            if (o instanceof List) {
                sb.append('[');
                boolean first = true;
                for (Object item : (List<?>) o) {
                    if (!first) sb.append(',');
                    first = false;
                    writeValue(item, sb);
                }
                sb.append(']');
                return;
            }
            writeString(o.toString(), sb);
        }

        private static void writeString(String s, StringBuilder sb) {
            sb.append('"');
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '"': sb.append("\\\""); break;
                    case '\\': sb.append("\\\\"); break;
                    case '\n': sb.append("\\n"); break;
                    case '\r': sb.append("\\r"); break;
                    case '\t': sb.append("\\t"); break;
                    default:
                        if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                        else sb.append(c);
                }
            }
            sb.append('"');
        }

        static Object parse(String s) {
            Parser p = new Parser(s);
            p.skipWs();
            Object v = p.parseValue();
            p.skipWs();
            if (p.pos != p.len) throw new IllegalArgumentException("trailing data in JSON");
            return v;
        }

        private static final class Parser {
            final String s;
            int pos = 0;
            final int len;

            Parser(String s) { this.s = s; this.len = s.length(); }

            void skipWs() { while (pos < len && Character.isWhitespace(s.charAt(pos))) pos++; }

            Object parseValue() {
                if (pos >= len) throw new IllegalArgumentException("unexpected end of JSON");
                char c = s.charAt(pos);
                if (c == '{') return parseObject();
                if (c == '[') return parseArray();
                if (c == '"') return parseString();
                if (c == 't') { expect("true"); return Boolean.TRUE; }
                if (c == 'f') { expect("false"); return Boolean.FALSE; }
                if (c == 'n') { expect("null"); return null; }
                return parseNumber();
            }

            void expect(String lit) {
                if (pos + lit.length() > len || !s.regionMatches(pos, lit, 0, lit.length())) {
                    throw new IllegalArgumentException("invalid JSON literal at " + pos);
                }
                pos += lit.length();
            }

            Map<String, Object> parseObject() {
                Map<String, Object> m = new LinkedHashMap<>();
                pos++;
                skipWs();
                if (pos < len && s.charAt(pos) == '}') { pos++; return m; }
                while (true) {
                    skipWs();
                    String key = parseString();
                    skipWs();
                    if (pos >= len || s.charAt(pos) != ':') throw new IllegalArgumentException("expected ':' at " + pos);
                    pos++;
                    skipWs();
                    Object val = parseValue();
                    m.put(key, val);
                    skipWs();
                    if (pos >= len) throw new IllegalArgumentException("unterminated object");
                    char c = s.charAt(pos);
                    if (c == ',') { pos++; continue; }
                    if (c == '}') { pos++; break; }
                    throw new IllegalArgumentException("expected ',' or '}' at " + pos);
                }
                return m;
            }

            List<Object> parseArray() {
                List<Object> arr = new ArrayList<>();
                pos++;
                skipWs();
                if (pos < len && s.charAt(pos) == ']') { pos++; return arr; }
                while (true) {
                    skipWs();
                    arr.add(parseValue());
                    skipWs();
                    if (pos >= len) throw new IllegalArgumentException("unterminated array");
                    char c = s.charAt(pos);
                    if (c == ',') { pos++; continue; }
                    if (c == ']') { pos++; break; }
                    throw new IllegalArgumentException("expected ',' or ']' at " + pos);
                }
                return arr;
            }

            String parseString() {
                if (s.charAt(pos) != '"') throw new IllegalArgumentException("expected string at " + pos);
                pos++;
                StringBuilder sb = new StringBuilder();
                while (true) {
                    if (pos >= len) throw new IllegalArgumentException("unterminated string");
                    char c = s.charAt(pos++);
                    if (c == '"') break;
                    if (c == '\\') {
                        if (pos >= len) throw new IllegalArgumentException("unterminated escape");
                        char esc = s.charAt(pos++);
                        switch (esc) {
                            case '"': sb.append('"'); break;
                            case '\\': sb.append('\\'); break;
                            case '/': sb.append('/'); break;
                            case 'b': sb.append('\b'); break;
                            case 'f': sb.append('\f'); break;
                            case 'n': sb.append('\n'); break;
                            case 'r': sb.append('\r'); break;
                            case 't': sb.append('\t'); break;
                            case 'u':
                                if (pos + 4 > len) throw new IllegalArgumentException("invalid unicode escape");
                                sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                                pos += 4;
                                break;
                            default: throw new IllegalArgumentException("invalid escape \\" + esc);
                        }
                    } else {
                        sb.append(c);
                    }
                }
                return sb.toString();
            }

            Object parseNumber() {
                int start = pos;
                if (pos < len && (s.charAt(pos) == '-' || s.charAt(pos) == '+')) pos++;
                boolean isDouble = false;
                while (pos < len && (Character.isDigit(s.charAt(pos)) || s.charAt(pos) == '.' || s.charAt(pos) == 'e' || s.charAt(pos) == 'E' || s.charAt(pos) == '+' || s.charAt(pos) == '-')) {
                    if (s.charAt(pos) == '.' || s.charAt(pos) == 'e' || s.charAt(pos) == 'E') isDouble = true;
                    pos++;
                }
                String num = s.substring(start, pos);
                if (num.isEmpty()) throw new IllegalArgumentException("invalid number at " + start);
                if (isDouble) return Double.parseDouble(num);
                try { return Long.parseLong(num); } catch (NumberFormatException e) { return Double.parseDouble(num); }
            }
        }
    }

    public boolean applyUpdate(String downloadUrl) {
        setError("Unsigned auto-update is disabled. Verify a signed release manifest with SignedUpdateInstaller.", "unsigned_update_disabled");
        return false;
    }

    public String downloadSignedUpdate(String manifestUrl, String pinnedReleaseKeyHex, String destination) throws Exception {
        return SignedUpdateInstaller.download(manifestUrl, pinnedReleaseKeyHex, this.name, destination);
    }

    public static class VerifiedUpdate {
        public final String application;
        public final String version;
        public final long sequence;
        public final URI url;
        public final long size;
        public final long expiresAt;
        public final byte[] digest;

        public VerifiedUpdate(String application, String version, long sequence, URI url, long size, long expiresAt, byte[] digest) {
            this.application = application;
            this.version = version;
            this.sequence = sequence;
            this.url = url;
            this.size = size;
            this.expiresAt = expiresAt;
            this.digest = digest;
        }
    }

    public static class SignedUpdate {
        public static final long MAX_ARTIFACT_BYTES = 256L * 1024 * 1024;
        public static final int MAX_MANIFEST_BYTES = 64 * 1024;

        public static VerifiedUpdate verify(String envelopeJson, String pinnedPublicKeyHex, String expectedApplication, long minimumSequence) throws Exception {
            if (envelopeJson == null || envelopeJson.getBytes(StandardCharsets.UTF_8).length > MAX_MANIFEST_BYTES ||
                minimumSequence < 0 || expectedApplication == null || expectedApplication.trim().isEmpty()) {
                throw new IllegalArgumentException("Invalid manifest parameters.");
            }
            Object parsedObj = Json.parse(envelopeJson);
            if (!(parsedObj instanceof Map)) throw new IllegalArgumentException("Manifest envelope must be a JSON object.");
            Map<?, ?> envelope = (Map<?, ?>) parsedObj;
            Object payloadObj = envelope.get("payload");
            Object sigObj = envelope.get("signature");
            if (!(payloadObj instanceof String) || !(sigObj instanceof String)) {
                throw new IllegalArgumentException("Manifest envelope missing payload or signature.");
            }
            byte[] payloadBytes = Base64.getDecoder().decode((String) payloadObj);
            byte[] signatureBytes = Base64.getDecoder().decode((String) sigObj);
            byte[] keyBytes = fromHex(pinnedPublicKeyHex);

            if (payloadBytes.length > 32768 || signatureBytes.length != 64 || keyBytes.length != 32) {
                throw new IllegalArgumentException("Invalid manifest size, signature or key length.");
            }

            EdECPublicKey pub = ed25519PublicFromRaw(keyBytes);
            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(pub);
            verifier.update(payloadBytes);
            if (!verifier.verify(signatureBytes)) {
                throw new CryptographicException("Release signature rejected.");
            }

            Object dataObj = Json.parse(new String(payloadBytes, StandardCharsets.UTF_8));
            if (!(dataObj instanceof Map)) throw new IllegalArgumentException("Manifest payload must be a JSON object.");
            Map<?, ?> data = (Map<?, ?>) dataObj;

            int schema = data.get("schema") instanceof Number ? ((Number) data.get("schema")).intValue() : 0;
            String app = data.get("application") instanceof String ? (String) data.get("application") : "";
            String ver = data.get("version") instanceof String ? (String) data.get("version") : "";
            long seq = data.get("sequence") instanceof Number ? ((Number) data.get("sequence")).longValue() : -1;
            long size = data.get("size") instanceof Number ? ((Number) data.get("size")).longValue() : -1;
            long expiresAt = data.get("expiresAt") instanceof Number ? ((Number) data.get("expiresAt")).longValue() : 0;
            String rawUrl = data.get("url") instanceof String ? (String) data.get("url") : "";
            String sha256Hex = data.get("sha256") instanceof String ? (String) data.get("sha256") : "";

            if (schema != 1 || !expectedApplication.equals(app) || ver.trim().isEmpty() || ver.length() > 100) {
                throw new IllegalArgumentException("Release identity or schema rejected.");
            }
            if (seq <= minimumSequence || seq > 9007199254740991L) {
                throw new IllegalArgumentException("Release sequence rejected.");
            }
            if (size < 1 || size > MAX_ARTIFACT_BYTES) {
                throw new IllegalArgumentException("Release artifact size rejected.");
            }
            long nowSec = System.currentTimeMillis() / 1000L;
            if (expiresAt <= nowSec) {
                throw new IllegalArgumentException("Release expired.");
            }

            URI parsedUrl = URI.create(rawUrl);
            if (!"https".equalsIgnoreCase(parsedUrl.getScheme()) || parsedUrl.getUserInfo() != null || parsedUrl.getFragment() != null) {
                throw new IllegalArgumentException("Release URL must be HTTPS without user info or fragment.");
            }

            if (sha256Hex.length() != 64) throw new IllegalArgumentException("Invalid sha256 hex length.");
            byte[] digest = fromHex(sha256Hex);
            if (digest.length != 32) throw new IllegalArgumentException("Invalid sha256 digest length.");

            return new VerifiedUpdate(app, ver, seq, parsedUrl, size, expiresAt, digest);
        }

        public static String download(VerifiedUpdate update, String destination, Duration timeout) throws Exception {
            if (update == null) throw new IllegalArgumentException("update cannot be null");
            if (update.expiresAt <= System.currentTimeMillis() / 1000L) {
                throw new IllegalStateException("Release expired.");
            }

            java.nio.file.Path target = java.nio.file.Paths.get(destination).toAbsolutePath();
            if (java.nio.file.Files.exists(target)) {
                throw new java.io.IOException("Destination file already exists.");
            }
            if (target.getParent() != null) {
                java.nio.file.Files.createDirectories(target.getParent());
            }

            java.nio.file.Path temporary = java.nio.file.Paths.get(target.toString() + "." + UUID.randomUUID().toString().replace("-", "") + ".partial");

            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .connectTimeout(Duration.ofSeconds(20))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(update.url)
                    .timeout(timeout != null ? timeout : Duration.ofMinutes(5))
                    .header("User-Agent", "NightAuth-Updater/1.0")
                    .GET()
                    .build();

            try {
                HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() != 200) {
                    throw new java.io.IOException("Artifact must return HTTP 200 without redirects (got " + response.statusCode() + ").");
                }
                OptionalLong cl = response.headers().firstValueAsLong("content-length");
                if (cl.isPresent() && cl.getAsLong() != update.size) {
                    throw new IllegalArgumentException("Artifact length differs from signed manifest.");
                }

                MessageDigest md = MessageDigest.getInstance("SHA-256");
                long total = 0;
                try (InputStream in = response.body();
                     java.io.OutputStream out = java.nio.file.Files.newOutputStream(temporary, java.nio.file.StandardOpenOption.CREATE_NEW, java.nio.file.StandardOpenOption.WRITE)) {
                    byte[] buffer = new byte[65536];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        total += read;
                        if (total > update.size) {
                            throw new IllegalArgumentException("Artifact exceeds signed size.");
                        }
                        md.update(buffer, 0, read);
                        out.write(buffer, 0, read);
                    }
                    out.flush();
                }

                if (total != update.size) {
                    throw new IllegalArgumentException("Artifact size differs from signed manifest.");
                }
                if (!MessageDigest.isEqual(md.digest(), update.digest)) {
                    throw new CryptographicException("Artifact hash rejected.");
                }
                if (update.expiresAt <= System.currentTimeMillis() / 1000L) {
                    throw new IllegalStateException("Release expired during download.");
                }

                java.nio.file.Files.move(temporary, target);
                return target.toString();
            } finally {
                try { java.nio.file.Files.deleteIfExists(temporary); } catch (Exception ignored) {}
            }
        }
    }

    public static class SignedUpdateInstaller {
        public static final int MAX_MANIFEST_BYTES = 64 * 1024;
        private static final Object LOCK = new Object();

        public static java.nio.file.Path getSequencePath(String application) throws Exception {
            if (application == null || application.trim().isEmpty() || application.length() > 100 || application.matches(".*[<>:\"/\\\\|?*].*")) {
                throw new IllegalArgumentException("Invalid application identifier.");
            }
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            String root;
            if (os.contains("win")) {
                root = System.getenv("LOCALAPPDATA");
                if (root == null || root.isEmpty()) root = System.getProperty("user.home") + "\\AppData\\Local";
            } else {
                root = System.getenv("XDG_DATA_HOME");
                if (root == null || root.isEmpty()) root = System.getProperty("user.home") + "/.local/share";
            }
            java.nio.file.Path dir = java.nio.file.Paths.get(root, "NightAuth", "release-sequences");
            java.nio.file.Files.createDirectories(dir);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(application.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : h) sb.append(String.format("%02x", b));
            return dir.resolve(sb.toString() + ".seq");
        }

        public static long readSequence(java.nio.file.Path path) throws Exception {
            if (!java.nio.file.Files.exists(path)) return 0;
            try {
                String text = java.nio.file.Files.readString(path, StandardCharsets.UTF_8).trim();
                long seq = Long.parseLong(text);
                if (seq < 0) throw new IllegalStateException("Negative sequence stored.");
                return seq;
            } catch (Exception e) {
                throw new IllegalStateException("Stored release sequence is corrupt; refusing update.", e);
            }
        }

        public static void writeSequenceAtomically(java.nio.file.Path path, long sequence) throws Exception {
            java.nio.file.Path temp = java.nio.file.Paths.get(path.toString() + "." + UUID.randomUUID().toString().replace("-", "") + ".tmp");
            try {
                java.nio.file.Files.writeString(temp, Long.toString(sequence) + "\n", StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.CREATE_NEW, java.nio.file.StandardOpenOption.WRITE);
                java.nio.file.Files.move(temp, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } finally {
                try { java.nio.file.Files.deleteIfExists(temp); } catch (Exception ignored) {}
            }
        }

        public static String download(String manifestUrl, String pinnedReleaseKeyHex, String application, String destination) throws Exception {
            URI parsed = URI.create(manifestUrl);
            if (!"https".equalsIgnoreCase(parsed.getScheme()) || parsed.getUserInfo() != null || parsed.getFragment() != null) {
                throw new IllegalArgumentException("Release manifest must use an HTTPS URL without user info or fragment.");
            }

            java.nio.file.Path statePath = getSequencePath(application);
            java.nio.file.Path lockPath = java.nio.file.Paths.get(statePath.toString() + ".lock");

            synchronized (LOCK) {
                java.nio.channels.FileChannel channel = null;
                java.nio.channels.FileLock fileLock = null;
                long start = System.currentTimeMillis();
                while (true) {
                    try {
                        channel = java.nio.channels.FileChannel.open(lockPath, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.WRITE);
                        fileLock = channel.tryLock();
                        if (fileLock != null) break;
                    } catch (Exception ignored) {}
                    if (channel != null && fileLock == null) {
                        try { channel.close(); } catch (Exception ignored) {}
                    }
                    if (System.currentTimeMillis() - start > 10000) {
                        throw new java.util.concurrent.TimeoutException("Could not acquire update lock.");
                    }
                    Thread.sleep(100);
                }

                try {
                    long currentSequence = readSequence(statePath);

                    HttpClient client = HttpClient.newBuilder()
                            .followRedirects(HttpClient.Redirect.NEVER)
                            .connectTimeout(Duration.ofSeconds(15))
                            .build();

                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(parsed)
                            .timeout(Duration.ofSeconds(30))
                            .header("User-Agent", "NightAuth-Updater/1.0")
                            .GET()
                            .build();

                    HttpResponse<byte[]> resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
                    if (resp.statusCode() != 200) {
                        throw new java.io.IOException("Release manifest must return HTTP 200 without redirects (got " + resp.statusCode() + ").");
                    }
                    byte[] manifestBytes = resp.body();
                    if (manifestBytes.length > MAX_MANIFEST_BYTES) {
                        throw new IllegalArgumentException("Release manifest exceeds the maximum allowed size.");
                    }

                    String envelopeJson = new String(manifestBytes, StandardCharsets.UTF_8);
                    VerifiedUpdate verified = SignedUpdate.verify(envelopeJson, pinnedReleaseKeyHex, application, currentSequence);
                    String targetPath = SignedUpdate.download(verified, destination, Duration.ofMinutes(5));
                    writeSequenceAtomically(statePath, verified.sequence);
                    return targetPath;
                } finally {
                    if (fileLock != null) {
                        try { fileLock.release(); } catch (Exception ignored) {}
                    }
                    if (channel != null) {
                        try { channel.close(); } catch (Exception ignored) {}
                    }
                    try { java.nio.file.Files.deleteIfExists(lockPath); } catch (Exception ignored) {}
                }
            }
        }
    }
}
