import axiosClient from "./axiosClient";
import type { ChatRequest, ChatResponse } from "@/types/chat";

// Frontend KHÔNG gọi LLM trực tiếp. Spring Boot giữ API key và tra menu thật.
export const chatApi = {
  send: (data: ChatRequest) =>
    axiosClient.post<ChatResponse>("/chatbot/messages", data).then((r) => r.data),
};
