package datn.nht.dinesmart.repository;

import datn.nht.dinesmart.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByActiveTrueOrderBySortOrderAscIdAsc();

    List<Category> findAllByOrderBySortOrderAscIdAsc();

    boolean existsByNameIgnoreCase(String name);
}
