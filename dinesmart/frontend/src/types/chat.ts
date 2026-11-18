import type { Dish } from "./menu";

export interface ChatMessage {
  id: string;
  role: "user" | "assistant";
  content: string;
  suggestedDishes?: Dish[];
}

export interface ChatRequest {
  message: string;
  history: { role: "user" | "assistant"; content: string }[];
}

export interface ChatResponse {
  reply: string;
  suggestedDishes?: Dish[];
}
