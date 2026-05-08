import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { renderHook, waitFor } from "@testing-library/react";
import { useQuote } from "./useQuote";

const fetchMock = vi.fn();

const ok = (body: unknown, status = 200) =>
  ({
    ok: status >= 200 && status < 300,
    status,
    statusText: status === 404 ? "Not Found" : "OK",
    json: () => Promise.resolve(body),
  }) as unknown as Response;

const sampleQuote = {
  from: "USD",
  to: "INR",
  sendAmount: 1000,
  midMarketRate: 83.35,
  bestReceiveAmount: 83120.45,
  baselineName: "Bank Transfer",
  lastRefreshAt: null,
  providers: [],
};

beforeEach(() => {
  vi.stubGlobal("fetch", fetchMock);
  fetchMock.mockReset();
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("useQuote", () => {
  it("returns null state when args are missing or zero", () => {
    const { result } = renderHook(() => useQuote(null, "INR", 1000));
    expect(result.current.quote).toBeNull();
    expect(result.current.loading).toBe(false);

    const { result: zero } = renderHook(() => useQuote("USD", "INR", 0));
    expect(zero.current.quote).toBeNull();
  });

  it("returns identity quote when from === to", async () => {
    const { result } = renderHook(() => useQuote("USD", "USD", 100));
    await waitFor(() =>
      expect(result.current.quote?.bestReceiveAmount).toBe(100),
    );
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("fetches /api/quotes after debounce and stores the result", async () => {
    fetchMock.mockResolvedValueOnce(ok(sampleQuote));

    const { result } = renderHook(() => useQuote("USD", "INR", 1000));

    await waitFor(
      () => expect(result.current.quote).not.toBeNull(),
      { timeout: 2000 },
    );
    expect(result.current.quote?.from).toBe("USD");
    expect(result.current.error).toBeNull();
  });

  it("maps 404 to a friendly message", async () => {
    fetchMock.mockResolvedValueOnce(
      ok({ error: "missing", status: 404 }, 404),
    );

    const { result } = renderHook(() => useQuote("USD", "XYZ", 100));

    await waitFor(
      () => expect(result.current.error).not.toBeNull(),
      { timeout: 2000 },
    );
    expect(result.current.error).toMatch(/No provider/);
  });

  it("propagates non-404 errors verbatim", async () => {
    fetchMock.mockResolvedValueOnce(ok({ error: "boom" }, 500));

    const { result } = renderHook(() => useQuote("USD", "INR", 100));
    await waitFor(
      () => expect(result.current.error).toBe("boom"),
      { timeout: 2000 },
    );
  });
});
