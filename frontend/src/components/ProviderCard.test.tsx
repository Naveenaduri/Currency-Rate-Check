import { describe, expect, it } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { ProviderCard } from "./ProviderCard";
import type { ProviderQuote } from "../api/client";

const baseQuote: ProviderQuote = {
  id: "wise",
  name: "Wise",
  logoUrl: "https://logos/wise.svg",
  type: "moneyTransferProvider",
  rate: 94.5,
  fee: 2.0,
  markup: 0,
  receiveAmount: 94405,
  savingsVsBaseline: 2455,
  deliveryDuration: 1,
  deliveryDurationType: "hours",
  dateCollected: "2026-05-08T03:46:25Z",
  bestDeal: true,
  baseline: false,
};

describe("ProviderCard", () => {
  it("renders provider name, logo, type, and savings", () => {
    render(
      <ProviderCard
        quote={baseQuote}
        fromCurrency="USD"
        toCurrency="INR"
        baselineName="Chase"
      />,
    );
    expect(screen.getByText("Wise")).toBeInTheDocument();
    expect(screen.getByText("Best Deal")).toBeInTheDocument();
    expect(screen.getByText("Money transfer")).toBeInTheDocument();
    expect(screen.getByText("1 hours")).toBeInTheDocument();
    expect(screen.getByText(/You save/)).toBeInTheDocument();
    expect(screen.getByText(/vs Chase/)).toBeInTheDocument();
    expect(screen.getByText(/1 USD = 94\.5000 INR/)).toBeInTheDocument();
  });

  it("falls back to the initial when the logo image fails to load", () => {
    render(
      <ProviderCard
        quote={baseQuote}
        fromCurrency="USD"
        toCurrency="INR"
        baselineName="Chase"
      />,
    );
    const img = document.querySelector("img");
    if (img) fireEvent.error(img);
    expect(screen.getByText("W")).toBeInTheDocument();
  });

  it("shows Standard Rate for the baseline provider", () => {
    render(
      <ProviderCard
        quote={{
          ...baseQuote,
          id: "chase",
          name: "Chase",
          type: "bank",
          fee: 10,
          bestDeal: false,
          baseline: true,
          savingsVsBaseline: 0,
        }}
        fromCurrency="USD"
        toCurrency="INR"
        baselineName="Chase"
      />,
    );
    expect(screen.getByText("Standard Rate")).toBeInTheDocument();
    expect(screen.queryByText("Best Deal")).not.toBeInTheDocument();
    expect(screen.getByText("Bank")).toBeInTheDocument();
    expect(screen.getByText("Higher fees")).toBeInTheDocument();
  });

  it("falls back to em-dash when savings are not positive and not baseline", () => {
    render(
      <ProviderCard
        quote={{
          ...baseQuote,
          bestDeal: false,
          savingsVsBaseline: -50,
          fee: 8,
          deliveryDuration: null,
          deliveryDurationType: null,
        }}
        fromCurrency="USD"
        toCurrency="INR"
        baselineName="Chase"
      />,
    );
    expect(screen.getByText("—")).toBeInTheDocument();
    expect(screen.getByText("Higher fees")).toBeInTheDocument();
  });

  it("renders an initial when there is no logo URL at all", () => {
    render(
      <ProviderCard
        quote={{ ...baseQuote, logoUrl: null }}
        fromCurrency="USD"
        toCurrency="INR"
        baselineName="Chase"
      />,
    );
    expect(screen.getByText("W")).toBeInTheDocument();
  });

  it("buckets fee labels by amount", () => {
    const { rerender } = render(
      <ProviderCard
        quote={{ ...baseQuote, fee: 0.5, bestDeal: false }}
        fromCurrency="USD"
        toCurrency="INR"
        baselineName="Chase"
      />,
    );
    expect(screen.getByText("Low fees")).toBeInTheDocument();

    rerender(
      <ProviderCard
        quote={{ ...baseQuote, fee: 4, bestDeal: false }}
        fromCurrency="USD"
        toCurrency="INR"
        baselineName="Chase"
      />,
    );
    expect(screen.getByText("Moderate fees")).toBeInTheDocument();
  });
});
