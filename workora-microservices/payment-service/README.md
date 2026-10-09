# payment-service

Bakong fee payments, QR generation and verification

## Purpose

Payment creation, idempotent references, local/KHQR-compatible QR payload
generation, payment verification, expiry handling, and payment history are
implemented. The default provider is a local simulator; configure a real
Bakong provider before production use.

## Endpoints

- GET /api/health – health check
- POST `/api/v1/payments` – create a payment and QR payload
- GET `/api/v1/payments` – list the current user's payments
- GET `/api/v1/payments/{id}` – retrieve a payment
- POST `/api/v1/payments/{id}/verify` – verify a provider reference
- POST `/api/v1/payments/{id}/refresh-status` – poll Bakong transaction status
- POST `/api/v1/payments/callbacks/bakong` – receive the bank payment webhook

Set `PAYMENT_PROVIDER=bakong` and configure `BAKONG_BASE_URL`, either
`BAKONG_TOKEN` or `BAKONG_USERNAME`/`BAKONG_PASSWORD`, and
`BAKONG_CALLBACK_URL`. The adapter calls `/authentication`, then
`/create-transaction` with the Workora reference, amount, currency, purpose,
and callback URL. It stores the returned QR code, deep link, and transaction
ID, and `/refresh-status` calls `/transaction-status`.

The callback is intentionally public because the bank cannot provide a
Keycloak token. Set `BAKONG_CALLBACK_SECRET` when the bank supports sending
the same value in `X-Bakong-Callback-Secret`; otherwise protect the callback
at the gateway or with the bank's documented signature mechanism before
production. The payment API is otherwise protected by Keycloak JWT validation.
