# Payment Gateway

A simplified payment gateway that lets a merchant process a card payment and
retrieve its details afterwards, simulating the acquiring bank side of the
flow per the exercise brief.

## Running it

Requires Docker and JDK 17 (built/tested on JDK 17).

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
domain/       Payment, PaymentStatus - internal record
repository/   PaymentRepository (interface) + in-memory ConcurrentHashMap impl
mapper/       PaymentMapper - the one place that converts between our API's
              DTOs, the bank's wire format, and the domain model
exception/    Typed exceptions + a single @ControllerAdvice mapping them to
              HTTP responses
```

## Key design decisions & assumptions

**Rejected payments are not persisted.** The brief states "no payment could be
created" for a Rejected outcome, so a failed-validation request gets no id and
is never stored - it returns `400` with `{"message": "Rejected", "details": [...]}`
and the bank is never called. Authorized/Declined payments are the only ones
that get an id and become retrievable via `GET`.

**Validation is split across two layers.** Per-field syntax (length, format,
ranges) uses Bean Validation annotations on the DTO. Cross-field/business
rules (expiry month+year, currency allow-list) live in `PaymentRequestValidator`.

**Expiry "in the future" is treated as "not yet expired."** A card expiring
this calendar month is still valid through the end of that month, so the
current month passes the check; anything before it fails.

**Currency allow-list is `GBP`, `USD`, `EUR`** (the brief caps it at 3 codes,
not which ones).

**A decimal `amount` (e.g. `4.59`) is silently truncated to `4`, not rejected.**
Known gap against the brief's "must be an integer" rule - Jackson's default
behavior truncates rather than erroring on a non-integral value for an `int`
field.

**CVV and the full card number are never persisted.** Only the last four
digits reach the domain model (`Payment`) and the API response - matches the
compliance note in the brief and limits the blast radius of the in-memory
store.

**A bank communication failure maps to `502`, not Authorized/Declined/Rejected.**
If the bank is unreachable or returns its `503`, that's a gateway-level failure
distinct from all three defined outcomes - nothing is persisted for it either.

**Bounded retry on the bank call (3 attempts) on `503`/connection-refused only,
not on a read timeout**, since a timeout can't rule out the bank already
authorized the card.

**Idempotency via an optional `Idempotency-Key` header.**
A repeat of the same key with the same body replays the original response
(no second bank call); a different body under the same key returns `409`.
Only a SHA-256 fingerprint of the request is stored, never the card data itself.

**Single-merchant assumption** - no merchant/tenant concept, no auth;
`PaymentRepository` and `IdempotencyStore` are global, shared by every caller.

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
