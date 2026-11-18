export interface Category {
  id: number;
  name: string;
}

export interface Dish {
  id: number;
  name: string;
  description?: string;
  price: number;
  imageUrl?: string;
  categoryId: number;
  available: boolean;
}

export type DishInput = Omit<Dish, "id">;
