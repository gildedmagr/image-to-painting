package online.pechat;

import nu.pattern.OpenCV;
import org.opencv.core.Core;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
class Application {

    public static void main(String... args) {
        System.out.println(Core.NATIVE_LIBRARY_NAME);
        //System.loadLibrary( Core.NATIVE_LIBRARY_NAME );
        OpenCV.loadShared();
        System.out.println("Loaded OpenCV version " + Core.VERSION);
        SpringApplication.run(Application.class, args);

    }


}