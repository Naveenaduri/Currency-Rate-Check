export interface RateQuote {
  from: string;
  to: string;
  rate: number;
  provider: string;
}

export interface ProviderQuote {
  provider: string;
  rate: number;
}

export interface RateComparison {
  from: string;
  to: string;
  best: ProviderQuote | null;
  quotes: ProviderQuote[];
}

export class ApiError extends Error {
  status: number;
  constructor(message: string, status: number) {
    super(message);
    this.status = status;
    this.name = "ApiError";
  }
}

const baseUrl = (): string => {
  const envBase = (import.meta as { env?: { VITE_API_BASE_URL?: string } }).env
    ?.VITE_API_BASE_URL;
  return envBase ?? "http://localhost:8080";
};

async function request<T>(
  path: string,
  init?: RequestInit & { signal?: AbortSignal },
): Promise<T> {
  const response = await fetch(`${baseUrl()}${path}`, {
    headers: { "Content-Type": "application/json", ...(init?.headers ?? {}) },
    ...init,
  });

  if (!response.ok) {
    let message = response.statusText;
    try {
      const body = await response.json();
      if (body && typeof body.error === "string") message = body.error;
    } catch {
      // body wasn't JSON; keep statusText
    }
    throw new ApiError(message, response.status);
  }

  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

export function getCurrencies(signal?: AbortSignal): Promise<string[]> {
  return request<string[]>("/api/currencies", { signal });
}

export function getProviders(signal?: AbortSignal): Promise<string[]> {
  return request<string[]>("/api/providers", { signal });
}

export function getRates(signal?: AbortSignal): Promise<RateQuote[]> {
  return request<RateQuote[]>("/api/rates", { signal });
}

export function getBestRate(
  from: string,
  to: string,
  signal?: AbortSignal,
): Promise<RateQuote> {
  return request<RateQuote>(
    `/api/rates/${encodeURIComponent(from)}/${encodeURIComponent(to)}`,
    { signal },
  );
}

export function getQuotes(
  from: string,
  to: string,
  signal?: AbortSignal,
): Promise<RateComparison> {
  return request<RateComparison>(
    `/api/rates/${encodeURIComponent(from)}/${encodeURIComponent(to)}/quotes`,
    { signal },
  );
}

export function refreshRates(): Promise<RateQuote[]> {
  return request<RateQuote[]>("/api/rates/refresh", { method: "POST" });
}
