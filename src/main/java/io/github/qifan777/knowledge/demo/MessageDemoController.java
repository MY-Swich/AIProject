package io.github.qifan777.knowledge.demo;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RequestMapping("demo/message")
@RestController
@AllArgsConstructor
public class MessageDemoController {
    private final ChatModel chatModel;
    private final VectorStore vectorStore;
    private final ChatMemory chatMemory = new ChatMemory() {
        private final ConcurrentHashMap<String, List<Message>> store = new ConcurrentHashMap<>();
        @Override public void add(String conversationId, List<Message> messages) {
            store.merge(conversationId, messages, (old, nu) -> { var all = new ArrayList<>(old); all.addAll(nu); return all; });
        }
        @Override public List<Message> get(String conversationId) { return store.getOrDefault(conversationId, List.of()); }
        @Override public void clear(String conversationId) { store.remove(conversationId); }
    };

    /**
    * 非流式问答
    * */
    @GetMapping("chat")
    public String chat(@RequestParam String prompt){
        ChatClient chatClient = ChatClient.create(chatModel);
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
    /**
     * 流式处理
     */
    @GetMapping(value = "chat/stream",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestParam String prompt){
        return ChatClient.create(chatModel).prompt()
                .user(prompt)
                .stream()
                .content();
    }

    /**
     * 流式问答 + 历史记忆
     *
     * @param prompt   用户提问
     * @param sessionId 会话 ID，MessageChatMemoryAdvisor 据此注入历史上下文
     */
    @GetMapping(value = "chat/stream/history",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStreamWithHistory(@RequestParam String prompt,@RequestParam String sessionId){

        MessageChatMemoryAdvisor messageChatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory)
                .build();

        return ChatClient.create(chatModel)
                .prompt()
                .user(prompt)
                .advisors(a -> a
                        .param(ChatMemory.CONVERSATION_ID, sessionId)
                        .advisors(messageChatMemoryAdvisor))
                .stream()
                .content();
    }

    /**
     * 流式问答 + RAG：先检索向量库，将匹配文档作为上下文注入 prompt
     *
     * @param prompt 用户提问，同时作为向量检索的查询文本
     */
    @GetMapping(value = "chat/stream/rag", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStreamWithRAG(@RequestParam String prompt){
        // 1. 从向量数据库检索相关文档
        List<Document> documents = vectorStore.similaritySearch(
                SearchRequest.builder().query(prompt).build());
        String context = documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));
        // 2. 构建带上下文的提示词
        String promptWithContext = """
                下面是上下文信息
                ---------------------
                %s
                ---------------------
                给定的上下文和提供的历史信息，而不是事先的知识，回复用户的意见。如果答案不在上下文中，告诉用户你不能回答这个问题。
                """.formatted(context);
        return ChatClient.create(chatModel)
                .prompt()
                .user(promptWithContext)
                .stream()
                .content();
    }

    /**
     * 流式问答 + 函数调用（工具）：激活 DocumentAnalyzeFunction 用于文档解析
     *
     * @param prompt 用户提问，AI 模型可按需调用 documentAnalyzeFunction 工具
     */
    @GetMapping(value = "chat/stream/function",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStreamWithFunction(@RequestParam String prompt){
        return ChatClient.create(chatModel)
                .prompt()
                .user(prompt)
                .tools("documentAnalyzeFunction")
                .stream()
                .content();
    }

}
