import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { ProviderList } from "./ProviderList";
import type { ProviderQuote, QuoteResponse } from "../api/client";

const makeProvider = (
  id: string,
  name: string,
  receive: number,
  isBest = false,
  isBaseline = false,
): ProviderQuote => ({
  id,
  name,
  logoUrl: null,
  type: "moneyTransferProvider",
  rate: 80,
  fee: 2,
  markup: 0,
  receiveAmount: receive,
  savingsVsBaseline: isBaseline ? 0 : receive - 80000,
  deliveryDuration: null,
  deliveryDurationType: null,
  dateCollected: null,
  bestDeal: isBest,
  baseline: isBaseline,
});

const buildQuote = (count: number): QuoteResponse => ({
  from: "USD",
  to: "INR",
  sendAmount: 1000,
  bestReceiveAmount: 83000,
  baselineName: "Bank Transfer",
  lastRefreshAt: null,
  source: "Wise V3 Comparisons API",
  providers: Array.from({ length: count }, (_, i) =>
    makeProvider(
      `p${i}`,
      `Provider ${i}`,
      83000 - i * 100,
      i === 0,
      i === count - 1,
    ),
  ),
});

describe("ProviderList", () => {
  it("renders error state", () => {
    render(
      <ProviderList
        quote={null}
        loading={false}
        error="boom"
        fromCurrency="USD"
        toCurrency="INR"
      />,
    );
    expect(screen.getByRole("alert")).toHaveTextContent("boom");
  });

  it("renders loading state when quote is null", () => {
    render(
      <ProviderList
        quote={null}
        loading={true}
        error={null}
        fromCurrency="USD"
        toCurrency="INR"
      />,
    );
    expect(screen.getByRole("status")).toHaveTextContent(/Loading/i);
  });

  it("renders empty state when there are no providers", () => {
    render(
      <ProviderList
        quote={buildQuote(0)}
        loading={false}
        error={null}
        fromCurrency="USD"
        toCurrency="INR"
      />,
    );
    expect(screen.getByRole("status")).toHaveTextContent(/No quotes/i);
  });

  it("renders only the first 4 providers and expands on click", async () => {
    render(
      <ProviderList
        quote={buildQuote(6)}
        loading={false}
        error={null}
        fromCurrency="USD"
        toCurrency="INR"
      />,
    );

    expect(screen.getByText("Provider 0")).toBeInTheDocument();
    expect(screen.getByText("Provider 3")).toBeInTheDocument();
    expect(screen.queryByText("Provider 4")).not.toBeInTheDocument();
    expect(screen.getByText(/View all providers \(6\)/)).toBeInTheDocument();

    await userEvent.click(screen.getByText(/View all providers/));

    expect(screen.getByText("Provider 4")).toBeInTheDocument();
    expect(screen.getByText("Provider 5")).toBeInTheDocument();
  });

  it("invokes onRefresh when the refresh button is clicked", async () => {
    const onRefresh = vi.fn();
    render(
      <ProviderList
        quote={buildQuote(3)}
        loading={false}
        error={null}
        fromCurrency="USD"
        toCurrency="INR"
        onRefresh={onRefresh}
      />,
    );

    await userEvent.click(screen.getByRole("button", { name: /Refresh quotes/i }));

    expect(onRefresh).toHaveBeenCalledTimes(1);
  });

  it("disables the refresh button while refreshing", () => {
    render(
      <ProviderList
        quote={buildQuote(3)}
        loading={false}
        error={null}
        fromCurrency="USD"
        toCurrency="INR"
        onRefresh={() => {}}
        refreshing={true}
      />,
    );

    const button = screen.getByRole("button", { name: /Refresh quotes/i });
    expect(button).toBeDisabled();
    expect(button).toHaveTextContent(/Refreshing/i);
  });
});
