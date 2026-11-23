package com.bishe;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    record Request(@NotBlank @Size(max=500) String question, Long attractionId) {}
    private final ChatService service;
    ChatController(ChatService service) { this.service=service; }
    @PostMapping ChatService.Response chat(@Valid @RequestBody Request request) { return service.answer(request.question(),request.attractionId()); }
}
