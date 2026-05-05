import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { act, renderHook, waitFor } from "@testing-library/react";
import { useCurrencies } from "./useCurrencies";

const fetchMock = vi.fn();

beforeEach(() => {
  vi.stubGlobal("fetch", fetchMock);
  fetchMock.mockReset();
});

afterEach(() => {
  vi.unstubAllGlobals();
});

const okResponse = (body: unknown) =>
  ({
    ok: true,
    status: 200,
    statusText: "OK",
    json: () => Promise.resolve(body),
  }) as unknown as Response;

describe("useCurrencies", () => {
  it("loads currencies on mount", async () => {
    fetchMock.mockResolvedValueOnce(okResponse(["USD", "EUR"]));

    const { result } = renderHook(() => useCurrencies());

    expect(result.current.loading).toBe(true);

    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.currencies).toEqual(["USD", "EUR"]);
    expect(result.current.error).toBeNull();
  });

  it("captures errors", async () => {
    fetchMock.mockRejectedValueOnce(new Error("boom"));

    const { result } = renderHook(() => useCurrencies());

    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.error).toBe("boom");
  });

  it("refresh re-fetches", async () => {
    fetchMock
      .mockResolvedValueOnce(okResponse(["USD"]))
      .mockResolvedValueOnce(okResponse(["USD", "EUR"]));

    const { result } = renderHook(() => useCurrencies());
    await waitFor(() => expect(result.current.currencies).toEqual(["USD"]));

    act(() => {
      result.current.refresh();
    });

    await waitFor(() =>
      expect(result.current.currencies).toEqual(["USD", "EUR"]),
    );
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });
});
