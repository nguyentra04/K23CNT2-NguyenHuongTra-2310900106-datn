import { createContext, useContext, useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import type { Dish } from "@/types/menu";

export interface CartItem {
  dish: Dish;
  quantity: number;
  note?: string;
}

interface CartState {
  items: CartItem[];
  count: number;
  total: number;
  add: (dish: Dish) => void;
  setQuantity: (dishId: number, quantity: number) => void;
  setNote: (dishId: number, note: string) => void;
  clear: () => void;
}

const CART_KEY = "dinesmart.cart.v1";
const CartContext = createContext<CartState | null>(null);

function loadCart(): CartItem[] {
  try {
    const raw = JSON.parse(localStorage.getItem(CART_KEY) ?? "[]");
    if (!Array.isArray(raw)) return [];
    return raw.filter(
      (i): i is CartItem =>
        i && typeof i.quantity === "number" && i.quantity > 0 &&
        i.dish && typeof i.dish.id === "number" && typeof i.dish.price === "number"
    );
  } catch {
    return [];
  }
}

export function CartProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<CartItem[]>(loadCart);

  useEffect(() => {
    localStorage.setItem(CART_KEY, JSON.stringify(items));
  }, [items]);

  const value = useMemo<CartState>(
    () => ({
      items,
      count: items.reduce((s, i) => s + i.quantity, 0),
      total: items.reduce((s, i) => s + i.quantity * i.dish.price, 0),
      add: (dish) =>
        setItems((prev) =>
          prev.some((i) => i.dish.id === dish.id)
            ? prev.map((i) => (i.dish.id === dish.id ? { ...i, quantity: i.quantity + 1 } : i))
            : [...prev, { dish, quantity: 1 }]
        ),
      setQuantity: (dishId, quantity) =>
        setItems((prev) =>
          quantity <= 0
            ? prev.filter((i) => i.dish.id !== dishId)
            : prev.map((i) => (i.dish.id === dishId ? { ...i, quantity } : i))
        ),
      setNote: (dishId, note) =>
        setItems((prev) => prev.map((i) => (i.dish.id === dishId ? { ...i, note } : i))),
      clear: () => setItems([]),
    }),
    [items]
  );

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}

export function useCart() {
  const ctx = useContext(CartContext);
  if (!ctx) throw new Error("useCart phải nằm trong <CartProvider>");
  return ctx;
}