import { useEffect, useState } from "react";
import { ApiError, getQuotes, type RateComparison } from "../api/client";

export interface UseRateResult {
  comparison: RateComparison | null;
  loading: boolean;
  error: string | null;
}

const DEBOUNCE_MS = 250;

/**
 * Fetches the multi-provider quote comparison for {@code from -> to} whenever
 * either side changes. Returns the full comparison so the UI can show both the
 * best-rate banner and the per-provider breakdown.
 */
export function useRate(
  from: string | null,
  to: string | null,
  refreshKey = 0,
): UseRateResult {
  const [comparison, setComparison] = useState<RateComparison | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!from || !to) {
      setComparison(null);
      setLoading(false);
      setError(null);
      return;
    }

    if (from === to) {
      const identity = { provider: "—", rate: 1 };
      setComparison({ from, to, best: identity, quotes: [identity] });
      setLoading(false);
      setError(null);
      return;
    }

    const controller = new AbortController();
    setLoading(true);
    setError(null);

    const handle = setTimeout(() => {
      getQuotes(from, to, controller.signal)
        .then((result) => {
          setComparison(result);
          setLoading(false);
        })
        .catch((err: unknown) => {
          if (controller.signal.aborted) return;
          if (err instanceof ApiError && err.status === 404) {
            setError("No provider quotes a direct rate for this pair");
          } else {
            setError(err instanceof Error ? err.message : "Failed to load rate");
          }
          setComparison(null);
          setLoading(false);
        });
    }, DEBOUNCE_MS);

    return () => {
      clearTimeout(handle);
      controller.abort();
    };
  }, [from, to, refreshKey]);

  return { comparison, loading, error };
}
