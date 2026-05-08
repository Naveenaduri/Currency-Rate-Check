import { currencyMeta } from "../data/currencies";

interface CurrencyPickerProps {
  id: string;
  label: string;
  value: string;
  options: string[];
  onChange: (code: string) => void;
}

/**
 * Compact "[flag] CODE  Currency Name  v" picker used inside the
 * AmountConverter panel.
 */
export function CurrencyPicker({
  id,
  label,
  value,
  options,
  onChange,
}: CurrencyPickerProps) {
  const meta = currencyMeta(value);

  return (
    <div className="currency-picker">
      <span className="flag" aria-hidden="true">{meta.flag}</span>
      <div className="currency-picker__text">
        <strong>{meta.code}</strong>
        <span className="currency-name">{meta.name}</span>
      </div>
      <select
        id={id}
        aria-label={label}
        className="currency-picker__select"
        value={value}
        onChange={(e) => onChange(e.target.value)}
      >
        {options.length === 0 ? (
          <option value="">—</option>
        ) : (
          options.map((code) => {
            const m = currencyMeta(code);
            return (
              <option key={code} value={code}>
                {m.flag} {m.code} — {m.name}
              </option>
            );
          })
        )}
      </select>
      <span className="currency-picker__chevron" aria-hidden="true">▾</span>
    </div>
  );
}
