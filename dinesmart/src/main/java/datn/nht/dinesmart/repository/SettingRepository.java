package datn.nht.dinesmart.repository;

import datn.nht.dinesmart.entity.RestaurantSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingRepository extends JpaRepository<RestaurantSetting, Integer> {
}
