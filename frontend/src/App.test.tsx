import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { App } from "./App";

const fetchMock = vi.fn();

const okResponse = (body: unknown, status = 200) =>
  ({
    ok: status >= 200 && status < 300,
    status,
    statusText: "OK",
    json: () => Promise.resolve(body),
  }) as unknown as Response;

const buildQuote = () => ({
  from: "USD",
  to: "INR",
  sendAmount: 1000,
  bestReceiveAmount: 94405.0,
  baselineName: "Chase",
  lastRefreshAt: "2026-05-08T03:46:25Z",
  source: "Wise V3 Comparisons API",
  providers: [
    {
      id: "wise",
      name: "Wise",
      logoUrl: "https://logos/wise.svg",
      type: "moneyTransferProvider",
      rate: 94.5,
      fee: 2.0,
      markup: 0,
      receiveAmount: 94405.0,
      savingsVsBaseline: 2455.0,
      deliveryDuration: 1,
      deliveryDurationType: "hours",
      dateCollected: "2026-05-08T03:46:25Z",
      bestDeal: true,
      baseline: false,
    },
    {
      id: "remitly",
      name: "Remitly",
      logoUrl: "https://logos/remitly.svg",
      type: "moneyTransferProvider",
      rate: 94.3,
      fee: 0,
      markup: 0.27,
      receiveAmount: 94300.0,
      savingsVsBaseline: 2350.0,
      deliveryDuration: null,
      deliveryDurationType: null,
      dateCollected: "2026-05-08T03:46:25Z",
      bestDeal: false,
      baseline: false,
    },
    {
      id: "chase",
      name: "Chase",
      logoUrl: "https://logos/chase.svg",
      type: "bank",
      rate: 91.95,
      fee: 10.0,
      markup: 3.2,
      receiveAmount: 91950.0,
      savingsVsBaseline: 0,
      deliveryDuration: null,
      deliveryDurationType: null,
      dateCollected: "2026-05-08T03:46:25Z",
      bestDeal: false,
      baseline: true,
    },
  ],
});

const handlers = async (input: RequestInfo | URL, init?: RequestInit) => {
  const url = String(input);
  if (url.endsWith("/api/currencies")) {
    return okResponse(["USD", "EUR", "GBP", "INR"]);
  }
  if (url.startsWith("http://localhost:8080/api/quotes/refresh")) {
    return okResponse(buildQuote());
  }
  if (url.startsWith("http://localhost:8080/api/quotes")) {
    return okResponse(buildQuote());
  }
  return okResponse(undefined, 404);
};

beforeEach(() => {
  vi.stubGlobal("fetch", fetchMock);
  fetchMock.mockReset();
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("App", () => {
  it("renders the converter and provider cards including best deal", async () => {
    fetchMock.mockImplementation(handlers);

    render(<App />);

    await waitFor(() =>
      expect(screen.getByLabelText("Send amount")).toBeInTheDocument(),
    );

    expect(await screen.findByText("Wise")).toBeInTheDocument();
    expect(await screen.findByText("Best Deal")).toBeInTheDocument();
    expect(screen.getByText("Remitly")).toBeInTheDocument();
    expect(screen.getByText("Chase")).toBeInTheDocument();
    expect(screen.getByText(/3 Providers compared/i)).toBeInTheDocument();
    expect(screen.getByText(/Standard Rate/i)).toBeInTheDocument();
  });

  it("computes the receive total from the top provider quote", async () => {
    fetchMock.mockImplementation(handlers);

    render(<App />);

    const output = await screen.findByLabelText("Estimated receive amount");
    await waitFor(() =>
      expect(output.textContent ?? "").toMatch(/94,405\.00/),
    );
  });

  it("clicking refresh calls /api/quotes/refresh and re-fetches", async () => {
    fetchMock.mockImplementation(handlers);

    render(<App />);

    await screen.findByText("Wise");
    fetchMock.mockClear();
    fetchMock.mockImplementation(handlers);

    await userEvent.click(
      screen.getByRole("button", { name: /Refresh quotes/i }),
    );

    await waitFor(() => {
      const calls = fetchMock.mock.calls.map((c: unknown[]) => String(c[0]));
      expect(calls.some((u) => u.includes("/api/quotes/refresh"))).toBe(true);
    });
  });
});
