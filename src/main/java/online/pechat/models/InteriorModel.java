package online.pechat.models;

public class InteriorModel {
    private String imageName;
    private int dowelX;
    private int dowelY;
    private int wallHeight;


    public InteriorModel() {
    }

    public InteriorModel(String imageName, int dowelX, int dowelY, int wallHeight) {
        this.imageName = imageName;
        this.dowelX = dowelX;
        this.dowelY = dowelY;
        this.wallHeight = wallHeight;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public int getDowelX() {
        return dowelX;
    }

    public void setDowelX(int dowelX) {
        this.dowelX = dowelX;
    }

    public int getDowelY() {
        return dowelY;
    }

    public void setDowelY(int dowelY) {
        this.dowelY = dowelY;
    }

    public int getWallHeight() {
        return wallHeight;
    }

    public void setWallHeight(int wallHeight) {
        this.wallHeight = wallHeight;
    }
}
