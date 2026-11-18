import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useCart } from "@/context/CartContext";
import { orderApi } from "@/api/orderApi";
import { formatVND } from "@/utils/format";
import Button from "@/components/common/Button";

export default function CartPage() {
  const { items, total, setQuantity, setNote, clear } = useCart();
  const navigate = useNavigate();
  const [tableNo, setTableNo] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (items.length === 0) {
    return (
      <div className="py-16 text-center">
        <p className="mb-4 text-muted">Giỏ hàng đang trống.</p>
        <Link to="/menu" className="font-medium text-brand-700 hover:underline">Chọn món từ thực đơn</Link>
      </div>
    );
  }

  const submit = async () => {
    if (!tableNo.trim()) {
      setError("Vui lòng nhập số bàn.");
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      const order = await orderApi.create({
        tableNo: tableNo.trim(),
        items: items.map((i) => ({ dishId: i.dish.id, quantity: i.quantity, note: i.note })),
      });
      clear();
      navigate(`/orders/${order.id}`);
    } catch {
      setError("Không gửi được đơn. Vui lòng thử lại.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="grid gap-8 lg:grid-cols-[1fr_20rem]">
      <section className="space-y-3">
        <h1 className="text-2xl font-bold">Giỏ hàng</h1>
        {items.map(({ dish, quantity, note }) => (
          <div key={dish.id} className="rounded-xl border border-line bg-white p-4">
            <div className="flex items-center justify-between gap-4">
              <div>
                <h3 className="font-semibold">{dish.name}</h3>
                <p className="text-sm text-muted">{formatVND(dish.price)}</p>
              </div>
              <div className="flex items-center gap-2">
                <Button variant="secondary" aria-label="Giảm" onClick={() => setQuantity(dish.id, quantity - 1)}>−</Button>
                <span className="w-6 text-center">{quantity}</span>
                <Button variant="secondary" aria-label="Tăng" onClick={() => setQuantity(dish.id, quantity + 1)}>+</Button>
              </div>
            </div>
            <input
              value={note ?? ""}
              onChange={(e) => setNote(dish.id, e.target.value)}
              placeholder="Ghi chú (ít cay, không hành...)"
              className="mt-3 w-full rounded-lg border border-line px-3 py-2 text-sm"
            />
          </div>
        ))}
      </section>

      <aside className="h-fit space-y-4 rounded-xl border border-line bg-white p-4">
        <label className="block text-sm">
          Số bàn
          <input
            value={tableNo}
            onChange={(e) => setTableNo(e.target.value)}
            className="mt-1 w-full rounded-lg border border-line px-3 py-2"
          />
        </label>
        <p className="flex justify-between text-lg font-semibold">
          <span>Tổng</span>
          <span>{formatVND(total)}</span>
        </p>
        {error && <p className="text-sm text-red-700">{error}</p>}
        <Button className="w-full" onClick={submit} disabled={submitting}>
          {submitting ? "Đang gửi..." : "Gửi đơn cho bếp"}
        </Button>
      </aside>
    </div>
  );
}
