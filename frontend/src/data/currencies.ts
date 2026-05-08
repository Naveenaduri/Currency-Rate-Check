export interface CurrencyMeta {
  code: string;
  name: string;
  flag: string; // emoji flag for cheap-and-cheerful display
}

export const CURRENCY_CATALOG: Record<string, CurrencyMeta> = {
  USD: { code: "USD", name: "US Dollar", flag: "🇺🇸" },
  EUR: { code: "EUR", name: "Euro", flag: "🇪🇺" },
  GBP: { code: "GBP", name: "British Pound", flag: "🇬🇧" },
  JPY: { code: "JPY", name: "Japanese Yen", flag: "🇯🇵" },
  INR: { code: "INR", name: "Indian Rupee", flag: "🇮🇳" },
  MXN: { code: "MXN", name: "Mexican Peso", flag: "🇲🇽" },
  PHP: { code: "PHP", name: "Philippine Peso", flag: "🇵🇭" },
  CAD: { code: "CAD", name: "Canadian Dollar", flag: "🇨🇦" },
  AUD: { code: "AUD", name: "Australian Dollar", flag: "🇦🇺" },
};

export const currencyMeta = (code: string): CurrencyMeta =>
  CURRENCY_CATALOG[code] ?? { code, name: code, flag: "🏳️" };

export const CURRENCY_SYMBOLS: Record<string, string> = {
  USD: "$",
  EUR: "€",
  GBP: "£",
  JPY: "¥",
  INR: "₹",
  MXN: "$",
  PHP: "₱",
  CAD: "$",
  AUD: "$",
};

export const currencySymbol = (code: string): string =>
  CURRENCY_SYMBOLS[code] ?? "";
