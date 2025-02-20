package io.github.qifan777.knowledge.demo;


import io.qifan.ai.dashscope.DashScopeAiChatModel;
import lombok.AllArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RequestMapping("demo/message")
@RestController
@AllArgsConstructor
public class MessageDemoController {
    private final DashScopeAiChatModel dashScopeAiChatModel;
    private final VectorStore vectorStore;
    private final ChatMemory chatMemory = new InMemoryChatMemory();

    /**
    * 非流式问答
    * */
    @GetMapping("chat")
    public String chat(@RequestParam String prompt){
        ChatClient chatClient = ChatClient.create(dashScopeAiChatModel);
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
        return ChatClient.create(dashScopeAiChatModel).prompt()
                .user(prompt)
                .stream()
                .content();
    }

    @GetMapping(value = "chat/stream/history",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStreamWithHistory(@RequestParam String prompt,@RequestParam String sessionId){

        MessageChatMemoryAdvisor messageChatMemoryAdvisor = new MessageChatMemoryAdvisor(chatMemory, sessionId, 10);

        return ChatClient.create(dashScopeAiChatModel)
                .prompt()
                .user(prompt)
                .advisors(messageChatMemoryAdvisor)
                .stream()
                .content();
    }

    @GetMapping(value = "chat/stream/rag", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStreamWithRAG(@RequestParam String prompt){
        // 1. 定义提示词模板，question_answer_context会被替换成向量数据库中查询到的文档。
        String promptWithContext = """
                下面是上下文信息
                ---------------------
                {question_answer_context}
                ---------------------
                给定的上下文和提供的历史信息，而不是事先的知识，回复用户的意见。如果答案不在上下文中，告诉用户你不能回答这个问题。
                """;
        QuestionAnswerAdvisor questionAnswerAdvisor = new QuestionAnswerAdvisor(vectorStore, SearchRequest.defaults(), promptWithContext);
        return ChatClient.create(dashScopeAiChatModel)
                .prompt()
                .user(prompt)
                .advisors(questionAnswerAdvisor)
                .stream()
                .content();
    }

    @GetMapping(value = "chat/stream/function",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStreamWithFunction(@RequestParam String prompt){
        return ChatClient.create(dashScopeAiChatModel)
                .prompt()
                .user(prompt)
                .functions("documentAnalyzeFunction")
                .stream()
                .content();
    }

}
