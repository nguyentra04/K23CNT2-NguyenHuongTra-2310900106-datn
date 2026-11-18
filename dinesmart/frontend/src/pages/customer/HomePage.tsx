import { Link } from "react-router-dom";
import { useMenu } from "@/hooks/useMenu";
import { useCart } from "@/context/CartContext";
import MenuItemCard from "@/components/MenuItemCard";
import Loading from "@/components/common/Loading";

export default function HomePage() {
  const { dishes, loading } = useMenu();
  const { add } = useCart();
  const featured = dishes.filter((d) => d.available).slice(0, 3);

  return (
    <div className="space-y-10">
      <section className="rounded-2xl bg-brand-600 p-8 text-white md:p-12">
        <h1 className="max-w-xl text-3xl font-bold md:text-4xl">Gọi món tại bàn, bếp nhận đơn ngay.</h1>
        <p className="mt-3 max-w-lg text-brand-100">
          Chọn món, ghi chú theo ý bạn và theo dõi tiến độ chế biến. Chưa biết ăn gì? Hỏi chatbot ở góc phải.
        </p>
        <Link
          to="/menu"
          className="mt-6 inline-block rounded-lg bg-white px-5 py-2.5 text-sm font-semibold text-brand-700 hover:bg-brand-50"
        >
          Xem thực đơn
        </Link>
      </section>

      <section>
        <h2 className="mb-4 text-xl font-semibold">Món nổi bật</h2>
        {loading ? (
          <Loading />
        ) : (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {featured.map((d) => (
              <MenuItemCard key={d.id} dish={d} onAdd={add} />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
