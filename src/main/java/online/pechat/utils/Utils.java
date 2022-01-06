package online.pechat.utils;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.DataBufferByte;
import java.awt.image.Kernel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;

public class Utils {
    private static Logger logger = LoggerFactory.getLogger(Utils.class);
    public static String IMAGE_DIR = "image/painting/";

    /**
     * Creates an gaussian blur filter
     *
     * @param radius     radius of the filter
     * @param horizontal whether it is horizontal blur
     * @return ConvolveOp  filter
     */
    public static ConvolveOp getGaussianBlurFilter(int radius,
                                                   boolean horizontal) {
        if (radius < 1) {
            throw new IllegalArgumentException("Radius must be >= 1");
        }
        int size = radius * 2 + 1;
        float[] data = new float[size];
        float sigma = radius / 3.0f;
        float twoSigmaSquare = 2.0f * sigma * sigma;
        float sigmaRoot = (float) Math.sqrt(twoSigmaSquare * Math.PI);
        float total = 0.0f;
        for (int i = -radius; i <= radius; i++) {
            float distance = i * i;
            int index = i + radius;
            data[index] = (float) Math.exp(-distance / twoSigmaSquare)
                    / sigmaRoot;
            total += data[index];
        }
        for (int i = 0; i < data.length; i++) {
            data[i] /= total;
        }
        Kernel kernel;
        if (horizontal) {
            kernel = new Kernel(size, 1, data);
        } else {
            kernel = new Kernel(1, size, data);
        }
        return new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
    }

    /**
     * Downloads and convert url of image to OpenCV matrix
     *
     * @param imageUrl string of image url
     * @return OpenCV matrix
     */
    public static Mat urlToMat(String imageUrl) {
        BufferedImage bufferedImage = null;
        try {
            URL url = new URL(imageUrl);
            bufferedImage = ImageIO.read(url);
        } catch (Exception e) {
            logger.error("Can't read image from URL: {}", imageUrl, e);
            return null;
        }

        if (bufferedImage == null) {
            logger.error("Can't read image from URL: {}", imageUrl);
            return null;
        }
        return bufferedImageToMat(bufferedImage);
    }

    /**
     * Converts {@link BufferedImage} to OpenCV matrix
     *
     * @param bufferedImage {@link BufferedImage}
     * @return OpenCV matrix
     */
    public static Mat bufferedImageToMat(BufferedImage bufferedImage) {
        byte[] pixels = ((DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData();

        logger.info(bufferedImage.getWidth() + " " + bufferedImage.getHeight());

        String extension = bufferedImage.getType() == BufferedImage.TYPE_INT_RGB ? "JPG" : "PNG";

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            ImageIO.write(bufferedImage, extension, byteArrayOutputStream);
            byteArrayOutputStream.flush();
        } catch (IOException e) {
            logger.error("Can't convert buffered image to OpenCV matrix", e);
            e.printStackTrace();
        }


        return Imgcodecs.imdecode(new MatOfByte(byteArrayOutputStream.toByteArray()), Imgcodecs.IMREAD_COLOR);
    }

    /**
     * Converts OpenCV matrix to {@link BufferedImage}
     * @param matrix OpenCV matrix
     * @return {@link BufferedImage}
     * @throws IOException
     */
    public static BufferedImage mat2BufferedImage(Mat matrix) throws IOException {
        MatOfByte mob = new MatOfByte();
        Imgcodecs.imencode(".png", matrix, mob);
        byte[] ba = mob.toArray();

        return ImageIO.read(new ByteArrayInputStream(ba));
    }

    /**
     * Saves {@link BufferedImage} to local disk
     *
     * @param parentPath first chunk of the path
     * @param id the unique ID, is used as directory name
     * @param finalName the file name
     * @param image {@link BufferedImage} instance
     * @return relative file path
     */
    public static String saveImage(String parentPath, String id, String finalName, BufferedImage image){
        String filePath = createFinalDir(parentPath, id) + finalName;
        String extension = image.getType() == BufferedImage.TYPE_INT_RGB ? "JPG" : "PNG";
        try {
            ImageIO.write(image, extension, new File(parentPath, filePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return filePath;
    }

    /**
     * Creates dir if it doesn't exist
     *
     * @param host parent part of the path
     * @param child second part of the path
     * @return concatenated path
     */
    public static String createFinalDir(String host, String child){
        String childDir = IMAGE_DIR + child;
        File directory = new File(host, childDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return childDir + File.separator;
    }
}
