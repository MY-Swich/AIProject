package io.github.qifan777.knowledge.ai.message;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.qifan777.knowledge.ai.message.dto.AiMessageInput;
import io.qifan.ai.dashscope.DashScopeAiChatModel;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;


@RequestMapping("message")
@RestController
@AllArgsConstructor
@Slf4j
public class AiMessageController {
    private final AiMessageRepository aiMessageRepository;
    private final AiMessageChatMemory chatMemory;
    private final DashScopeAiChatModel dashScopeAiChatModel;
    private final ObjectMapper objectMapper;

    @DeleteMapping("history/{sessionId}")
    public void deleteHistory(@PathVariable String sessionId){
        aiMessageRepository.deleteBySessionId(sessionId);
    }

    @PostMapping
    public void save(@RequestBody AiMessageInput input){
        aiMessageRepository.save(input.toEntity());
    }

    @PostMapping(value = "chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(@RequestBody AiMessageInput input){
        var advisor = new MessageChatMemoryAdvisor(chatMemory, input.getSessionId(),10);
        return ChatClient
                .create(dashScopeAiChatModel)
                .prompt()
                .user(promptUserSpec -> {
                    promptUserSpec.text(input.getTextContent());
//                    Message message = AiMessageChatMemory.toSpringAiMessage(input.toEntity());
                })
                .advisors(advisor)
                .stream()
                .chatResponse()
//                .content()
                .map(response -> ServerSentEvent.builder(toJsonStr(response))
                        .event("message")
                        .build());
    }

    @SneakyThrows
    public String toJsonStr(ChatResponse chatResponse){
        return objectMapper.writeValueAsString(chatResponse);
    }


}
