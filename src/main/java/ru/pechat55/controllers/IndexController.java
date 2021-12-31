package ru.pechat55.controllers;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.pechat55.utils.Utils;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.DataBufferByte;
import java.awt.image.Kernel;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

@RestController
@RequestMapping("/employees")
public class IndexController {
    private  static  String PATH = "/Users/andriiprotsenko/Documents/";
   private static Logger logger = LoggerFactory.getLogger(IndexController.class);

    public IndexController() {

    }

    @GetMapping(value = "/{id}", produces = MediaType.TEXT_PLAIN_VALUE)
    private Mono<String> getEmployeeById(@PathVariable String id) {
       /* UUID.randomUUID();
        BufferedImage originalImage = ImageIO.read(new File(PATH, "test.jpg"));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write( originalImage, "jpg", baos );
        baos.flush();
        byte[] imageInByte = baos.toByteArray();
        baos.close();
        ByteBuffer buf = ByteBuffer.wrap(imageInByte);
        return ServerResponse
                .ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(BodyInserters.fromDataBuffers(Flux.just(buf));*/
        return Mono.just(id);
    }

    public static void generateFiles(){
        //PATH = "/code/";
        logger.info("Starting transformation...");
        long startTime = System.currentTimeMillis();
        String filename = PATH + "test.jpg";
        String filenameFinal = PATH + "result.png";
        String filenameShadow = PATH + "shadow.png";
        String filenameBorder = PATH + "border.png";
        String filenameFinalWithPoints = "C:\\Users\\Andriy\\IdeaProjects\\pechat-canvas\\src\\main\\resources\\points.jpeg";

        Mat image = Imgcodecs.imread(filename);
        /*BufferedImage bufferedImage = null;
        try {
            bufferedImage = ImageIO.read(new File(PATH, "test.jpg"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] pixels = ((DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData();

        logger.info(bufferedImage.getWidth()+" "+bufferedImage.getHeight());
        Mat image =  Mat.zeros(bufferedImage.getWidth(), bufferedImage.getHeight(), CvType.CV_8U);
        //image.put(bufferedImage.getWidth(), bufferedImage.getHeight(), pixels);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            ImageIO.write(bufferedImage, "jpg", byteArrayOutputStream);
            byteArrayOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }


        image = Imgcodecs.imdecode(new MatOfByte(byteArrayOutputStream.toByteArray()), Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);*/
        //logger.info(image.dump());
        if (image != null) {

            Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2BGRA);
            //Imgproc.GaussianBlur(image, image, new Size(3, 3), 10);
            image = prepareImageForPainting(image);

            Point[] srcTri = new Point[4];
            srcTri[0] = new Point(0, 0);
            srcTri[1] = new Point(image.cols() - 1, 0);
            srcTri[2] = new Point(image.cols() - 1, image.rows() - 1);
            srcTri[3] = new Point(0, image.rows() - 1);


            Point[] dstTri = new Point[4];
            dstTri[0] = new Point(0, 40);
            dstTri[1] = new Point(image.cols() - 1, 0);
            dstTri[2] = new Point(image.cols() - 1, image.rows() - 1);
            dstTri[3] = new Point(0, image.rows() - 60);

            // write points
            //drawTransformPoints(filenameFinalWithPoints, image, srcTri, dstTri);

            Mat warpDst = Mat.zeros(image.rows(), image.cols(), CvType.CV_8U);

            //Mat warpMat = Imgproc.getAffineTransform( new MatOfPoint2f(srcTri), new MatOfPoint2f(dstTri) );
            Mat warpMat = Imgproc.getPerspectiveTransform(new MatOfPoint2f(srcTri), new MatOfPoint2f(dstTri));


            //Imgproc.warpAffine( image, warpDst, warpMat, warpDst.size() );

            Imgproc.warpPerspective(image, warpDst, warpMat, warpDst.size(), Imgproc.INTER_CUBIC, Core.BORDER_TRANSPARENT, new Scalar(0, 0, 0, 255));
            Imgproc.line(warpDst, new Point(warpDst.width() - 1, 0), new Point(warpDst.width() - 1, warpDst.height()), new Scalar(125, 125, 125, 200), 1);

            // Imgproc.resize(image, warpDst, new Size(100,100), 0, 0, INTER_AREA);

            Mat border = createPaintingBorder(image);



            System.out.println(warpDst.channels());

            Imgcodecs.imwrite(filenameFinal, warpDst);
            Imgcodecs.imwrite(filenameBorder, border);

            try{
                create3DPainting();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        createInteriorWithPainting();
        long endTime = System.currentTimeMillis();

        logger.info("That took {} seconds", (endTime - startTime) / 1000);
        logger.info("Transformation finished...");
    }

    // create border of the painting
    private static Mat createPaintingBorder(Mat image) {
        // copy image
        Mat border = new Mat(image, new Rect(0, 0, image.cols(), image.rows()));

        // create crop area
        Rect rectCrop = new Rect(border.width() - 20, 0, 20, border.height());
        // crop image
        border = new Mat(border, rectCrop);
        // flip horizontally
        Core.flip(border, border, 1);
        // decrease brightness
        border.convertTo(border, -1, 1, -20);

        // perspective transformation points
        Point[] srcTri = new Point[4];
        srcTri[0] = new Point(0, 0);
        srcTri[1] = new Point(border.cols() - 1, 0);
        srcTri[2] = new Point(border.cols() - 1, border.rows() - 1);
        srcTri[3] = new Point(0, border.rows() - 1);

        Point[] dstTri = new Point[4];
        dstTri[0] = new Point(0, 0);
        dstTri[1] = new Point(border.cols() - 1, 20);
        dstTri[2] = new Point(border.cols() - 1, border.rows() - 30);
        dstTri[3] = new Point(0, border.rows() - 1);


        // perspective transformation
        Mat warpMat = Imgproc.getPerspectiveTransform(new MatOfPoint2f(srcTri), new MatOfPoint2f(dstTri));
        Mat warpDst = Mat.zeros(border.rows(), border.cols(), CvType.CV_8U);
        Imgproc.warpPerspective(border, warpDst, warpMat, warpDst.size(), Imgproc.INTER_CUBIC, Core.BORDER_TRANSPARENT, new Scalar(0, 0, 0, 255));
        return warpDst;
    }

    // put the painting to interiors
    private static void createInteriorWithPainting() {
        try {
            BufferedImage wall = ImageIO.read(new File("/Users/andriiprotsenko/IdeaProjects/image-to-painting/src/main/resources/images/textured-wall-finishes.jpg"));
            BufferedImage picture = ImageIO.read(new File(PATH, "test.jpg"));

            // create the new image, canvas size is the max. of both image sizes
            int w = wall.getWidth();
            int h = wall.getHeight();
            BufferedImage combined = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

            int k = 10;

            // paint both images, preserving the alpha channels
            Graphics g = combined.getGraphics();
            g.drawImage(wall, 0, 0, null);
            g.drawImage(picture, 150, 50, picture.getWidth() / k, picture.getHeight() / k, null);
            g.dispose();

            ImageIO.write(combined, "PNG", new File(PATH, "final-interior.jpg"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // crop and rotate image
    private static Mat prepareImageForPainting(Mat image) {
        float finalWidth = 300;
        float finalHeight = 400;
        Mat res = new Mat(image, new Rect(0, 0, image.cols(), image.rows()));

        // picture is in portrait mode or square
        if(image.rows() >= image.cols()){
            System.out.println("Portrait");
        }else{
            System.out.println("Landscape");

            Core.rotate(image, res, Core.ROTATE_90_COUNTERCLOCKWISE);

            float k = res.width() / finalWidth;

            Imgproc.resize(res, res, new Size(res.width() / k, res.height() / k), 0, 0,  Imgproc.INTER_AREA);

            //Rect rectCrop = new Rect((res.width() / 2) - (300 / res.width()), (res.height() / 2) - (400 / res.height()), 300, 400);
            Rect rectCrop = new Rect(0, 25, (int) finalWidth, res.height() - 50);
            //res = new Mat(res, rectCrop);
            //Core.rotate(res, res, Core.ROTATE_90_CLOCKWISE);
        }

        return res;
    }

    public static void create3DPainting() throws IOException {
        BufferedImage image = ImageIO.read(new File(PATH, "result.png"));
        BufferedImage border = ImageIO.read(new File(PATH, "border.png"));

        // create the new image, canvas size is the max. of both image sizes
        int w = Math.max(image.getWidth(), border.getWidth());
        int h = Math.max(image.getHeight(), border.getHeight());
        BufferedImage combined = new BufferedImage(w , h , BufferedImage.TYPE_INT_ARGB);

        // paint both images, preserving the alpha channels
        Graphics g = combined.getGraphics();
        g.drawImage(image, 10, 10, null);
        g.drawImage(border, w + 10, 10, null);
        g.dispose();

        BufferedImage shadow = createDropShadow(combined, 30);

        g = shadow.getGraphics();
        g.drawImage(image, (shadow.getWidth() - combined.getWidth()) / 2, (shadow.getHeight() - combined.getHeight()) / 2, null);
        g.drawImage(border, w + (shadow.getWidth() - combined.getWidth()) / 2, (shadow.getHeight() - combined.getHeight()) / 2, null);
        g.dispose();

        ImageIO.write(shadow, "PNG", new File(PATH, "painting-3d.png"));
    }

    /**
     * Create an smooth drop shadow for a given image
     *
     * @param image given image
     * @param size  size of shadow
     * @return BufferedImage   new image with drop shadows
     */
    public static BufferedImage createDropShadow(BufferedImage image,
                                                 int size) {
        BufferedImage shadow = new BufferedImage(image.getWidth() + 4 * size, image.getHeight() + 4 * size, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2 = shadow.createGraphics();
        g2.drawImage(image, size * 2, size * 2, null);

        // composite
        g2.setComposite(AlphaComposite.SrcIn);
        g2.setColor(Color.DARK_GRAY);
        g2.fillRoundRect(0, 0, shadow.getWidth(), shadow.getHeight(), 30, 30);

        g2.dispose();

        shadow = Utils.getGaussianBlurFilter(size, true).filter(shadow, null);
        shadow = Utils.getGaussianBlurFilter(size, false).filter(shadow, null);

        return shadow;

    }




    private static void drawTransformPoints(String filenameFinalWithPoints, Mat image, Point[] srcTri, Point[] dstTri) {
        Mat imageWithPoints = Mat.zeros(image.rows(), image.cols(), image.type());
        Imgproc.circle(imageWithPoints, srcTri[0], 5, new Scalar(255, 0, 0), 2, Imgproc.LINE_8, 0);
        Imgproc.circle(imageWithPoints, srcTri[1], 5, new Scalar(255, 0, 0), 2, Imgproc.LINE_8, 0);
        Imgproc.circle(imageWithPoints, srcTri[2], 5, new Scalar(255, 0, 0), 2, Imgproc.LINE_8, 0);
        Imgproc.circle(imageWithPoints, srcTri[3], 5, new Scalar(255, 0, 0), 2, Imgproc.LINE_8, 0);

        Imgproc.circle(imageWithPoints, dstTri[0], 5, new Scalar(0, 0, 255), 2, Imgproc.LINE_8, 0);
        Imgproc.circle(imageWithPoints, dstTri[1], 5, new Scalar(0, 0, 255), 2, Imgproc.LINE_8, 0);
        Imgproc.circle(imageWithPoints, dstTri[2], 5, new Scalar(0, 0, 255), 2, Imgproc.LINE_8, 0);
        Imgproc.circle(imageWithPoints, dstTri[3], 5, new Scalar(0, 0, 255), 2, Imgproc.LINE_8, 0);
        Imgcodecs.imwrite(filenameFinalWithPoints, imageWithPoints);
    }
}
