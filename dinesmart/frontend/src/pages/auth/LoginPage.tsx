import { useState } from "react";
import type { FormEvent } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "@/context/AuthContext";
import { homeByRole } from "@/types/auth";
import Button from "@/components/common/Button";

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      const user = await login(username, password);
      const from = (location.state as { from?: string } | null)?.from;
      navigate(from ?? homeByRole[user.role], { replace: true });
    } catch {
      setError("Sai tên đăng nhập hoặc mật khẩu.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center p-4">
      <form onSubmit={submit} className="w-full max-w-sm space-y-4 rounded-2xl border border-line bg-white p-6">
        <h1 className="text-xl font-bold">Đăng nhập nhân viên</h1>
        <label className="block text-sm">
          Tên đăng nhập
          <input
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
            autoComplete="username"
            className="mt-1 w-full rounded-lg border border-line px-3 py-2"
          />
        </label>
        <label className="block text-sm">
          Mật khẩu
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            autoComplete="current-password"
            className="mt-1 w-full rounded-lg border border-line px-3 py-2"
          />
        </label>
        {error && <p className="text-sm text-red-700">{error}</p>}
        <Button disabled={submitting} className="w-full">
          {submitting ? "Đang đăng nhập..." : "Đăng nhập"}
        </Button>
      </form>
    </div>
  );
}
