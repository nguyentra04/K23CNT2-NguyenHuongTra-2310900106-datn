import { useState } from "react";
import { orderApi } from "@/api/orderApi";
import { useOrders } from "@/hooks/useOrders";
import { nextStatus } from "@/types/order";
import type { Order, OrderStatus } from "@/types/order";
import OrderCard from "@/components/OrderCard";
import Button from "@/components/common/Button";
import Loading from "@/components/common/Loading";
import ErrorNotice from "@/components/common/ErrorNotice";

const COLUMNS: { status: OrderStatus; title: string; action: string }[] = [
  { status: "PENDING", title: "Đơn mới", action: "Bắt đầu nấu" },
  { status: "COOKING", title: "Đang nấu", action: "Đã nấu xong" },
  { status: "READY", title: "Chờ phục vụ", action: "Đã mang ra bàn" },
];

export default function KitchenBoardPage() {
  const { orders, loading, error, reload } = useOrders(["PENDING", "COOKING", "READY"], 3000);
  const [busyId, setBusyId] = useState<number | null>(null);

  const advance = async (o: Order) => {
    const next = nextStatus[o.status];
    if (!next) return;
    setBusyId(o.id);
    try {
      await orderApi.updateStatus(o.id, next);
      await reload();
    } finally {
      setBusyId(null);
    }
  };

  if (loading) return <Loading />;

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">Bảng bếp</h1>
      {error && <ErrorNotice message={error} onRetry={reload} />}
      <div className="grid gap-4 lg:grid-cols-3">
        {COLUMNS.map((col) => {
          const list = orders.filter((o) => o.status === col.status);
          return (
            <section key={col.status} className="space-y-3 rounded-xl bg-white/60 p-3 ring-1 ring-line">
              <h2 className="font-semibold">
                {col.title} ({list.length})
              </h2>
              {list.length === 0 && <p className="py-6 text-center text-sm text-muted">Không có đơn.</p>}
              {list.map((o) => (
                <OrderCard
                  key={o.id}
                  order={o}
                  actions={
                    <Button disabled={busyId === o.id} onClick={() => advance(o)}>
                      {col.action}
                    </Button>
                  }
                />
              ))}
            </section>
          );
        })}
      </div>
    </div>
  );
}
