package ru.pechat55;

import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import org.opencv.imgcodecs.Imgcodecs;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;

import static org.opencv.core.CvType.CV_8U;
import static org.opencv.core.CvType.CV_8UC4;
import static org.opencv.imgproc.Imgproc.INTER_AREA;


class Application {
    private  static final String PATH = "C:\\Users\\Andriy\\IdeaProjects\\pechat-canvas\\src\\main\\resources\\";
    public static void main(String... args) {
        System.out.println("Works");
        Bootstrap.start();
        OpenCV.loadShared();
        System.loadLibrary( Core.NATIVE_LIBRARY_NAME );

    }

    public static void generateFiles(){
        String filename = PATH + "test.jpeg";
        String filenameFinal = PATH + "result.png";
        String filenameShadow = PATH + "shadow.png";
        String filenameBorder = PATH + "border.png";
        String filenameFinalWithPoints = "C:\\Users\\Andriy\\IdeaProjects\\pechat-canvas\\src\\main\\resources\\points.jpeg";

        Mat image = Imgcodecs.imread(filename, Imgcodecs.IMREAD_UNCHANGED);
        if (image != null) {
            Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2BGRA);
            //Imgproc.GaussianBlur(image, image, new Size(3, 3), 10);
            image = cropImage(image);

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
            drawTransformPoints(filenameFinalWithPoints, image, srcTri, dstTri);

            Mat warpDst = Mat.zeros(image.rows(), image.cols(), CvType.CV_8U);

            //Mat warpMat = Imgproc.getAffineTransform( new MatOfPoint2f(srcTri), new MatOfPoint2f(dstTri) );
            Mat warpMat = Imgproc.getPerspectiveTransform(new MatOfPoint2f(srcTri), new MatOfPoint2f(dstTri));


            //Imgproc.warpAffine( image, warpDst, warpMat, warpDst.size() );

            Imgproc.warpPerspective(image, warpDst, warpMat, warpDst.size(), Imgproc.INTER_CUBIC, Core.BORDER_TRANSPARENT, new Scalar(0, 0, 0, 255));
            Imgproc.line(warpDst, new Point(warpDst.width() - 1, 0), new Point(warpDst.width() - 1, warpDst.height()), new Scalar(125, 125, 125, 200), 1);

            // Imgproc.resize(image, warpDst, new Size(100,100), 0, 0, INTER_AREA);

            Mat border = createBorder(image);
            Mat shadow = createShadow(warpDst);



            System.out.println(warpDst.channels());

            Imgcodecs.imwrite(filenameFinal, warpDst);
            Imgcodecs.imwrite(filenameBorder, border);
            Imgcodecs.imwrite(filenameShadow, shadow);

            try{
                combine();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        System.out.println("Finished");
    }

    private static Mat createBorder(Mat image) {

        // copy image
        Mat border = new Mat(image, new Rect(0, 0, image.cols(), image.rows()));

        // create crop area
        Rect rectCrop = new Rect(border.width() - 20, 0, 20, border.height());
        // crop image
        border = new Mat(border, rectCrop);
        // flip horizontally
        Core.flip(border, border, 1);

        // perspective transform

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

        border.convertTo(border, -1, 1, -20);
       // Imgproc.cvtColor(border, border, Imgproc.COLOR_BGR2RGB);
        Mat warpMat = Imgproc.getPerspectiveTransform(new MatOfPoint2f(srcTri), new MatOfPoint2f(dstTri));
        Mat warpDst = Mat.zeros(border.rows(), border.cols(), CvType.CV_8U);
        Imgproc.warpPerspective(border, warpDst, warpMat, warpDst.size(), Imgproc.INTER_CUBIC, Core.BORDER_TRANSPARENT, new Scalar(0, 0, 0, 255));

        //Imgproc.line(warpDst, new Point(0, 0), new Point(0, warpDst.height()), new Scalar(0, 125, 125), 1);
        return warpDst;
    }

    private static Mat createShadow(Mat image) {
        // copy image
        //Mat shadow = new Mat(image, new Rect(0, 0, image.cols(), image.rows()));
        Mat shadow = Mat.zeros(image.rows(), image.cols(), CvType.CV_8U);
        Imgproc.GaussianBlur(image, shadow, new Size(5, 5), 10);
        return shadow;
    }

    private static Mat cropImage(Mat image) {
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

    public static void combine() throws IOException {
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

        ImageIO.write(shadow, "PNG", new File(PATH, "combined.png"));
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
        BufferedImage shadow = new BufferedImage(image.getWidth() + 4
                * size, image.getHeight() + 4 * size,
                BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2 = shadow.createGraphics();
        g2.drawImage(image, size * 2, size * 2, null);

        // composite
        g2.setComposite(AlphaComposite.SrcIn);
        g2.setColor(Color.DARK_GRAY);
        g2.fillRoundRect(0, 0, shadow.getWidth(), shadow.getHeight(), 30, 30);

        g2.dispose();

        shadow = getGaussianBlurFilter(size, true).filter(shadow, null);
        shadow = getGaussianBlurFilter(size, false).filter(shadow, null);

        return shadow;

    }

    /**
     * Create an gaussian blur filter
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