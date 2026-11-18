package datn.nht.dinesmart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class ChatDtos {
    private ChatDtos() {}

    public record ChatRequest(@Size(max = 64) String sessionToken,
                              @NotBlank @Size(max = 1000) String message) {}

    public record ChatResponse(String sessionToken, String reply, int remaining, boolean limitReached, String model) {}
}
