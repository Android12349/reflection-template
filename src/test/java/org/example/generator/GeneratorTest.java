package org.example.generator;

import org.example.classes.BinaryTreeNode;
import org.example.classes.Cart;
import org.example.classes.Example;
import org.example.classes.Product;
import org.example.classes.Rectangle;
import org.example.classes.Shape;
import org.example.classes.Triangle;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class GeneratorTest {
    private final Generator generator = new Generator(new Random(12345));

    enum Color { RED, GREEN, BLUE }

    static class Person {
        private final String name;
        private final int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    @Generatable
    interface Vehicle {
    }

    @Generatable
    static class Bicycle implements Vehicle {
        final int gears;

        Bicycle(int gears) {
            this.gears = gears;
        }
    }

    @Generatable
    abstract static class Animal {
    }

    @Generatable
    static class Cat extends Animal {
        final String name;

        Cat(String name) {
            this.name = name;
        }
    }

    interface Unmarked {
    }

    static class Unsupported {
        Unsupported(Path path) {
        }
    }

    @Test
    void primitiveWrappersAndStrings() {
        assertTrue(generator.generateValueOfType(int.class) > 0);
        assertTrue(generator.generateValueOfType(Integer.class) > 0);
        assertTrue(generator.generateValueOfType(long.class) > 0);
        assertTrue(generator.generateValueOfType(Long.class) > 0);
        assertTrue(generator.generateValueOfType(short.class) > 0);
        assertTrue(generator.generateValueOfType(Short.class) > 0);
        assertTrue(generator.generateValueOfType(byte.class) > 0);
        assertTrue(generator.generateValueOfType(Byte.class) > 0);
        assertTrue(generator.generateValueOfType(double.class) > 0);
        assertTrue(generator.generateValueOfType(Double.class) > 0);
        assertTrue(generator.generateValueOfType(float.class) > 0);
        assertTrue(generator.generateValueOfType(Float.class) > 0);
        assertNotNull(generator.generateValueOfType(boolean.class));
        assertNotNull(generator.generateValueOfType(Boolean.class));
        assertTrue(Character.isLetter(generator.generateValueOfType(char.class)));
        assertTrue(Character.isLetter(generator.generateValueOfType(Character.class)));
        assertTrue(generator.generateValueOfType(String.class).length() >= 5);
    }

    @Test
    void enumsAndArrays() {
        assertNotNull(generator.generateValueOfType(Color.class));
        assertTrue(generator.generateValueOfType(int[].class).length > 0);
        assertTrue(generator.generateValueOfType(String[].class).length > 0);
        assertTrue(generator.generateValueOfType(Color[].class).length > 0);
        Rectangle[] rectangles = generator.generateValueOfType(Rectangle[].class);
        assertTrue(rectangles.length > 0);
        assertTrue(rectangles[0].getArea() > 0);
    }

    @Test
    void rawCollectionsContainGeneratedValues() {
        List<?> list = generator.generateValueOfType(List.class);
        assertFalse(list.isEmpty());
        assertTrue(list.stream().allMatch(item -> item instanceof String && !((String) item).isEmpty()));
        Map<?, ?> map = generator.generateValueOfType(Map.class);
        assertFalse(map.isEmpty());
        assertTrue(map.entrySet().stream().allMatch(entry -> entry.getKey() instanceof String && entry.getValue() instanceof Integer));
    }

    @Test
    void templateAndNewObjects() {
        assertNotNull(generator.generateValueOfType(Example.class));
        Product product = generator.generateValueOfType(Product.class);
        assertFalse(product.getName().isEmpty());
        assertTrue(product.getPrice() > 0);
        Cart cart = generator.generateValueOfType(Cart.class);
        assertFalse(cart.getItems().isEmpty());
        assertTrue(cart.getItems().get(0) instanceof Product);
        BinaryTreeNode node = generator.generateValueOfType(BinaryTreeNode.class);
        assertNotNull(node.getData());
        assertNull(node.getLeft());
        assertNull(node.getRight());
        Person person = generator.generateValueOfType(Person.class);
        assertFalse(person.name.isEmpty());
        assertTrue(person.age > 0);
    }

    @Test
    void shapeAndRegisteredAbstractTypes() {
        Shape shape = generator.generateValueOfType(Shape.class);
        assertTrue(shape instanceof Rectangle || shape instanceof Triangle);
        assertTrue(shape.getPerimeter() > 0);
        assertTrue(generator.generateValueOfType(Rectangle.class).getArea() > 0);
        assertTrue(generator.generateValueOfType(Triangle.class).getPerimeter() > 0);
        Generator extended = new Generator(new Random(3), Bicycle.class, Cat.class);
        assertTrue(extended.generateValueOfType(Vehicle.class) instanceof Bicycle);
        assertTrue(extended.generateValueOfType(Animal.class) instanceof Cat);
    }

    @Test
    void unsupportedRequestsHaveUsefulExceptions() {
        assertTrue(assertThrows(GenerationException.class,
                () -> generator.generateValueOfType(Unmarked.class)).getMessage().contains("@Generatable"));
        assertTrue(assertThrows(GenerationException.class,
                () -> generator.generateValueOfType(Unsupported.class)).getMessage().contains("Unsupported"));
        assertThrows(GenerationException.class, () -> generator.generateValueOfType(Object.class));
        assertThrows(GenerationException.class, () -> generator.generateValueOfType(null));
        assertThrows(IllegalArgumentException.class, () -> new Generator(new Random(), Unmarked.class));
    }
}
