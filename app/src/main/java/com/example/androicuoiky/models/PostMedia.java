
package com.example.androicuoiky.models;

public class PostMedia {

    private String url;
    private String type; // IMAGE hoặc VIDEO
    private String storagePath;

    public PostMedia() {
        // Firebase Firestore cần constructor rỗng
    }

    public PostMedia(String url, String type, String storagePath) {
        this.url = url;
        this.type = type;
        this.storagePath = storagePath;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }
}
