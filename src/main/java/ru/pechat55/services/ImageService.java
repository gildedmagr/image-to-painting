package ru.pechat55.services;

import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.pechat55.models.InteriorModel;
import ru.pechat55.models.PaintingModel;
import ru.pechat55.utils.Utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ImageService {
    private static Logger logger = LoggerFactory.getLogger(ImageService.class);
    private static String PATH = "D:\\tmp\\";

    @Autowired
    HttpService httpService;


    public List<String> generatePreviews(String url, String originHost, int width, int height) {
        logger.info("Starting transformation...");
        long startTime = System.currentTimeMillis();
        List<String> responseImages = new ArrayList<>();

        Mat image = Utils.urlToMat(url);

        if (image != null) {

            PaintingModel paintingModel = new PaintingModel(width, height, image);

            Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2BGRA);

            AtomicBoolean isImageRotated = new AtomicBoolean(false);
            image = prepareImageForPainting(image, isImageRotated, width, height);

            Mat warpImage = imagePerspectiveTransform(image);

            try {
                Mat border = createPaintingBorder(image);
                create3DPainting(Utils.mat2BufferedImage(warpImage), Utils.mat2BufferedImage(border), originHost, responseImages);
                createInteriorWithPainting(paintingModel, isImageRotated, originHost, responseImages);
            } catch (IOException e) {
                logger.info("Problem to create 3D painting", e);
            }
        }

        long endTime = System.currentTimeMillis();

        logger.info("That took {} milliseconds", endTime - startTime);
        logger.info("Transformation finished...");
        return responseImages;
    }

    private Mat imagePerspectiveTransform(Mat image) {
        Point[] srcTri = new Point[4];
        srcTri[0] = new Point(0, 0);
        srcTri[1] = new Point(image.cols() - 1, 0);
        srcTri[2] = new Point(image.cols() - 1, image.rows() - 1);
        srcTri[3] = new Point(0, image.rows() - 1);


        Point[] dstTri = new Point[4];
        dstTri[0] = new Point(50, 40);
        dstTri[1] = new Point(image.cols() - 1, 0);
        dstTri[2] = new Point(image.cols() - 1, image.rows() - 1);
        dstTri[3] = new Point(50, image.rows() - 60);

        // write points
        //drawTransformPoints(filenameFinalWithPoints, image, srcTri, dstTri);

        Mat warpImage = Mat.zeros(image.rows(), image.cols(), CvType.CV_8U);
        Mat warpMat = Imgproc.getPerspectiveTransform(new MatOfPoint2f(srcTri), new MatOfPoint2f(dstTri));

        Imgproc.warpPerspective(image, warpImage, warpMat, warpImage.size(), Imgproc.INTER_CUBIC, Core.BORDER_TRANSPARENT, new Scalar(255, 255, 255, 255));
        Imgproc.line(warpImage, new Point(warpImage.width() - 1, 0), new Point(warpImage.width() - 1, warpImage.height()), new Scalar(255, 255, 255, 200), 2);
        return warpImage;
    }

    // crop and rotate image
    private Mat prepareImageForPainting(Mat image, AtomicBoolean isImageRotated, int width, int height) {
        float finalWidth = width * 10 * 3;
        float finalHeight = height * 10 * 3;
        Mat res = new Mat(image, new Rect(0, 0, image.cols(), image.rows()));


        // picture is in landscape mode or square
        if (image.rows() <= image.cols()) {
            logger.info("Picture is in landscape mode, size: {}x{}", image.cols(), image.rows());
            Core.rotate(image, res, Core.ROTATE_90_COUNTERCLOCKWISE);
            isImageRotated.set(true);
        }

        float max = Math.max(finalWidth / (float) res.width(), finalHeight / (float) res.height());
        Imgproc.resize(res, res, new Size(res.width() * max, res.height() * max), 0, 0, Imgproc.INTER_AREA);


        int offset;
        Rect rectCrop = null;
        // need to crop by width
        if (res.width() != finalWidth) {
            logger.info("crop by width");
            offset = (int) (res.width() - finalWidth) / 2;
            rectCrop = new Rect(offset, 0, res.width() - offset * 2, (int) finalHeight);
        } else { // crop by height
            logger.info("crop by height");
            offset = (int) (res.height() - finalHeight) / 2;
            rectCrop = new Rect(0, offset, (int) finalWidth, res.height() - offset * 2);
        }

        res = new Mat(res, rectCrop);

        //Imgcodecs.imwrite(PATH + "crop.jpg", res);
        return res;
    }

    // create border of the painting
    private static Mat createPaintingBorder(Mat image) {
        // copy image
        Mat border = new Mat(image, new Rect(0, 0, image.cols(), image.rows()));

        // border width
        int borderWidth = image.width() / 100 * 2;
        // create crop area
        Rect rectCrop = new Rect(border.width() - borderWidth, 0, borderWidth, border.height());
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
        Imgproc.warpPerspective(border, warpDst, warpMat, warpDst.size(), Imgproc.INTER_CUBIC, Core.BORDER_TRANSPARENT, new Scalar(255, 255, 255, 255));
        return warpDst;
    }

    // put the painting to interiors
    private void createInteriorWithPainting(PaintingModel paintingModel, AtomicBoolean isImageRotated,
                                            String originHost, List<String> responseImages) {
        List<InteriorModel> walls = new ArrayList<>();
        //walls.add(new InteriorModel("/images/textured-wall-finishes.jpg", 270, 150, 200));
        //walls.add(new InteriorModel("/images/wall.jpg", 1445, 424, 200));
        walls.add(new InteriorModel("/wall1.jpg", 860, 410, 200));
        walls.add(new InteriorModel("/wall2.jpg", 830, 340, 200));
        walls.add(new InteriorModel("/wall3.jpg", 1200, 350, 200));
        AtomicInteger index = new AtomicInteger();

        walls.forEach(interiorWall ->
                drawPictureOnWall(paintingModel, index, interiorWall, isImageRotated, originHost)
                        .ifPresent(responseImages::add)
        );

    }

    // draw picture on the wall using the coordinates and calculated size
    private Optional<String> drawPictureOnWall(PaintingModel paintingModel, AtomicInteger index,
                                               InteriorModel interiorWall, AtomicBoolean isImageRotated, String originHost) {
        // get IS from resource directory
        InputStream is = ImageService.class.getResourceAsStream(interiorWall.getImageName());

        if (is == null) {
            logger.error("Can't retrieve input stream for image: {}", interiorWall.getImageName());
            return Optional.empty();
        }

        // read image
        BufferedImage wall = null;
        try {
            wall = ImageIO.read(is);
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }

        // create the new image
        int w = wall.getWidth();
        int h = wall.getHeight();
        BufferedImage combined = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        // clone cropped image
        Mat croppedImage = paintingModel.getImage().clone();
        logger.info("Image was rotated: {}", isImageRotated);
        if (isImageRotated.get()) {
            //Core.rotate(croppedImage, croppedImage, Core.ROTATE_90_CLOCKWISE);
        }

        logger.info("Picture height: {}", croppedImage.height());
        float percentOfNumber = (float) paintingModel.getHeight() / interiorWall.getWallHeight() * 100;
        if (percentOfNumber >= 50) {
            percentOfNumber *= 0.7;
        }
        float pixelsForPicture = (float) h / 100 * percentOfNumber;
        float k = pixelsForPicture / croppedImage.height();

        logger.info("K for resize is: {}", k);


        logger.info("Final height: {}, width: {}", croppedImage.height() * k, croppedImage.width() * k);
        // resize according to the interior
        Imgproc.resize(croppedImage, croppedImage, new Size(croppedImage.width() * k, croppedImage.height() * k), 0, 0, Imgproc.INTER_AREA);

        // convert to buffered image
        BufferedImage picture;
        try {
            picture = Utils.mat2BufferedImage(croppedImage);
        } catch (Exception e) {
            logger.error("Can't convert OpenCV matrix to buffered image", e);
            return Optional.empty();
        }
        // create shadow image
        BufferedImage shadow = createDropShadow(picture, 10);

        // calculate position for picture on the wall
        int pictureX = interiorWall.getDowelX() - picture.getWidth() / 2;
        int pictureY = interiorWall.getDowelY() - picture.getHeight() / 2;

        // calculate position for the shadow
        int shadowX = pictureX - (shadow.getWidth() - picture.getWidth()) / 2;
        int shadowY = pictureY - (shadow.getHeight() - picture.getHeight()) / 2;


        // paint picture and shadow on the wall
        Graphics g = combined.getGraphics();
        g.drawImage(wall, 0, 0, null);
        g.drawImage(shadow, shadowX, shadowY, shadow.getWidth(), shadow.getHeight(), null);
        g.drawImage(picture, pictureX, pictureY, picture.getWidth(), picture.getHeight(), null);
        g.dispose();
        //ImageIO.write(combined, "PNG", new File(PATH, "final-interior-" + (index.getAndIncrement()) + ".png"));
        return httpService.upload(originHost, combined);
    }


    public void create3DPainting(BufferedImage image, BufferedImage border, String originHost, List<String> responseImages) {
        // create the new image, canvas size is the max. of both image sizes
        int w = Math.max(image.getWidth(), border.getWidth());
        int h = Math.max(image.getHeight(), border.getHeight());
        BufferedImage combined = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        // paint both images, preserving the alpha channels
        Graphics2D g = combined.createGraphics();
        g.drawImage(image, 10, 10, null);
        g.drawImage(border, w + 10, 10, null);
        g.dispose();

        BufferedImage shadow = createDropShadow(combined, 30);

        g = shadow.createGraphics();

        g.drawImage(image, (shadow.getWidth() - combined.getWidth()) / 2, (shadow.getHeight() - combined.getHeight()) / 2, null);
        g.drawImage(border, w + (shadow.getWidth() - combined.getWidth()) / 2, (shadow.getHeight() - combined.getHeight()) / 2, null);

        g.dispose();

        Optional<String> fileUrl = httpService.upload(originHost, shadow);
        fileUrl.ifPresent(responseImages::add);
        //ImageIO.write(shadow, "PNG", new File(PATH, "painting-3d.png"));
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
}
