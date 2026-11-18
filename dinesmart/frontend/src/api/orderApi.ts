import axiosClient from "./axiosClient";
import type { CreateOrderRequest, Order, OrderStatus, PaymentMethod } from "@/types/order";

export interface AdminStats {
  revenueToday: number;
  ordersToday: number;
  topDishes: { name: string; quantity: number }[];
}

export const orderApi = {
  create: (data: CreateOrderRequest) => axiosClient.post<Order>("/orders", data).then((r) => r.data),
  get: (id: number) => axiosClient.get<Order>(`/orders/${id}`).then((r) => r.data),
  list: (statuses?: OrderStatus[]) =>
    axiosClient
      .get<Order[]>("/orders", { params: statuses?.length ? { status: statuses.join(",") } : undefined })
      .then((r) => r.data),
  updateStatus: (id: number, status: OrderStatus) =>
    axiosClient.patch<Order>(`/orders/${id}/status`, { status }).then((r) => r.data),
  pay: (id: number, method: PaymentMethod) =>
    axiosClient.post<Order>(`/orders/${id}/pay`, { method }).then((r) => r.data),
  stats: () => axiosClient.get<AdminStats>("/admin/stats").then((r) => r.data),
};
