import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { renderHook, waitFor } from "@testing-library/react";
import { useRate } from "./useRate";

const fetchMock = vi.fn();

const okResponse = (body: unknown) =>
  ({
    ok: true,
    status: 200,
    statusText: "OK",
    json: () => Promise.resolve(body),
  }) as unknown as Response;

const errorResponse = (status: number, body: unknown) =>
  ({
    ok: false,
    status,
    statusText: "Not Found",
    json: () => Promise.resolve(body),
  }) as unknown as Response;

const comparison = (best: string, others: string[] = []) => ({
  from: "USD",
  to: "EUR",
  best: { provider: best, rate: 0.93 },
  quotes: [
    { provider: best, rate: 0.93 },
    ...others.map((p) => ({ provider: p, rate: 0.91 })),
  ],
});

beforeEach(() => {
  vi.stubGlobal("fetch", fetchMock);
  fetchMock.mockReset();
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("useRate", () => {
  it("returns null when from or to is empty", () => {
    const { result } = renderHook(() => useRate("", "EUR"));
    expect(result.current.comparison).toBeNull();
    expect(result.current.loading).toBe(false);
  });

  it("returns identity comparison for same currency without fetching", () => {
    const { result } = renderHook(() => useRate("USD", "USD"));

    expect(result.current.comparison?.best?.rate).toBe(1);
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("fetches comparison after debounce and surfaces best provider", async () => {
    fetchMock.mockResolvedValueOnce(okResponse(comparison("Wise", ["Remitly"])));

    const { result } = renderHook(() => useRate("USD", "EUR"));

    expect(result.current.loading).toBe(true);

    await waitFor(
      () => expect(result.current.comparison?.best?.provider).toBe("Wise"),
      { timeout: 1500 },
    );
    expect(result.current.comparison?.quotes).toHaveLength(2);
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("debounces rapid changes - only the latest fires", async () => {
    fetchMock.mockResolvedValue(okResponse(comparison("Remitly")));

    const { result, rerender } = renderHook(
      ({ from, to }: { from: string; to: string }) => useRate(from, to),
      { initialProps: { from: "USD", to: "EUR" } },
    );

    rerender({ from: "USD", to: "GBP" });

    await waitFor(
      () => expect(result.current.comparison?.best?.provider).toBe("Remitly"),
      { timeout: 1500 },
    );
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("maps 404 to friendly error", async () => {
    fetchMock.mockResolvedValueOnce(
      errorResponse(404, { error: "No provider quotes...", status: 404 }),
    );

    const { result } = renderHook(() => useRate("USD", "XYZ"));

    await waitFor(
      () =>
        expect(result.current.error).toBe(
          "No provider quotes a direct rate for this pair",
        ),
      { timeout: 1500 },
    );
    expect(result.current.comparison).toBeNull();
  });

  it("surfaces non-404 errors with original message", async () => {
    fetchMock.mockRejectedValueOnce(new Error("network down"));

    const { result } = renderHook(() => useRate("USD", "EUR"));

    await waitFor(() => expect(result.current.error).toBe("network down"), {
      timeout: 1500,
    });
  });

  it("re-fetches when refreshKey changes", async () => {
    fetchMock.mockResolvedValue(okResponse(comparison("Remitly")));

    const { result, rerender } = renderHook(
      ({ k }: { k: number }) => useRate("USD", "EUR", k),
      { initialProps: { k: 0 } },
    );

    await waitFor(
      () => expect(result.current.comparison?.best?.provider).toBe("Remitly"),
      { timeout: 1500 },
    );
    expect(fetchMock).toHaveBeenCalledTimes(1);

    rerender({ k: 1 });

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2), {
      timeout: 1500,
    });
  });
});
