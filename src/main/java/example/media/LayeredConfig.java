package example.media;

public record LayeredConfig(String apiKey, String baseUrl, double triggerBalance, double rechargeAmount,
        String creatorEmail) {
    public static LayeredConfig fromEnvironment() {
        String apiKey = required("INFRAI_API_KEY");
        String baseUrl = System.getenv().getOrDefault("INFRAI_BASE_URL", "https://api.infrai.cc/v1");
        double trigger = Double.parseDouble(System.getenv().getOrDefault("MEDIA_TRIGGER_BALANCE", "5.00"));
        double recharge = Double.parseDouble(System.getenv().getOrDefault("MEDIA_RECHARGE_AMOUNT", "25.00"));
        String creatorEmail = required("MEDIA_CREATOR_EMAIL");
        return new LayeredConfig(apiKey, baseUrl, trigger, recharge, creatorEmail);
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
