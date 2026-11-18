package datn.nht.dinesmart.repository;

import datn.nht.dinesmart.entity.Role;
import datn.nht.dinesmart.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByResetTokenHash(String resetTokenHash);

    List<User> findByRoleInOrderByIdDesc(Collection<Role> roles);
}
