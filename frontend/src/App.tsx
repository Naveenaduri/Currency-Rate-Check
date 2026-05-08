import { useEffect, useMemo, useState } from "react";
import { AmountConverter } from "./components/AmountConverter";
import { ProviderList } from "./components/ProviderList";
import { useCurrencies } from "./hooks/useCurrencies";
import { useQuote } from "./hooks/useQuote";
import { refreshQuote } from "./api/client";
import "./App.css";

const DEFAULT_FROM = "USD";
const DEFAULT_TO = "INR";
const DEFAULT_AMOUNT = "1000";

const parseAmount = (raw: string): number => {
  const cleaned = raw.replace(/,/g, "").trim();
  const n = Number(cleaned);
  return Number.isFinite(n) ? n : 0;
};

export function App() {
  const {
    currencies,
    loading: currenciesLoading,
    error: currenciesError,
  } = useCurrencies();
  const [from, setFrom] = useState<string>(DEFAULT_FROM);
  const [to, setTo] = useState<string>(DEFAULT_TO);
  const [sendAmount, setSendAmount] = useState<string>(DEFAULT_AMOUNT);
  const [refreshKey, setRefreshKey] = useState(0);
  const [refreshing, setRefreshing] = useState(false);

  useEffect(() => {
    if (currencies.length === 0) return;
    if (!currencies.includes(from)) {
      setFrom(currencies.includes(DEFAULT_FROM) ? DEFAULT_FROM : currencies[0]);
    }
    if (!currencies.includes(to)) {
      setTo(currencies.includes(DEFAULT_TO) ? DEFAULT_TO : currencies[1] ?? currencies[0]);
    }
  }, [currencies, from, to]);

  const numericAmount = useMemo(() => parseAmount(sendAmount), [sendAmount]);

  const { quote, loading: quoteLoading, error: quoteError } = useQuote(
    from,
    to,
    numericAmount,
    refreshKey,
  );

  const handleSwap = () => {
    setFrom(to);
    setTo(from);
  };

  const handleRefresh = async () => {
    if (!from || !to || numericAmount <= 0) return;
    setRefreshing(true);
    try {
      await refreshQuote(from, to, numericAmount);
      setRefreshKey((n) => n + 1);
    } catch {
      // hook will surface errors on the subsequent fetch
    } finally {
      setRefreshing(false);
    }
  };

  return (
    <main className="app">
      <header className="app-header">
        <h1>Compare exchange rates</h1>
        <p className="subtitle">
          Live multi-provider quotes from the Wise V3 Comparisons API.
        </p>
      </header>

      {currenciesError && <p role="alert">{currenciesError}</p>}

      <div className="card-shell card-shell--top">
        <AmountConverter
          sendAmount={sendAmount}
          onSendAmountChange={setSendAmount}
          receiveAmount={quote?.providers[0]?.receiveAmount ?? null}
          loading={quoteLoading || currenciesLoading}
          fromCurrency={from}
          toCurrency={to}
          currencies={currencies}
          onFromChange={setFrom}
          onToChange={setTo}
          onSwap={handleSwap}
        />
        <p className="info-banner" role="note">
          <span className="info-icon" aria-hidden="true">ⓘ</span>
          We compare real exchange rates and fees from multiple providers to get
          you the best deal.
        </p>
      </div>

      <ProviderList
        quote={quote}
        loading={quoteLoading}
        error={quoteError}
        fromCurrency={from}
        toCurrency={to}
        onRefresh={handleRefresh}
        refreshing={refreshing}
      />
    </main>
  );
}

export default App;
