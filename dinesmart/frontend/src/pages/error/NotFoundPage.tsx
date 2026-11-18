import { Link } from "react-router-dom";

export default function NotFoundPage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-3 p-4 text-center">
      <h1 className="text-2xl font-bold">Không tìm thấy trang</h1>
      <Link to="/" className="font-medium text-brand-700 hover:underline">Về trang chủ</Link>
    </div>
  );
}
