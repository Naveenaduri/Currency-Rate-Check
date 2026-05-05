import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { ProviderBar } from "./ProviderBar";

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

describe("ProviderBar", () => {
  it("renders providers from the API and a refresh button", async () => {
    fetchMock.mockResolvedValueOnce(
      okResponse(["Remitly", "Wise", "Western Union"]),
    );
    const onRefresh = vi.fn();

    render(
      <ProviderBar refreshKey={0} onRefresh={onRefresh} refreshing={false} />,
    );

    await waitFor(() => expect(screen.getByText("Remitly")).toBeInTheDocument());
    expect(screen.getByText("Wise")).toBeInTheDocument();
    expect(screen.getByText("Western Union")).toBeInTheDocument();

    await userEvent.click(
      screen.getByRole("button", { name: /Refresh quotes/i }),
    );
    expect(onRefresh).toHaveBeenCalled();
  });

  it("disables the button while refreshing", async () => {
    fetchMock.mockResolvedValueOnce(okResponse(["Remitly"]));

    render(
      <ProviderBar refreshKey={0} onRefresh={() => undefined} refreshing={true} />,
    );

    await waitFor(() => expect(screen.getByText("Remitly")).toBeInTheDocument());
    const button = screen.getByRole("button");
    expect(button).toBeDisabled();
    expect(button).toHaveTextContent(/Refreshing/);
  });

  it("shows an alert when the providers fetch fails", async () => {
    fetchMock.mockRejectedValueOnce(new Error("boom"));

    render(
      <ProviderBar
        refreshKey={0}
        onRefresh={() => undefined}
        refreshing={false}
      />,
    );

    await waitFor(() =>
      expect(screen.getByRole("alert")).toHaveTextContent("boom"),
    );
  });
});
