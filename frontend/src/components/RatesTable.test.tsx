import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { RatesTable } from "./RatesTable";

const fetchMock = vi.fn();

const okResponse = (body: unknown) =>
  ({
    ok: true,
    status: 200,
    statusText: "OK",
    json: () => Promise.resolve(body),
  }) as unknown as Response;

beforeEach(() => {
  vi.stubGlobal("fetch", fetchMock);
  fetchMock.mockReset();
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("RatesTable", () => {
  it("renders rates with provider column", async () => {
    fetchMock.mockResolvedValueOnce(
      okResponse([
        { from: "USD", to: "EUR", rate: 0.93, provider: "Wise" },
        { from: "USD", to: "GBP", rate: 0.79, provider: "Remitly" },
      ]),
    );

    render(<RatesTable refreshKey={0} />);

    await waitFor(() => expect(screen.getByRole("table")).toBeInTheDocument());
    expect(screen.getAllByRole("row")).toHaveLength(3);
    expect(screen.getByText("Wise")).toBeInTheDocument();
    expect(screen.getByText("Remitly")).toBeInTheDocument();
  });

  it("shows empty state when no rates", async () => {
    fetchMock.mockResolvedValueOnce(okResponse([]));

    render(<RatesTable refreshKey={0} />);

    await waitFor(() =>
      expect(screen.getByText("No rates yet.")).toBeInTheDocument(),
    );
  });

  it("shows error message on failure", async () => {
    fetchMock.mockRejectedValueOnce(new Error("nope"));

    render(<RatesTable refreshKey={0} />);

    await waitFor(() =>
      expect(screen.getByRole("alert")).toHaveTextContent("nope"),
    );
  });
});
