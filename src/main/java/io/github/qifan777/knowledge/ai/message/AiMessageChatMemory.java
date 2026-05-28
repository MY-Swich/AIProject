package io.github.qifan777.knowledge.ai.message;

// OER — 网上报销：聊天记忆持久化桥接层
// 将 AI 对话消息持久化到 MySQL（通过 Jimmer ORM），
// 实现 ChatMemory 接口以接入 Spring AI 的 MessageChatMemoryAdvisor
import cn.hutool.core.collection.CollectionUtil;
import io.qifan.infrastructure.common.exception.BusinessException;
import lombok.AllArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AiMessageChatMemory implements ChatMemory {

    private final AiMessageRepository aiMessageRepository;
    // 每次获取历史消息的最大条数，通过配置项 ai.chat.memory.last-n 控制，默认 10
    private final int defaultLastN;

    /**
     * @param aiMessageRepository Jimmer 仓库，负责 AI 消息的持久化
     * @param defaultLastN        历史消息回溯条数，由 {@code ai.chat.memory.last-n} 配置
     */
    public AiMessageChatMemory(AiMessageRepository aiMessageRepository,
                               @Value("${ai.chat.memory.last-n:10}") int defaultLastN) {
        this.aiMessageRepository = aiMessageRepository;
        this.defaultLastN = defaultLastN;
    }

    /**
     * 持久化由 MessageChatMemoryAdvisor 自动触发，本方法为空。
     * 实际写入由 AiMessageController 在接收前端消息时通过 save() 完成。
     */
    @Override
    public void add(String conversationId, List<Message> messages) {
    }

    /**
     * 按会话 ID 获取最近 N 条历史消息
     *
     * @param conversationId 会话 ID（对应 AiSession.id）
     * @return 转换为 Spring AI Message 对象列表，供 MessageChatMemoryAdvisor 注入到 prompt
     */
    @Override
    public List<Message> get(String conversationId) {
        return aiMessageRepository
                .findBySessionId(conversationId, defaultLastN)
                .stream()
                .map(AiMessageChatMemory::toSpringAiMessage)
                .toList();
    }

    /**
     * 清除指定会话的所有历史消息
     *
     * @param conversationId 会话 ID
     */
    @Override
    public void clear(String conversationId) {
        aiMessageRepository.deleteBySessionId(conversationId);
    }

    /**
     * 将 Spring AI Message 转换为业务实体 AiMessage（持久化用）
     *
     * @param message   Spring AI 消息（UserMessage / AssistantMessage / SystemMessage）
     * @param sessionId 关联的会话 ID
     * @return Jimmer Draft 实体
     */
    public static AiMessage toAiMessage(Message message, String sessionId) {
        return AiMessageDraft.$.produce(draft -> {
            draft.setSessionId(sessionId)
                    .setTextContent(message.getText())
                    .setType(message.getMessageType())
                    .setMedias(new ArrayList<>());
            // UserMessage 可能携带图片等附件媒体
            if (message instanceof UserMessage userMessage &&
                    !CollectionUtil.isEmpty(userMessage.getMedia())) {
                List<AiMessage.Media> mediaList = ((UserMessage) message)
                        .getMedia()
                        .stream()
                        .map(media -> {
                            AiMessage.Media newMedia = new AiMessage.Media();
                            newMedia.setType(media.getMimeType().getType());
                            // TODO(@username): media.getData() 返回类型因供应商而异，当前按 toString 处理，需按实际类型调整
                            Object data = media.getData();
                            if (data != null) {
                                newMedia.setData(media.getData().toString());
                            }
                            return newMedia;
                        })
                        .toList();
                draft.setMedias(mediaList);
            }
        });
    }

    /**
     * 将业务实体 AiMessage 转换为 Spring AI Message（模型调用用）
     *
     * @param aiMessage 业务实体
     * @return Spring AI 消息类型（UserMessage / AssistantMessage / SystemMessage）
     * @throws BusinessException 不支持的消息类型
     */
    public static Message toSpringAiMessage(AiMessage aiMessage) {
        List<Media> mediaList = new ArrayList<>();
        if (!CollectionUtil.isEmpty(aiMessage.medias())) {
            mediaList = aiMessage.medias().stream().map(AiMessageChatMemory::toSpringAiMedia).toList();
        }
        if (aiMessage.type().equals(MessageType.ASSISTANT)) {
            return new AssistantMessage(aiMessage.textContent());
        }
        if (aiMessage.type().equals(MessageType.USER)) {
            return UserMessage.builder().text(aiMessage.textContent()).media(mediaList).build();
        }
        if (aiMessage.type().equals(MessageType.SYSTEM)) {
            return new SystemMessage(aiMessage.textContent());
        }
        throw new BusinessException("不支持的消息类型");
    }

    /**
     * 将业务 Media 转换为 Spring AI Media
     *
     * @param media 业务媒体（type=图片格式, data=图片 URL）
     * @return Spring AI Media 对象
     */
    public static Media toSpringAiMedia(AiMessage.Media media) {
        return new Media(MimeType.valueOf(media.getType()), URI.create(media.getData()));
    }
}
