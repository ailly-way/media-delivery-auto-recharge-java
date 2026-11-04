package example.media;

public final class Main {
    public static void main(String[] args) throws Exception {
        LayeredConfig config = LayeredConfig.fromEnvironment();
        InfraiClient client = new InfraiClient(config.apiKey(), config.baseUrl());
        client.configureAutorecharge(config.triggerBalance(), config.rechargeAmount(), "media-account-policy");
        String balanceEnvelope = client.balance();
        double balance = extractBalance(balanceEnvelope);
        MediaDeliveryService service = new MediaDeliveryService(client, config);
        MediaDeliveryService.DeliveryPlan plan = service.ingestAndDeliver(
                new MediaDeliveryService.Asset("asset-2026-001", config.creatorEmail()), balance);
        System.out.println(plan);
    }

    private static double extractBalance(String envelope) {
        int marker = envelope.indexOf("\"balance\":");
        if (marker < 0) throw new IllegalArgumentException("balance missing from response envelope");
        int start = marker + 10;
        int end = start;
        while (end < envelope.length() && "0123456789.-".indexOf(envelope.charAt(end)) >= 0) end++;
        return Double.parseDouble(envelope.substring(start, end));
    }
}
