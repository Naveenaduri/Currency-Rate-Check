interface CurrencySelectorProps {
  label: string;
  value: string;
  options: string[];
  onChange: (code: string) => void;
  id: string;
}

export function CurrencySelector({
  label,
  value,
  options,
  onChange,
  id,
}: CurrencySelectorProps) {
  return (
    <label htmlFor={id} className="currency-selector">
      <span>{label}</span>
      <select
        id={id}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        aria-label={label}
      >
        {options.length === 0 ? (
          <option value="">No currencies</option>
        ) : (
          options.map((code) => (
            <option key={code} value={code}>
              {code}
            </option>
          ))
        )}
      </select>
    </label>
  );
}
