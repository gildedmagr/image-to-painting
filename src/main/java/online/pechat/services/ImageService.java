package online.pechat.services;

import online.pechat.models.PaintingModel;
import online.pechat.models.PreviewResponseModel;
import online.pechat.utils.Utils;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import online.pechat.models.InteriorModel;
import online.pechat.models.RequestModel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ImageService {
    private static Logger logger = LoggerFactory.getLogger(ImageService.class);

    /**
     * Creates pseudo 3D picture from 2D and generates designs with painting
     *
     * @param requestParam - the input parameters
     * @return list with links to generated images
     */
    public PreviewResponseModel generatePreviews(RequestModel requestParam) {
        logger.info("Starting transformation...");
        long startTime = System.currentTimeMillis();
        List<String> responseImages = new ArrayList<>();

        String uid = requestParam.getUid() == null || "".equals(requestParam.getUid()) ? UUID.randomUUID().toString() : requestParam.getUid();

        String imagePath = requestParam.getHost() + File.separator + requestParam.getUrl();
        logger.info("Reading image, path: {}", imagePath);
        Mat image = Imgcodecs.imread(imagePath);
        logger.info("Image has been read, converting to RGBA");
        Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2BGRA);

        AtomicBoolean isImageRotated = new AtomicBoolean(false);

        // crop and rotate image
        logger.info("Going to crop, resize and rotate image");
        Mat preparedImage = prepareImageForPainting(image, isImageRotated, requestParam.getHost(), uid, requestParam.getWidth(), requestParam.getHeight());

        // create model with prepared image
        PaintingModel paintingModel = new PaintingModel(requestParam.getWidth(), requestParam.getHeight(), preparedImage);

        logger.info("Create pictures with interior and painting");
        createInteriorWithPainting(paintingModel, isImageRotated, requestParam.getHost(), uid, responseImages);
        logger.info("Created pictures with interior and painting, number of interiors: {}", responseImages.size());

        logger.info("Create 3D painting");
        create3DPainting(preparedImage, requestParam.getHost(), uid, responseImages);
        logger.info("Created 3D painting");

        long endTime = System.currentTimeMillis();

        logger.info("That took {} milliseconds", endTime - startTime);
        logger.info("Transformation finished...");
        Collections.reverse(responseImages);
        return new PreviewResponseModel(uid, responseImages);
    }

    // crop and rotate image
    private Mat prepareImageForPainting(Mat image, AtomicBoolean isImageRotated, String originHost, String productId, int width, int height) {
        // get biggest side
        float maxSide = Math.min(width, height);

        // calculate width and height using bounding rectangle
        float finalWidth = width / (maxSide / 500);//width * 10 * 3; 80 * 10 * 3 = 2400
        float finalHeight = height / (maxSide / 500);//height * 10 * 3; 100 * 10 * 3 = 3000

        // create result matrix
        Mat res = new Mat(image, new Rect(0, 0, image.cols(), image.rows()));

        // picture is in landscape mode or square
        // rotate it
        if (image.rows() <= image.cols()) {
            logger.info("Picture is in landscape mode, size: {}x{}", image.cols(), image.rows());
            Core.rotate(image, res, Core.ROTATE_90_COUNTERCLOCKWISE);
            isImageRotated.set(true);
        }

        // get coefficient for resize image
        float max = Math.max(finalWidth / (float) res.width(), finalHeight / (float) res.height());
        int resizeWidth = (int)Math.ceil(res.width() * max);
        int resizeHeight = (int)Math.ceil(res.height() * max);

        logger.info("Resize image: {}x{}", resizeWidth, resizeHeight);
        // resize image
        Imgproc.resize(res, res, new Size(resizeWidth, resizeHeight), 0, 0, Imgproc.INTER_CUBIC);


        int offset;
        Rect rectCrop;
        // crop image by center
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
        logger.info("Crop area: {}", rectCrop);
        Mat croppedMat = new Mat(res, rectCrop);
        // rotate image back if it was rotated in previous step
        if(isImageRotated.get()){
            Core.rotate(croppedMat, res, Core.ROTATE_90_CLOCKWISE);
            return res;
        }
        return croppedMat;
    }

    // transform image using warp perspective
    private Mat imagePerspectiveTransform(Mat image) {
        // original points
        Point[] srcTri = new Point[4];
        srcTri[0] = new Point(0, 0);
        srcTri[1] = new Point(image.cols() - 1, 0);
        srcTri[2] = new Point(image.cols() - 1, image.rows() - 1);
        srcTri[3] = new Point(0, image.rows() - 1);

        // transform points
        int warpByXAxis = 50;
        int warpByYAxisTop = 40;
        int warpByYAxisBottom = 60;
        Point[] dstTri = new Point[4];
        dstTri[0] = new Point(warpByXAxis, warpByYAxisTop);
        dstTri[1] = new Point(image.cols() - 1, 0);
        dstTri[2] = new Point(image.cols() - 1, image.rows() - 1);
        dstTri[3] = new Point(warpByXAxis, image.rows() - warpByYAxisBottom);

        // get transform matrix
        Mat warpMat = Imgproc.getPerspectiveTransform(new MatOfPoint2f(srcTri), new MatOfPoint2f(dstTri));

        // create empty final matrix
        Mat warpImage = Mat.zeros(image.rows(), image.cols(), CvType.CV_16U);

        // perform warp transformation
        Imgproc.warpPerspective(image, warpImage, warpMat, warpImage.size(), Imgproc.INTER_CUBIC, Core.BORDER_CONSTANT, new Scalar(255, 255, 255, 255));

        // draw line on right edge
        Imgproc.line(warpImage, new Point(warpImage.width() - 1, 0), new Point(warpImage.width() - 1, warpImage.height()), new Scalar(255, 255, 255, 50), 1);

        // remove white area after transformation
        Rect rectCrop = new Rect(warpByXAxis, 0, image.width() - warpByXAxis, image.rows());
        return new Mat(warpImage, rectCrop);
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
        border.convertTo(border, -1, 0.8, -20);

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

        // empty final matrix
        Mat warpDst = Mat.zeros(border.rows(), border.cols(), CvType.CV_16U);

        // warp transformation
        Imgproc.warpPerspective(border, warpDst, warpMat, warpDst.size(), Imgproc.INTER_CUBIC, Core.BORDER_CONSTANT, new Scalar(255, 255, 255, 255));

        return warpDst;
    }

    // put the painting to interiors
    private void createInteriorWithPainting(PaintingModel paintingModel, AtomicBoolean isImageRotated,
                                            String originHost, String productId, List<String> responseImages) {
        List<InteriorModel> walls = new ArrayList<>();

        walls.add(new InteriorModel("/wall1.jpg", 860, 410, 250));
        walls.add(new InteriorModel("/wall2.jpg", 830, 340, 250));
        walls.add(new InteriorModel("/wall3.jpg", 1200, 350, 250));
        AtomicInteger index = new AtomicInteger();

        walls.forEach(interiorWall ->
                drawPictureOnWall(paintingModel, index, interiorWall, isImageRotated, originHost, productId)
                        .ifPresent(responseImages::add)
        );

    }

    // draw picture on the wall using the coordinates and calculated size
    private Optional<String> drawPictureOnWall(PaintingModel paintingModel, AtomicInteger index,
                                               InteriorModel interiorWall, AtomicBoolean isImageRotated,
                                               String originHost, String productId) {
        logger.info("Creating interior: {} with painting", interiorWall.getImageName());
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
            logger.error("Can't read interior picture, error: {}", e.getMessage());
            return Optional.empty();
        }

        // create the new image
        int w = wall.getWidth();
        int h = wall.getHeight();
        BufferedImage combined = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        // clone cropped image
        Mat croppedImage = paintingModel.getImage().clone();

        logger.info("Picture height: {}", croppedImage.height());
        float percentOfNumber = (float) paintingModel.getHeight() / interiorWall.getWallHeight() * 100;

        float pixelsForPicture = (float) h / 100 * percentOfNumber;
        float k = pixelsForPicture / croppedImage.height();


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


        String fileName = "final-interior-" + (index.getAndIncrement()) + ".jpg";
        String filePath = Utils.saveImage(originHost, productId, fileName, combined);
        logger.info("Created interior: {} with painting, final name: {}", interiorWall.getImageName(), fileName);
        return Optional.of(filePath);
    }


    public void create3DPainting(Mat croppedImage, String serverPath, String productId, List<String> responseImages) {

        logger.info("Transform image using perspective");
        Mat warpImageMat = imagePerspectiveTransform(croppedImage);
        logger.info("Create 3D border to imitate a painting");
        Mat borderMat = createPaintingBorder(croppedImage);

        BufferedImage image = null;
        BufferedImage border = null;
        try {
            image = Utils.mat2BufferedImage(warpImageMat);
            border = Utils.mat2BufferedImage(borderMat);
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("Can't convert OpenCV matrix to buffered image", e);
        }

        // create the new image, canvas size is the max. of both image sizes
        int w = Math.max(image.getWidth(), border.getWidth());
        int h = Math.max(image.getHeight(), border.getHeight());
        BufferedImage combined = new BufferedImage(w + border.getWidth(), h, BufferedImage.TYPE_INT_RGB);

        // paint both images, preserving the alpha channels
        logger.info("Combine transformed image and border");
        Graphics combinedGraphics = combined.createGraphics();
        combinedGraphics.drawImage(image, 0, 0, null);
        combinedGraphics.drawImage(border, w, 0, null);
        combinedGraphics.dispose();
        logger.info("Images have been combined");

       /* BufferedImage finalPicture = createDropShadow(combined, 30);
        Graphics finalPictureGraphics = finalPicture.createGraphics();
        finalPictureGraphics.drawImage(image, (finalPicture.getWidth() - combined.getWidth()) / 2, (finalPicture.getHeight() - combined.getHeight()) / 2, null);
        finalPictureGraphics.drawImage(border, w + (finalPicture.getWidth() - combined.getWidth()) / 2, (finalPicture.getHeight() - combined.getHeight()) / 2, null);
        finalPictureGraphics.dispose();*/


        String fileName = "painting-3d.png";
        String filePath = Utils.saveImage(serverPath, productId, fileName, combined);
        responseImages.add(filePath);

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
