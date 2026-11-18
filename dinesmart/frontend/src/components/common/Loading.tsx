export default function Loading({ label = "Đang tải..." }: { label?: string }) {
  return (
    <div className="flex items-center justify-center gap-3 p-10 text-muted" role="status">
      <span className="h-5 w-5 animate-spin rounded-full border-2 border-brand-600 border-t-transparent" />
      {label}
    </div>
  );
}
