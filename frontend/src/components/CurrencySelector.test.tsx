import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { CurrencySelector } from "./CurrencySelector";

describe("CurrencySelector", () => {
  it("renders options and reports changes", async () => {
    const onChange = vi.fn();
    render(
      <CurrencySelector
        id="from"
        label="From"
        value="USD"
        options={["USD", "EUR", "GBP"]}
        onChange={onChange}
      />,
    );

    const select = screen.getByLabelText("From") as HTMLSelectElement;
    expect(select.value).toBe("USD");
    expect(screen.getAllByRole("option")).toHaveLength(3);

    await userEvent.selectOptions(select, "EUR");
    expect(onChange).toHaveBeenCalledWith("EUR");
  });

  it("shows placeholder when no options", () => {
    render(
      <CurrencySelector
        id="empty"
        label="Empty"
        value=""
        options={[]}
        onChange={() => undefined}
      />,
    );

    expect(screen.getByText("No currencies")).toBeInTheDocument();
  });
});
