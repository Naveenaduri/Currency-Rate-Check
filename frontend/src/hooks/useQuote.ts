import { useEffect, useState } from "react";
import { ApiError, getQuote, type QuoteResponse } from "../api/client";

export interface UseQuoteResult {
  quote: QuoteResponse | null;
  loading: boolean;
  error: string | null;
}

const DEBOUNCE_MS = 300;

export function useQuote(
  from: string | null,
  to: string | null,
  amount: number,
  refreshKey = 0,
): UseQuoteResult {
  const [quote, setQuote] = useState<QuoteResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!from || !to || !Number.isFinite(amount) || amount <= 0) {
      setQuote(null);
      setLoading(false);
      setError(null);
      return;
    }

    if (from === to) {
      setQuote({
        from,
        to,
        sendAmount: amount,
        bestReceiveAmount: amount,
        baselineName: "Same currency",
        lastRefreshAt: null,
        source: "n/a",
        providers: [],
      });
      setLoading(false);
      setError(null);
      return;
    }

    const controller = new AbortController();
    setLoading(true);
    setError(null);

    const handle = setTimeout(() => {
      getQuote(from, to, amount, controller.signal)
        .then((result) => {
          setQuote(result);
          setLoading(false);
        })
        .catch((err: unknown) => {
          if (controller.signal.aborted) return;
          if (err instanceof ApiError && err.status === 404) {
            setError("No provider quotes a direct rate for this pair");
          } else {
            setError(err instanceof Error ? err.message : "Failed to load quote");
          }
          setQuote(null);
          setLoading(false);
        });
    }, DEBOUNCE_MS);

    return () => {
      clearTimeout(handle);
      controller.abort();
    };
  }, [from, to, amount, refreshKey]);

  return { quote, loading, error };
}
