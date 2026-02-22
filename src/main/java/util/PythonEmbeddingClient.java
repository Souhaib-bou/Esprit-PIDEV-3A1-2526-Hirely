package util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class PythonEmbeddingClient {
    private static final String DEFAULT_BASE_URL = "http://127.0.0.1:8008";

    private final HttpClient client;
    private final String baseUrl;

    public PythonEmbeddingClient() {
        this(resolveBaseUrl());
    }

    public PythonEmbeddingClient(String baseUrl) {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.baseUrl = sanitizeBaseUrl(baseUrl);
    }

    public EmbeddingResult embed(String text, String type) throws Exception {
        String endpoint = baseUrl + "/embed";
        JsonObject payload = new JsonObject();
        payload.addProperty("text", text == null ? "" : text);
        payload.addProperty("type", type == null || type.isBlank() ? "post" : type.toLowerCase());

        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .header("User-Agent", "HirelyForum/1.0 (JavaFX)")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        long startNs = System.nanoTime();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        long latencyMs = elapsedMs(startNs);

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Python embedding request failed (" + response.statusCode() + ")");
        }

        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray arr = root.getAsJsonArray("embedding");
        if (arr == null || arr.size() == 0) {
            throw new IOException("Python embedding response missing embedding vector");
        }

        double[] vec = new double[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            vec[i] = arr.get(i).getAsDouble();
        }

        int dim = root.has("dim") && !root.get("dim").isJsonNull() ? root.get("dim").getAsInt() : vec.length;
        String model = root.has("model") && !root.get("model").isJsonNull() ? root.get("model").getAsString()
                : "unknown";

        return new EmbeddingResult(vec, dim, model, trimRaw(response.body()), latencyMs);
    }

    private static String resolveBaseUrl() {
        String fromEnv = System.getenv("PY_AI_URL");
        if (fromEnv == null || fromEnv.isBlank()) {
            return DEFAULT_BASE_URL;
        }
        return fromEnv.trim();
    }

    private static String sanitizeBaseUrl(String raw) {
        String v = (raw == null || raw.isBlank()) ? DEFAULT_BASE_URL : raw.trim();
        if (v.endsWith("/")) {
            return v.substring(0, v.length() - 1);
        }
        return v;
    }

    private static String trimRaw(String raw) {
        if (raw == null) {
            return "";
        }
        String compact = raw.replace('\n', ' ').replace('\r', ' ');
        if (compact.length() <= 2000) {
            return compact;
        }
        return compact.substring(0, 2000) + "...";
    }

    private static long elapsedMs(long startNs) {
        return (System.nanoTime() - startNs) / 1_000_000;
    }

    public static final class EmbeddingResult {
        private final double[] embedding;
        private final int dim;
        private final String model;
        private final String raw;
        private final long latencyMs;

        public EmbeddingResult(double[] embedding, int dim, String model, String raw, long latencyMs) {
            this.embedding = embedding;
            this.dim = dim;
            this.model = model;
            this.raw = raw;
            this.latencyMs = latencyMs;
        }

        public double[] getEmbedding() {
            return embedding;
        }

        public int getDim() {
            return dim;
        }

        public String getModel() {
            return model;
        }

        public String getRaw() {
            return raw;
        }

        public long getLatencyMs() {
            return latencyMs;
        }
    }
}
