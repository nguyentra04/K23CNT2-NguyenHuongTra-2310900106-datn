import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { orderApi } from "@/api/orderApi";
import type { Order, OrderStatus } from "@/types/order";
import { statusLabel } from "@/types/order";
import { formatVND } from "@/utils/format";
import Loading from "@/components/common/Loading";
import ErrorNotice from "@/components/common/ErrorNotice";

const STEPS: OrderStatus[] = ["PENDING", "COOKING", "READY", "SERVED"];

export default function TrackOrderPage() {
  const { id } = useParams();
  const [order, setOrder] = useState<Order | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!id) return;
    const load = () =>
      orderApi
        .get(Number(id))
        .then((o) => {
          setOrder(o);
          setError(null);
        })
        .catch(() => setError("Không tìm thấy đơn hàng."));
    load();
    const t = setInterval(load, 5000);
    return () => clearInterval(t);
  }, [id]);

  if (error) return <ErrorNotice message={error} />;
  if (!order) return <Loading />;

  const current = STEPS.indexOf(order.status);

  return (
    <div className="mx-auto max-w-xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Đơn #{order.id} · Bàn {order.tableNo}</h1>
        <p className="text-muted">{statusLabel[order.status]}</p>
      </div>

      <ol className="grid grid-cols-4 gap-2">
        {STEPS.map((s, i) => (
          <li
            key={s}
            className={`rounded-lg p-2 text-center text-xs font-medium ${
              i <= current ? "bg-brand-600 text-white" : "bg-white text-muted ring-1 ring-line"
            }`}
          >
            {statusLabel[s]}
          </li>
        ))}
      </ol>

      <ul className="divide-y divide-line rounded-xl border border-line bg-white">
        {order.items.map((it) => (
          <li key={it.dishId} className="flex justify-between p-3 text-sm">
            <span>{it.quantity} × {it.dishName}</span>
            <span>{formatVND(it.quantity * it.unitPrice)}</span>
          </li>
        ))}
        <li className="flex justify-between p-3 font-semibold">
          <span>Tổng cộng</span>
          <span>{formatVND(order.total)}</span>
        </li>
      </ul>

      <Link to="/menu" className="inline-block text-sm font-medium text-brand-700 hover:underline">
        Gọi thêm món
      </Link>
    </div>
  );
}
