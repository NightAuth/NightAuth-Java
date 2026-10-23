package com.nightauth.console;

import com.nightauth.sdk.NightAuthApi;
import java.io.File;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Main {
    public static final String APP_NAME = "";
    public static final String APP_API_KEY = "";
    public static final String APP_VERSION = "1.0";
    public static final String APP_TOKEN = "";

    static final String ENV_APP_NAME = "NIGHTAUTH_APP_NAME";
    static final String ENV_API_KEY = "NIGHTAUTH_API_KEY";
    static final String ENV_BASE_URL = "NIGHTAUTH_BASE_URL";
    static final String ENV_LOOPBACK = "NIGHTAUTH_ALLOW_INSECURE_LOOPBACK";
    static final String DEFAULT_APP_NAME = APP_NAME;
    static final String DEFAULT_API_KEY = APP_API_KEY;
    static final String DEFAULT_BASE_URL = "https://127.0.0.1:3000/api/v1";
    static final String RUNTIME_BASE_URL = "https://nightauth.org/api/v1";

    private static final String C_RESET    = "\u001B[0m";
    private static final String C_BOLD     = "\u001B[1m";
    private static final String C_DIM      = "\u001B[38;2;100;116;139m";
    private static final String C_MUTED    = "\u001B[38;2;148;163;184m";
    private static final String C_TEXT     = "\u001B[38;2;226;232;240m";
    private static final String C_WHITE    = "\u001B[38;2;248;250;252m";
    private static final String C_BLUE     = "\u001B[38;2;59;130;246m";
    private static final String C_GLOW     = "\u001B[38;2;96;165;250m";
    private static final String C_CYAN     = "\u001B[38;2;56;189;248m";
    private static final String C_GREEN    = "\u001B[38;2;52;211;153m";
    private static final String C_AMBER    = "\u001B[38;2;251;191;36m";
    private static final String C_RED      = "\u001B[38;2;248;113;113m";
    private static final String C_BORDER   = "\u001B[38;2;42;50;70m";
    private static final String C_BADGE_BG = "\u001B[48;2;23;37;68m";
    private static final String C_GREEN_BG = "\u001B[48;2;14;46;38m";
    private static final String C_RED_BG   = "\u001B[48;2;56;20;26m";

    private static final String SYM_DOT     = "\u25CF";
    private static final String SYM_CIRCLE  = "\u25CC";
    private static final String SYM_BLOCK   = "\u258C";
    private static final String SYM_CHECK   = "\u2713";
    private static final String SYM_CROSS   = "\u2715";
    private static final String SYM_BULLET  = "\u2022";
    private static final String SYM_ARROW   = "\u276F";
    private static final String SYM_MID_DOT = "\u00B7";
    private static final String SYM_DASH    = "\u2014";

    private static final DateTimeFormatter UTC_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    private static Scanner scanner = new Scanner(System.in, java.nio.charset.StandardCharsets.UTF_8);
    private static NightAuthApi nightAuth;

    private static void initConsoleEncoding() {
        if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
            try {
                new ProcessBuilder("cmd.exe", "/c", "chcp 65001 >nul").inheritIO().start().waitFor();
            } catch (Exception ignored) {}
        }
        try {
            System.setOut(new java.io.PrintStream(new java.io.FileOutputStream(java.io.FileDescriptor.out), true, java.nio.charset.StandardCharsets.UTF_8));
            System.setErr(new java.io.PrintStream(new java.io.FileOutputStream(java.io.FileDescriptor.err), true, java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception ignored) {}
        try {
            scanner = new Scanner(System.in, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception ignored) {}
    }

    static String configError(Map<String, String> env) {
        String name = valueOf(env, ENV_APP_NAME, "");
        String apiKey = valueOf(env, ENV_API_KEY, "");
        if (name.isEmpty()) {
            return "Missing required environment variable: " + ENV_APP_NAME;
        }
        if (apiKey.isEmpty()) {
            return "Missing required environment variable: " + ENV_API_KEY;
        }
        return null;
    }

    static NightAuthApi buildClient(Map<String, String> env) {
        String name = valueOf(env, ENV_APP_NAME, DEFAULT_APP_NAME);
        String apiKey = valueOf(env, ENV_API_KEY, DEFAULT_API_KEY);
        if (name.isEmpty()) {
            name = ask("Enter App Name: ");
        }
        if (apiKey.isEmpty()) {
            apiKey = ask("Enter API Key: ");
        }
        String baseUrl = valueOf(env, ENV_BASE_URL, valueOf(env, "NIGHTAUTH_API_URL", DEFAULT_BASE_URL));
        boolean loopback = "1".equals(valueOf(env, ENV_LOOPBACK, "")) || "true".equalsIgnoreCase(valueOf(env, ENV_LOOPBACK, ""));
        try {
            return new NightAuthApi(name, apiKey, valueOf(env, "NIGHTAUTH_APP_VERSION", "1.0"), valueOf(env, "NIGHTAUTH_APP_TOKEN", ""), baseUrl, loopback);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage() + " (base URL: " + baseUrl + ", " + ENV_LOOPBACK + "=" + (loopback ? "1" : "0") + ")", e);
        }
    }

    private static String valueOf(Map<String, String> env, String key, String fallback) {
        if (env == null) return fallback;
        String value = env.get(key);
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private static void clearScreen() {
        try {
            if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
                return;
            }
        } catch (Exception ignored) {}
        System.out.print("\u001B[2J\u001B[H");
        System.out.flush();
    }

    private static void printHr() {
        System.out.printf("  %s────────────────────────────────────────────────────────────────────────────%s\n", C_BORDER, C_RESET);
    }

    private static void printSectionHeader(String title, String subtitle) {
        System.out.printf("\n  %s%s%s %s%s%s%s", C_BLUE, SYM_BLOCK, C_RESET, C_WHITE, C_BOLD, title, C_RESET);
        if (subtitle != null && !subtitle.isEmpty()) {
            System.out.printf("  %s%s  %s%s", C_DIM, SYM_MID_DOT, subtitle, C_RESET);
        }
        System.out.print("\n");
        printHr();
    }

    private static void printOk(String title, String detail) {
        System.out.printf("\n  %s%s%s %s SUCCESS %s %s%s%s%s", C_GREEN_BG, C_GREEN, C_BOLD, SYM_CHECK, C_RESET, C_WHITE, C_BOLD, title, C_RESET);
        if (detail != null && !detail.isEmpty()) {
            System.out.printf(" %s%s %s%s", C_MUTED, SYM_DASH, detail, C_RESET);
        }
        System.out.print("\n\n");
    }

    private static void printErr(String title, String detail) {
        System.out.printf("\n  %s%s%s %s ERROR %s %s%s%s%s", C_RED_BG, C_RED, C_BOLD, SYM_CROSS, C_RESET, C_WHITE, C_BOLD, title, C_RESET);
        if (detail != null && !detail.isEmpty()) {
            System.out.printf(" %s%s %s%s", C_RED, SYM_DASH, detail, C_RESET);
        }
        System.out.print("\n\n");
    }

    private static void printInfo(String label, String value) {
        System.out.printf("  %s%s%s %s%-18s%s %s%s%s\n", C_BLUE, SYM_BULLET, C_RESET, C_MUTED, label, C_RESET, C_CYAN, value, C_RESET);
    }

    private static String ask(String prompt) {
        System.out.printf("  %s%s%s %s%s%s", C_GLOW, SYM_ARROW, C_RESET, C_TEXT, prompt, C_RESET);
        System.out.flush();
        if (scanner.hasNextLine()) {
            return scanner.nextLine().trim();
        }
        System.exit(0);
        return "";
    }

    private static String formatTimestamp(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        if (s.isEmpty()) return "";
        if (s.matches("^\\d+$")) {
            try {
                long val = Long.parseLong(s);
                if (val > 0) {
                    Instant inst = val > 100000000000L ? Instant.ofEpochMilli(val) : Instant.ofEpochSecond(val);
                    return UTC_FORMATTER.format(inst);
                }
            } catch (Exception ignored) {}
        }
        try {
            Instant inst = Instant.parse(s);
            return UTC_FORMATTER.format(inst);
        } catch (Exception ignored) {}
        try {
            String iso = s.replace(' ', 'T');
            if (!iso.endsWith("Z") && !iso.contains("+") && iso.indexOf("-", 10) == -1) {
                iso += "Z";
            }
            Instant inst = Instant.parse(iso);
            return UTC_FORMATTER.format(inst);
        } catch (Exception ignored) {}
        return s;
    }

    private static void renderBanner(String appName, String hwid, boolean connected) {
        System.out.print("\n");
        System.out.printf("  %s%s  _   _ _       _     _      _         _   _     \n%s", C_BLUE, C_BOLD, C_RESET);
        System.out.printf("  %s%s | \\ | (_) __ _| |__ | |_   / \\  _   _| |_| |__  \n%s", C_BLUE, C_BOLD, C_RESET);
        System.out.printf("  %s%s |  \\| | |/ _` | '_ \\| __| / _ \\| | | | __| '_ \\ \n%s", C_GLOW, C_BOLD, C_RESET);
        System.out.printf("  %s%s | |\\  | | (_| | | | | |_ / ___ \\ |_| | |_| | | |\n%s", C_GLOW, C_BOLD, C_RESET);
        System.out.printf("  %s%s |_| \\_|_|\\__, |_| |_|\\__/_/   \\_\\__,_|\\__|_| |_|\n%s", C_CYAN, C_BOLD, C_RESET);
        System.out.printf("  %s%s          |___/                                  %s  %s%s%s NATIVE JAVA CLI %s\n\n", C_CYAN, C_BOLD, C_RESET, C_BADGE_BG, C_GLOW, C_BOLD, C_RESET);

        System.out.printf("  %s╭──────────────────────────────────────────────────────────────────────────╮%s\n", C_BORDER, C_RESET);
        System.out.printf("  %s│%s  %sAPPLICATION%s  %s%s%-26.26s%s  %sSTATUS%s  %s%-16s%s %s│%s\n",
                C_BORDER, C_RESET, C_DIM, C_RESET, C_WHITE, C_BOLD,
                (appName != null && !appName.isEmpty()) ? appName : "Unconfigured",
                C_RESET, C_DIM, C_RESET,
                connected ? (C_GREEN + SYM_DOT + " ") : (C_AMBER + SYM_CIRCLE + " "),
                connected ? "Connected" : "Initializing",
                C_RESET, C_BORDER, C_RESET);
        if (hwid != null && !hwid.isEmpty()) {
            System.out.printf("  %s│%s  %sDEVICE HWID%s  %s%-58.58s%s %s│%s\n",
                    C_BORDER, C_RESET, C_DIM, C_RESET, C_CYAN, hwid, C_RESET, C_BORDER, C_RESET);
        }
        System.out.printf("  %s╰──────────────────────────────────────────────────────────────────────────╯%s\n", C_BORDER, C_RESET);
    }

    private static void performHandshake(String appName) {
        System.out.printf("\n  %s%s Establishing encrypted session with NightAuth Cloud...%s\r", C_MUTED, SYM_CIRCLE, C_RESET);
        System.out.flush();
        try {
            nightAuth.init();
        } catch (Exception ex) {
            System.out.print("                                                                      \r");
            System.out.flush();
            renderBanner(appName, "", false);
            printErr("Handshake Exception", ex.getMessage());
            System.exit(1);
        }
        System.out.print("                                                                      \r");
        System.out.flush();

        if (!nightAuth.response.success) {
            renderBanner(appName, "", false);
            printErr("Handshake Failed", nightAuth.response.message);
            System.out.printf("  %sPlease verify your App Name, API Key, and Server URL.%s\n\n", C_DIM, C_RESET);
            ask("Press Enter to exit...");
            System.exit(1);
        }

        String hwid = NightAuthApi.getHardwareId();
        clearScreen();
        renderBanner(appName, hwid, true);
    }

    private static boolean handleLogin() {
        printSectionHeader("ACCOUNT SIGN IN", "Authenticate with your username and password");
        String user = ask("Username: ");
        String pass = ask("Password: ");
        String totp = ask("2FA Code " + C_DIM + "(Optional, Enter to skip)" + C_TEXT + ": ");

        if (nightAuth.login(user, pass, totp)) {
            printOk("Session Authenticated", "Welcome back to NightAuth.");
            return true;
        }

        printErr("Login Failed", nightAuth.response.message);
        return false;
    }

    private static boolean handleRegister() {
        printSectionHeader("CREATE NEW ACCOUNT", "Activate a license key and bind your credentials");
        String user = ask("Username: ");
        String pass = ask("Password: ");
        String key = ask("License Key: ");
        String email = ask("Email Address " + C_DIM + "(Optional, Enter to skip)" + C_TEXT + ": ");

        if (nightAuth.register(user, pass, key, email)) {
            printOk("Account Registered & Activated", "Session started automatically.");
            return true;
        }

        String detail = nightAuth.response.message != null ? nightAuth.response.message : "";
        if (nightAuth.response.error != null && !nightAuth.response.error.isEmpty()) {
            detail += " [" + nightAuth.response.error + "]";
        }
        printErr("Registration Failed", detail);
        return false;
    }

    private static boolean handleLicenseOnly() {
        printSectionHeader("LICENSE KEY AUTHENTICATION", "Instant passwordless login bound to your HWID");
        String key = ask("Enter License Key: ");

        if (nightAuth.license(key)) {
            printOk("License Validated", "Direct hardware-bound session active.");
            return true;
        }

        printErr("License Validation Failed", nightAuth.response.message);
        return false;
    }

    private static boolean handleUpgrade(String knownUser) {
        printSectionHeader("UPGRADE ACCOUNT SUBSCRIPTION", "Attach a new license key tier to an account");
        String user = knownUser;
        if (user != null && !user.isEmpty()) {
            printInfo("Target Account:", user);
        } else {
            user = ask("Target Username: ");
        }
        String key = ask("Upgrade License Key: ");

        if (nightAuth.upgrade(user, key)) {
            printOk("Upgrade Complete", nightAuth.response.message);
            return true;
        } else {
            printErr("Upgrade Failed", nightAuth.response.message);
            return false;
        }
    }

    private static void handleForgotPassword() {
        printSectionHeader("PASSWORD RECOVERY", "Request a reset link to your registered email");
        String email = ask("Registered Email: ");

        if (nightAuth.forgotPassword(email)) {
            printOk("Recovery Dispatched", "If an account matches, a password reset link was sent to your email.");
        } else {
            printErr("Request Failed", nightAuth.response.message);
        }
    }

    private static void handleResetPassword() {
        printSectionHeader("COMPLETE PASSWORD RESET", "Confirm recovery token and set a new password");
        String token = ask("Reset Token: ");
        String newPass = ask("New Password: ");

        if (nightAuth.resetPassword(token, newPass)) {
            printOk("Password Updated", "Your password has been reset successfully.");
        } else {
            printErr("Reset Failed", nightAuth.response.message);
        }
    }

    private static void handleResetHwid() {
        printSectionHeader("RESET HARDWARE FINGERPRINT (HWID)", "Unbind previous device to authorize a new machine");
        String user = ask("Username: ");
        String pass = ask("Password: ");
        String totp = ask("2FA Code " + C_DIM + "(Optional, Enter to skip)" + C_TEXT + ": ");

        if (nightAuth.resetHwid(user, pass, totp)) {
            printOk("HWID Reset", nightAuth.response.message);
        } else {
            printErr("HWID Reset Failed", nightAuth.response.message);
        }
    }

    private static void handleCheckBlacklist() {
        printSectionHeader("HARDWARE BLACKLIST CHECK", "Verify if a machine fingerprint is restricted");
        String targetHwid = ask("Hardware ID " + C_DIM + "(Press Enter for local machine HWID)" + C_TEXT + ": ");
        if (targetHwid.isEmpty()) {
            targetHwid = NightAuthApi.getHardwareId();
        }

        boolean isBlacklisted = nightAuth.checkBlacklist(targetHwid);
        if (nightAuth.response.success) {
            if (isBlacklisted) {
                printErr("Blacklisted", "This Hardware ID is currently restricted.");
            } else {
                printOk("Clean Fingerprint", "Hardware ID is not on the blacklist.");
            }
        } else {
            printErr("Blacklist Query Failed", nightAuth.response.message);
        }
    }

    private static void handleVerifyHash() {
        printSectionHeader("BINARY INTEGRITY CHECK", "Validate SHA-256 checksum against server registry");
        String hash = ask("SHA-256 File Hash: ");
        boolean verified = nightAuth.verifyFileHash(hash);
        if (nightAuth.response.success) {
            if (verified) {
                printOk("Integrity Verified", "Checksum matches authorized server release.");
            } else {
                printErr("Verification Failed", "Checksum does not match any approved file release.");
            }
        } else {
            printErr("Verification Failed", nightAuth.response.message);
        }
    }

    private static boolean handleAuthenticationFlow() {
        while (true) {
            printSectionHeader("AUTHENTICATION & PRE-SESSION GATEWAY", "Select an operation to continue");

            System.out.printf("  %s%s%s 01 %s %sSign In (Username & Password)%s          %s%s%s 06 %s %sComplete Password Reset (Token)%s\n",
                    C_BADGE_BG, C_GLOW, C_BOLD, C_RESET, C_WHITE, C_RESET, C_BADGE_BG, C_GLOW, C_BOLD, C_RESET, C_WHITE, C_RESET);
            System.out.printf("  %s%s%s 02 %s %sRegister New Account + License%s         %s%s%s 07 %s %sReset Hardware Binding (HWID)%s\n",
                    C_BADGE_BG, C_GLOW, C_BOLD, C_RESET, C_WHITE, C_RESET, C_BADGE_BG, C_GLOW, C_BOLD, C_RESET, C_WHITE, C_RESET);
            System.out.printf("  %s%s%s 03 %s %sInstant License Key Login%s              %s%s%s 08 %s %sCheck HWID Blacklist Status%s\n",
                    C_BADGE_BG, C_GLOW, C_BOLD, C_RESET, C_WHITE, C_RESET, C_BADGE_BG, C_GLOW, C_BOLD, C_RESET, C_WHITE, C_RESET);
            System.out.printf("  %s%s%s 04 %s %sUpgrade Existing Account Plan%s          %s%s%s 09 %s %sVerify Binary SHA-256 Hash%s\n",
                    C_BADGE_BG, C_GLOW, C_BOLD, C_RESET, C_WHITE, C_RESET, C_BADGE_BG, C_GLOW, C_BOLD, C_RESET, C_WHITE, C_RESET);
            System.out.printf("  %s%s%s 05 %s %sForgot Password (Email Link)%s           %s%s%s 10 %s %sExit Application%s\n\n",
                    C_BADGE_BG, C_GLOW, C_BOLD, C_RESET, C_WHITE, C_RESET, C_RED_BG, C_RED, C_BOLD, C_RESET, C_MUTED, C_RESET);

            String sel = ask("Select option [1-10]: ");

            if ("1".equals(sel) || "01".equals(sel)) {
                if (handleLogin()) return true;
            } else if ("2".equals(sel) || "02".equals(sel)) {
                if (handleRegister()) return true;
            } else if ("3".equals(sel) || "03".equals(sel)) {
                if (handleLicenseOnly()) return true;
            } else if ("4".equals(sel) || "04".equals(sel)) {
                handleUpgrade("");
            } else if ("5".equals(sel) || "05".equals(sel)) {
                handleForgotPassword();
            } else if ("6".equals(sel) || "06".equals(sel)) {
                handleResetPassword();
            } else if ("7".equals(sel) || "07".equals(sel)) {
                handleResetHwid();
            } else if ("8".equals(sel) || "08".equals(sel)) {
                handleCheckBlacklist();
            } else if ("9".equals(sel) || "09".equals(sel)) {
                handleVerifyHash();
            } else if ("10".equals(sel) || "0".equals(sel)) {
                return false;
            }
        }
    }

    private static void renderUserProfile() {
        NightAuthApi.UserData u = nightAuth.user;
        String createdStr = formatTimestamp(u.createdAt);
        String lastLoginStr = formatTimestamp(u.lastLoginAt);
        System.out.print("\n");
        System.out.printf("  %s╭─ %s%sAUTHENTICATED IDENTITY & ENTITLEMENTS%s ──────────────────────────────────╮%s\n",
                C_BORDER, C_GLOW, C_BOLD, C_BORDER, C_RESET);
        System.out.printf("  %s│%s  %sUsername :%s %s%s%-24.24s%s  %sStatus     :%s %s%s%-20.20s%s %s│%s\n",
                C_BORDER, C_RESET, C_DIM, C_RESET, C_WHITE, C_BOLD,
                !u.username.isEmpty() ? u.username : "Authenticated",
                C_RESET, C_DIM, C_RESET, C_GREEN, C_BOLD,
                !u.status.isEmpty() ? u.status : "ACTIVE",
                C_RESET, C_BORDER, C_RESET);
        System.out.printf("  %s│%s  %sEmail    :%s %s%-24.24s%s  %sClient IP  :%s %s%-20.20s%s %s│%s\n",
                C_BORDER, C_RESET, C_DIM, C_RESET, C_TEXT,
                !u.email.isEmpty() ? u.email : "Not linked",
                C_RESET, C_DIM, C_RESET, C_CYAN,
                !u.ip.isEmpty() ? u.ip : "Protected / TLS",
                C_RESET, C_BORDER, C_RESET);
        System.out.printf("  %s│%s  %sCreated  :%s %s%-24.24s%s  %sLast Login :%s %s%-20.20s%s %s│%s\n",
                C_BORDER, C_RESET, C_DIM, C_RESET, C_MUTED,
                !createdStr.isEmpty() ? createdStr : "-",
                C_RESET, C_DIM, C_RESET, C_MUTED,
                !lastLoginStr.isEmpty() ? lastLoginStr : "Just now",
                C_RESET, C_BORDER, C_RESET);
        System.out.printf("  %s├──────────────────────────────────────────────────────────────────────────┤%s\n", C_BORDER, C_RESET);

        if (!u.subscriptions.isEmpty()) {
            for (NightAuthApi.SubscriptionData sub : u.subscriptions) {
                String expStr = formatTimestamp(sub.expiry);
                System.out.printf("  %s│%s  %s%s%s %s%s%-16.16s%s %sKey:%s %s%-22.22s%s %sExp:%s %s%-16.16s%s %s│%s\n",
                        C_BORDER, C_RESET, C_GREEN, SYM_DOT, C_RESET, C_WHITE, C_BOLD,
                        sub.subscription,
                        C_RESET, C_DIM, C_RESET, C_CYAN,
                        !sub.key.isEmpty() ? sub.key : "Direct Entitlement",
                        C_RESET, C_DIM, C_RESET, C_AMBER,
                        !expStr.isEmpty() ? expStr : "Lifetime",
                        C_RESET, C_BORDER, C_RESET);
            }
        } else {
            System.out.printf("  %s│%s  %sNo active subscription tiers attached to this account.%s                   %s│%s\n",
                    C_BORDER, C_RESET, C_DIM, C_RESET, C_BORDER, C_RESET);
        }
        System.out.printf("  %s╰──────────────────────────────────────────────────────────────────────────╯%s\n", C_BORDER, C_RESET);
    }

    private static void handleChatRoom() {
        printSectionHeader("LIVE CHANNEL CHAT", "Type /exit at any time to leave the channel");
        String room = ask("Channel name (e.g. general): ");
        if (room.isEmpty()) room = "general";

        List<NightAuthApi.ChatMessage> msgs = nightAuth.getChatMessages(room, 0L, true);
        System.out.printf("\n  %s╭─ %s%s#%-20s%s ──────────────────────────────────────────────────╮%s\n",
                C_BORDER, C_GLOW, C_BOLD, room, C_BORDER, C_RESET);
        if (msgs.isEmpty()) {
            System.out.printf("  %s│%s  %sNo messages in this channel yet. Start the conversation below.%s          %s│%s\n",
                    C_BORDER, C_RESET, C_DIM, C_RESET, C_BORDER, C_RESET);
        } else {
            for (NightAuthApi.ChatMessage m : msgs) {
                System.out.printf("  %s│%s %s[%s]%s %s%s%s%s: %s%s%s\n",
                        C_BORDER, C_RESET, C_DIM, m.timestamp, C_RESET, C_CYAN, C_BOLD, m.author, C_RESET, C_TEXT, m.message, C_RESET);
            }
        }
        System.out.printf("  %s╰──────────────────────────────────────────────────────────────────────────╯%s\n\n", C_BORDER, C_RESET);

        while (true) {
            String input = ask(C_CYAN + "#" + room + C_RESET + " message: ");
            if ("/exit".equalsIgnoreCase(input)) {
                System.out.printf("  %sLeft channel #%s.%s\n\n", C_DIM, room, C_RESET);
                return;
            }
            if (input.isEmpty()) continue;
            if (!nightAuth.sendChatMessage(input, room)) {
                printErr("Send Failed", nightAuth.response.message);
            }
        }
    }

    private static void renderStatsTable() {
        NightAuthApi.AppStats stats = nightAuth.getStats();
        if (stats != null) {
            System.out.print("\n");
            System.out.printf("  %s╭─ %s%sAPPLICATION LIVE TELEMETRY%s ─────────────────────────────────────────────╮%s\n",
                    C_BORDER, C_GLOW, C_BOLD, C_BORDER, C_RESET);
            System.out.printf("  %s│%s  %sTotal Users     :%s %s%s%-14d%s  %sTotal Licenses  :%s %s%s%-18d%s %s│%s\n",
                    C_BORDER, C_RESET, C_DIM, C_RESET, C_WHITE, C_BOLD, stats.totalUsers, C_RESET,
                    C_DIM, C_RESET, C_WHITE, C_BOLD, stats.totalLicenses, C_RESET, C_BORDER, C_RESET);
            System.out.printf("  %s│%s  %sActive Users    :%s %s%s%-14d%s  %sUsed Licenses   :%s %s%s%-18d%s %s│%s\n",
                    C_BORDER, C_RESET, C_DIM, C_RESET, C_GREEN, C_BOLD, stats.activeUsers, C_RESET,
                    C_DIM, C_RESET, C_CYAN, C_BOLD, stats.usedLicenses, C_RESET, C_BORDER, C_RESET);
            System.out.printf("  %s│%s  %sBanned Users    :%s %s%s%-14d%s  %sUnused Licenses :%s %s%s%-18d%s %s│%s\n",
                    C_BORDER, C_RESET, C_DIM, C_RESET, C_RED, C_BOLD, stats.bannedUsers, C_RESET,
                    C_DIM, C_RESET, C_AMBER, C_BOLD, stats.unusedLicenses, C_RESET, C_BORDER, C_RESET);
            System.out.printf("  %s│%s  %sTarget Version  :%s %s%-52s%s %s│%s\n",
                    C_BORDER, C_RESET, C_DIM, C_RESET, C_GLOW,
                    !stats.version.isEmpty() ? stats.version : nightAuth.version,
                    C_RESET, C_BORDER, C_RESET);
            System.out.printf("  %s╰──────────────────────────────────────────────────────────────────────────╯%s\n\n", C_BORDER, C_RESET);
        } else {
            printErr("Telemetry Error", nightAuth.response.message);
        }
    }

    private static void handleSetup2FA() {
        printSectionHeader("TWO-FACTOR AUTHENTICATION (TOTP)", "Enroll an authenticator app (RFC 6238)");
        NightAuthApi.TwoFactorSetupData setup = nightAuth.setup2FA();
        if (setup != null) {
            printInfo("TOTP Secret Key:", setup.secret);
            printInfo("Provisioning URI:", setup.uri);
            String code = ask("Enter 6-digit Authenticator code: ");
            NightAuthApi.TwoFactorConfirmData conf = nightAuth.confirm2FA(code);
            if (conf != null) {
                printOk("2FA Activated", "Store these recovery backup codes safely:");
                for (String bc : conf.backupCodes) {
                    System.out.printf("    %s%s %s%s\n", C_AMBER, SYM_BULLET, bc, C_RESET);
                }
                System.out.print("\n");
            } else {
                printErr("Verification Failed", nightAuth.response.message);
            }
        } else {
            printErr("2FA Setup Failed", nightAuth.response.message);
        }
    }

    private static void handleDownloadFile() {
        printSectionHeader("PROTECTED ASSET DOWNLOADER", "Download encrypted files via scoped access ticket");
        String fileId = ask("Enter File ID or Asset Name: ");
        byte[] content = nightAuth.downloadFile(fileId);
        if (content != null) {
            int size = content.length;
            Arrays.fill(content, (byte) 0);
            printOk("Download Complete", "Retrieved " + size + " bytes securely.");
        } else {
            printErr("Download Failed", nightAuth.response.message);
        }
    }

    private static void handleListAllUserVars() {
        if (!nightAuth.getUserVariables()) {
            printErr("Fetch Failed", nightAuth.response.message);
            return;
        }
        printSectionHeader("USER CLOUD VARIABLES", "Per-user encrypted key-value pairs");
        Object varsObj = nightAuth.response.payload != null ? nightAuth.response.payload.get("variables") : null;
        if (!(varsObj instanceof List) || ((List<?>) varsObj).isEmpty()) {
            System.out.printf("  %sNo user variables stored for this account yet.%s\n\n", C_DIM, C_RESET);
            return;
        }
        for (Object item : (List<?>) varsObj) {
            if (item instanceof Map) {
                Map<?, ?> m = (Map<?, ?>) item;
                String k = m.get("key") != null ? String.valueOf(m.get("key")) : "";
                String v = m.get("value") != null ? String.valueOf(m.get("value")) : "";
                System.out.printf("  %s%s%s %s%s%-22s%s = %s%s%s\n", C_BLUE, SYM_BULLET, C_RESET, C_CYAN, C_BOLD, k, C_RESET, C_WHITE, v, C_RESET);
            }
        }
        System.out.print("\n");
    }

    private static void handleTriggerWebhook() {
        printSectionHeader("SERVER-SIDE WEBHOOK TRIGGER", "Execute an authenticated webhook endpoint");
        String webId = ask("Webhook ID: ");
        String payload = ask("JSON Payload " + C_DIM + "(Enter for {})" + C_TEXT + ": ");
        if (payload.isEmpty()) payload = "{}";

        try {
            NightAuthApi.WebhookResult result = nightAuth.triggerWebhook(webId, null, payload, null);
            if (result != null && result.success) {
                printOk("Webhook Dispatched", "HTTP " + result.status + " OK");
            } else {
                printErr("Webhook Failed", nightAuth.response.message);
            }
        } catch (Exception ex) {
            printErr("Invalid Webhook Payload", ex.getMessage());
        }
    }

    private static void handleEventsFeed() {
        printSectionHeader("REAL-TIME APPLICATION EVENTS (SSE)", "Press Enter at any time to stop listening");
        NightAuthApi.EventSubscription sub = nightAuth.subscribeEvents(null, (data, id) -> {
            String safeId = id != null ? id : "-";
            System.out.printf("  %s%s EVENT %s %s[%s]%s %s%sevent%s: %s\n",
                    C_BADGE_BG, C_GLOW, C_RESET, C_DIM, safeId, C_RESET, C_CYAN, C_BOLD, C_RESET, data);
        });
        if (sub == null) {
            printErr("Stream Interrupted", nightAuth.response.message);
            return;
        }
        ask("");
        sub.close();
        printOk("Stream Closed", "Disconnected from real-time event feed.");
    }

    private static void handleSelfBan() {
        printSectionHeader("EMERGENCY KILLSWITCH / BAN", "Report security violation and lock this session");
        String reason = ask("Ban Reason " + C_DIM + "(Optional)" + C_TEXT + ": ");

        if (nightAuth.ban(reason)) {
            printOk("Killswitch Executed", nightAuth.response.message);
        } else {
            printErr("Ban Report Failed", nightAuth.response.message);
        }
    }

    private static void handleHeartbeat() {
        if (nightAuth.heartbeat()) {
            printOk("Heartbeat Acknowledged", nightAuth.response.message);
        } else {
            printErr("Heartbeat Failed", nightAuth.response.message);
        }
    }

    private static void handleKillSessionDemo() {
        printSectionHeader("SESSION REVOCATION & INVALIDATION VERIFICATION", "3-Step Live Security Audit");
        System.out.printf("  %s1. Verifying active session before revocation...%s\n", C_MUTED, C_RESET);
        if (nightAuth.check()) {
            printOk("Step 1 Passed", "Session is active and authenticated on server.");
        } else {
            printErr("Step 1 Failed", nightAuth.response.message);
            return;
        }

        System.out.printf("  %s2. Revoking and terminating session on server (/logout)...%s\n", C_MUTED, C_RESET);
        if (nightAuth.logout()) {
            printOk("Step 2 Passed", "Server confirmed session revocation.");
        } else {
            printErr("Step 2 Failed", nightAuth.response.message);
        }

        System.out.printf("  %s3. Verifying that subsequent session check is rejected...%s\n", C_MUTED, C_RESET);
        if (nightAuth.check()) {
            printErr("Step 3 Unexpected", "Session was still accepted after revocation.");
        } else {
            printOk("Step 3 Passed", "Server rejected revoked session as expected.");
        }
    }

    private static void runApplicationFeaturesMenu() {
        while (true) {
            printSectionHeader("WORKSPACE MODULES & ACTIONS", "Select a command [1-21]");

            System.out.printf("  %sCLOUD VARIABLES & CHAT%s                    %sSECURITY & TELEMETRY%s\n", C_DIM, C_RESET, C_DIM, C_RESET);
            System.out.printf("  %s%s%s 01 %s %sRead Global Variable%s                 %s%s%s 10 %s %sSetup Two-Factor Auth (2FA)%s\n",
                    C_BADGE_BG, C_GLOW, C_BOLD, C_RESET, C_WHITE, C_RESET, C_BADGE_BG, C_GLOW, C_BOLD, C_RESET, C_WHITE, C_RESET);
            System.out.printf("  %s%s%s 02 %s %sGet User Cloud Variable%s              %s%s%s 11 %s %sDisable Two-Factor Auth (2FA)%s\n",
                    C_BADGE_BG, C_GLOW, C_BOLD, C_RESET, C_WHITE, C_RESET, C_BADGE_BG, C_GLOW, C_BOLD, C_RESET, C_WHITE, C_RESET);
            System.out.printf("  %s%s%s 03 %s %sSet User Cloud Variable%s              %s%s%s 12 %s %sDownload Protected Asset%s\n",
                    C_BADGE_BG, C_GLOW, C_BOLD, C_RESET, C_WHITE, C_RESET, C_BADGE_BG, C_GLOW, C_BOLD, C_RESET, C_WHITE, C_RESET);
            System.out.printf("  %s%s%s 04 %s %sList All User Variables%s              %s%s%s 13 %s %sTrigger Server Webhook%s\n",
                    C_BADGE_BG, C_GLOW, C_BOLD, C_RESET, C_WHITE, C_RESET, C_BADGE_BG, C_GLOW, C_BOLD, C_RESET, C_WHITE, C_RESET);
            System.out.printf("  %s%s%s 05 %s %sEnter Live Chat Channel%s              %s%s%s 14 %s %sSend Audit Log Event%s\n",
                    C_BADGE_BG, C_GLOW, C_BOLD, C_RESET, C_WHITE, C_RESET, C_BADGE_BG, C_GLOW, C_BOLD, C_RESET, C_WHITE, C_RESET);
            System.out.printf("  %s%s%s 19 %s %sFetch Paginated Chat History%s         %s%s%s 15 %s %sVerify Session Entitlement%s\n\n",
                    C_BADGE_BG, C_GLOW, C_BOLD, C_RESET, C_WHITE, C_RESET, C_BADGE_BG, C_GLOW, C_BOLD, C_RESET, C_WHITE, C_RESET);

            System.out.printf("  %sACCOUNT & INSIGHTS%s                        %sSESSION LIFECYCLE%s\n", C_DIM, C_RESET, C_DIM, C_RESET);
            System.out.printf("  %s%s%s 06 %s %sApplication Live Statistics%s          %s%s%s 16 %s %sSend Heartbeat Ping%s\n",
                    C_BADGE_BG, C_GLOW, C_BOLD, C_RESET, C_WHITE, C_RESET, C_BADGE_BG, C_GLOW, C_BOLD, C_RESET, C_WHITE, C_RESET);
            System.out.printf("  %s%s%s 07 %s %sView Online Active Users%s             %s%s%s 17 %s %sEmergency Killswitch / Ban%s\n",
                    C_BADGE_BG, C_GLOW, C_BOLD, C_RESET, C_WHITE, C_RESET, C_BADGE_BG, C_GLOW, C_BOLD, C_RESET, C_WHITE, C_RESET);
            System.out.printf("  %s%s%s 08 %s %sChange Account Username%s              %s%s%s 18 %s %sStream Live Events (SSE)%s\n",
                    C_BADGE_BG, C_GLOW, C_BOLD, C_RESET, C_WHITE, C_RESET, C_BADGE_BG, C_GLOW, C_BOLD, C_RESET, C_WHITE, C_RESET);
            System.out.printf("  %s%s%s 09 %s %sUpgrade Plan with License Key%s        %s%s%s 20 %s %sSession Revocation Audit Demo%s\n",
                    C_BADGE_BG, C_GLOW, C_BOLD, C_RESET, C_WHITE, C_RESET, C_BADGE_BG, C_GLOW, C_BOLD, C_RESET, C_WHITE, C_RESET);
            System.out.printf("                                            %s%s%s 21 %s %sSign Out & Terminate Session%s\n\n",
                    C_RED_BG, C_RED, C_BOLD, C_RESET, C_MUTED, C_RESET);

            String choice = ask("Action [1-21]: ");
            int num = 0;
            try {
                num = Integer.parseInt(choice);
            } catch (NumberFormatException ignored) {}

            if (num == 1) {
                String gKey = ask("Global Variable Key: ");
                String gVal = nightAuth.getGlobalVar(gKey);
                if (!gVal.isEmpty()) {
                    printOk(gKey, gVal);
                } else {
                    printErr("Fetch Failed", nightAuth.response.message);
                }
            } else if (num == 2) {
                String uKey = ask("User Variable Key: ");
                String uVal = nightAuth.getUserVar(uKey);
                if (!uVal.isEmpty()) {
                    printOk(uKey, uVal);
                } else {
                    printErr("Fetch Failed", nightAuth.response.message);
                }
            } else if (num == 3) {
                String sKey = ask("User Variable Key: ");
                String sVal = ask("Value to Store: ");
                if (nightAuth.setUserVar(sKey, sVal)) {
                    printOk("Variable Saved", sKey);
                } else {
                    printErr("Save Failed", nightAuth.response.message);
                }
            } else if (num == 4) {
                handleListAllUserVars();
            } else if (num == 5) {
                handleChatRoom();
            } else if (num == 6) {
                renderStatsTable();
            } else if (num == 7) {
                List<String> users = nightAuth.getOnlineUsers();
                String hdr = users.size() + " active session(s)";
                printSectionHeader("ONLINE ACTIVE SESSIONS", hdr);
                for (String u : users) {
                    System.out.printf("  %s%s%s %s%s%s\n", C_GREEN, SYM_DOT, C_RESET, C_WHITE, u, C_RESET);
                }
                System.out.print("\n");
            } else if (num == 8) {
                String newU = ask("New Username: ");
                String p1 = ask("Confirm Password: ");
                if (nightAuth.changeUsername(newU, p1)) {
                    printOk("Username Updated", nightAuth.user.username);
                    System.out.printf("  %sFor security, your session has been closed. Please sign in with your new username.%s\n\n", C_AMBER, C_RESET);
                    return;
                } else {
                    printErr("Update Failed", nightAuth.response.message);
                }
            } else if (num == 9) {
                if (handleUpgrade(nightAuth.user.username)) {
                    nightAuth.check();
                    renderUserProfile();
                }
            } else if (num == 10) {
                handleSetup2FA();
            } else if (num == 11) {
                String p3 = ask("Account Password: ");
                String code3 = ask("6-digit Authenticator Code: ");
                if (nightAuth.disable2FA(p3, code3)) {
                    printOk("2FA Disabled", "Two-factor protection has been removed from this account.");
                } else {
                    printErr("Disable Failed", nightAuth.response.message);
                }
            } else if (num == 12) {
                handleDownloadFile();
            } else if (num == 13) {
                handleTriggerWebhook();
            } else if (num == 14) {
                String logMsg = ask("Audit Log Message: ");
                if (nightAuth.log(logMsg)) {
                    printOk("Audit Log Recorded", "Delivered to application telemetry feed.");
                } else {
                    printErr("Log Failed", nightAuth.response.message);
                }
            } else if (num == 15) {
                if (nightAuth.check()) {
                    printOk("Session Verified", "Active and cryptographically valid.");
                    renderUserProfile();
                } else {
                    printErr("Session Invalid", nightAuth.response.message);
                }
            } else if (num == 16) {
                handleHeartbeat();
            } else if (num == 17) {
                handleSelfBan();
            } else if (num == 18) {
                handleEventsFeed();
            } else if (num == 19) {
                String histRoom = ask("Channel Name: ");
                String histBefore = ask("Before Message ID " + C_DIM + "(Optional, Enter for latest)" + C_TEXT + ": ");
                String histMr = ask("Mark messages as read? (y/n): ");
                long beforeId = 0L;
                if (!histBefore.isEmpty()) {
                    try {
                        beforeId = Long.parseLong(histBefore);
                    } catch (NumberFormatException ignored) {}
                }
                boolean markRead = !histMr.isEmpty() && (histMr.charAt(0) == 'y' || histMr.charAt(0) == 'Y');

                List<NightAuthApi.ChatMessage> hist = nightAuth.getChatMessages(histRoom, beforeId, markRead);
                if (!nightAuth.response.success) {
                    printErr("History Query Failed", nightAuth.response.message);
                } else {
                    String sub = hist.size() + " message(s) retrieved";
                    printSectionHeader(histRoom, sub);
                    for (NightAuthApi.ChatMessage m : hist) {
                        System.out.printf("  %s[%s]%s %s%s%s%s: %s%s%s %s(#%s %s -)%s\n",
                                C_DIM, m.timestamp, C_RESET, C_CYAN, C_BOLD, m.author, C_RESET, C_TEXT, m.message, C_RESET, C_DIM, m.id, SYM_MID_DOT, C_RESET);
                    }
                    System.out.print("\n");
                }
            } else if (num == 20) {
                handleKillSessionDemo();
                return;
            } else if (num == 21) {
                nightAuth.logout();
                printOk("Signed Out", "Session revoked cleanly. Goodbye!");
                return;
            }
        }
    }

    private static boolean ensureConsoleWindow(String[] args) {
        for (String a : args) {
            if ("--console-attached".equals(a)) {
                return false;
            }
        }
        if (System.console() != null) {
            return false;
        }

        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("win")) {
            return false;
        }

        String cmd = ProcessHandle.current().info().command().orElse("").toLowerCase();
        boolean isJavaw = cmd.endsWith("javaw.exe") || cmd.endsWith("javaw");

        if (!isJavaw && (System.getenv("PROMPT") != null || System.getenv("WT_SESSION") != null)) {
            return false;
        }

        try {
            File codeSource = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (codeSource.isFile() && codeSource.getName().toLowerCase().endsWith(".jar")) {
                File javaHome = new File(System.getProperty("java.home"));
                File javaExe = new File(javaHome, "bin/java.exe");
                String javaCmd = javaExe.exists() ? javaExe.getAbsolutePath() : "java";

                List<String> command = new ArrayList<>();
                command.add("cmd.exe");
                command.add("/c");
                command.add("start");
                command.add("NightAuth Native Java CLI");
                command.add("cmd.exe");
                command.add("/c");
                command.add("chcp 65001 >nul && \"" + javaCmd + "\" -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -jar \"" + codeSource.getAbsolutePath() + "\" --console-attached");
                for (String a : args) {
                    command.add(a);
                }

                ProcessBuilder pb = new ProcessBuilder(command);
                pb.directory(codeSource.getParentFile());
                pb.start();
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public static void main(String[] args) {
        initConsoleEncoding();
        if (ensureConsoleWindow(args)) {
            return;
        }

        Map<String, String> env = new HashMap<>(System.getenv());
        env.putIfAbsent(ENV_APP_NAME, DEFAULT_APP_NAME);
        env.putIfAbsent(ENV_API_KEY, DEFAULT_API_KEY);
        if (!env.containsKey(ENV_BASE_URL) && !env.containsKey("NIGHTAUTH_API_URL")) {
            env.put(ENV_BASE_URL, RUNTIME_BASE_URL);
        }

        String appName = valueOf(env, ENV_APP_NAME, DEFAULT_APP_NAME);
        clearScreen();
        renderBanner(appName, "", false);

        try {
            nightAuth = buildClient(env);
        } catch (IllegalArgumentException e) {
            printErr("Configuration Error", e.getMessage());
            ask("Press Enter to exit...");
            System.exit(2);
        }

        performHandshake(nightAuth.name);

        if (!handleAuthenticationFlow()) {
            nightAuth.close();
            return;
        }

        String hwid = NightAuthApi.getHardwareId();
        clearScreen();
        renderBanner(nightAuth.name, hwid, true);
        renderUserProfile();

        runApplicationFeaturesMenu();
        nightAuth.close();
    }
}
