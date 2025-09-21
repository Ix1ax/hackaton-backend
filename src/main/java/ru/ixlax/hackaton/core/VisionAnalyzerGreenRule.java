package ru.ixlax.hackaton.core;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ru.ixlax.hackaton.domain.entity.Camera;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "app.camera.monitor.analyzer", havingValue = "green")
public class VisionAnalyzerGreenRule implements VisionAnalyzer {

    @Value("${app.camera.monitor.snapshot-base:http://gateway/snapshots/}")
    String snapshotBase;

    @Override
    public Optional<String> detect(Camera c, String userPrompt, String snapshotUrl) {
        try {
            String slug = (c.getExternalId()!=null && !c.getExternalId().isBlank())
                    ? c.getExternalId() : ("cam" + c.getId());

            String url = (snapshotUrl != null && !snapshotUrl.isBlank())
                    ? snapshotUrl
                    : snapshotBase + URLEncoder.encode(slug + ".jpg", StandardCharsets.UTF_8);

            BufferedImage img = ImageIO.read(new URL(url));
            if (img == null) return Optional.empty();

            int w = img.getWidth(), h = img.getHeight();
            long total = 0, greenish = 0;

            for (int y = 0; y < h; y += 2) {
                for (int x = 0; x < w; x += 2) {
                    int rgb = img.getRGB(x, y);
                    int r = (rgb >> 16) & 0xff;
                    int g = (rgb >> 8) & 0xff;
                    int b = (rgb) & 0xff;
                    total++;
                    if (g > 120 && g > r + 20 && g > b + 20) greenish++;
                }
            }

            double ratio = (total == 0) ? 0.0 : (double) greenish / (double) total;
            if (ratio > 0.25) { // порог можно крутить
                return Optional.of("vision: green-dominant ~" + Math.round(ratio * 100) + "%");
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}