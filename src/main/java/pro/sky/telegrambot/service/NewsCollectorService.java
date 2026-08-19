package pro.sky.telegrambot.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import pro.sky.telegrambot.model.WeatherNews;
import pro.sky.telegrambot.repository.WeatherNewsRepository;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;

@Service
public class NewsCollectorService {

    private final WeatherNewsRepository repository;

    public NewsCollectorService(WeatherNewsRepository repository) {
        this.repository = repository;
    }

    @Scheduled(cron = "0 0 */6 * * *")
    public void collectNews() {
        System.out.println("📰 Сбор и сохранение новостей...");

        try {
            URL url = new URL("http://meteoinfo.ru/forecasts5000/russia/moscow-area/moscow");
            InputStream inputStream = url.openStream();

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(inputStream);
            doc.getDocumentElement().normalize();

            NodeList items = doc.getElementsByTagName("item");
            int count = 0;

            for (int i = 0; i < items.getLength() && count < 5; i++) {
                Node item = items.item(i);
                NodeList children = item.getChildNodes();

                String title = "";
                String description = "";
                String link = "";

                for (int j = 0; j < children.getLength(); j++) {
                    Node child = children.item(j);
                    String nodeName = child.getNodeName();
                    if ("title".equals(nodeName)) {
                        title = child.getTextContent();
                    } else if ("description".equals(nodeName)) {
                        description = child.getTextContent();
                    } else if ("link".equals(nodeName)) {
                        link = child.getTextContent();
                    }
                }

                if (title.isEmpty()) continue;

                // Создаем запись в БД
                WeatherNews news = new WeatherNews();
                news.setTitle(title);
                news.setDescription(description);
                news.setLink(link);
                news.setSource("Meteoinfo.ru");
                news.setPublishedAt(LocalDateTime.now());
                news.setSentimentScore(analyzeSentiment(title + " " + description));

                repository.save(news);
                count++;
                System.out.println("   ✓ " + title.substring(0, Math.min(50, title.length())) + "...");
            }

            System.out.println("   ✅ Сохранено " + count + " записей прогноза");

        } catch (Exception e) {
            System.err.println("   ❌ Ошибка: " + e.getMessage());
        }
    }

    private double analyzeSentiment(String text) {
        if (text == null) return 0;
        String lower = text.toLowerCase();
        double score = 0;

        if (lower.contains("солнце") || lower.contains("тепло") ||
                lower.contains("хорош") || lower.contains("ясно") ||
                lower.contains("без осадков")) score += 0.2;

        if (lower.contains("дождь") || lower.contains("шторм") ||
                lower.contains("холод") || lower.contains("снег") ||
                lower.contains("ураган") || lower.contains("наводнение") ||
                lower.contains("град")) score -= 0.2;

        return Math.max(-1, Math.min(1, score));
    }

    public void collectNow() {
        collectNews();
    }
}