import { useMemo, useState } from "react";
import { useMenu } from "@/hooks/useMenu";
import { useCart } from "@/context/CartContext";
import MenuItemCard from "@/components/MenuItemCard";
import Loading from "@/components/common/Loading";
import ErrorNotice from "@/components/common/ErrorNotice";

export default function MenuPage() {
  const { dishes, categories, loading, error, reload } = useMenu();
  const { add } = useCart();
  const [categoryId, setCategoryId] = useState<number | null>(null);
  const [query, setQuery] = useState("");

  const visible = useMemo(
    () =>
      dishes.filter(
        (d) =>
          (categoryId === null || d.categoryId === categoryId) &&
          d.name.toLowerCase().includes(query.trim().toLowerCase())
      ),
    [dishes, categoryId, query]
  );

  if (loading) return <Loading />;
  if (error) return <ErrorNotice message={error} onRetry={reload} />;

  const chip = (active: boolean) =>
    `rounded-full px-4 py-1.5 text-sm font-medium ${
      active ? "bg-brand-600 text-white" : "bg-white text-muted ring-1 ring-line hover:text-ink"
    }`;

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="text-2xl font-bold">Thực đơn</h1>
        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Tìm món..."
          className="rounded-lg border border-line bg-white px-3 py-2 text-sm sm:w-64"
        />
      </div>

      <div className="flex flex-wrap gap-2">
        <button className={chip(categoryId === null)} onClick={() => setCategoryId(null)}>Tất cả</button>
        {categories.map((c) => (
          <button key={c.id} className={chip(categoryId === c.id)} onClick={() => setCategoryId(c.id)}>
            {c.name}
          </button>
        ))}
      </div>

      {visible.length === 0 ? (
        <p className="py-10 text-center text-muted">Không có món nào khớp. Thử bỏ bộ lọc hoặc đổi từ khóa.</p>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {visible.map((d) => (
            <MenuItemCard key={d.id} dish={d} onAdd={add} />
          ))}
        </div>
      )}
    </div>
  );
}
