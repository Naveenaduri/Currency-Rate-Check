import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { RateDisplay } from "./RateDisplay";
import type { RateComparison } from "../api/client";

const comparison: RateComparison = {
  from: "USD",
  to: "EUR",
  best: { provider: "Wise", rate: 0.93 },
  quotes: [
    { provider: "Wise", rate: 0.93 },
    { provider: "Remitly", rate: 0.92 },
  ],
};

describe("RateDisplay", () => {
  it("prompts when no currencies are selected", () => {
    render(
      <RateDisplay
        from=""
        to=""
        comparison={null}
        loading={false}
        error={null}
      />,
    );
    expect(screen.getByText(/Pick a from and to currency/i)).toBeInTheDocument();
  });

  it("shows loading state", () => {
    render(
      <RateDisplay
        from="USD"
        to="EUR"
        comparison={null}
        loading={true}
        error={null}
      />,
    );
    expect(screen.getByText(/Loading rate/i)).toBeInTheDocument();
  });

  it("shows error state", () => {
    render(
      <RateDisplay
        from="USD"
        to="EUR"
        comparison={null}
        loading={false}
        error="No provider quotes a direct rate for this pair"
      />,
    );
    expect(screen.getByRole("alert")).toHaveTextContent(/No provider/i);
  });

  it("renders best rate and provider", () => {
    render(
      <RateDisplay
        from="USD"
        to="EUR"
        comparison={comparison}
        loading={false}
        error={null}
      />,
    );
    expect(screen.getByText(/1 USD/)).toBeInTheDocument();
    expect(screen.getByText(/EUR/)).toBeInTheDocument();
    expect(screen.getByText(/Wise/)).toBeInTheDocument();
  });

  it("formats rates >= 100 with two decimals", () => {
    const yenComparison: RateComparison = {
      from: "USD",
      to: "JPY",
      best: { provider: "Remitly", rate: 156.123456 },
      quotes: [{ provider: "Remitly", rate: 156.123456 }],
    };
    render(
      <RateDisplay
        from="USD"
        to="JPY"
        comparison={yenComparison}
        loading={false}
        error={null}
      />,
    );
    expect(screen.getByText(/156\.12 JPY/)).toBeInTheDocument();
  });

  it("renders nothing when no comparison, no loading, no error", () => {
    const { container } = render(
      <RateDisplay
        from="USD"
        to="EUR"
        comparison={null}
        loading={false}
        error={null}
      />,
    );
    expect(container.firstChild).toBeNull();
  });
});
