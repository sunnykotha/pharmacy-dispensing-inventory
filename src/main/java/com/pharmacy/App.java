package com.pharmacy;

public class App {
    public String message() {
        return "Pharmacy Dispensing and Inventory";
    }

    public static void main(String[] args) {
        System.out.println(new App().message());
    }
}