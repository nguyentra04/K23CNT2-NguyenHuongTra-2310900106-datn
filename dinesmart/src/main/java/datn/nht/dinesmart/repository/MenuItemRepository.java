package datn.nht.dinesmart.repository;

import datn.nht.dinesmart.entity.MenuItem;
import datn.nht.dinesmart.entity.MenuStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    @Query("select m from MenuItem m join fetch m.category where m.id in :ids")
    List<MenuItem> findAllWithCategory(@Param("ids") Collection<Long> ids);

    @Query("select m from MenuItem m join fetch m.category where m.id = :id")
    Optional<MenuItem> findWithCategory(@Param("id") Long id);

    boolean existsByCategoryIdAndNameIgnoreCase(Long categoryId, String name);

    long countByCategoryId(Long categoryId);

    List<MenuItem> findByStatusNot(MenuStatus status);
}
