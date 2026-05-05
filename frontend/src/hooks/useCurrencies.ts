import { useCallback, useEffect, useState } from "react";
import { getCurrencies } from "../api/client";

export interface UseCurrenciesResult {
  currencies: string[];
  loading: boolean;
  error: string | null;
  refresh: () => void;
}

export function useCurrencies(): UseCurrenciesResult {
  const [currencies, setCurrencies] = useState<string[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [tick, setTick] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError(null);
    getCurrencies(controller.signal)
      .then((list) => {
        setCurrencies(list);
        setLoading(false);
      })
      .catch((err: unknown) => {
        if (controller.signal.aborted) return;
        setError(err instanceof Error ? err.message : "Failed to load currencies");
        setLoading(false);
      });
    return () => controller.abort();
  }, [tick]);

  const refresh = useCallback(() => setTick((n) => n + 1), []);

  return { currencies, loading, error, refresh };
}
