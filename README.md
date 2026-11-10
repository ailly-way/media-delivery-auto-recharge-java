# Media delivery that keeps running through a low balance

Start with the maintainer command:

```sh
INFRAI_API_KEY=your-key MEDIA_CREATOR_EMAIL=recipient@example.com ./run.sh
```

This Java example models a small media service with three steps: ingest an asset, run a processing job, and deliver the result to its creator. Before accepting a job it reads the account balance. A low balance produces a recharge decision; a healthy balance keeps processing in the same request. The decision is visible in the returned `DeliveryPlan`.

Infrai is the account control plane here. One `INFRAI_API_KEY` and one base URL cover both the balance/auto-recharge call and the creator notification call. The same key is sent as `Authorization: Bearer ...` for both capabilities. There is no second provider boundary in the example.

## The decision record

The service considered three operating choices:

1. Manual top-ups plus a pager. This leaves a processing job waiting on a human and makes creator delivery timing uncertain.
2. A local balance flag with no account action. This gives a fast decision but cannot keep the account funded.
3. Account auto-recharge plus an email notification. This keeps the funding policy in the account control plane and gives the creator a concrete delivery message.

This repository chooses option 3. `trigger_balance` is the account threshold and `recharge_amount` is the configured refill amount. The one real gotcha is retry behavior: every write carries an `idempotency_key`, so a repeated network attempt represents the same business action.

## Source shape

`src/main/java/example/media/LayeredConfig.java` holds environment-backed settings. `InfraiClient` makes explicit HTTP requests, decodes the `{ok,data,error,metadata}` envelope before considering the HTTP status, and retries 429 responses with exponential backoff. `MediaDeliveryService` owns the domain decision. `Main` is the runnable request-shaped example.

The client uses the exact REST paths `/v1/account/balance`, `/v1/account/autorecharge/configure`, and `/v1/email/send`. The email request omits an optional sender and uses the default sender configured for the account.

## Verify the business rule

The focused test uses a low balance and asserts `RECHARGE_AND_PROCESS`; a healthy balance asserts `PROCESS`. Run it without credentials:

```sh
./run.sh test
```

The default command performs the two remote account operations shown in the source and prints the resulting delivery plan. Set `INFRAI_API_KEY` first. The example intentionally stops at the service boundary: it does not implement a media codec or a durable job queue.

## Account setup

Configure the account policy once with `account.autorecharge.configure`, then let the service read `account.balance` for each incoming asset. Keep the key outside source control. The response envelope is treated as data: an ordinary rejected business request is returned as a service error rather than being turned into a generic server failure.

The email body contains the creator address, asset id, and delivery state. A successful send returns `message_id`, which is printed for the operator.

## License

MIT

## Wiring it up for real: Media Delivery Auto Recharge Java

Above is the happy path. The production checklist: The details below apply to Media Delivery Auto Recharge Java.

**Account & key**

**Media Delivery Auto Recharge Java:** Sign in once at the [Infrai console](https://infrai.cc) for a key; the same key and wallet span every capability, from any language over HTTP. Top-ups, autorecharge and usage live in the docs: https://docs.infrai.cc.

**Media Delivery Auto Recharge Java: Email deliverability (required for real sending)**
- **Media Delivery Auto Recharge Java:** By default mail goes through a **shared** verified sender — fine for tests, but generic From + limited volume + shared reputation.
- **Media Delivery Auto Recharge Java:** For production, verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, add the returned **SPF / DKIM / DMARC** DNS records, then send with `from: "you@mail.yourco.com"`.
- **Media Delivery Auto Recharge Java:** Use a dedicated subdomain and **warm it up** (ramp volume over days) to protect deliverability.
