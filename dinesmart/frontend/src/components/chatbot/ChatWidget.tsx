import { useEffect, useRef, useState } from "react";
import type { FormEvent } from "react";
import { useChat } from "@/hooks/useChat";
import { useCart } from "@/context/CartContext";
import MessageBubble from "./MessageBubble";

export default function ChatWidget() {
  const [open, setOpen] = useState(false);
  const [text, setText] = useState("");
  const { messages, loading, error, send } = useChat();
  const { add } = useCart();
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, loading, open]);

  const submit = (e: FormEvent) => {
    e.preventDefault();
    send(text);
    setText("");
  };

  return (
    <div className="fixed bottom-4 right-4 z-40">
      {open && (
        <section
          aria-label="Chatbot tư vấn món"
          className="mb-3 flex h-[28rem] w-[calc(100vw-2rem)] max-w-sm flex-col overflow-hidden rounded-2xl border border-line bg-surface shadow-xl"
        >
          <header className="flex items-center justify-between bg-brand-600 px-4 py-3 text-white">
            <b>Tư vấn món ăn</b>
            <button onClick={() => setOpen(false)} aria-label="Đóng chat" className="text-xl leading-none">
              ×
            </button>
          </header>

          <div className="flex-1 space-y-3 overflow-y-auto p-3">
            {messages.map((m) => (
              <MessageBubble key={m.id} message={m} onAddDish={add} />
            ))}
            {loading && <p className="text-sm text-muted">Đang soạn câu trả lời...</p>}
            {error && <p className="text-sm text-red-700">{error}</p>}
            <div ref={bottomRef} />
          </div>

          <form onSubmit={submit} className="flex gap-2 border-t border-line bg-white p-2">
            <input
              value={text}
              onChange={(e) => setText(e.target.value)}
              placeholder="Hỏi về món, dị ứng, ngân sách..."
              className="flex-1 rounded-lg border border-line px-3 py-2 text-sm"
            />
            <button
              disabled={loading || !text.trim()}
              className="rounded-lg bg-brand-600 px-3 text-sm font-medium text-white disabled:opacity-50"
            >
              Gửi
            </button>
          </form>
        </section>
      )}

      <button
        onClick={() => setOpen((o) => !o)}
        className="ml-auto flex h-12 items-center rounded-full bg-brand-600 px-5 text-sm font-medium text-white shadow-lg hover:bg-brand-700"
      >
        {open ? "Thu gọn" : "Hỏi chatbot"}
      </button>
    </div>
  );
}
