package com.junwenzheng.execution.rules;

import java.math.BigDecimal;

public enum TokyoTickSizeTable {
    TOPIX_500 {
        @Override
        public double tickSize(
                double price
        ) {
            validatePrice(price);

            if (price <= 1_000.0) {
                return 0.1;
            }

            if (price <= 3_000.0) {
                return 0.5;
            }

            if (price <= 10_000.0) {
                return 1.0;
            }

            if (price <= 30_000.0) {
                return 5.0;
            }

            if (price <= 100_000.0) {
                return 10.0;
            }

            if (price <= 300_000.0) {
                return 50.0;
            }

            if (price <= 1_000_000.0) {
                return 100.0;
            }

            if (price <= 3_000_000.0) {
                return 500.0;
            }

            if (price <= 10_000_000.0) {
                return 1_000.0;
            }

            if (price <= 30_000_000.0) {
                return 5_000.0;
            }

            return 10_000.0;
        }
    },

    OTHER_ISSUE {
        @Override
        public double tickSize(
                double price
        ) {
            validatePrice(price);

            if (price <= 3_000.0) {
                return 1.0;
            }

            if (price <= 5_000.0) {
                return 5.0;
            }

            if (price <= 30_000.0) {
                return 10.0;
            }

            if (price <= 50_000.0) {
                return 50.0;
            }

            if (price <= 300_000.0) {
                return 100.0;
            }

            if (price <= 500_000.0) {
                return 500.0;
            }

            if (price <= 3_000_000.0) {
                return 1_000.0;
            }

            if (price <= 5_000_000.0) {
                return 5_000.0;
            }

            if (price <= 30_000_000.0) {
                return 10_000.0;
            }

            if (price <= 50_000_000.0) {
                return 50_000.0;
            }

            return 100_000.0;
        }
    };

    public abstract double tickSize(
            double price
    );

    public boolean isAligned(
            double price
    ) {
        validatePrice(price);

        BigDecimal priceValue =
                BigDecimal.valueOf(price);

        BigDecimal tickValue =
                BigDecimal.valueOf(
                        tickSize(price)
                );

        return priceValue
                .remainder(tickValue)
                .compareTo(BigDecimal.ZERO)
                == 0;
    }

    public void validateAligned(
            double price
    ) {
        if (!isAligned(price)) {
            throw new IllegalArgumentException(
                    "price "
                            + price
                            + " is not aligned to tick "
                            + tickSize(price)
            );
        }
    }

    static void validatePrice(
            double price
    ) {
        if (
                !Double.isFinite(price)
                        || price <= 0.0
        ) {
            throw new IllegalArgumentException(
                    "price must be finite and positive"
            );
        }
    }
}
