import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  ApiError,
  getBestRate,
  getCurrencies,
  getProviders,
  getQuotes,
  getRates,
  refreshRates,
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

  it("getProviders hits /api/providers", async () => {
    fetchMock.mockResolvedValueOnce(mockResponse(["Remitly", "Wise"]));

    const result = await getProviders();

    expect(result).toEqual(["Remitly", "Wise"]);
    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/api/providers",
      expect.any(Object),
    );
  });

  it("getRates returns RateQuote[]", async () => {
    fetchMock.mockResolvedValueOnce(
      mockResponse([{ from: "USD", to: "EUR", rate: 0.92, provider: "Remitly" }]),
    );

    const result = await getRates();

    expect(result).toHaveLength(1);
    expect(result[0].provider).toBe("Remitly");
  });

  it("getBestRate returns the best quote", async () => {
    fetchMock.mockResolvedValueOnce(
      mockResponse({ from: "USD", to: "EUR", rate: 0.93, provider: "Wise" }),
    );

    const rate = await getBestRate("USD", "EUR");

    expect(rate.provider).toBe("Wise");
    expect(rate.rate).toBe(0.93);
    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/api/rates/USD/EUR",
      expect.any(Object),
    );
  });

  it("getQuotes returns the comparison payload", async () => {
    fetchMock.mockResolvedValueOnce(
      mockResponse({
        from: "USD",
        to: "EUR",
        best: { provider: "Wise", rate: 0.93 },
        quotes: [
          { provider: "Wise", rate: 0.93 },
          { provider: "Remitly", rate: 0.92 },
        ],
      }),
    );

    const comparison = await getQuotes("USD", "EUR");

    expect(comparison.best?.provider).toBe("Wise");
    expect(comparison.quotes).toHaveLength(2);
    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/api/rates/USD/EUR/quotes",
      expect.any(Object),
    );
  });

  it("refreshRates POSTs to /api/rates/refresh", async () => {
    fetchMock.mockResolvedValueOnce(mockResponse([]));

    await refreshRates();

    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/api/rates/refresh",
      expect.objectContaining({ method: "POST" }),
    );
  });

  it("getBestRate maps 404 to ApiError with backend message", async () => {
    fetchMock.mockResolvedValueOnce(
      mockResponse(
        { error: "No provider quotes a direct rate for USD -> XYZ", status: 404 },
        { status: 404 },
      ),
    );

    await expect(getBestRate("USD", "XYZ")).rejects.toMatchObject({
      name: "ApiError",
      status: 404,
      message: "No provider quotes a direct rate for USD -> XYZ",
    });
  });

  it("falls back to statusText when error body is not JSON", async () => {
    fetchMock.mockResolvedValueOnce(
      mockResponse(undefined, { status: 404, jsonError: true }),
    );

    const err = await getBestRate("USD", "EUR").catch((e) => e);

    expect(err).toBeInstanceOf(ApiError);
    expect(err.message).toBe("Not Found");
    expect(err.status).toBe(404);
  });
});
