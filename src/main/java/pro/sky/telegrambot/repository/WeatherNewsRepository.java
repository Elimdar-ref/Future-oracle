package pro.sky.telegrambot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pro.sky.telegrambot.model.WeatherNews;

import java.util.List;

public interface WeatherNewsRepository extends JpaRepository<WeatherNews, Long> {
    List<WeatherNews> findTop10ByOrderByPublishedAtDesc();
}