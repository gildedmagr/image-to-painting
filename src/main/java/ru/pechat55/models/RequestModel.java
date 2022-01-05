package ru.pechat55.models;

public class RequestModel {
    String url;
    String host;
    String id;
    int width;
    int height;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width / 10;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height / 10;
    }

    @Override
    public String toString() {
        return "RequestModel{" +
                "url='" + url + '\'' +
                ", host='" + host + '\'' +
                ", id='" + id + '\'' +
                ", width=" + width +
                ", height=" + height +
                '}';
    }
}
