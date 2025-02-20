package io.github.qifan777.knowledge.ai.message;

import org.babyfish.jimmer.spring.repository.JRepository;

import java.util.List;

public interface AiMessageRepository extends JRepository<AiMessage, String> {
    AiMessageTable t = new AiMessageTable();
    default List<AiMessage> findBySessionId(String sessionId, int lastN){
//        """
//                select * from ai_messsage t where t.session_id = #{sessionId} order by
//                """
        return sql().createQuery(t).where(t.sessionId().eq(sessionId))
                .orderBy(t.createdTime().asc())
                .select(t)
                .limit(lastN)
                .execute();
    }

    default void deleteBySessionId(String sessionId){
        sql().createDelete(t).where(t.sessionId().eq(sessionId))
                .execute();
    }

}
