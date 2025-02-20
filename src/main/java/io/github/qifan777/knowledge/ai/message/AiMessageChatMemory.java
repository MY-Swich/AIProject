package io.github.qifan777.knowledge.ai.message;

import cn.hutool.core.collection.CollectionUtil;
import io.qifan.infrastructure.common.exception.BusinessException;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.model.Media;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class AiMessageChatMemory implements ChatMemory {

    private final AiMessageRepository aiMessageRepository;

    @Override
    public void add(String conversationId, List<Message> messages) {
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        return aiMessageRepository
                .findBySessionId(conversationId, lastN)
                .stream()
                // 转成Message对象
                .map(AiMessageChatMemory::toSpringAiMessage)
                .toList();
    }

    @Override
    public void clear(String conversationId) {
        aiMessageRepository.deleteBySessionId(conversationId);
    }

    public static AiMessage toAiMessage(Message message, String sessionId) {
        return AiMessageDraft.$.produce(draft -> {
            draft.setSessionId(sessionId)
                    .setTextContent(message.getContent())
                    .setType(message.getMessageType())
                    .setMedias(new ArrayList<>());
            if (message instanceof UserMessage userMessage &&
                    !CollectionUtil.isEmpty(userMessage.getMedia())) {
                List<AiMessage.Media> mediaList = ((UserMessage) message)
                        .getMedia()
                        .stream()
                        .map(media -> {
                            AiMessage.Media newMedia = new AiMessage.Media();
                            newMedia.setType(media.getMimeType().getType());
                            // 假设setData方法期望一个byte[]类型的参数
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

    public static Message toSpringAiMessage(AiMessage aiMessage) {
        List<Media> mediaList = new ArrayList<>();
        if (!CollectionUtil.isEmpty(aiMessage.medias())) {
            mediaList = aiMessage.medias().stream().map(AiMessageChatMemory::toSpringAiMedia).toList();
        }
        if (aiMessage.type().equals(MessageType.ASSISTANT)) {
            return new AssistantMessage(aiMessage.textContent());
        }
        if (aiMessage.type().equals(MessageType.USER)) {
            return new UserMessage(aiMessage.textContent(), mediaList);
        }
        if (aiMessage.type().equals(MessageType.SYSTEM)) {
            return new SystemMessage(aiMessage.textContent());
        }
        throw new BusinessException("不支持的消息类型");
    }

    @SneakyThrows
    public static Media toSpringAiMedia(AiMessage.Media media) {
        return new Media(new MediaType(media.getType()), new URL(media.getData()));
    }
}
