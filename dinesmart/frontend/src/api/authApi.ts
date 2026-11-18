import axiosClient from "./axiosClient";
import type { LoginResponse, User } from "@/types/auth";

export const authApi = {
  login: (username: string, password: string) =>
    axiosClient.post<LoginResponse>("/auth/login", { username, password }).then((r) => r.data),
  me: () => axiosClient.get<User>("/auth/me").then((r) => r.data),
};
