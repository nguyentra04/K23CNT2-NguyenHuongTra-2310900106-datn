import type { Dish } from "@/types/menu";
import { formatVND } from "@/utils/format";
import Button from "./common/Button";

export default function MenuItemCard({ dish, onAdd }: { dish: Dish; onAdd: (d: Dish) => void }) {
  return (
    <article className="flex flex-col overflow-hidden rounded-xl border border-line bg-white">
      {dish.imageUrl ? (
        <img src={dish.imageUrl} alt={dish.name} className="h-40 w-full object-cover" loading="lazy" />
      ) : (
        <div className="flex h-40 items-center justify-center bg-brand-50 text-sm text-muted">Chưa có ảnh</div>
      )}
      <div className="flex flex-1 flex-col gap-1 p-4">
        <h3 className="font-semibold">{dish.name}</h3>
        {dish.description && <p className="line-clamp-2 text-sm text-muted">{dish.description}</p>}
        <div className="mt-auto flex items-center justify-between pt-3">
          <span className="font-semibold text-brand-700">{formatVND(dish.price)}</span>
          <Button disabled={!dish.available} onClick={() => onAdd(dish)}>
            {dish.available ? "Thêm vào giỏ" : "Hết món"}
          </Button>
        </div>
      </div>
    </article>
  );
}
