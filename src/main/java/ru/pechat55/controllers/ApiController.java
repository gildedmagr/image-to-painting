package ru.pechat55.controllers;

import com.google.gson.Gson;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import reactor.core.publisher.Mono;
import ru.pechat55.services.ImageService;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/painting/")
public class ApiController {
    private static Logger logger = LoggerFactory.getLogger(ApiController.class);

    @Autowired
    ImageService imageService;

    @PostMapping("generate-preview")
    private Mono<List<String>> generatePreviews(@RequestBody Object body) {
        String jsonInString = new Gson().toJson(body);
        JSONObject mJSONObject = new JSONObject(jsonInString);
        String imageUrl = String.valueOf(mJSONObject.get("url"));
        String originHost = String.valueOf(mJSONObject.get("host"));
        int width = Integer.parseInt(mJSONObject.get("width").toString());
        int height = Integer.parseInt(mJSONObject.get("height").toString());

        logger.info("Generating previews for image: {}, request: {}, host: {}", imageUrl, body, originHost);
        List<String> responseImages  = imageService.generatePreviews(imageUrl, originHost, width, height);

        System.out.println(responseImages);

        return Mono.just(responseImages);
    }

    private void validateBody(){

    }
}
