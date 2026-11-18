import { lazy, Suspense } from "react";
import { Route, Routes } from "react-router-dom";
import ProtectedRoute from "./ProtectedRoute";
import CustomerLayout from "@/layouts/CustomerLayout";
import StaffLayout from "@/layouts/StaffLayout";
import Loading from "@/components/common/Loading";
import HomePage from "@/pages/customer/HomePage";
import MenuPage from "@/pages/customer/MenuPage";
import CartPage from "@/pages/customer/CartPage";
import TrackOrderPage from "@/pages/customer/TrackOrderPage";
import LoginPage from "@/pages/auth/LoginPage";
import ForbiddenPage from "@/pages/error/ForbiddenPage";
import NotFoundPage from "@/pages/error/NotFoundPage";

// Khách không phải tải code của nhân viên
const PosPage = lazy(() => import("@/pages/cashier/PosPage"));
const KitchenBoardPage = lazy(() => import("@/pages/kitchen/KitchenBoardPage"));
const DashboardPage = lazy(() => import("@/pages/admin/DashboardPage"));
const DishManagePage = lazy(() => import("@/pages/admin/DishManagePage"));

export default function AppRoutes() {
  return (
    <Suspense fallback={<Loading />}>
      <Routes>
        <Route element={<CustomerLayout />}>
          <Route path="/" element={<HomePage />} />
          <Route path="/menu" element={<MenuPage />} />
          <Route path="/cart" element={<CartPage />} />
          <Route path="/orders/:id" element={<TrackOrderPage />} />
        </Route>

        <Route path="/login" element={<LoginPage />} />

        <Route element={<ProtectedRoute roles={["CASHIER", "ADMIN"]} />}>
          <Route element={<StaffLayout />}>
            <Route path="/cashier" element={<PosPage />} />
          </Route>
        </Route>

        <Route element={<ProtectedRoute roles={["KITCHEN", "ADMIN"]} />}>
          <Route element={<StaffLayout />}>
            <Route path="/kitchen" element={<KitchenBoardPage />} />
          </Route>
        </Route>

        <Route element={<ProtectedRoute roles={["ADMIN"]} />}>
          <Route element={<StaffLayout />}>
            <Route path="/admin" element={<DashboardPage />} />
            <Route path="/admin/dishes" element={<DishManagePage />} />
          </Route>
        </Route>

        <Route path="/403" element={<ForbiddenPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </Suspense>
  );
}
