package io.github.qifan777.knowledge.ai.message;


import io.github.qifan777.knowledge.ai.message.dto.AiMessageInput;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import tools.jackson.databind.ObjectMapper;
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
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    /**
     * 删除指定会话的完整历史消息
     *
     * @param sessionId 会话 ID
     */
    @DeleteMapping("history/{sessionId}")
    public void deleteHistory(@PathVariable String sessionId){
        aiMessageRepository.deleteBySessionId(sessionId);
    }

    /**
     * 保存前端发来的单条消息（文字或媒体）
     *
     * @param input 消息内容（textContent + 可选的 medias）
     */
    @PostMapping
    public void save(@RequestBody AiMessageInput input){
        aiMessageRepository.save(input.toEntity());
    }

    /**
     * 流式 AI 对话（SSE），携带历史记忆
     *
     * @param input 包含 textContent（用户消息）和 sessionId（会话 ID）
     * @return SSE 流，每事件包含 ChatResponse JSON（含完整 token 及 finish 原因）
     */
    @PostMapping(value = "chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(@RequestBody AiMessageInput input){
        var advisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
        return ChatClient
                .create(chatModel)
                .prompt()
                .user(promptUserSpec -> {
                    promptUserSpec.text(input.getTextContent());
                    // TODO(@username): 消息持久化已由 AiMessageChatMemory.add() 接管，此处无需重复转换
                    // Message message = AiMessageChatMemory.toSpringAiMessage(input.toEntity());
                })
                .advisors(a -> a
                        .param(ChatMemory.CONVERSATION_ID, input.getSessionId())
                        .advisors(advisor))
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
