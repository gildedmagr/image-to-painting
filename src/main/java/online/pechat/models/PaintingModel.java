package online.pechat.models;

import org.opencv.core.Mat;

public class PaintingModel {
    int width;
    int height;
    Mat image;

    public PaintingModel(int width, int height, Mat image) {
        this.width = width;
        this.height = height;
        this.image = image;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public Mat getImage() {
        return image;
    }

    public void setImage(Mat image) {
        this.image = image;
    }
}
