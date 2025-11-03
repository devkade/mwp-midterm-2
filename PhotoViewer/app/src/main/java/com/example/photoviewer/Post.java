package com.example.photoviewer;

import android.graphics.Bitmap;

public class Post {
    private int id;
    private String title;
    private String text;
    private String imageUrl;
    private Bitmap imageBitmap;

    public Post(int id, String title, String text, String imageUrl, Bitmap imageBitmap) {
        this.id = id;
        this.title = title;
        this.text = text;
        this.imageUrl = imageUrl;
        this.imageBitmap = imageBitmap;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Bitmap getImageBitmap() {
        return imageBitmap;
    }
}
