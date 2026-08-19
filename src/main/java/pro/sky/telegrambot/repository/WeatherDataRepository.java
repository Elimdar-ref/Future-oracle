package pro.sky.telegrambot.repository;

import pro.sky.telegrambot.model.WeatherData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface WeatherDataRepository extends JpaRepository<WeatherData, Long> {

    @Query("SELECT w FROM WeatherData w WHERE w.city = :city ORDER BY w.recordedAt DESC")
    List<WeatherData> findLastNByCity(@Param("city") String city,
                                      org.springframework.data.domain.Pageable pageable);

    List<WeatherData> findByCityAndRecordedAtBetween(String city,
                                                     LocalDateTime start,
                                                     LocalDateTime end);
}