import { Link } from "react-router-dom";
import { useAuth } from "@/context/AuthContext";
import { homeByRole } from "@/types/auth";

export default function ForbiddenPage() {
  const { user } = useAuth();
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-3 p-4 text-center">
      <h1 className="text-2xl font-bold">Bạn không có quyền vào trang này</h1>
      <p className="text-muted">Hãy đăng nhập bằng tài khoản phù hợp hoặc quay lại khu vực của bạn.</p>
      <Link to={user ? homeByRole[user.role] : "/login"} className="font-medium text-brand-700 hover:underline">
        {user ? "Về trang của tôi" : "Đăng nhập"}
      </Link>
    </div>
  );
}
