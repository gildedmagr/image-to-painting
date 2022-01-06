package online.pechat.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import online.pechat.models.PreviewResponseModel;
import online.pechat.models.RequestModel;
import online.pechat.services.ImageService;

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
    private Mono<PreviewResponseModel> generatePreviews(@RequestBody RequestModel body) {

        logger.info("Generating previews for image: {}, request: {}", body.getUrl(), body);
        PreviewResponseModel response  = imageService.generatePreviews(body);
        return Mono.just(response);
    }
}
