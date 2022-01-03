package ru.pechat55.models;

import java.util.ArrayList;
import java.util.List;

public class UploadResponse {
    private List<ResponseFile> files = new ArrayList<ResponseFile>();
    private List<Object> error = new ArrayList<Object>();

    public List<ResponseFile> getFiles() {
        return files;
    }

    public void setFiles(List<ResponseFile> files) {
        this.files = files;
    }

    public List<Object> getError() {
        return error;
    }

    public void setError(List<Object> error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "UploadResponse{" +
                "files=" + files +
                ", error=" + error +
                '}';
    }
}
