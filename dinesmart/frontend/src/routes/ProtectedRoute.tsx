import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "@/context/AuthContext";
import Loading from "@/components/common/Loading";
import type { Role } from "@/types/auth";

// Lưu ý: chỉ ẩn giao diện. Phân quyền thật nằm ở Spring Security.
export default function ProtectedRoute({ roles }: { roles: Role[] }) {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) return <Loading />;
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  if (!roles.includes(user.role)) return <Navigate to="/403" replace />;
  return <Outlet />;
}
