/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.sbt.jschool.session;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Scanner;
import org.junit.Test;
import ru.sbt.jschool.session2.OutputFormatter;

import static org.junit.Assert.assertEquals;

public class OutputFormatterTest {
    @Test public void testFormatter0() throws Exception { doTest("0"); }
    @Test public void testFormatter1() throws Exception { doTest("1"); }
    @Test public void testFormatter2() throws Exception { doTest("2"); }
    @Test public void testFormatter3() throws Exception { doTest("3"); }
    @Test public void testFormatter4() throws Exception { doTest("4"); }

    private void doTest(String dir) throws Exception {
        Scanner sc = new Scanner(OutputFormatterTest.class.getResourceAsStream("/" + dir + "/input.csv"));

        int size = Integer.valueOf(sc.nextLine());
        String[] types = sc.nextLine().split(",");
        String[] names = sc.nextLine().split(",");

        Object[][] data = new Object[size][];
        for (int i = 0; i < size; i++) {
            String[] strLine = sc.nextLine().split(",", -1);
            Object[] line = new Object[strLine.length];
            for (int j = 0; j < strLine.length; j++) {
                line[j] = format(strLine[j], types[j]);
            }
            data[i] = line;
        }

        // Запись в временный файл
        File temp = File.createTempFile("test" + dir, "txt");
        temp.deleteOnExit();
        try (FileOutputStream output = new FileOutputStream(temp)) {
            OutputFormatter formatter = new OutputFormatter(new PrintStream(output));
            formatter.output(names, data);
        }

        // Вывод в консоль для наглядности
        System.out.println("=== Test " + dir + " output ===");
        OutputFormatter formatterConsole = new OutputFormatter(System.out);
        formatterConsole.output(names, data);
        System.out.println("=== End of test " + dir + " output ===");

        // Проверка с эталонным выводом
        try (Scanner actualOutput = new Scanner(temp);
             Scanner expectedOutput = new Scanner(OutputFormatterTest.class.getResourceAsStream("/" + dir + "/output.txt"))) {

            while (expectedOutput.hasNextLine()) {
                String expected = expectedOutput.nextLine();

                if (!actualOutput.hasNextLine()) {
                    throw new AssertionError(
                            "Expected output is \"" + expected + "\", but actual output is empty!");
                }

                String actual = actualOutput.nextLine();

                actual = actual.replace((char) 160, (char) 32);
                expected = expected.replace((char) 160, (char) 32);

                assertEquals(expected, actual);
            }
        }
    }

    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");

    private Object format(String str, String type) throws ParseException {
        if ("".equals(str))
            return null;

        switch (type) {
            case "string":
                return str;
            case "number":
                return Integer.valueOf(str);
            case "date":
                return dateFormat.parse(str);
            case "money":
                return Double.valueOf(str);
        }

        throw new RuntimeException("Unknown data type: " + type);
    }
}
