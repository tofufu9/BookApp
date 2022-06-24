package com.example.bookapp.models;
public  class ModelCategory {
    String id, category, uid;
    long timestamp;
    // Lưu ý khi khai báo hàm và thuộc tính phải tuân theo Firebase thì Firebase mới nhận đc

    // Constructor empty required for firebase
    public ModelCategory() {

    }


    //parametrized constructor
    public ModelCategory(String id, String category, String uid, long timestamp) {
        this.id = id;
        this.category = category;
        this.uid = uid;
        this.timestamp = timestamp;
    }


    //Getters & Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}