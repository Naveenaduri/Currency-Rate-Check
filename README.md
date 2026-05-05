# Currency Exchange Rate App

Polls multiple remittance providers, caches their quotes in memory, and exposes the **best direct rate** (and the per-provider breakdown) for any `from -> to` currency pair. The React frontend auto-renders the best rate, the underlying provider, and a comparison panel against the other providers as the user changes currencies.

## Repository layout

```
Remitly/
  backend/      Spring Boot 3 + Java 17 (Maven, JaCoCo)
  frontend/     React 18 + TypeScript (Vite, Vitest)
  README.md
```

## Architecture

```
+-----------------------------+
|   ExchangeRateProvider[]    |  <-- one bean per provider
+--------------+--------------+
               |
        @Scheduled poll                    +--------------------------+
               v                           |     web layer            |
+-----------------------------+   uses     |  CurrencyController      |
|       RateAggregator        +----------> |  RateController          |
|  Map<provider, Map<pair,    |            |  ProviderController      |
|       BigDecimal>>          |            +--------------------------+
+-----------------------------+
```

- **`ExchangeRateProvider`** is the integration point. Each provider supplies its own `fetchRates()` snapshot.
- Two reference implementations ship in the box:
  - **`SimulatedProvider`** – three beans (`Remitly`, `Wise`, `Western Union`), each with a different spread and small per-poll jitter so prices visibly move. This is what runs by default, so the app is fully self-contained.
  - **`HttpExchangeRateProvider`** – generic Frankfurter-compatible client (`GET /latest?from={base}` → `{ "rates": { ... } }`). Off by default; flip `fx.providers.frankfurter.enabled=true` to add it as a fourth provider.
- **`RateAggregator`** runs on `@PostConstruct` and again every `fx.poll.interval-ms` (default 30 s). It calls every provider, replaces that provider's cached snapshot on success, and **keeps the previous snapshot on failure** so a flaky provider doesn't drop the whole feed.
- "Best" for `from -> to` = the highest rate across providers (customer receives the most destination currency per unit of source).

Direct conversion only — there is no transitive path computation, by design.

## Running

### Prerequisites
- JDK 17+ (tested under Java 21)
- Node.js 18+ and npm 9+
- Internet access on first run so the bundled Maven wrapper can fetch Maven 3.9.6

### Backend (port 8080)

```
cd backend
.\mvnw.cmd spring-boot:run        # Windows
./mvnw spring-boot:run            # macOS / Linux
```

You should see `Started FxApplication` followed by silent `RateAggregator` polls every 30 s.

### Frontend (port 5173)

```
cd frontend
npm install
npm run dev
```

Open <http://localhost:5173>. The frontend uses `http://localhost:8080` for the API by default; override via `VITE_API_BASE_URL` in a `.env.local`.

## API contract

| Method | Path | Result |
|--------|------|--------|
| GET | `/api/providers` | `["Remitly","Wise","Western Union"]` |
| GET | `/api/currencies` | Sorted union of currencies seen across all provider snapshots |
| GET | `/api/rates` | Best `RateQuote` per pair: `[{ "from","to","rate","provider" }, ...]` |
| GET | `/api/rates/{from}/{to}` | Best `RateQuote` for the pair, or 404 if no provider quotes it |
| GET | `/api/rates/{from}/{to}/quotes` | `RateComparison` — `{ "from","to","best": {...}, "quotes": [{provider,rate}, ...] }` sorted desc, or 404 if no quotes |
| POST | `/api/rates/refresh` | Triggers an immediate re-poll of every provider, then returns the new best rates |

Errors come back as `{ "error": "...", "status": <code> }`.

## Configuration

`backend/src/main/resources/application.properties`:

```properties
# How often the aggregator polls every registered provider.
fx.poll.interval-ms=30000

# Frankfurter HTTP provider is opt-in (requires internet at runtime).
fx.providers.frankfurter.enabled=false
fx.providers.frankfurter.base-url=https://api.frankfurter.app
fx.providers.frankfurter.currencies=USD,EUR,GBP,JPY,INR
```

To plug in a real provider, implement `ExchangeRateProvider` and register it as a Spring `@Bean` (see `config/ProviderConfig.java`). The aggregator picks it up automatically.

## Tests and coverage

### Backend (JaCoCo, 85% line bundle threshold)

```
cd backend
.\mvnw.cmd verify
```

`verify` runs unit tests (provider, aggregator, service, exception handler), `@WebMvcTest` slices for each controller, and a full `@SpringBootTest` smoke that exercises the live aggregator with the simulated providers. The HTML report is at `backend/target/site/jacoco/index.html`.

### Frontend (Vitest + V8 coverage, 85% / 80% thresholds)

```
cd frontend
npm test          # 37 tests, single run
npm run coverage  # tests + coverage thresholds
```

Latest run: 99% statements, 88% branches, 100% functions. HTML report at `frontend/coverage/index.html`.

## What's intentionally out of scope

- No graph traversal / transitive path computation.
- No automatic inverse-rate computation; `A->B` does not imply `B->A` unless a provider quotes it.
- No persistence beyond process lifetime; restart re-polls.
- No authentication.
- Currency add/remove endpoints are gone — currencies are derived from whatever the providers currently quote.
