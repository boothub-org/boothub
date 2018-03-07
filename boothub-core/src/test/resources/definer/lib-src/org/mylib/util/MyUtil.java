package org.mylib.util;

public class MyUtil {
    static String alternateCase(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            sb.append((i % 2 == 0) ? Character.toLowerCase(s.charAt(i)) : Character.toUpperCase(s.charAt(i)));
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        String s = (args.length > 0) ? args[0] : "this is some text with alternate case";
        System.out.println(alternateCase(s));
    }
}
