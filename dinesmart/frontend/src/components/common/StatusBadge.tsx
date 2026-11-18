import { statusLabel } from "@/types/order";
import type { OrderStatus } from "@/types/order";

const color: Record<OrderStatus, string> = {
  PENDING: "bg-amber-100 text-amber-800",
  COOKING: "bg-orange-100 text-orange-800",
  READY: "bg-brand-100 text-brand-700",
  SERVED: "bg-sky-100 text-sky-800",
  PAID: "bg-slate-200 text-slate-700",
  CANCELLED: "bg-red-100 text-red-700",
};

export default function StatusBadge({ status }: { status: OrderStatus }) {
  return (
    <span className={`rounded-full px-2.5 py-1 text-xs font-medium ${color[status]}`}>
      {statusLabel[status]}
    </span>
  );
}
