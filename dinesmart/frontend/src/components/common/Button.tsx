import type { ButtonHTMLAttributes } from "react";

type Variant = "primary" | "secondary" | "danger" | "ghost";

const styles: Record<Variant, string> = {
  primary: "bg-brand-600 text-white hover:bg-brand-700",
  secondary: "border border-line bg-white text-ink hover:bg-brand-50",
  danger: "bg-red-600 text-white hover:bg-red-700",
  ghost: "text-brand-700 hover:bg-brand-50",
};

interface Props extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
}

export default function Button({ variant = "primary", className = "", ...rest }: Props) {
  return (
    <button
      {...rest}
      className={`inline-flex items-center justify-center rounded-lg px-4 py-2 text-sm font-medium transition-colors disabled:cursor-not-allowed disabled:opacity-50 ${styles[variant]} ${className}`}
    />
  );
}
