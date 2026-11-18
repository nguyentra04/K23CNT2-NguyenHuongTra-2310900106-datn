import { useCallback, useState } from "react";
import { chatApi } from "@/api/chatApi";
import type { ChatMessage } from "@/types/chat";

const uid = () => Math.random().toString(36).slice(2);

const WELCOME: ChatMessage = {
  id: "welcome",
  role: "assistant",
  content: "DineSmart xin chào! Hôm nay bạn muốn ăn gì? ",
};

export function useChat() {
  const [messages, setMessages] = useState<ChatMessage[]>([WELCOME]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const send = useCallback(
    async (text: string) => {
      const content = text.trim();
      if (!content || loading) return;

      const history = messages
        .filter((m) => m.id !== "welcome")
        .map((m) => ({ role: m.role, content: m.content }));

      setMessages((prev) => [...prev, { id: uid(), role: "user", content }]);
      setLoading(true);
      setError(null);

      try {
        const res = await chatApi.send({ message: content, history });
        setMessages((prev) => [
          ...prev,
          { id: uid(), role: "assistant", content: res.reply, suggestedDishes: res.suggestedDishes },
        ]);
      } catch {
        setError("Chatbot đang bận. Bạn thử gửi lại sau ít giây nhé.");
      } finally {
        setLoading(false);
      }
    },
    [messages, loading]
  );

  return { messages, loading, error, send };
}
