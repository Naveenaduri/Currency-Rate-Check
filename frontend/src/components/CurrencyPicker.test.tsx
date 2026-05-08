import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { CurrencyPicker } from "./CurrencyPicker";

describe("CurrencyPicker", () => {
  it("renders flag and code", () => {
    render(
      <CurrencyPicker
        id="from"
        label="From"
        value="USD"
        options={["USD", "INR"]}
        onChange={() => {}}
      />,
    );
    expect(screen.getByText("USD")).toBeInTheDocument();
    expect(screen.getByText("US Dollar")).toBeInTheDocument();
  });

  it("falls back gracefully for unknown codes", () => {
    render(
      <CurrencyPicker
        id="from"
        label="From"
        value="ZZZ"
        options={["ZZZ"]}
        onChange={() => {}}
      />,
    );
    expect(screen.getAllByText("ZZZ").length).toBeGreaterThan(0);
  });

  it("emits onChange when the underlying select changes", async () => {
    const onChange = vi.fn();
    render(
      <CurrencyPicker
        id="from"
        label="From"
        value="USD"
        options={["USD", "INR"]}
        onChange={onChange}
      />,
    );

    await userEvent.selectOptions(screen.getByLabelText("From"), "INR");

    expect(onChange).toHaveBeenCalledWith("INR");
  });

  it("renders a placeholder when no options are available", () => {
    render(
      <CurrencyPicker
        id="from"
        label="From"
        value=""
        options={[]}
        onChange={() => {}}
      />,
    );
    const select = screen.getByLabelText("From") as HTMLSelectElement;
    expect(select.options).toHaveLength(1);
  });
});
