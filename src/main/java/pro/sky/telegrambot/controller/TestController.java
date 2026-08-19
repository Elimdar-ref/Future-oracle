package pro.sky.telegrambot.controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pro.sky.telegrambot.model.WeatherData;
import pro.sky.telegrambot.repository.WeatherDataRepository;

import java.util.List;

@RestController
public class TestController {

    private final WeatherDataRepository repository;

    public TestController(WeatherDataRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/test")
    public String test() {
        return "✅ Future Oracle работает! Записей в БД: " + repository.count();
    }

    @GetMapping("/test/data")
    public List<WeatherData> showData() {
        return repository.findAll();
    }
}