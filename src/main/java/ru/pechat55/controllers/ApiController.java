package ru.pechat55.controllers;

import com.google.gson.Gson;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import ru.pechat55.models.RequestModel;
import ru.pechat55.models.UploadResponse;
import ru.pechat55.services.ImageService;

import java.util.List;

@RestController
@RequestMapping("/api/painting/")
public class ApiController {
    private static Logger logger = LoggerFactory.getLogger(ApiController.class);

    @Autowired
    ImageService imageService;

    /**
     * This endpoint generates 3D painting and preview for picture
     *
     * @param body input parameters
     * @return list of the url
     */
    @PostMapping("generate-preview")
    private Mono<List<String>> generatePreviews(@RequestBody RequestModel body) {

        logger.info("Generating previews for image: {}, request: {}", body.getUrl(), body);
        List<String> responseImages  = imageService.generatePreviews(body);

        System.out.println(responseImages);

        return Mono.just(responseImages);
    }
}
