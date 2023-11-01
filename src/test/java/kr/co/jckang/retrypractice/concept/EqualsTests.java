package kr.co.jckang.retrypractice.concept;

public class EqualsTests {
    public static void main(String[] args) {
        String abc = "ddd";
        String def = "fff";
        System.out.println(abc == def);
        System.out.println(abc.equals(def));
        System.out.println(abc.getClass().hashCode());
        System.out.println(abc.getClass().hashCode() == def.getClass().hashCode());
        System.out.println(abc.getClass() == def.getClass());
    }
}
