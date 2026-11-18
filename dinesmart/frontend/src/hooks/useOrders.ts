import { useCallback, useEffect, useState } from "react";
import { orderApi } from "@/api/orderApi";
import type { Order, OrderStatus } from "@/types/order";

/**
 * Lấy danh sách đơn và tự làm mới mỗi `intervalMs`.
 * Giai đoạn đầu dùng polling cho đơn giản; sau này thay bằng WebSocket/SSE.
 */
export function useOrders(statuses: OrderStatus[], intervalMs = 5000) {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const key = statuses.join(",");

  const reload = useCallback(async () => {
    try {
      const data = await orderApi.list(key ? (key.split(",") as OrderStatus[]) : undefined);
      setOrders(data);
      setError(null);
    } catch {
      setError("Không tải được danh sách đơn.");
    } finally {
      setLoading(false);
    }
  }, [key]);

  useEffect(() => {
    reload();
    const t = setInterval(reload, intervalMs);
    return () => clearInterval(t);
  }, [reload, intervalMs]);

  return { orders, loading, error, reload };
}
