import { Link, NavLink, Outlet } from "react-router-dom";
import { useAuth } from "@/context/AuthContext";
import { useCart } from "@/context/CartContext";
import { homeByRole } from "@/types/auth";
import ChatWidget from "@/components/chatbot/ChatWidget";

const link = ({ isActive }: { isActive: boolean }) =>
  `rounded-md px-3 py-2 text-sm font-medium ${isActive ? "bg-brand-50 text-brand-700" : "text-muted hover:text-ink"}`;

export default function CustomerLayout() {
  const { user, logout } = useAuth();
  const { count } = useCart();

  return (
    <div className="flex min-h-screen flex-col">
      <header className="sticky top-0 z-30 border-b border-line bg-white">
        <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4">
          <Link to="/" className="text-lg font-bold text-brand-700">
            Bếp Nhà
          </Link>
          <nav className="flex items-center gap-1">
            <NavLink to="/" end className={link}>Trang chủ</NavLink>
            <NavLink to="/menu" className={link}>Thực đơn</NavLink>
            <NavLink to="/cart" className={link}>Giỏ hàng ({count})</NavLink>
            {user && user.role !== "CUSTOMER" ? (
              <Link to={homeByRole[user.role]} className="ml-2 rounded-md px-3 py-2 text-sm text-brand-700">
                Vào khu nhân viên
              </Link>
            ) : null}
            {user ? (
              <button onClick={logout} className="ml-2 rounded-md px-3 py-2 text-sm text-muted hover:text-ink">
                Đăng xuất
              </button>
            ) : (
              <Link to="/login" className="ml-2 rounded-md px-3 py-2 text-sm text-muted hover:text-ink">
                Nhân viên đăng nhập
              </Link>
            )}
          </nav>
        </div>
      </header>

      <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-8">
        <Outlet />
      </main>

      <footer className="border-t border-line bg-white py-6 text-center text-sm text-muted">
        © {new Date().getFullYear()} Bếp Nhà
      </footer>
      <ChatWidget />
    </div>
  );
}
