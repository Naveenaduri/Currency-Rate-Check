import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { QuoteComparison } from "./QuoteComparison";
import type { RateComparison } from "../api/client";

const comparison: RateComparison = {
  from: "USD",
  to: "EUR",
  best: { provider: "Wise", rate: 0.93 },
  quotes: [
    { provider: "Wise", rate: 0.93 },
    { provider: "Remitly", rate: 0.92 },
    { provider: "Western Union", rate: 0.90 },
  ],
};

describe("QuoteComparison", () => {
  it("renders nothing when there is no comparison", () => {
    const { container } = render(<QuoteComparison comparison={null} />);
    expect(container.firstChild).toBeNull();
  });

  it("renders nothing when there are no quotes", () => {
    const { container } = render(
      <QuoteComparison
        comparison={{ from: "USD", to: "EUR", best: null, quotes: [] }}
      />,
    );
    expect(container.firstChild).toBeNull();
  });

  it("highlights the best provider and shows percentage delta for the rest", () => {
    render(<QuoteComparison comparison={comparison} />);

    const rows = screen.getAllByRole("row");
    expect(rows).toHaveLength(4);

    const wiseRow = rows[1];
    expect(wiseRow).toHaveAttribute("data-best", "true");
    expect(wiseRow).toHaveTextContent("BEST");
    expect(wiseRow).toHaveTextContent("—");

    const remitlyRow = rows[2];
    expect(remitlyRow).toHaveAttribute("data-best", "false");
    expect(remitlyRow.textContent).toMatch(/-1\.0[0-9]+%/);
  });
});
