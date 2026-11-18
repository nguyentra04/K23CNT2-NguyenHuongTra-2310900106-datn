import { useState } from "react";
import { orderApi } from "@/api/orderApi";
import { useOrders } from "@/hooks/useOrders";
import type { Order, PaymentMethod } from "@/types/order";
import OrderCard from "@/components/OrderCard";
import Button from "@/components/common/Button";
import Loading from "@/components/common/Loading";
import ErrorNotice from "@/components/common/ErrorNotice";

const METHODS: { value: PaymentMethod; label: string }[] = [
  { value: "CASH", label: "Tiền mặt" },
  { value: "CARD", label: "Thẻ" },
  { value: "TRANSFER", label: "Chuyển khoản" },
];

export default function PosPage() {
  const { orders, loading, error, reload } = useOrders(["PENDING", "COOKING", "READY", "SERVED"]);
  const [busyId, setBusyId] = useState<number | null>(null);

  const pay = async (o: Order, method: PaymentMethod) => {
    setBusyId(o.id);
    try {
      await orderApi.pay(o.id, method);
      await reload();
    } finally {
      setBusyId(null);
    }
  };

  const cancel = async (o: Order) => {
    if (!window.confirm(`Hủy đơn #${o.id} của bàn ${o.tableNo}?`)) return;
    setBusyId(o.id);
    try {
      await orderApi.updateStatus(o.id, "CANCELLED");
      await reload();
    } finally {
      setBusyId(null);
    }
  };

  if (loading) return <Loading />;

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">Thanh toán</h1>
      {error && <ErrorNotice message={error} onRetry={reload} />}
      {orders.length === 0 ? (
        <p className="py-10 text-center text-muted">Chưa có đơn nào cần thanh toán.</p>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {orders.map((o) => (
            <OrderCard
              key={o.id}
              order={o}
              showTotal
              actions={
                <>
                  {METHODS.map((m) => (
                    <Button key={m.value} disabled={busyId === o.id} onClick={() => pay(o, m.value)}>
                      {m.label}
                    </Button>
                  ))}
                  <Button variant="ghost" disabled={busyId === o.id} onClick={() => cancel(o)}>
                    Hủy đơn
                  </Button>
                </>
              }
            />
          ))}
        </div>
      )}
    </div>
  );
}
