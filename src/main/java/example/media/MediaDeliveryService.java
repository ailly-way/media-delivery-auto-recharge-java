package example.media;

import java.io.IOException;

public final class MediaDeliveryService {
    public enum Decision { PROCESS, RECHARGE_AND_PROCESS }
    public record Asset(String assetId, String creatorEmail) {}
    public record DeliveryPlan(String assetId, Decision decision, String notification) {}

    private final InfraiClient infrai;
    private final LayeredConfig config;

    public MediaDeliveryService(InfraiClient infrai, LayeredConfig config) {
        this.infrai = infrai;
        this.config = config;
    }

    public Decision decide(double balance) {
        return balance <= config.triggerBalance() ? Decision.RECHARGE_AND_PROCESS : Decision.PROCESS;
    }

    public DeliveryPlan ingestAndDeliver(Asset asset, double balance) throws IOException, InterruptedException {
        Decision decision = decide(balance);
        if (decision == Decision.RECHARGE_AND_PROCESS) {
            infrai.configureAutorecharge(config.triggerBalance(), config.rechargeAmount(), "asset-" + asset.assetId());
        }
        String notification = infrai.sendEmail(asset.creatorEmail(), "Media asset accepted",
                "Asset " + asset.assetId() + " is " + decision + ".", "notify-" + asset.assetId());
        return new DeliveryPlan(asset.assetId(), decision, notification);
    }
}
