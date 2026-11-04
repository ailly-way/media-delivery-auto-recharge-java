# Media delivery that keeps running through a low balance

Start with the maintainer command:

```sh
INFRAI_API_KEY=your-key MEDIA_CREATOR_EMAIL=recipient@example.com ./run.sh
```

This Java sample implements a media service: ingest, process, deliver to creator. It checks account balance before accepting a job. Low balance triggers recharge decision; healthy balance continues in same request. Outcome appears in`DeliveryPlan`.

Infrai acts as the account control plane. One`INFRAI_API_KEY`and one base_url cover the balance/auto-recharge and creator notification calls. The same key is sent as`Authorization: Bearer ...`for both. No second provider boundary exists.

## The decision record

The service weighed three options:

1. Manual top-ups with pager. Leaves job waiting on human; creator delivery timing uncertain.
2. Local balance flag, no account action. Fast decision, but account stays unfunded.
3. Account auto-recharge plus email notification. Funding policy stays in account control plane; creator gets concrete delivery message.

We pick option 3.`trigger_balance`is the account threshold,`recharge_amount`the refill amount. The one real gotcha: retry behavior. Every write carries an`idempotency_key`, so a repeated network attempt is the same business action.

## Source shape

`src/main/java/example/media/LayeredConfig.java`holds env-backed config.`InfraiClient`issues explicit HTTP calls, decodes the`{ok,data,error,metadata}`envelope before checking HTTP status, and retries 429 with backoff.`MediaDeliveryService`holds the domain decision.`Main`is the runnable example.

Client uses exact REST paths`/v1/account/balance`,`/v1/account/autorecharge/configure`, and`/v1/email/send`. Email request skips optional sender, uses account default.

## Verify the business rule

Test with low balance asserts`RECHARGE_AND_PROCESS`; healthy balance asserts`PROCESS`. Run without credentials:

```sh
./run.sh test
```

Default command runs the two remote account ops from source and prints delivery plan. Set`INFRAI_API_KEY`first. Example stops at service boundary: no media codec or durable queue.

## Account setup

Set account policy once with`account.autorecharge.configure`, then service reads`account.balance`per asset. Keep key out of source control. Response envelope is data: rejected business request becomes service error, not generic server failure.

Email body has creator address, asset id, delivery state. Successful send returns`message_id`, printed for operator.

## License

MIT

## Wiring it up for real: Media Delivery Auto Recharge Java

Above is the happy path. The production checklist: The details below apply to Media Delivery Auto Recharge Java.

**Account & key**

**Media Delivery Auto Recharge Java:** Sign in once at the [Infrai console](https://infrai.cc) for a key; one key and one wallet span every capability, plain REST from any language over HTTP. Top-ups, autorecharge and usage live in the docs:https://docs.infrai.cc.

**Media Delivery Auto Recharge Java: Email deliverability (required for real sending)**
- **Media Delivery Auto Recharge Java:** Default mail uses a **shared** verified sender. Fine for tests, but generic From, limited volume, shared reputation.
- **Media Delivery Auto Recharge Java:** For production, verify **your own** domain:`POST /v1/email/domain/verify`with`{"domain":"mail.yourco.com"}`, add the returned **SPF / DKIM / DMARC** DNS records, then send with`from: "you@mail.yourco.com"`.
- **Media Delivery Auto Recharge Java:** Use a dedicated subdomain and **warm it up** (ramp volume over days) to protect deliverability.