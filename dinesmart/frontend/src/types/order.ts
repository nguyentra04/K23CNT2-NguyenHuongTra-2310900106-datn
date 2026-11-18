export type OrderStatus =
  | "PENDING"
  | "COOKING"
  | "READY"
  | "SERVED"
  | "PAID"
  | "CANCELLED";

export type PaymentMethod = "CASH" | "CARD" | "TRANSFER";

export interface OrderItem {
  dishId: number;
  dishName: string;
  quantity: number;
  unitPrice: number;
  note?: string;
}

export interface Order {
  id: number;
  tableNo: string;
  status: OrderStatus;
  items: OrderItem[];
  total: number;
  createdAt: string;
}

export interface CreateOrderRequest {
  tableNo: string;
  items: { dishId: number; quantity: number; note?: string }[];
}

export const statusLabel: Record<OrderStatus, string> = {
  PENDING: "Chờ bếp nhận",
  COOKING: "Đang chế biến",
  READY: "Đã xong, chờ phục vụ",
  SERVED: "Đã phục vụ",
  PAID: "Đã thanh toán",
  CANCELLED: "Đã hủy",
};

/** Bước tiếp theo bếp/thu ngân có thể chuyển */
export const nextStatus: Partial<Record<OrderStatus, OrderStatus>> = {
  PENDING: "COOKING",
  COOKING: "READY",
  READY: "SERVED",
};
