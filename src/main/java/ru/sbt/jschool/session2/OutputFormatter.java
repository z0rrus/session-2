package ru.sbt.jschool.session2;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * OutputFormatter with Printer interface.
 */
public class OutputFormatter {
    private final PrintStream out;

    private final Map<Class<?>, Printer> knownPrinters = new HashMap<>();
    private final Printer anyPrinter = new AnyPrinter();

    public OutputFormatter(PrintStream out) {
        this.out = out;
        registerPrinter(new StringPrinter());
        registerPrinter(new DatePrinter());
        registerPrinter(new DoublePrinter());
        registerPrinter(new NumberPrinter());
    }

    private void registerPrinter(Printer printer) {
        for (Class<?> clazz : printer.supported()) {
            knownPrinters.put(clazz, printer);
        }
    }

    private Printer getPrinter(Object obj) {
        if (obj == null) return anyPrinter;
        Printer p = knownPrinters.get(obj.getClass());
        if (p != null) return p;
        // Fallback: ищем подходящего по родству типов
        for (Map.Entry<Class<?>, Printer> e : knownPrinters.entrySet()) {
            if (e.getKey().isAssignableFrom(obj.getClass())) return e.getValue();
        }
        return anyPrinter;
    }

    public void output(String[] names, Object[][] data) {
        if (names == null || data == null) {
            throw new IllegalArgumentException("Column names and data must not be null");
        }

        int colCount = names.length;
        int rowCount = data.length;

        Printer[] colPrinters = new Printer[colCount];
        for (int c = 0; c < colCount; c++) {
            Object firstNotNull = null;
            for (int r = 0; r < rowCount; r++) {
                if (data[r][c] != null) {
                    firstNotNull = data[r][c];
                    break;
                }
            }
            colPrinters[c] = getPrinter(firstNotNull);
        }

        int[] colWidths = new int[colCount];
        for (int c = 0; c < colCount; c++) {
            // Берём максимальную длину из названия столбца и данных
            int maxLen = names[c].length();

            for (int r = 0; r < rowCount; r++) {
                int length = colPrinters[c].length(data[r][c]);
                if (length > maxLen) {
                    maxLen = length;
                }
            }
            colWidths[c] = maxLen;
        }

        printBorder(colWidths);
        out.print("|");
        for (int c = 0; c < colCount; c++) {
            out.print(center(names[c], colWidths[c]));
            out.print("|");
        }
        out.println();
        printBorder(colWidths);

        for (int r = 0; r < rowCount; r++) {
            out.print("|");
            for (int c = 0; c < colCount; c++) {
                Printer printer = colPrinters[c];
                String cell = printer.print(data[r][c]);
                // Форматируем с учётом выравнивания, реализованного в Printer
                out.print(printer.align(cell, colWidths[c]));
                out.print("|");
            }
            out.println();
            printBorder(colWidths);
        }
    }

    // Border and alignment helpers
    private void printBorder(int[] colWidths) {
        out.print("+");
        for (int w : colWidths) {
            for (int i = 0; i < w; i++) {
                out.print("-");
            }
            out.print("+");
        }
        out.println();
    }

    private String center(String s, int w) {
        if (s.length() >= w) return s;
        int leftPadding = (w - s.length()) / 2;
        int rightPadding = w - s.length() - leftPadding;
        return repeat(' ', leftPadding) + s + repeat(' ', rightPadding);
    }

    private String repeat(char ch, int count) {
        if (count <= 0) return "";
        char[] arr = new char[count];
        Arrays.fill(arr, ch);
        return new String(arr);
    }

    // Printer interface and implementations:

    public interface Printer {
        List<Class<?>> supported();

        /**
         * Возвращает длину отформатированной строки для данного объекта, без дополнительного форматирования.
         */
        int length(Object obj);

        /**
         * Форматирует объект в строку.
         */
        String print(Object obj);

        /**
         * Выравнивает текст по ширине для таблицы.
         * Можно реализовать как left, right или center в зависимости от типа данных.
         */
        String align(String s, int width);
    }

    public static class StringPrinter implements Printer {
        @Override
        public List<Class<?>> supported() {
            return Collections.singletonList(String.class);
        }

        @Override
        public int length(Object obj) {
            if (obj == null) return 1;
            return obj.toString().replace("\n", " ").length();
        }

        @Override
        public String print(Object obj) {
            return obj == null ? "-" : obj.toString().replace("\n", " ");
        }

        @Override
        public String align(String s, int width) {
            // Текст — по левому краю
            if (s.length() >= width) return s;
            return s + repeat(' ', width - s.length());
        }

        private String repeat(char ch, int count) {
            if (count <= 0) return "";
            char[] arr = new char[count];
            Arrays.fill(arr, ch);
            return new String(arr);
        }
    }

    public static class DatePrinter implements Printer {
        private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy");

        @Override
        public List<Class<?>> supported() {
            return Collections.singletonList(Date.class);
        }

        @Override
        public int length(Object obj) {
            // Длина фиксирована: 10 символов формата dd.MM.yyyy или 1 если null
            return obj == null ? 1 : 10;
        }

        @Override
        public String print(Object obj) {
            return obj == null ? "-" : DATE_FORMAT.format((Date) obj);
        }

        @Override
        public String align(String s, int width) {
            // Даты — по правому краю
            if (s.length() >= width) return s;
            return repeat(' ', width - s.length()) + s;
        }

        private String repeat(char ch, int count) {
            if (count <= 0) return "";
            char[] arr = new char[count];
            Arrays.fill(arr, ch);
            return new String(arr);
        }
    }

    public static class DoublePrinter implements Printer {
        private static final DecimalFormat MONEY_FORMAT;

        static {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("ru", "RU"));
            symbols.setGroupingSeparator(' ');
            symbols.setDecimalSeparator(',');
            MONEY_FORMAT = new DecimalFormat("#,##0.00", symbols);
            MONEY_FORMAT.setGroupingUsed(true);
        }

        @Override
        public List<Class<?>> supported() {
            return Arrays.asList(Double.class, Float.class);
        }

        @Override
        public int length(Object obj) {
            if (obj == null) return 1;
            double d = ((Number) obj).doubleValue();
            // Форматируем один раз для получения длины (не оптимально, но иначе сложно)
            // Можно наращивать кеширование, если нужно
            return MONEY_FORMAT.format(d).length();
        }

        @Override
        public String print(Object obj) {
            if (obj == null) return "-";
            double d = ((Number) obj).doubleValue();
            return MONEY_FORMAT.format(d);
        }

        @Override
        public String align(String s, int width) {
            // Числа с плавающей точкой — по правому краю
            if (s.length() >= width) return s;
            return repeat(' ', width - s.length()) + s;
        }

        private String repeat(char ch, int count) {
            if (count <= 0) return "";
            char[] arr = new char[count];
            Arrays.fill(arr, ch);
            return new String(arr);
        }
    }

    public static class NumberPrinter implements Printer {
        private static final DecimalFormat NUMBER_FORMAT;

        static {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("ru", "RU"));
            symbols.setGroupingSeparator(' ');
            symbols.setDecimalSeparator(',');
            NUMBER_FORMAT = new DecimalFormat("#,###", symbols);
            NUMBER_FORMAT.setGroupingUsed(true);
        }

        @Override
        public List<Class<?>> supported() {
            return Arrays.asList(Integer.class, Long.class, Short.class, Byte.class);
        }

        @Override
        public int length(Object obj) {
            if (obj == null) return 1;
            long l = ((Number) obj).longValue();
            return NUMBER_FORMAT.format(l).length();
        }

        @Override
        public String print(Object obj) {
            if (obj == null) return "-";
            long l = ((Number) obj).longValue();
            return NUMBER_FORMAT.format(l);
        }

        @Override
        public String align(String s, int width) {
            // Целые числа — по правому краю
            if (s.length() >= width) return s;
            return repeat(' ', width - s.length()) + s;
        }

        private String repeat(char ch, int count) {
            if (count <= 0) return "";
            char[] arr = new char[count];
            Arrays.fill(arr, ch);
            return new String(arr);
        }
    }

    public static class AnyPrinter implements Printer {
        @Override
        public List<Class<?>> supported() {
            return Collections.emptyList();
        }

        @Override
        public int length(Object obj) {
            if (obj == null) return 1;
            return Objects.toString(obj).length();
        }

        @Override
        public String print(Object obj) {
            return obj == null ? "-" : Objects.toString(obj);
        }

        @Override
        public String align(String s, int width) {
            // По умолчанию выравниваем слева
            if (s.length() >= width) return s;
            return s + repeat(' ', width - s.length());
        }

        private String repeat(char ch, int count) {
            if (count <= 0) return "";
            char[] arr = new char[count];
            Arrays.fill(arr, ch);
            return new String(arr);
        }
    }
}
