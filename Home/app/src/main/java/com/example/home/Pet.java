package com.example.home;

public class Pet {
    private String id, name, type, breed, gender, description, imageUrl;
    private String size, hairLength, color, address, phone, createdDate;
    private int age;
    private double price;

    public Pet(String id, String name, String type, String breed, int age,
               String gender, String description, double price, String imageUrl,
               String size, String hairLength, String color, String address,
               String phone, String createdDate) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.breed = breed;
        this.age = age;
        this.gender = gender;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
        this.size = size;
        this.hairLength = hairLength;
        this.color = color;
        this.address = address;
        this.phone = phone;
        this.createdDate = createdDate;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getBreed() { return breed; }
    public int getAge() { return age; }
    public String getGender() { return gender; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }
    public String getImageUrl() { return imageUrl; }
    public String getSize() { return size; }
    public String getHairLength() { return hairLength; }
    public String getColor() { return color; }
    public String getAddress() { return address; }
    public String getPhone() { return phone; }
    public String getCreatedDate() { return createdDate; }

}
