import { useEffect, useState } from "react";
import { CurrencySelector } from "./components/CurrencySelector";
import { RateDisplay } from "./components/RateDisplay";
import { RatesTable } from "./components/RatesTable";
import { QuoteComparison } from "./components/QuoteComparison";
import { ProviderBar } from "./components/ProviderBar";
import { useCurrencies } from "./hooks/useCurrencies";
import { useRate } from "./hooks/useRate";
import { refreshRates } from "./api/client";
import "./App.css";

export function App() {
  const {
    currencies,
    loading: currenciesLoading,
    error: currenciesError,
    refresh: refreshCurrencies,
  } = useCurrencies();
  const [from, setFrom] = useState<string>("");
  const [to, setTo] = useState<string>("");
  const [refreshKey, setRefreshKey] = useState(0);
  const [refreshing, setRefreshing] = useState(false);

  useEffect(() => {
    if (currencies.length === 0) {
      setFrom("");
      setTo("");
      return;
    }
    if (!from || !currencies.includes(from)) {
      setFrom(currencies[0]);
    }
    if (!to || !currencies.includes(to)) {
      setTo(currencies[1] ?? currencies[0]);
    }
  }, [currencies, from, to]);

  const { comparison, loading: rateLoading, error: rateError } = useRate(
    from,
    to,
    refreshKey,
  );

  const handleRefresh = async () => {
    setRefreshing(true);
    try {
      await refreshRates();
    } catch {
      // surfaced to the user via the rate panel on the next fetch
    } finally {
      refreshCurrencies();
      setRefreshKey((n) => n + 1);
      setRefreshing(false);
    }
  };

  return (
    <main className="app">
      <header>
        <h1>Currency Exchange Rates</h1>
        <p className="subtitle">
          Best rate across multiple remittance providers, polled live.
        </p>
      </header>

      <ProviderBar
        refreshKey={refreshKey}
        onRefresh={handleRefresh}
        refreshing={refreshing}
      />

      {currenciesError && <p role="alert">{currenciesError}</p>}
      {currenciesLoading ? (
        <p role="status">Loading currencies...</p>
      ) : (
        <section className="converter" aria-label="Convert currency">
          <div className="selectors">
            <CurrencySelector
              id="from"
              label="From"
              value={from}
              options={currencies}
              onChange={setFrom}
            />
            <CurrencySelector
              id="to"
              label="To"
              value={to}
              options={currencies}
              onChange={setTo}
            />
          </div>
          <RateDisplay
            from={from}
            to={to}
            comparison={comparison}
            loading={rateLoading}
            error={rateError}
          />
          <QuoteComparison comparison={comparison} />
        </section>
      )}

      <section aria-label="All rates">
        <h2>Best rates by pair</h2>
        <RatesTable refreshKey={refreshKey} />
      </section>
    </main>
  );
}

export default App;
