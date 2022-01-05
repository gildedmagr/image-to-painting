package ru.pechat55.models;

import java.util.List;

public class PreviewResponseModel {
    String uid;
    List<String> images;

    public PreviewResponseModel(String uid, List<String> images) {
        this.uid = uid;
        this.images = images;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }
}
