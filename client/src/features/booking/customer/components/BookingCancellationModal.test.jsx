import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import BookingCancellationModal from "./BookingCancellationModal";

describe("BookingCancellationModal", () => {
  it("explains the consequences and confirms without requesting a reason", () => {
    const onConfirm = vi.fn();
    render(
      <BookingCancellationModal
        bookingCode="BK-001"
        seatLabels="H10, H11"
        onClose={vi.fn()}
        onConfirm={onConfirm}
      />,
    );

    expect(
      screen.getByRole("dialog", { name: "Hủy đơn đang giữ?" }),
    ).toHaveAttribute("aria-modal", "true");
    expect(
      screen.getByText(/ghế H10, H11 sẽ được trả lại ngay/i),
    ).toBeInTheDocument();
    expect(screen.queryByRole("textbox")).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Xác nhận hủy" }));

    expect(onConfirm).toHaveBeenCalledWith();
  });

  it("renders API errors and locks dismissal while processing", () => {
    const onClose = vi.fn();
    render(
      <BookingCancellationModal
        error="Đơn đã hết hạn"
        pending
        onClose={onClose}
        onConfirm={vi.fn()}
      />,
    );

    expect(screen.getByRole("alert")).toHaveTextContent("Đơn đã hết hạn");
    expect(screen.getByRole("button", { name: "Đóng" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Đang hủy..." })).toBeDisabled();
    fireEvent.keyDown(document, { key: "Escape" });
    expect(onClose).not.toHaveBeenCalled();
  });
});
