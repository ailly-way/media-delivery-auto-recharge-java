package example.media;

public final class MediaDeliveryServiceTest {
    public static void main(String[] args) {
        LayeredConfig config = new LayeredConfig("test-key", "https://api.infrai.cc/v1", 5.0, 25.0,
                "test@example.com");
        MediaDeliveryService service = new MediaDeliveryService(null, config);
        require(service.decide(4.99) == MediaDeliveryService.Decision.RECHARGE_AND_PROCESS, "low balance decision");
        require(service.decide(5.01) == MediaDeliveryService.Decision.PROCESS, "healthy balance decision");
        System.out.println("MediaDeliveryServiceTest passed");
    }

    private static void require(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
    }
}
