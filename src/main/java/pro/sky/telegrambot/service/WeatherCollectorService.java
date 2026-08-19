package pro.sky.telegrambot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import pro.sky.telegrambot.model.WeatherData;
import pro.sky.telegrambot.repository.WeatherDataRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class WeatherCollectorService {

    @Value("${weather.api.key}")
    private String weatherApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WeatherDataRepository repository;

    private final List<String> cities = Arrays.asList("Москва", "Санкт-Петербург", "Екатеренбург");

    public WeatherCollectorService(WeatherDataRepository repository) {
        this.repository = repository;
    }

    // Сбор данных каждые 3 часа
    @Scheduled(cron = "0 0 */3 * * *")
    public void collectWeatherData() {
        System.out.println("🌤️ Начинаем сбор данных о погоде...");

        for (String city : cities) {
            try {
                String url = String.format(
                        "https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=metric",
                        city, weatherApiKey
                );

                String json = restTemplate.getForObject(url, String.class);
                JsonNode root = objectMapper.readTree(json);

                WeatherData data = new WeatherData();
                data.setCity(city);
                data.setRecordedAt(LocalDateTime.now());
                data.setTemperature(root.path("main").path("temp").asDouble());
                data.setHumidity(root.path("main").path("humidity").asDouble());
                data.setWeatherDescription(root.path("weather").get(0).path("description").asText());
                data.setSource("OpenWeatherMap");
                data.setRawData(json);

                repository.save(data);
                System.out.println("   ✓ " + city + ": " + data.getTemperature() + "°C");

            } catch (Exception e) {
                System.err.println("   ❌ Ошибка для " + city + ": " + e.getMessage());
            }
        }
        System.out.println("✅ Сбор данных завершен!");
    }
}