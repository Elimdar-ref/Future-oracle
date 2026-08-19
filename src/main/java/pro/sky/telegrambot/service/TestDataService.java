package pro.sky.telegrambot.service;

import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.WeatherData;
import pro.sky.telegrambot.repository.WeatherDataRepository;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;

@Service
public class TestDataService {

    private final WeatherDataRepository repository;

    public TestDataService(WeatherDataRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void init() {
        if (repository.count() > 0) {
            System.out.println("📊 Данные уже есть в БД: " + repository.count() + " записей");
            return;
        }

        WeatherData data = new WeatherData();
        data.setCity("Москва");
        data.setRecordedAt(LocalDateTime.now());
        data.setTemperature(21.5);
        data.setHumidity(60.0);
        data.setWeatherDescription("Солнечно");
        data.setSource("Test");

        repository.save(data);
        System.out.println("✅ Тестовые данные сохранены в БД!");

        long count = repository.count();
        System.out.println("📊 Всего записей в БД: " + count);
    }
}