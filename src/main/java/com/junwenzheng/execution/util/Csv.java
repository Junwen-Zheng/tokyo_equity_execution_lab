package com.junwenzheng.execution.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Csv {
    private Csv() {
    }

    public record Row(
            int lineNumber,
            List<String> values
    ) {
        public Row {
            if (lineNumber <= 0) {
                throw new IllegalArgumentException(
                        "lineNumber must be positive"
                );
            }

            values = List.copyOf(values);
        }
    }

    public static List<Row> readNumberedRows(
            Path path
    ) throws IOException {
        List<Row> rows = new ArrayList<>();

        try (
                BufferedReader reader =
                        Files.newBufferedReader(path)
        ) {
            String line;
            int lineNumber = 0;
            boolean firstDataLine = true;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (line.isBlank()) {
                    continue;
                }

                if (
                        firstDataLine
                                && line.toLowerCase()
                                .startsWith("timestamp_ms")
                ) {
                    firstDataLine = false;
                    continue;
                }

                firstDataLine = false;

                rows.add(
                        new Row(
                                lineNumber,
                                Arrays.asList(
                                        line.split(",", -1)
                                )
                        )
                );
            }
        }

        return List.copyOf(rows);
    }

    public static List<String[]> readRows(
            Path path
    ) throws IOException {
        List<String[]> rows = new ArrayList<>();

        for (Row row : readNumberedRows(path)) {
            rows.add(
                    row.values().toArray(String[]::new)
            );
        }

        return List.copyOf(rows);
    }
}
