import axios from "axios";

export const TOKEN_KEY = "token";

const axiosClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? "/api",
  headers: { "Content-Type": "application/json" },
  timeout: 20000,
});

axiosClient.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

axiosClient.interceptors.response.use(
  (res) => res,
  (error) => {
    // Token hết hạn/không hợp lệ -> báo cho AuthContext đăng xuất
    if (error.response?.status === 401) {
      window.dispatchEvent(new Event("auth:logout"));
    }
    return Promise.reject(error);
  }
);

export default axiosClient;
