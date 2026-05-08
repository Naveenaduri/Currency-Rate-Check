import { useState } from "react";
import type { QuoteResponse } from "../api/client";
import { ProviderCard } from "./ProviderCard";

interface ProviderListProps {
  quote: QuoteResponse | null;
  loading: boolean;
  error: string | null;
  fromCurrency: string;
  toCurrency: string;
  onRefresh?: () => void;
  refreshing?: boolean;
}

const INITIAL_VISIBLE = 4;

export function ProviderList({
  quote,
  loading,
  error,
  fromCurrency,
  toCurrency,
  onRefresh,
  refreshing,
}: ProviderListProps) {
  const [expanded, setExpanded] = useState(false);

  if (error) {
    return (
      <p role="alert" className="list-message">
        {error}
      </p>
    );
  }
  if (loading && !quote) {
    return <p role="status" className="list-message">Loading providers...</p>;
  }
  if (!quote) return null;
  if (quote.providers.length === 0) {
    return <p role="status" className="list-message">No quotes available.</p>;
  }

  const visible = expanded
    ? quote.providers
    : quote.providers.slice(0, INITIAL_VISIBLE);
  const hidden = quote.providers.length - visible.length;

  return (
    <section className="provider-list" aria-label="Provider quotes">
      <header className="provider-list__header">
        <h2>Best exchange rate for you</h2>
        <span className="pill" aria-label={`${quote.providers.length} providers compared`}>
          {quote.providers.length} Providers compared
        </span>
        {onRefresh && (
          <button
            type="button"
            className="how-we-compare refresh-button"
            onClick={onRefresh}
            disabled={refreshing}
            aria-label="Refresh quotes"
          >
            {refreshing ? "Refreshing…" : "Refresh quotes"}
          </button>
        )}
      </header>

      <div className="provider-cards">
        {visible.map((q) => (
          <ProviderCard
            key={q.id}
            quote={q}
            fromCurrency={fromCurrency}
            toCurrency={toCurrency}
            baselineName={quote.baselineName}
          />
        ))}
      </div>

      {quote.providers.length > INITIAL_VISIBLE && (
        <button
          type="button"
          className="view-all"
          onClick={() => setExpanded((prev) => !prev)}
        >
          {expanded
            ? `Show top ${INITIAL_VISIBLE} only`
            : `View all providers (${quote.providers.length})`}
          <span aria-hidden="true">{expanded ? " ▴" : " ▾"}</span>
        </button>
      )}
      {!expanded && hidden > 0 && null}
    </section>
  );
}
