package example.media;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class InfraiClient {
    // account.autorecharge.configure is the account policy write used by this service.
    private final String apiKey;
    private final String baseUrl;
    private final HttpClient http;

    public InfraiClient(String apiKey, String baseUrl) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public String balance() throws IOException, InterruptedException {
        return send("GET", "/account/balance", "{}", "balance-read");
    }

    public String configureAutorecharge(double triggerBalance, double rechargeAmount, String idempotencyKey)
            throws IOException, InterruptedException {
        String body = "{\"trigger_balance\":" + triggerBalance + ",\"recharge_amount\":"
                + rechargeAmount + ",\"idempotency_key\":\"" + quote(idempotencyKey) + "\"}";
        return send("PUT", "/account/autorecharge/configure", body, idempotencyKey);
    }

    public String sendEmail(String to, String subject, String body, String idempotencyKey)
            throws IOException, InterruptedException {
        String json = "{\"to\":\"" + quote(to) + "\",\"subject\":\"" + quote(subject)
                + "\",\"body\":\"" + quote(body) + "\",\"idempotency_key\":\""
                + quote(idempotencyKey) + "\"}";
        return send("POST", "/email/send", json, idempotencyKey);
    }

    private String send(String method, String path, String body, String idempotencyKey)
            throws IOException, InterruptedException {
        for (int attempt = 0; attempt < 3; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("Idempotency-Key", idempotencyKey)
                    .method(method, HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            String envelope = response.body();
            if (envelope.contains("\"ok\":false")) {
                throw new IOException("Infrai business error: " + envelope);
            }
            if (envelope.contains("\"ok\":true")) {
                return envelope;
            }
            if (response.statusCode() == 429 && attempt < 2) {
                Thread.sleep((long) Math.pow(2, attempt) * 250L);
                continue;
            }
            if (response.statusCode() >= 500) {
                throw new IOException("Infrai transport response: " + response.statusCode());
            }
            throw new IOException("Unexpected Infrai envelope: " + envelope);
        }
        throw new IOException("Retry limit reached");
    }

    private static String quote(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
