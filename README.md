# Remittance comparison app

A small full-stack app that **pulls live, real, multi-provider remittance quotes
from the public Wise V3 Comparisons API** and renders the comparison UI you
expect from sites like Monito or Wise's own "compare" page. No hardcoded
provider data, no auth keys.

```
React + Vite (frontend)  ──►  Spring Boot 3 / Java 17 (backend)
                                            │
                                            ▼
                          https://api.wise.com/v3/comparisons/
                          (public, no auth, ~9 providers per pair)
```

## How rates are sourced

Wise publishes a public comparison endpoint at
`https://api.wise.com/v3/comparisons/?sourceCurrency=USD&targetCurrency=INR&sendAmount=1000`
that returns competitor quotes (Remitly, Xoom, MoneyGram, Instarem, OFX, plus
banks like Chase, Wells Fargo, State Bank of India, etc.) with **real rates,
fees, logos, and collection timestamps**. The backend wraps that with a small
TTL cache (default 5 min) so we don't hammer Wise on every keystroke.

For each user-entered amount we keep the cached rate + fee per corridor and
recompute `receiveAmount = (sendAmount - fee) × rate` instantaneously, picking
the worst bank (or worst-receiving provider, if no bank is in the result) as
the "Standard rate" baseline that "You save … vs ___" is computed against.

## Project layout

```
backend/   Spring Boot 3 service (Java 17, Maven, JaCoCo)
frontend/  React 18 + TypeScript + Vite (Vitest, React Testing Library)
```

## Running locally

### Backend (port 8080)

```bash
cd backend
./mvnw spring-boot:run        # macOS/Linux
.\mvnw.cmd spring-boot:run    # Windows PowerShell
```

The backend lazy-loads on the first `/api/quotes` request — no warm-up needed.

### Frontend (port 5173)

```bash
cd frontend
npm install
npm run dev
```

Open <http://localhost:5173>. The frontend talks to `http://localhost:8080`
by default; override with `VITE_API_BASE_URL` in a `.env.local`.

## REST API

| Method & path                                 | Purpose                                       |
| --------------------------------------------- | --------------------------------------------- |
| `GET  /api/currencies`                        | Configured currency dropdown list             |
| `GET  /api/providers`                         | Names of providers seen in cached corridors   |
| `GET  /api/quotes?from=USD&to=INR&amount=1000`| **Full comparison response for the UI**       |
| `POST /api/quotes/refresh?from=...&to=...&amount=...` | Force a cache miss and refetch        |

Sample `GET /api/quotes` response:

```json
{
  "from": "USD",
  "to": "INR",
  "sendAmount": 1000.00,
  "bestReceiveAmount": 94447.34,
  "baselineName": "Chase (US)",
  "lastRefreshAt": "2026-05-08T04:56:21Z",
  "source": "Wise V3 Comparisons API",
  "providers": [
    {
      "id": "remitly",
      "name": "Remitly",
      "logoUrl": "https://dq8dwmysp7hk1.cloudfront.net/logos/remitly.svg",
      "type": "moneyTransferProvider",
      "rate": 94.447340,
      "fee": 0.00,
      "markup": 0.116,
      "receiveAmount": 94447.34,
      "savingsVsBaseline": 3190.67,
      "dateCollected": "2026-05-08T04:48:05Z",
      "bestDeal": true,
      "baseline": false
    }
    /* ... 8 more providers ... */
  ]
}
```

## Configuration knobs

Edit `backend/src/main/resources/application.properties` (or pass `-D` flags):

| Property                                | Default                                    | Effect |
| --------------------------------------- | ------------------------------------------ | ------ |
| `fx.wise.base-url`                      | `https://api.wise.com`                     | Override for proxies / test stubs |
| `fx.wise.connect-timeout-ms`            | `5000`                                     | Connect timeout to Wise |
| `fx.wise.read-timeout-ms`               | `8000`                                     | Read timeout for Wise responses |
| `fx.cache.sample-amount`                | `1000`                                     | Send amount used when warming the cache |
| `fx.cache.ttl-seconds`                  | `300`                                      | TTL before a corridor is re-fetched |
| `fx.currencies.supported`               | `USD,EUR,GBP,JPY,INR,MXN,PHP,AUD,CAD,SGD,CHF` | Currencies shown in the picker |
| `fx.cors.allowed-origin-patterns`       | `http://localhost:*,http://127.0.0.1:*`    | CORS origin patterns |

## Testing

### Backend

```bash
cd backend
./mvnw verify
```

JaCoCo enforces ≥ 85 % line coverage; the build fails otherwise. Reports
land in `backend/target/site/jacoco/index.html`.

### Frontend

```bash
cd frontend
npm run test          # one-shot
npm run coverage      # with v8 coverage report (≥ 85 % statements + branches)
```

## Architecture notes

- `WiseComparisonClient` is the single network seam. It speaks the Wise V3
  Comparisons API directly — no model translation in between, just JSON →
  records.
- `RateAggregator` keeps an in-memory `ConcurrentHashMap<CurrencyPair,
  CachedComparison>` with a TTL. Concurrent callers for the same corridor
  funnel through `Map.compute`, so we never fan out duplicate requests for the
  same pair.
- If a Wise refresh fails *after* we already have a cached snapshot, the
  aggregator serves stale data instead of throwing — so a transient outage
  doesn't break the UI.
- The frontend debounces send-amount edits (300 ms) and uses
  `AbortController` to cancel stale in-flight requests, so editing the
  amount stays smooth.
- The `ProviderCard` falls back to a name-initial avatar if a provider's
  CDN-hosted logo fails to load.

## Caveats

- Wise's V3 endpoint is documented as deprecated; if/when they shut it off,
  swap the base URL to `/v4/` and add Basic Auth via Wise's affiliate program.
  The `WiseComparisonClient` interface stays the same.
- Rates and fees come straight from Wise's collection pipeline and are
  refreshed by Wise on their own cadence (per-provider `dateCollected`
  timestamps appear in the UI's debug payload). They are intended for
  comparison, not as committed live execution rates.
