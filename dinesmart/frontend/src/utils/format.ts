const vnd = new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" });
export const formatVND = (n: number) => vnd.format(n);

export const formatTime = (iso: string) =>
  new Date(iso).toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" });
