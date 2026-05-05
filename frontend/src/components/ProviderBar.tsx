import { useEffect, useState } from "react";
import { getProviders } from "../api/client";

interface ProviderBarProps {
  refreshKey: number;
  onRefresh: () => void;
  refreshing: boolean;
}

export function ProviderBar({ refreshKey, onRefresh, refreshing }: ProviderBarProps) {
  const [providers, setProviders] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    setError(null);
    getProviders(controller.signal)
      .then(setProviders)
      .catch((err: unknown) => {
        if (controller.signal.aborted) return;
        setError(err instanceof Error ? err.message : "Failed to load providers");
      });
    return () => controller.abort();
  }, [refreshKey]);

  return (
    <section className="provider-bar" aria-label="Remittance providers">
      <div>
        <h2>Providers</h2>
        {error ? (
          <p role="alert">{error}</p>
        ) : providers.length === 0 ? (
          <p role="status">Loading providers...</p>
        ) : (
          <ul>
            {providers.map((p) => (
              <li key={p}>{p}</li>
            ))}
          </ul>
        )}
      </div>
      <button
        type="button"
        onClick={onRefresh}
        disabled={refreshing}
        aria-label="Refresh quotes from providers"
      >
        {refreshing ? "Refreshing..." : "Refresh quotes"}
      </button>
    </section>
  );
}
