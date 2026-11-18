import { useState } from "react";
import type { FormEvent } from "react";
import { menuApi } from "@/api/menuApi";
import { useMenu } from "@/hooks/useMenu";
import { formatVND } from "@/utils/format";
import Button from "@/components/common/Button";
import Loading from "@/components/common/Loading";
import ErrorNotice from "@/components/common/ErrorNotice";

export default function DishManagePage() {
  const { dishes, categories, loading, error, reload } = useMenu();
  const [name, setName] = useState("");
  const [price, setPrice] = useState("");
  const [categoryId, setCategoryId] = useState<number | "">("");
  const [formError, setFormError] = useState<string | null>(null);

  const create = async (e: FormEvent) => {
    e.preventDefault();
    if (categoryId === "") {
      setFormError("Chọn danh mục cho món.");
      return;
    }
    try {
      await menuApi.create({ name, price: Number(price), categoryId, available: true });
      setName("");
      setPrice("");
      setFormError(null);
      reload();
    } catch {
      setFormError("Không thêm được món. Kiểm tra lại thông tin.");
    }
  };

  const toggle = async (id: number, available: boolean) => {
    await menuApi.update(id, { available: !available });
    reload();
  };

  const remove = async (id: number, dishName: string) => {
    if (!window.confirm(`Xóa món "${dishName}"?`)) return;
    await menuApi.remove(id);
    reload();
  };

  const catName = (id: number) => categories.find((c) => c.id === id)?.name ?? "-";

  if (loading) return <Loading />;
  if (error) return <ErrorNotice message={error} onRetry={reload} />;

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Quản lý món</h1>

      <form onSubmit={create} className="flex flex-wrap items-end gap-3 rounded-xl border border-line bg-white p-4">
        <label className="text-sm">
          Tên món
          <input value={name} onChange={(e) => setName(e.target.value)} required
            className="mt-1 block rounded-lg border border-line px-3 py-2" />
        </label>
        <label className="text-sm">
          Giá (VND)
          <input type="number" min={0} value={price} onChange={(e) => setPrice(e.target.value)} required
            className="mt-1 block w-32 rounded-lg border border-line px-3 py-2" />
        </label>
        <label className="text-sm">
          Danh mục
          <select value={categoryId} onChange={(e) => setCategoryId(e.target.value ? Number(e.target.value) : "")}
            className="mt-1 block rounded-lg border border-line px-3 py-2">
            <option value="">Chọn...</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>{c.name}</option>
            ))}
          </select>
        </label>
        <Button>Thêm món</Button>
        {formError && <p className="w-full text-sm text-red-700">{formError}</p>}
      </form>

      <div className="overflow-x-auto rounded-xl border border-line bg-white">
        <table className="w-full text-left text-sm">
          <thead className="border-b border-line bg-surface text-muted">
            <tr>
              <th className="p-3">Món</th>
              <th className="p-3">Danh mục</th>
              <th className="p-3">Giá</th>
              <th className="p-3">Trạng thái</th>
              <th className="p-3" />
            </tr>
          </thead>
          <tbody className="divide-y divide-line">
            {dishes.map((d) => (
              <tr key={d.id}>
                <td className="p-3 font-medium">{d.name}</td>
                <td className="p-3">{catName(d.categoryId)}</td>
                <td className="p-3">{formatVND(d.price)}</td>
                <td className="p-3">{d.available ? "Đang bán" : "Hết món"}</td>
                <td className="flex justify-end gap-2 p-3">
                  <Button variant="secondary" onClick={() => toggle(d.id, d.available)}>
                    {d.available ? "Tạm hết" : "Mở bán"}
                  </Button>
                  <Button variant="danger" onClick={() => remove(d.id, d.name)}>Xóa</Button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
