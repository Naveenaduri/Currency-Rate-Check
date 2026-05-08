import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  ApiError,
  getCurrencies,
  getQuote,
  refreshQuote,
} from "./client";

const mockResponse = (
  body: unknown,
  init: { status?: number; ok?: boolean; jsonError?: boolean } = {},
): Response => {
  const status = init.status ?? 200;
  const ok = init.ok ?? (status >= 200 && status < 300);
  return {
    ok,
    status,
    statusText: status === 404 ? "Not Found" : "OK",
    json: init.jsonError
      ? () => Promise.reject(new Error("not json"))
      : () => Promise.resolve(body),
  } as unknown as Response;
};

describe("api client", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    vi.stubGlobal("fetch", fetchMock);
    fetchMock.mockReset();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("getCurrencies hits /api/currencies", async () => {
    fetchMock.mockResolvedValueOnce(mockResponse(["USD", "EUR"]));

    const result = await getCurrencies();

    expect(result).toEqual(["USD", "EUR"]);
    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/api/currencies",
      expect.any(Object),
    );
  });

  it("getQuote hits /api/quotes with from/to/amount", async () => {
    const payload = {
      from: "USD",
      to: "INR",
      sendAmount: 1000,
      bestReceiveAmount: 94300.0,
      baselineName: "Chase",
      lastRefreshAt: "2026-05-08T03:46:25Z",
      source: "Wise V3 Comparisons API",
      providers: [],
    };
    fetchMock.mockResolvedValueOnce(mockResponse(payload));

    const result = await getQuote("USD", "INR", 1000);

    expect(result.bestReceiveAmount).toBe(94300.0);
    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/api/quotes?from=USD&to=INR&amount=1000",
      expect.any(Object),
    );
  });

  it("refreshQuote POSTs to /api/quotes/refresh", async () => {
    fetchMock.mockResolvedValueOnce(
      mockResponse({
        from: "USD",
        to: "INR",
        sendAmount: 1000,
        bestReceiveAmount: 0,
        baselineName: "",
        lastRefreshAt: null,
        source: "Wise",
        providers: [],
      }),
    );

    await refreshQuote("USD", "INR", 1000);

    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/api/quotes/refresh?from=USD&to=INR&amount=1000",
      expect.objectContaining({ method: "POST" }),
    );
  });

  it("getQuote maps 404 to ApiError with backend message", async () => {
    fetchMock.mockResolvedValueOnce(
      mockResponse(
        { error: "Wise has no comparison data for USD -> XYZ", status: 404 },
        { status: 404 },
      ),
    );

    await expect(getQuote("USD", "XYZ", 100)).rejects.toMatchObject({
      name: "ApiError",
      status: 404,
      message: "Wise has no comparison data for USD -> XYZ",
    });
  });

  it("falls back to statusText when error body is not JSON", async () => {
    fetchMock.mockResolvedValueOnce(
      mockResponse(undefined, { status: 404, jsonError: true }),
    );

    const err = await getQuote("USD", "EUR", 100).catch((e: Error) => e);

    expect(err).toBeInstanceOf(ApiError);
    expect((err as ApiError).message).toBe("Not Found");
    expect((err as ApiError).status).toBe(404);
  });
});
