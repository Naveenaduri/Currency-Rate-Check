import { useEffect, useState } from "react";
import { getRates, type RateQuote } from "../api/client";

interface RatesTableProps {
  refreshKey: number;
}

export function RatesTable({ refreshKey }: RatesTableProps) {
  const [rates, setRates] = useState<RateQuote[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError(null);
    getRates(controller.signal)
      .then((list) => {
        setRates(list);
        setLoading(false);
      })
      .catch((err: unknown) => {
        if (controller.signal.aborted) return;
        setError(err instanceof Error ? err.message : "Failed to load rates");
        setLoading(false);
      });
    return () => controller.abort();
  }, [refreshKey]);

  if (loading) return <p role="status">Loading rates...</p>;
  if (error) return <p role="alert">{error}</p>;
  if (rates.length === 0) return <p role="status">No rates yet.</p>;

  return (
    <table className="rates-table" aria-label="Best rates by pair">
      <thead>
        <tr>
          <th scope="col">From</th>
          <th scope="col">To</th>
          <th scope="col">Best rate</th>
          <th scope="col">Provider</th>
        </tr>
      </thead>
      <tbody>
        {rates.map((r) => (
          <tr key={`${r.from}-${r.to}`}>
            <td>{r.from}</td>
            <td>{r.to}</td>
            <td>{r.rate}</td>
            <td>{r.provider}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
