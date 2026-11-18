import { useEffect, useState } from "react";
import { menuApi } from "@/api/menuApi";
import type { Category, Dish } from "@/types/menu";

export function useMenu() {
  const [dishes, setDishes] = useState<Dish[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const reload = () => {
    setLoading(true);
    Promise.all([menuApi.dishes(), menuApi.categories()])
      .then(([d, c]) => {
        setDishes(d);
        setCategories(c);
        setError(null);
      })
      .catch(() => setError("Không tải được thực đơn. Vui lòng thử lại."))
      .finally(() => setLoading(false));
  };

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(reload, []);

  return { dishes, categories, loading, error, reload };
}
