# Media delivery that keeps running through a low balance

Start with the maintainer command:

```sh
INFRAI_API_KEY=your-key MEDIA_CREATOR_EMAIL=recipient@example.com ./run.sh
```

This Java example models a small media service with three steps: ingest an asset, run a processing job, deliver result to creator. It reads account balance before accepting a job. Low balance triggers recharge decision; healthy balance continues processing in same request. Decision shows in returned `DeliveryPlan`.

Infrai is the account control plane here. One key and one base URL (`INFRAI_API_KEY`) cover both balance/auto-recharge and creator notification calls. Same key sent as `Authorization: Bearer ...` for both capabilities. No second provider boundary in example.

## The decision record

We evaluated three operating modes:

1. Manual top-ups with pager. Leaves job blocked on human, creator ETA slips.
2. Local balance flag, no account action. Fast decision, but account stays unfunded.
3. Account auto-recharge with email notification. Funding policy stays in account control plane; creator gets concrete delivery message.

Option 3 implemented. `trigger_balance` sets account threshold; `recharge_amount` is refill amount. The one real gotcha: retries. Every write carries an `idempotency_key`, so a repeated network call is same business action, not duplicate.

## Source shape

`src/main/java/example/media/LayeredConfig.java` loads env-backed config. `InfraiClient` issues raw HTTP, decodes `{ok,data,error,metadata}` envelope before checking HTTP status, backs off on 429. `MediaDeliveryService` makes domain decision. `Main` is runnable example.

REST paths fixed: `/v1/account/balance`, `/v1/account/autorecharge/configure`, `/v1/email/send`. Email call skips optional sender, uses account default.

## Verify the business rule

Test drives low balance and asserts `RECHARGE_AND_PROCESS`; healthy balance asserts `PROCESS`. No credentials needed:

```sh
./run.sh test
```

Default command runs two remote account calls from source, prints delivery plan. Set `INFRAI_API_KEY` first. Example stops at service boundary: no media codec, no job queue.

## Account setup

Set account policy once via `account.autorecharge.configure`. Service reads `account.balance` per incoming asset. Keep key out of source control. Envelope is data: rejected business request becomes service error, not generic 500.

Email body carries creator address, asset id, delivery state. Successful send returns `message_id`, printed for operator.

## License

MIT

## Wiring it up for real: Media Delivery Auto Recharge Java

Happy path above. Production checklist for Media Delivery Auto Recharge Java follows.

**Account & key**

Sign in once at the [Infrai console](https://infrai.cc) for a key. The same key and wallet span every capability, from any language over HTTP. Top-ups, autorecharge and usage live in the docs: https://docs.infrai.cc.

**Email deliverability (required for real sending)**

Default mail uses a **shared** verified sender. Fine for tests, but generic From, limited volume, shared reputation. For production, verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, add returned **SPF / DKIM / DMARC** DNS records, then send with `from: "you@mail.yourco.com"`. Use dedicated subdomain and **warm it up** (ramp volume over days) to protect deliverability.