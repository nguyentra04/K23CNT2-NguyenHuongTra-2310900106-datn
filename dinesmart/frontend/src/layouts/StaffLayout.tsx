import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "@/context/AuthContext";
import type { Role } from "@/types/auth";

interface NavItem {
  to: string;
  label: string;
  roles: Role[];
}

// Một sidebar cho cả 3 vai trò nhân viên, lọc theo role
const NAV: NavItem[] = [
  { to: "/cashier", label: "Thanh toán", roles: ["CASHIER", "ADMIN"] },
  { to: "/kitchen", label: "Bảng bếp", roles: ["KITCHEN", "ADMIN"] },
  { to: "/admin", label: "Tổng quan", roles: ["ADMIN"] },
  { to: "/admin/dishes", label: "Quản lý món", roles: ["ADMIN"] },
];

const roleLabel: Record<Role, string> = {
  CUSTOMER: "Khách",
  CASHIER: "Thu ngân",
  KITCHEN: "Bếp",
  ADMIN: "Quản trị",
};

export default function StaffLayout() {
  const { user, logout } = useAuth();
  const items = NAV.filter((n) => user && n.roles.includes(user.role));

  return (
    <div className="flex min-h-screen">
      <aside className="flex w-56 shrink-0 flex-col border-r border-line bg-white p-4">
        <p className="mb-6 text-lg font-bold text-brand-700">Bếp Nhà</p>
        <nav className="flex flex-col gap-1">
          {items.map((n) => (
            <NavLink
              key={n.to}
              to={n.to}
              end
              className={({ isActive }: { isActive: boolean }) =>
                `rounded-md px-3 py-2 text-sm font-medium ${
                  isActive ? "bg-brand-50 text-brand-700" : "text-muted hover:bg-surface hover:text-ink"
                }`
              }
            >
              {n.label}
            </NavLink>
          ))}
        </nav>
        <div className="mt-auto border-t border-line pt-4 text-sm">
          <p className="font-medium">{user?.fullName}</p>
          <p className="mb-3 text-muted">{user && roleLabel[user.role]}</p>
          <button onClick={logout} className="text-brand-700 hover:underline">
            Đăng xuất
          </button>
        </div>
      </aside>
      <main className="flex-1 overflow-x-auto p-6">
        <Outlet />
      </main>
    </div>
  );
}
