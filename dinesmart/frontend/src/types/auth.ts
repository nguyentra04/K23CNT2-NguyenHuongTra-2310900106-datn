export const Role = {
  CUSTOMER: "CUSTOMER",
  CASHIER: "CASHIER",
  KITCHEN: "KITCHEN",
  ADMIN: "ADMIN",
} as const;
export type Role = (typeof Role)[keyof typeof Role];

export interface User {
  id: number;
  username: string;
  fullName: string;
  role: Role;
}

export interface LoginResponse {
  token: string;
  user: User;
}

/** Trang mặc định sau khi đăng nhập theo vai trò */
export const homeByRole: Record<Role, string> = {
  CUSTOMER: "/",
  CASHIER: "/cashier",
  KITCHEN: "/kitchen",
  ADMIN: "/admin",
};
