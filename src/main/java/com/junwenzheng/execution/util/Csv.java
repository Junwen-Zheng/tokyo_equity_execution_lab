package com.junwenzheng.execution.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class Csv {
    private Csv() {}

    public static List<String[]> readRows(Path path) throws IOException {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                if (first && line.toLowerCase().startsWith("timestamp_ms")) {
                    first = false;
                    continue;
                }
                first = false;
                rows.add(line.split(",", -1));
            }
        }
        return rows;
    }
}
