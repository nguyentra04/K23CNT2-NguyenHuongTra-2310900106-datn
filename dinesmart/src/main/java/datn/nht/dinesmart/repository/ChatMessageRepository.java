package datn.nht.dinesmart.repository;

import datn.nht.dinesmart.entity.ChatMessage;
import datn.nht.dinesmart.entity.ChatRole;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    long countBySessionTokenAndRole(String sessionToken, ChatRole role);

    List<ChatMessage> findBySessionTokenAndRoleInOrderByIdDesc(String sessionToken, Collection<ChatRole> roles, Pageable page);

    List<ChatMessage> findAllByOrderByIdDesc(Pageable page);

    List<ChatMessage> findByRoleOrderByIdDesc(ChatRole role, Pageable page);
}
