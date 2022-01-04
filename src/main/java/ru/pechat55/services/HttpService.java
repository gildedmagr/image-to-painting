package ru.pechat55.services;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import ru.pechat55.models.ResponseFile;
import ru.pechat55.models.UploadResponse;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Service
public class HttpService {
    private static Logger logger = LoggerFactory.getLogger(HttpService.class);
    public static final String ROUTE_UPLOAD = "/index.php?route=tool/upload";

    @Autowired
    private WebClient.Builder webClientBuilder;

    public Optional<String> upload(String host, BufferedImage bufferedImage) {
        final WebClient webClient = webClientBuilder.build();
        if (!host.startsWith("https://")) {
            host = "https://" + host;
        }
        String url = host + ROUTE_UPLOAD;
        logger.info("Preparing request to URL: {}", url);
        String response = webClient.post()
                .uri(url)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(fromFile(bufferedImage)))
                .exchangeToMono(resp -> resp.bodyToMono(String.class))
                .block();

        logger.info("Response: {}", response);
        UploadResponse uploadResponse = new Gson().fromJson(response, UploadResponse.class);
        if (uploadResponse.getError().size() > 0) {
            uploadResponse.getError().forEach(e -> logger.error("Error during uploading image to server, error: {}", e));
            return Optional.empty();
        }

        return uploadResponse.getFiles().stream()
                .map(ResponseFile::getFile)
                .findFirst();
    }

    public MultiValueMap<String, HttpEntity<?>> fromFile(BufferedImage bufferedImage) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(bufferedImage, "PNG", baos);
            baos.flush();
        } catch (IOException e) {
            logger.error("Can't convert buffered image to input stream", e);
        }

        String header = String.format("form-data; name=%s; filename=%s", "file", UUID.randomUUID() + ".png");

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", new ByteArrayResource(baos.toByteArray())).header("Content-Disposition", header);
        return builder.build();
    }
}
