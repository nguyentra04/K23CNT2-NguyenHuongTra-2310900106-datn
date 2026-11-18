import axiosClient from "./axiosClient";
import type { Category, Dish, DishInput } from "@/types/menu";

export const menuApi = {
  categories: () => axiosClient.get<Category[]>("/menu/categories").then((r) => r.data),
  dishes: () => axiosClient.get<Dish[]>("/menu/dishes").then((r) => r.data),
  // Dành cho ADMIN
  create: (data: DishInput) => axiosClient.post<Dish>("/menu/dishes", data).then((r) => r.data),
  update: (id: number, data: Partial<DishInput>) =>
    axiosClient.put<Dish>(`/menu/dishes/${id}`, data).then((r) => r.data),
  remove: (id: number) => axiosClient.delete(`/menu/dishes/${id}`),
};
