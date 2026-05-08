import { CurrencyPicker } from "./CurrencyPicker";

interface AmountConverterProps {
  sendAmount: string;
  onSendAmountChange: (next: string) => void;
  receiveAmount: number | null;
  loading: boolean;
  fromCurrency: string;
  toCurrency: string;
  currencies: string[];
  onFromChange: (code: string) => void;
  onToChange: (code: string) => void;
  onSwap: () => void;
}

const formatReceive = (value: number | null, loading: boolean) => {
  if (loading) return "…";
  if (value === null || !Number.isFinite(value)) return "—";
  return new Intl.NumberFormat("en-US", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value);
};

export function AmountConverter({
  sendAmount,
  onSendAmountChange,
  receiveAmount,
  loading,
  fromCurrency,
  toCurrency,
  currencies,
  onFromChange,
  onToChange,
  onSwap,
}: AmountConverterProps) {
  return (
    <section className="amount-converter" aria-label="Amount converter">
      <div className="amount-block">
        <label htmlFor="sendAmount" className="block-label">
          You send
        </label>
        <input
          id="sendAmount"
          className="amount-input"
          type="text"
          inputMode="decimal"
          value={sendAmount}
          onChange={(e) => onSendAmountChange(e.target.value)}
          aria-label="Send amount"
        />
        <CurrencyPicker
          id="from"
          label="From currency"
          value={fromCurrency}
          options={currencies}
          onChange={onFromChange}
        />
      </div>

      <button
        type="button"
        className="swap-button"
        onClick={onSwap}
        aria-label="Swap currencies"
        title="Swap currencies"
      >
        <span aria-hidden="true">⇄</span>
      </button>

      <div className="amount-block">
        <span className="block-label">You receive (Estimated)</span>
        <output
          className="amount-output"
          aria-label="Estimated receive amount"
          aria-live="polite"
        >
          {formatReceive(receiveAmount, loading)}
        </output>
        <CurrencyPicker
          id="to"
          label="To currency"
          value={toCurrency}
          options={currencies}
          onChange={onToChange}
        />
      </div>
    </section>
  );
}
