package org.example;


import org.example.classes.Shape;
import org.example.generator.Generator;

public class GenerateExample {
    public static void main(String[] args) {
        var gen = new Generator();
        Shape generated = gen.generateValueOfType(Shape.class);
        System.out.println(generated.getClass().getSimpleName() + ": perimeter=" + generated.getPerimeter());
    }
}
