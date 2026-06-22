package com.junwenzheng.execution.market;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Random;

public final class SyntheticMarketDataGenerator {
    private SyntheticMarketDataGenerator() {}

    public static void writeSample(Path path, String symbol, int events, long seed) throws IOException {
        Files.createDirectories(path.getParent());
        Random random = new Random(seed);
        StringBuilder sb = new StringBuilder("timestamp_ms,symbol,bid,ask,last,volume\n");
        double price = 100.0;
        for (int i = 0; i < events; i++) {
            long ts = i * 1_000L;
            double drift = 0.00005 * i;
            double shock = random.nextGaussian() * 0.035;
            price = Math.max(20.0, price + drift + shock);
            double spread = 0.015 + random.nextDouble() * 0.025;
            double bid = price - spread / 2.0;
            double ask = price + spread / 2.0;
            long volume = Math.max(100, Math.round(600 + Math.abs(random.nextGaussian()) * 950 + 200 * Math.sin(i / 12.0)));
            sb.append(String.format(Locale.US, "%d,%s,%.4f,%.4f,%.4f,%d%n", ts, symbol, bid, ask, price, volume));
        }
        Files.writeString(path, sb.toString());
    }
}
