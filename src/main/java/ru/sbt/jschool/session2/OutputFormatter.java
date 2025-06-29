package ru.sbt.jschool.session2;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class OutputFormatter {
    private final PrintStream out;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy");

    private static final DecimalFormat MONEY_FORMAT;
    private static final DecimalFormat NUMBER_FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("ru", "RU"));
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');

        MONEY_FORMAT = new DecimalFormat("#,##0.00", symbols);
        MONEY_FORMAT.setGroupingUsed(true);

        NUMBER_FORMAT = new DecimalFormat("#,###", symbols);
        NUMBER_FORMAT.setGroupingUsed(true);
    }

    public OutputFormatter(PrintStream out) {
        this.out = out;
    }

    public void output(String[] names, Object[][] data) {
        if (names == null || data == null) {
            throw new IllegalArgumentException("Column names and data must not be null");
        }

        int colCount = names.length;
        int rowCount = data.length;

        Class<?>[] types = new Class<?>[colCount];
        for (int c = 0; c < colCount; c++) {
            types[c] = inferColumnType(data, c);
        }

        int[] colWidths = new int[colCount];
        for (int c = 0; c < colCount; c++) {
            colWidths[c] = names[c].length();
        }

        String[][] formattedData = new String[rowCount][colCount];
        for (int r = 0; r < rowCount; r++) {
            for (int c = 0; c < colCount; c++) {
                String formatted = formatCell(data[r][c], types[c]);
                formattedData[r][c] = formatted;
                if (formatted.length() > colWidths[c]) {
                    colWidths[c] = formatted.length();
                }
            }
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
                String cell = formattedData[r][c];
                if (types[c] == String.class) {
                    out.print(leftAlign(cell, colWidths[c]));
                } else {
                    out.print(rightAlign(cell, colWidths[c]));
                }
                out.print("|");
            }
            out.println();
            printBorder(colWidths);
        }
    }

    private Class<?> inferColumnType(Object[][] data, int colIndex) {
        for (Object[] row : data) {
            Object val = row[colIndex];
            if (val != null) {
                if (val instanceof Date) return Date.class;
                if (val instanceof Float || val instanceof Double) return Double.class;
                if (val instanceof Integer || val instanceof Long || val instanceof Short || val instanceof Byte)
                    return Long.class;
                return String.class;
            }
        }
        return String.class;
    }

    private String formatCell(Object value, Class<?> type) {
        if (value == null) {
            return "-";
        }
        if (type == String.class) {
            return value.toString().replace("\n", " ");
        } else if (type == Date.class) {
            return DATE_FORMAT.format((Date) value);
        } else if (type == Double.class) {
            double d = ((Number) value).doubleValue();
            return MONEY_FORMAT.format(d);
        } else if (type == Long.class) {
            long l = ((Number) value).longValue();
            return NUMBER_FORMAT.format(l);
        } else {
            return value.toString();
        }
    }

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

    private String leftAlign(String s, int w) {
        if (s.length() >= w) return s;
        return s + repeat(' ', w - s.length());
    }

    private String rightAlign(String s, int w) {
        if (s.length() >= w) return s;
        return repeat(' ', w - s.length()) + s;
    }

    private String repeat(char ch, int count) {
        char[] arr = new char[count];
        for (int i = 0; i < count; i++) arr[i] = ch;
        return new String(arr);
    }
}
