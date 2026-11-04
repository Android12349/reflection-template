package org.example.classes;

import java.util.List;

public class Cart {
    private List<Product> items;

    public Cart(List<Product> items) {
        this.items = items;
    }

    public List<Product> getItems() {
        return items;
    }

    public void setItems(List<Product> items) {
        this.items = items;
    }

}
