import Button from "./Button";

export default function ErrorNotice({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div className="flex items-center justify-between gap-4 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-800">
      <span>{message}</span>
      {onRetry && (
        <Button variant="secondary" onClick={onRetry}>
          Thử lại
        </Button>
      )}
    </div>
  );
}
