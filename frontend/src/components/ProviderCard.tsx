import { useState } from "react";
import type { ProviderQuote } from "../api/client";
import { currencySymbol } from "../data/currencies";

interface ProviderCardProps {
  quote: ProviderQuote;
  fromCurrency: string;
  toCurrency: string;
  baselineName: string;
}

const formatNumber = (n: number, decimals = 2) =>
  new Intl.NumberFormat("en-US", {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  }).format(n);

const formatRate = (n: number) =>
  new Intl.NumberFormat("en-US", {
    minimumFractionDigits: 4,
    maximumFractionDigits: 4,
  }).format(n);

const titleCaseType = (type: string | null): string | null => {
  if (!type) return null;
  if (type === "moneyTransferProvider") return "Money transfer";
  if (type === "bank") return "Bank";
  return type;
};

const formatDelivery = (
  duration: number | null,
  durationType: string | null,
): string | null => {
  if (!duration || !durationType) return null;
  return `${duration} ${durationType}`;
};

const feeBucket = (fee: number) => {
  if (fee <= 1) return { label: "Low fees", className: "fee-low" };
  if (fee <= 5) return { label: "Moderate fees", className: "fee-moderate" };
  return { label: "Higher fees", className: "fee-higher" };
};

export function ProviderCard({
  quote,
  fromCurrency,
  toCurrency,
  baselineName,
}: ProviderCardProps) {
  const [logoFailed, setLogoFailed] = useState(false);
  const initial = quote.name.charAt(0).toUpperCase();
  const toSym = currencySymbol(toCurrency);
  const fromSym = currencySymbol(fromCurrency);

  const savings = quote.savingsVsBaseline;
  const showSavings = !quote.baseline && savings > 0;
  const fee = feeBucket(quote.fee);
  const subtitle = titleCaseType(quote.type);
  const delivery = formatDelivery(
    quote.deliveryDuration,
    quote.deliveryDurationType,
  );

  return (
    <article
      className={`provider-card${quote.bestDeal ? " is-best" : ""}`}
      data-testid={`provider-${quote.id}`}
      aria-label={`${quote.name} quote`}
    >
      <div className="provider-card__brand">
        <div className="provider-logo" aria-hidden="true">
          {quote.logoUrl && !logoFailed ? (
            <img
              src={quote.logoUrl}
              alt=""
              onError={() => setLogoFailed(true)}
            />
          ) : (
            <span className="provider-logo__initial">{initial}</span>
          )}
        </div>
        <div className="provider-card__name">
          <div className="provider-name-row">
            <strong>{quote.name}</strong>
            {quote.bestDeal && <span className="badge badge-best">Best Deal</span>}
          </div>
          <div className="provider-meta">
            {subtitle && <span>{subtitle}</span>}
            {subtitle && delivery && <span className="dot" aria-hidden="true">•</span>}
            {delivery && <span className="delivery">{delivery}</span>}
          </div>
        </div>
      </div>

      <div className="provider-card__receive">
        <span className="muted">You receive</span>
        <div className="receive-amount">
          {toSym}
          {formatNumber(quote.receiveAmount)} {toCurrency}
        </div>
        <div className="rate-line">
          1 {fromCurrency} = {formatRate(quote.rate)} {toCurrency}
        </div>
      </div>

      <div className="provider-card__fees">
        <span className="muted">Total fees</span>
        <div className="fee-amount">
          {fromSym}
          {formatNumber(quote.fee)} {fromCurrency}
        </div>
        <div className={`fee-label ${fee.className}`}>{fee.label}</div>
      </div>

      <div className="provider-card__cta">
        <button type="button" className="cta-button">
          Get this rate <span aria-hidden="true">→</span>
        </button>
        <div className="savings-line">
          {showSavings ? (
            <span className="savings-positive">
              You save {toSym}
              {formatNumber(savings)}
              <small> vs {baselineName}</small>
            </span>
          ) : quote.baseline ? (
            <span className="muted">Standard Rate</span>
          ) : (
            <span className="muted">—</span>
          )}
        </div>
      </div>
    </article>
  );
}
