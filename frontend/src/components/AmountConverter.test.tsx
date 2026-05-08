import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { AmountConverter } from "./AmountConverter";

const baseProps = {
  sendAmount: "1000",
  onSendAmountChange: () => {},
  receiveAmount: 83120.45,
  loading: false,
  fromCurrency: "USD",
  toCurrency: "INR",
  currencies: ["USD", "INR", "EUR"],
  onFromChange: () => {},
  onToChange: () => {},
  onSwap: () => {},
};

describe("AmountConverter", () => {
  it("shows the formatted receive amount", () => {
    render(<AmountConverter {...baseProps} />);
    expect(
      screen.getByLabelText("Estimated receive amount"),
    ).toHaveTextContent(/83,120\.45/);
  });

  it("renders an ellipsis while loading", () => {
    render(<AmountConverter {...baseProps} loading={true} receiveAmount={null} />);
    expect(
      screen.getByLabelText("Estimated receive amount"),
    ).toHaveTextContent("…");
  });

  it("renders an em-dash when receive is null and not loading", () => {
    render(<AmountConverter {...baseProps} receiveAmount={null} />);
    expect(
      screen.getByLabelText("Estimated receive amount"),
    ).toHaveTextContent("—");
  });

  it("forwards send amount edits to onSendAmountChange", async () => {
    const onSendAmountChange = vi.fn();
    render(
      <AmountConverter {...baseProps} onSendAmountChange={onSendAmountChange} />,
    );
    await userEvent.clear(screen.getByLabelText("Send amount"));
    await userEvent.type(screen.getByLabelText("Send amount"), "5");
    expect(onSendAmountChange).toHaveBeenCalled();
  });

  it("invokes onSwap when the swap button is clicked", async () => {
    const onSwap = vi.fn();
    render(<AmountConverter {...baseProps} onSwap={onSwap} />);
    await userEvent.click(screen.getByLabelText("Swap currencies"));
    expect(onSwap).toHaveBeenCalled();
  });
});
