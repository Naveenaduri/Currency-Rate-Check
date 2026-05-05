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

const handlers = (rateBest: { provider: string; rate: number }) =>
  async (input: RequestInfo | URL) => {
    const url = String(input);
    if (url.endsWith("/api/currencies")) {
      return okResponse(["USD", "EUR", "GBP"]);
    }
    if (url.endsWith("/api/providers")) {
      return okResponse(["Remitly", "Wise", "Western Union"]);
    }
    if (url.endsWith("/api/rates")) {
      return okResponse([
        { from: "USD", to: "EUR", rate: rateBest.rate, provider: rateBest.provider },
      ]);
    }
    if (url.endsWith("/api/rates/refresh")) {
      return okResponse([]);
    }
    if (url.endsWith("/quotes")) {
      return okResponse({
        from: "USD",
        to: "EUR",
        best: rateBest,
        quotes: [rateBest, { provider: "Remitly", rate: 0.91 }],
      });
    }
    if (url.includes("/api/rates/")) {
      return okResponse({ from: "USD", to: "EUR", ...rateBest });
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
  it("renders providers, converter, comparison and rates table", async () => {
    fetchMock.mockImplementation(handlers({ provider: "Wise", rate: 0.93 }));

    render(<App />);

    await waitFor(() =>
      expect(screen.getByLabelText("From")).toBeInTheDocument(),
    );
    await waitFor(() =>
      expect(screen.getAllByText("Wise").length).toBeGreaterThan(0),
    );
    await waitFor(() =>
      expect(screen.getByLabelText("Best rates by pair")).toBeInTheDocument(),
    );
    expect(
      await screen.findByLabelText("Provider quote comparison"),
    ).toBeInTheDocument();
    expect(screen.getByText(/Best via/)).toBeInTheDocument();
  });

  it("clicking refresh calls /api/rates/refresh and re-fetches", async () => {
    fetchMock.mockImplementation(handlers({ provider: "Remitly", rate: 0.92 }));

    render(<App />);

    await waitFor(() =>
      expect(screen.getByLabelText("From")).toBeInTheDocument(),
    );

    fetchMock.mockClear();
    fetchMock.mockImplementation(handlers({ provider: "Wise", rate: 0.94 }));

    await userEvent.click(
      screen.getByRole("button", { name: /Refresh quotes/i }),
    );

    await waitFor(() => {
      const calls = fetchMock.mock.calls.map((c: unknown[]) => String(c[0]));
      expect(calls).toEqual(
        expect.arrayContaining([
          "http://localhost:8080/api/rates/refresh",
        ]),
      );
    });
  });
});
