package ru.pechat55;

import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ru.pechat55.controllers.IndexController;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;

import static org.opencv.core.CvType.CV_8U;
import static org.opencv.core.CvType.CV_8UC4;
import static org.opencv.imgproc.Imgproc.INTER_AREA;


@SpringBootApplication
class Application {

    public static void main(String... args) {
        Bootstrap.start();
        OpenCV.loadShared();
        //System.loadLibrary( Core.NATIVE_LIBRARY_NAME );
        System.out.println("Loaded OpenCV version "+ Core.VERSION);
        //SpringApplication.run(Application.class, args);
       // IndexController.generateFiles();
    }


}