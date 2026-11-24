# Payment Gateway

A simplified payment gateway that lets a merchant process a card payment and
retrieve its details afterwards, simulating the acquiring bank side of the
flow per the exercise brief.

## Running it

Requires Docker and JDK 17+ (built/tested on JDK 23).

```bash
docker-compose up -d        # starts the bank simulator on localhost:8080
mvn spring-boot:run         # starts the gateway on localhost:8090
```

Run the tests (the full suite, including the integration test, needs the
simulator running first):

```bash
docker-compose up -d
mvn test
```

## API

| | |
|---|---|
| `POST /payments` | Process a payment. `201` + payment details if Authorized/Declined by the bank, `400` if Rejected (failed validation, bank never called). Accepts an optional `Idempotency-Key` header - see below. |
| `GET /payments/{id}` | Retrieve a previously processed payment. `200` + details, or `404` if unknown. |

## Architecture

```
controller/   HTTP layer only - no business logic
service/      PaymentService - orchestrates validate -> call bank -> persist
validation/   PaymentRequestValidator - the cross-field/business rules that
              Bean Validation annotations can't express
bank/         AcquiringBankClient (interface) + AcquiringBankHttpClient (impl) -
              the one adapter that knows it's talking to this specific bank/simulator
domain/       Payment, PaymentStatus - internal record, deliberately minimal
repository/   PaymentRepository (interface) + in-memory ConcurrentHashMap impl
mapper/       PaymentMapper - the one place that converts between our API's
              DTOs, the bank's wire format, and the domain model
exception/    Typed exceptions + a single @ControllerAdvice mapping them to
              HTTP responses
```

`AcquiringBankClient` and `PaymentRepository` are interfaces specifically so
`PaymentService` depends on abstractions, not concrete implementations - the
bank integration or storage mechanism can change without touching orchestration
logic or tests.

## Key design decisions & assumptions

**Rejected payments are not persisted.** The brief states "no payment could be
created" for a Rejected outcome, so a failed-validation request gets no id and
is never stored - it returns `400` with `{"message": "Rejected", "details": [...]}`
and the bank is never called. Authorized/Declined payments are the only ones
that get an id and become retrievable via `GET`.

**Validation is split across two layers.** Per-field syntax (length, numeric
format, ranges) lives as Bean Validation annotations directly on the request
DTO - declarative and framework-native, nothing to hand-roll. Cross-field and
business-policy rules that annotations can't express cleanly - expiry
month+year combined, and the currency allow-list - live in a dedicated
`PaymentRequestValidator`, run after the DTO-level checks pass.

**Expiry "in the future" is treated as "not yet expired."** A card expiring
this calendar month is still valid through the end of that month, so the
current month passes the check; anything before it fails.

**Currency allow-list is `GBP`, `USD`, `EUR`.** All three use the same 2-decimal
minor-unit convention, so the `amount` field is handled as an opaque integer
throughout - no currency-specific decimal conversion is needed anywhere in the
code. (This wouldn't hold if a 0-decimal currency like JPY or a 3-decimal one
like KWD were ever added to the allow-list.)

**CVV and the full card number are never persisted.** Only the last four
digits reach the domain model (`Payment`) and the API response - matches the
compliance note in the brief and limits the blast radius of the in-memory
store.

**A bank communication failure maps to `502`, not Authorized/Declined/Rejected.**
If the bank is unreachable or returns its `503`, that's a gateway-level failure
distinct from all three defined outcomes - nothing is persisted for it either.

**No async queue.** The merchant needs the real Authorized/Declined decision in
the same HTTP response, so the bank call is synchronous. A queue would only
make sense for later stages this exercise doesn't cover (capture, settlement,
webhook retries).

**Bounded retry on the bank call, but only where it's safe.** `AcquiringBankHttpClient`
retries (3 attempts, short backoff) on the bank's own `503` and on a refused/
unestablished connection - both cases where nothing was authorized. It
deliberately does **not** retry on a read timeout: the request may have already
reached the bank and been authorized, and retrying blind could cause a double
authorization with no way to detect it from our side.

**No circuit breaker.** Earns its complexity at sustained production traffic
protecting a shared downstream dependency - not meaningfully demonstrable
against a single in-process simulator. Documented as a known gap rather than
built.

**Idempotency is supported via an optional `Idempotency-Key` header**, a
deliberate addition on top of the two required endpoints - not asked for by
the spec, and not required for either endpoint to work correctly without it.
It's included to show awareness of a real payments-industry risk (a merchant's
client retries after losing the response and ends up with two payments for one
purchase) without changing the required contract: a merchant who never sends
the header sees no difference in behavior at all. First use of a key
processes normally and remembers `key -> (request, response)`; a repeat of the
same key with the same request body replays the original response without
calling the bank again; a repeat with a *different* body is rejected with
`409 Conflict` rather than silently replayed, since that mismatch usually
signals a bug on the caller's side rather than a legitimate retry. Known gap:
no per-key locking, so two concurrent requests both using the same brand-new
key could both slip past the "have I seen this key" check before either saves
- acceptable for this scope, but a production version would need to serialize
concurrent first-uses of a key.

## Testing

- **Unit** - `PaymentRequestValidatorTest`, `PaymentMapperTest`,
  `PaymentServiceImplTest` (mocked collaborators, including the idempotency
  replay/conflict/no-key cases), `AcquiringBankHttpClientTest` (mocked
  `RestTemplate`, covers the retry/no-retry decision matrix).
- **Controller slice** (`@WebMvcTest`) - `PaymentControllerValidationTest`,
  exercising the `@Valid` path and malformed-UUID handling with the service
  mocked out.
- **Integration** (`@SpringBootTest`, real bank simulator) -
  `PaymentGatewayIntegrationTest`, covering authorized/declined/bank-unavailable
  card endings end-to-end, retrieval, not-found, and the idempotency
  replay/conflict behavior over real HTTP headers.
