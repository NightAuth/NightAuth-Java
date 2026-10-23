package com.nightauth.console;

import com.nightauth.sdk.NightAuthApi;

import java.util.Map;

/**
 * Offline checks for the console configuration. No network: the client is only
 * constructed, never initialized.
 */
public final class ConsoleConfigTest {

    public static void main(String[] args) {
        int passed = 0, failed = 0;
        Runnable[] cases = {
                ConsoleConfigTest::testMissingApiKeyIsReported,
                ConsoleConfigTest::testMissingAppNameIsReported,
                ConsoleConfigTest::testPlainHttpLoopbackRequiresOptIn,
                ConsoleConfigTest::testPlainHttpLoopbackWithOptInBuilds,
                ConsoleConfigTest::testHttpsUrlBuildsWithoutNetwork,
                ConsoleConfigTest::testDefaultBaseUrlIsNotProduction,
        };
        for (Runnable c : cases) {
            try {
                c.run();
                passed++;
            } catch (Throwable t) {
                failed++;
                System.out.println("FAIL: " + t);
            }
        }
        System.out.println("passed=" + passed + " failed=" + failed);
        if (failed > 0) System.exit(1);
        System.out.println("ALL CONSOLE CONFIG TESTS PASSED");
    }

    static void testMissingApiKeyIsReported() {
        String error = Main.configError(Map.of(Main.ENV_APP_NAME, "App"));
        if (error == null || !error.contains(Main.ENV_API_KEY)) {
            throw new AssertionError("missing API key must be reported, got: " + error);
        }
    }

    static void testMissingAppNameIsReported() {
        String error = Main.configError(Map.of(Main.ENV_API_KEY, "key"));
        if (error == null || !error.contains(Main.ENV_APP_NAME)) {
            throw new AssertionError("missing app name must be reported, got: " + error);
        }
    }

    static void testPlainHttpLoopbackRequiresOptIn() {
        Map<String, String> env = Map.of(
                Main.ENV_APP_NAME, "App",
                Main.ENV_API_KEY, "key",
                Main.ENV_BASE_URL, "http://localhost:3000");
        try {
            Main.buildClient(env);
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("http loopback without " + Main.ENV_LOOPBACK + "=1 must be refused");
    }

    static void testPlainHttpLoopbackWithOptInBuilds() {
        Map<String, String> env = Map.of(
                Main.ENV_APP_NAME, "App",
                Main.ENV_API_KEY, "key",
                Main.ENV_BASE_URL, "http://localhost:3000",
                Main.ENV_LOOPBACK, "1");
        NightAuthApi api = Main.buildClient(env);
        if (!"http://localhost:3000".equals(api.getApiUrl())) {
            throw new AssertionError("client must use the configured base URL, got " + api.getApiUrl());
        }
    }

    static void testHttpsUrlBuildsWithoutNetwork() {
        Map<String, String> env = Map.of(
                Main.ENV_APP_NAME, "App",
                Main.ENV_API_KEY, "key",
                Main.ENV_BASE_URL, "https://127.0.0.1:9");
        if (Main.buildClient(env) == null) throw new AssertionError("https client must build");
    }

    static void testDefaultBaseUrlIsNotProduction() {
        if (Main.DEFAULT_BASE_URL.contains("nightauth.org")) {
            throw new AssertionError("default base URL must not be production: " + Main.DEFAULT_BASE_URL);
        }
    }
}
