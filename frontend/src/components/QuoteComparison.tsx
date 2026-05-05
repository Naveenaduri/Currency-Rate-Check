import type { RateComparison } from "../api/client";

interface QuoteComparisonProps {
  comparison: RateComparison | null;
}

export function QuoteComparison({ comparison }: QuoteComparisonProps) {
  if (!comparison || comparison.quotes.length === 0) {
    return null;
  }
  const bestProvider = comparison.best?.provider;
  return (
    <table className="quote-comparison" aria-label="Provider quote comparison">
      <thead>
        <tr>
          <th scope="col">Provider</th>
          <th scope="col">Rate</th>
          <th scope="col">vs best</th>
        </tr>
      </thead>
      <tbody>
        {comparison.quotes.map((q) => {
          const best = comparison.best?.rate ?? q.rate;
          const delta = ((q.rate - best) / best) * 100;
          const isBest = q.provider === bestProvider;
          return (
            <tr
              key={q.provider}
              className={isBest ? "row-best" : undefined}
              data-best={isBest ? "true" : "false"}
            >
              <td>
                {q.provider}
                {isBest && (
                  <span className="best-pill" aria-label="best rate">
                    BEST
                  </span>
                )}
              </td>
              <td>{q.rate}</td>
              <td>{isBest ? "—" : `${delta.toFixed(3)}%`}</td>
            </tr>
          );
        })}
      </tbody>
    </table>
  );
}
