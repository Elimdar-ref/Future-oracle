package pro.sky.telegrambot.controller;

import org.springframework.web.bind.annotation.*;
import pro.sky.telegrambot.model.WeatherData;
import pro.sky.telegrambot.model.WeatherNews;
import pro.sky.telegrambot.repository.WeatherDataRepository;
import pro.sky.telegrambot.repository.WeatherNewsRepository;
import pro.sky.telegrambot.service.NewsCollectorService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class PredictionController {

    private final WeatherDataRepository weatherRepository;
    private final WeatherNewsRepository newsRepository;
    private final NewsCollectorService newsCollector;

    public PredictionController(WeatherDataRepository weatherRepository,
                                WeatherNewsRepository newsRepository,
                                NewsCollectorService newsCollector) {
        this.weatherRepository = weatherRepository;
        this.newsRepository = newsRepository;
        this.newsCollector = newsCollector;
    }

    @GetMapping("/cities")
    public List<String> getCities() {
        return weatherRepository.findAll().stream()
                .map(WeatherData::getCity)
                .distinct()
                .collect(Collectors.toList());
    }

    @GetMapping("/predict/{city}")
    public Map<String, Object> getPrediction(@PathVariable String city) {
        Map<String, Object> result = new HashMap<>();

        // ПОГОДА из БД
        List<WeatherData> history = weatherRepository.findByCityAndRecordedAtBetween(
                city,
                LocalDateTime.now().minusDays(30),
                LocalDateTime.now()
        );

        if (history.isEmpty()) {
            result.put("error", "Нет данных для " + city);
            return result;
        }

        List<WeatherData> recent = history.stream().limit(7).collect(Collectors.toList());

        double firstTemp = recent.get(recent.size() - 1).getTemperature();
        double lastTemp = recent.get(0).getTemperature();
        double trend = lastTemp - firstTemp;
        double avgTemp = recent.stream().mapToDouble(WeatherData::getTemperature).average().orElse(0);
        double predictedTemp = avgTemp + (trend * 0.3);

        // НОВОСТИ ИЗ БД
        List<WeatherNews> news = newsRepository.findTop10ByOrderByPublishedAtDesc();
        double avgSentiment = news.stream()
                .mapToDouble(WeatherNews::getSentimentScore)
                .average()
                .orElse(0);

        // УВЕРЕННОСТЬ
        double confidence = Math.min(0.95, 0.3 + (history.size() / 100.0));
        confidence = Math.max(0.1, Math.min(0.95, confidence + (avgSentiment * 0.1)));

        // РИСК
        String riskLevel = confidence > 0.7 ? "LOW" : confidence > 0.4 ? "MEDIUM" : "HIGH";
        String riskReason = confidence > 0.7 ?
                "Достаточно данных (" + history.size() + " записей)" :
                "Мало данных (" + history.size() + " записей)";

        // ОТВЕТ
        result.put("city", city);
        result.put("predictedDate", LocalDate.now().plusDays(1).toString());
        result.put("predictedTemperature", Math.round(predictedTemp * 10) / 10.0);
        result.put("currentTemperature", Math.round(lastTemp * 10) / 10.0);
        result.put("confidence", Math.round(confidence * 100));
        result.put("riskLevel", riskLevel);
        result.put("riskReason", riskReason);
        result.put("arguments", String.format(
                "Температура %s (%.1f°C → %.1f°C). Средняя: %.1f°C. " +
                        "Новостей: %d, тональность: %.2f",
                trend > 0 ? "повышается" : "понижается",
                firstTemp, lastTemp, avgTemp,
                news.size(), avgSentiment
        ));
        result.put("dataPoints", history.size());
        result.put("newsCount", news.size());
        result.put("newsSentiment", Math.round(avgSentiment * 100) / 100.0);

        return result;
    }

    @GetMapping("/collect-news")
    public String collectNewsNow() {
        newsCollector.collectNow();
        return "✅ Новости собраны и сохранены в БД!";
    }
}
