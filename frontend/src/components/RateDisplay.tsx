import type { RateComparison } from "../api/client";

interface RateDisplayProps {
  from: string;
  to: string;
  comparison: RateComparison | null;
  loading: boolean;
  error: string | null;
}

const formatRate = (rate: number) => {
  if (rate >= 100) return rate.toFixed(2);
  if (rate >= 1) return rate.toFixed(4);
  return rate.toFixed(6);
};

export function RateDisplay({
  from,
  to,
  comparison,
  loading,
  error,
}: RateDisplayProps) {
  if (!from || !to) {
    return <p role="status">Pick a from and to currency to see the rate.</p>;
  }
  if (loading) {
    return (
      <p role="status">
        Loading rate for {from} {"->"} {to}...
      </p>
    );
  }
  if (error) {
    return (
      <p role="alert" className="rate-error">
        {error}
      </p>
    );
  }
  if (!comparison || !comparison.best) {
    return null;
  }
  return (
    <div className="rate-display" role="status">
      <p className="rate-line">
        <strong>1 {comparison.from}</strong> ={" "}
        <strong>
          {formatRate(comparison.best.rate)} {comparison.to}
        </strong>
      </p>
      <p className="rate-provider">
        Best via <strong>{comparison.best.provider}</strong>
      </p>
    </div>
  );
}
