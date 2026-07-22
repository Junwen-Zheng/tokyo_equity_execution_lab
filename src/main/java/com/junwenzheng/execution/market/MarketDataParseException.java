package com.junwenzheng.execution.market;

import java.nio.file.Path;

public final class MarketDataParseException
        extends IllegalArgumentException {

    private final Path path;
    private final int lineNumber;

    public MarketDataParseException(
            Path path,
            int lineNumber,
            String message,
            Throwable cause
    ) {
        super(
                path + ":" + lineNumber + ": " + message,
                cause
        );

        this.path = path;
        this.lineNumber = lineNumber;
    }

    public Path path() {
        return path;
    }

    public int lineNumber() {
        return lineNumber;
    }
}
