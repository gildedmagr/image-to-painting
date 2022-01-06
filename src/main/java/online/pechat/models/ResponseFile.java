package online.pechat.models;

public class ResponseFile {
    private String name;
    private String extension;
    private String type;
    private String size;
    private String path;
    private String file;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    @Override
    public String toString() {
        return "ResponseFile{" +
                "name='" + name + '\'' +
                ", extension='" + extension + '\'' +
                ", type='" + type + '\'' +
                ", size='" + size + '\'' +
                ", path='" + path + '\'' +
                ", file='" + file + '\'' +
                '}';
    }
}
