export interface ProviderQuote {
  id: string;
  name: string;
  logoUrl: string | null;
  type: string | null;
  rate: number;
  fee: number;
  markup: number | null;
  receiveAmount: number;
  savingsVsBaseline: number;
  deliveryDuration: number | null;
  deliveryDurationType: string | null;
  dateCollected: string | null;
  bestDeal: boolean;
  baseline: boolean;
}

export interface QuoteResponse {
  from: string;
  to: string;
  sendAmount: number;
  bestReceiveAmount: number;
  baselineName: string;
  lastRefreshAt: string | null;
  source: string;
  providers: ProviderQuote[];
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

export function getQuote(
  from: string,
  to: string,
  amount: number,
  signal?: AbortSignal,
): Promise<QuoteResponse> {
  const query = new URLSearchParams({
    from,
    to,
    amount: String(amount),
  });
  return request<QuoteResponse>(`/api/quotes?${query.toString()}`, { signal });
}

export function refreshQuote(
  from: string,
  to: string,
  amount: number,
): Promise<QuoteResponse> {
  const query = new URLSearchParams({
    from,
    to,
    amount: String(amount),
  });
  return request<QuoteResponse>(`/api/quotes/refresh?${query.toString()}`, {
    method: "POST",
  });
}
