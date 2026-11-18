import type { ReactNode } from "react";
import type { Order } from "@/types/order";
import { formatTime, formatVND } from "@/utils/format";
import StatusBadge from "./common/StatusBadge";

interface Props {
  order: Order;
  showTotal?: boolean;
  actions?: ReactNode;
}

export default function OrderCard({ order, showTotal = false, actions }: Props) {
  return (
    <article className="rounded-xl border border-line bg-white p-4">
      <header className="mb-3 flex items-start justify-between gap-2">
        <div>
          <h3 className="font-semibold">Bàn {order.tableNo}</h3>
          <p className="text-xs text-muted">
            Đơn #{order.id} · {formatTime(order.createdAt)}
          </p>
        </div>
        <StatusBadge status={order.status} />
      </header>

      <ul className="space-y-1 text-sm">
        {order.items.map((it) => (
          <li key={it.dishId} className="flex justify-between gap-2">
            <span>
              {it.quantity} × {it.dishName}
              {it.note && <em className="block text-xs text-muted">Ghi chú: {it.note}</em>}
            </span>
            {showTotal && <span className="text-muted">{formatVND(it.quantity * it.unitPrice)}</span>}
          </li>
        ))}
      </ul>

      {showTotal && (
        <p className="mt-3 flex justify-between border-t border-line pt-3 font-semibold">
          <span>Tổng cộng</span>
          <span>{formatVND(order.total)}</span>
        </p>
      )}
      {actions && <div className="mt-3 flex flex-wrap gap-2">{actions}</div>}
    </article>
  );
}
