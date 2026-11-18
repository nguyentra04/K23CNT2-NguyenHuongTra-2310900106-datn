import type { ChatMessage } from "@/types/chat";
import type { Dish } from "@/types/menu";
import { formatVND } from "@/utils/format";

interface Props {
  message: ChatMessage;
  onAddDish: (d: Dish) => void;
}

export default function MessageBubble({ message, onAddDish }: Props) {
  const mine = message.role === "user";
  return (
    <div className={`flex flex-col gap-2 ${mine ? "items-end" : "items-start"}`}>
      <p
        className={`max-w-[85%] whitespace-pre-wrap rounded-2xl px-3 py-2 text-sm ${
          mine ? "bg-brand-600 text-white" : "bg-white text-ink ring-1 ring-line"
        }`}
      >
        {message.content}
      </p>
      {message.suggestedDishes?.map((d) => (
        <div
          key={d.id}
          className="flex w-[85%] items-center justify-between gap-2 rounded-lg bg-white p-2 text-sm ring-1 ring-line"
        >
          <span>
            <b className="block">{d.name}</b>
            <span className="text-muted">{formatVND(d.price)}</span>
          </span>
          <button
            onClick={() => onAddDish(d)}
            className="rounded-md bg-brand-50 px-2 py-1 text-xs font-medium text-brand-700 hover:bg-brand-100"
          >
            Thêm vào giỏ
          </button>
        </div>
      ))}
    </div>
  );
}
