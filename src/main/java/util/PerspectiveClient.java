package util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

import util.Secrets;

public final class PerspectiveClient {
    private static final String ENDPOINT =
            "https://commentanalyzer.googleapis.com/v1alpha1/comments:analyze?key=";
    private static final int MAX_ATTEMPTS = 2;

    private final HttpClient client;

    public PerspectiveClient() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public double analyzeToxicity(String text, String apiKey) throws Exception {
        return analyze(text, apiKey).getToxicity();
    }

    public PerspectiveResult analyze(String text, String apiKey) throws Exception {
        String key = normalizeKey(apiKey);
        if (key.isBlank()) {
            throw new IllegalArgumentException("Missing Perspective API key");
        }
        if (isPlaceholderKey(key)) {
            throw new IllegalArgumentException("Replace YOUR_KEY_HERE");
        }

        String payload = buildPayload(text);
        HttpRequest request = HttpRequest.newBuilder(URI.create(ENDPOINT + key))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .header("User-Agent", "HirelyForum/1.0 (JavaFX)")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        Exception lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            DebugLog.info("Perspective", "Sending analyze request attempt " + attempt + "/" + MAX_ATTEMPTS
                    + ", textLen=" + safeLength(text)
                    + ", keyLen=" + key.length()
                    + ", key=" + maskKey(key)
                    + ", endpoint=" + ENDPOINT + "<key omitted>"
                    + ", payloadLen=" + payload.length());
            long startNs = System.nanoTime();
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                long latencyMs = elapsedMs(startNs);
                String bodyPreview = preview(response.body());
                DebugLog.info("Perspective", "Response status=" + response.statusCode()
                        + ", bodyPreview=" + bodyPreview);
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    if (response.statusCode() == 403) {
                        DebugLog.info("Perspective", "403 usually means API key restricted / wrong key / API not enabled / billing policy");
                    }
                    throw new IOException("Perspective request failed (status="
                            + response.statusCode() + ", bodyPreview=" + bodyPreview + ")");
                }

                double toxicity = parseToxicity(response.body());
                DebugLog.info("Perspective", "Parsed TOXICITY summaryScore.value=" + toxicity);
                return new PerspectiveResult(toxicity, trimRaw(response.body()), latencyMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw ie;
            } catch (IOException io) {
                lastError = io;
                if (attempt < MAX_ATTEMPTS && isTransientNetworkError(io)) {
                    DebugLog.info("Perspective", "Transient network error, will retry once: " + safeMessage(io));
                    continue;
                }
                throw io;
            }
        }

        if (lastError instanceof Exception) {
            throw (Exception) lastError;
        }
        throw new IOException("Perspective analyze failed");
    }

    public PerspectiveResult selfTest() throws Exception {
        return analyze("hello", Secrets.PERSPECTIVE_API_KEY);
    }

    private String buildPayload(String text) {
        JsonObject root = new JsonObject();
        JsonObject comment = new JsonObject();
        comment.addProperty("text", text == null ? "" : text);
        root.add("comment", comment);

        JsonArray languages = new JsonArray();
        languages.add("en");
        root.add("languages", languages);

        JsonObject requested = new JsonObject();
        requested.add("TOXICITY", new JsonObject());
        root.add("requestedAttributes", requested);

        return root.toString();
    }

    private double parseToxicity(String body) throws IOException {
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            JsonObject attributeScores = root.getAsJsonObject("attributeScores");
            JsonObject toxicity = attributeScores == null ? null : attributeScores.getAsJsonObject("TOXICITY");
            JsonObject summary = toxicity == null ? null : toxicity.getAsJsonObject("summaryScore");
            if (summary == null || !summary.has("value")) {
                throw new IOException("Perspective response missing TOXICITY summaryScore");
            }

            double value = summary.get("value").getAsDouble();
            if (value < 0) {
                return 0;
            }
            if (value > 1) {
                return 1;
            }
            return value;
        } catch (RuntimeException ex) {
            DebugLog.error("Perspective", "Failed to parse response body: " + preview(body), ex);
            throw new IOException("Failed to parse Perspective response", ex);
        }
    }

    private String maskKey(String key) {
        if (key == null || key.isBlank()) {
            return "<empty>";
        }
        if (key.length() <= 8) {
            return "****";
        }
        return key.substring(0, 4) + "..." + key.substring(key.length() - 4);
    }

    private String normalizeKey(String apiKey) {
        if (apiKey == null) {
            return "";
        }
        String trimmed = apiKey.trim();
        if (trimmed.length() >= 2) {
            if ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
                    || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
                trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
            }
        }
        return trimmed;
    }

    private boolean isPlaceholderKey(String key) {
        return "YOUR_KEY_HERE".equalsIgnoreCase(key);
    }

    private boolean isTransientNetworkError(Throwable ex) {
        if (ex instanceof HttpTimeoutException || ex instanceof SocketTimeoutException || ex instanceof ConnectException) {
            return true;
        }
        if (ex instanceof SocketException) {
            String msg = safeMessage(ex).toLowerCase();
            return msg.contains("connection reset") || msg.contains("timed out") || msg.contains("refused");
        }
        Throwable cause = ex.getCause();
        return cause != null && cause != ex && isTransientNetworkError(cause);
    }

    private String safeMessage(Throwable ex) {
        if (ex == null) {
            return "";
        }
        String msg = ex.getMessage();
        if (msg == null || msg.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return msg.replace('\n', ' ').replace('\r', ' ').trim();
    }

    private int safeLength(String value) {
        return value == null ? 0 : value.length();
    }

    private String preview(String value) {
        if (value == null) {
            return "<null>";
        }
        String compact = value.replace('\n', ' ').replace('\r', ' ');
        if (compact.length() <= 220) {
            return compact;
        }
        return compact.substring(0, 220) + "...";
    }

    private String trimRaw(String value) {
        if (value == null) {
            return "";
        }
        String compact = value.replace('\n', ' ').replace('\r', ' ');
        if (compact.length() <= 2000) {
            return compact;
        }
        return compact.substring(0, 2000) + "...";
    }

    private long elapsedMs(long startNs) {
        return (System.nanoTime() - startNs) / 1_000_000;
    }

    public static final class PerspectiveResult {
        private final double toxicity;
        private final String raw;
        private final long latencyMs;

        public PerspectiveResult(double toxicity, String raw, long latencyMs) {
            this.toxicity = toxicity;
            this.raw = raw;
            this.latencyMs = latencyMs;
        }

        public double getToxicity() {
            return toxicity;
        }

        public String getRaw() {
            return raw;
        }

        public long getLatencyMs() {
            return latencyMs;
        }
    }
}
