import { useEffect, useState } from "react";
import { orderApi } from "@/api/orderApi";
import type { AdminStats } from "@/api/orderApi";
import { formatVND } from "@/utils/format";
import Loading from "@/components/common/Loading";
import ErrorNotice from "@/components/common/ErrorNotice";

export default function DashboardPage() {
  const [stats, setStats] = useState<AdminStats | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    orderApi.stats().then(setStats).catch(() => setError("Không tải được số liệu."));
  }, []);

  if (error) return <ErrorNotice message={error} />;
  if (!stats) return <Loading />;

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Tổng quan hôm nay</h1>
      <div className="grid gap-4 sm:grid-cols-2">
        <div className="rounded-xl border border-line bg-white p-5">
          <p className="text-sm text-muted">Doanh thu</p>
          <p className="mt-1 text-3xl font-bold">{formatVND(stats.revenueToday)}</p>
        </div>
        <div className="rounded-xl border border-line bg-white p-5">
          <p className="text-sm text-muted">Số đơn</p>
          <p className="mt-1 text-3xl font-bold">{stats.ordersToday}</p>
        </div>
      </div>

      <section className="rounded-xl border border-line bg-white p-5">
        <h2 className="mb-3 font-semibold">Món bán chạy</h2>
        {stats.topDishes.length === 0 ? (
          <p className="text-sm text-muted">Chưa có dữ liệu.</p>
        ) : (
          <ul className="divide-y divide-line text-sm">
            {stats.topDishes.map((d) => (
              <li key={d.name} className="flex justify-between py-2">
                <span>{d.name}</span>
                <span className="text-muted">{d.quantity} phần</span>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
