package com.mohymods.kingdomsmp.economy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MarketPricingTest {
    @Test void buyingAndSellingMoveInOppositeDirectionsThroughAllFourBands() {
        long[] payouts = {8, 8, 5, 5, 3, 3, 1, 1};
        long[] costs = {1, 1, 4, 4, 6, 6, 8, 8};
        for (int used = 0; used < 8; used++) {
            assertEquals(payouts[used], MarketPricing.unit(1, 8, used, 8, false));
            assertEquals(costs[used], MarketPricing.unit(1, 8, used, 8, true));
        }
        assertEquals(34, MarketPricing.total(1, 8, 0, 8, 8, false));
        assertEquals(38, MarketPricing.total(1, 8, 0, 8, 8, true));
    }

    @Test void unevenQuotasRemainPositiveAndHaveDeterministicMarginalPrices() {
        long[] payouts = {8, 8, 5, 3, 1};
        long[] costs = {1, 1, 4, 6, 8};
        for (int used = 0; used < 5; used++) {
            assertEquals(payouts[used], MarketPricing.unit(1, 8, used, 5, false));
            assertEquals(costs[used], MarketPricing.unit(1, 8, used, 5, true));
        }
        assertEquals(25, MarketPricing.total(1, 8, 0, 5, 5, false));
        assertEquals(20, MarketPricing.total(1, 8, 0, 5, 5, true));
    }

    @Test void aBatchCrossingABandDoesNotUseItsFirstPriceForEveryItem() {
        assertEquals(32, MarketPricing.total(4, 8, 63, 256, 5, false));
        assertEquals(28, MarketPricing.total(4, 8, 63, 256, 5, true));
        assertEquals(32, MarketPricing.total(4, 8, 63, 256, 1, false)
                + MarketPricing.total(4, 8, 64, 256, 4, false));
    }

    @Test void splittingAnySmallBatchAtAnyPointCannotImprovePriceOrPayout() {
        for (int limit = 1; limit <= 17; limit++) {
            for (int minimum = 1; minimum <= 3; minimum++) {
                for (int maximum = minimum; maximum <= minimum + 4; maximum++) {
                    for (boolean increasing : new boolean[]{false, true}) {
                        for (int used = 0; used < limit; used++) {
                            for (int quantity = 1; quantity <= limit - used; quantity++) {
                                long whole = MarketPricing.total(minimum, maximum, used, limit, quantity, increasing);
                                for (int first = 1; first < quantity; first++) {
                                    long split = MarketPricing.total(minimum, maximum, used, limit, first, increasing)
                                            + MarketPricing.total(minimum, maximum, used + first, limit, quantity - first, increasing);
                                    assertEquals(whole, split, "A split must retain the exact marginal total");
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test void pricesStayWithinBoundsAndRemainMonotonicForSmallAndLargeQuotas() {
        for (int limit : new int[]{1, 2, 3, 5, 7, 16, 31, 256, 1_000_000}) {
            for (long[] bounds : new long[][]{{1, 1}, {1, 2}, {1, 8}, {4, 8}, {999_990, 1_000_000}}) {
                for (boolean increasing : new boolean[]{false, true}) {
                    long previous = increasing ? bounds[0] : bounds[1];
                    // Sample every small quota position and the boundaries of large bands.
                    int[] positions = limit <= 256 ? java.util.stream.IntStream.range(0, limit).toArray()
                            : new int[]{0, 249_999, 250_000, 499_999, 500_000, 749_999, 750_000, 999_999};
                    for (int used : positions) {
                        long price = MarketPricing.unit(bounds[0], bounds[1], used, limit, increasing);
                        assertTrue(price >= bounds[0] && price <= bounds[1] && price > 0);
                        assertTrue(increasing ? price >= previous : price <= previous);
                        previous = price;
                    }
                }
            }
        }
    }

    @Test void equalPriceBoundsStayConstantForEveryBandAndBatch() {
        for (boolean increasing : new boolean[]{false, true}) {
            assertEquals(7, MarketPricing.unit(7, 7, 15, 16, increasing));
            assertEquals(112, MarketPricing.total(7, 7, 0, 16, 16, increasing));
            assertEquals(1, MarketPricing.unit(1, 1, 0, 1, increasing));
        }
    }

    @Test void affordabilityUsesMarginalPricesAndAcceptsOnlyTheFundedPrefix() {
        assertEquals(4, MarketPricing.affordable(4, 8, 63, 256, 5, 31, false));
        assertEquals(5, MarketPricing.affordable(4, 8, 63, 256, 5, 32, false));
        assertEquals(4, MarketPricing.affordable(4, 8, 63, 256, 5, 27, true));
        assertEquals(5, MarketPricing.affordable(4, 8, 63, 256, 5, 28, true));
    }

    @Test void affordabilityCannotExceedStockRequestOrBudget() {
        for (int limit : new int[]{1, 3, 7, 16}) {
            for (int used = 0; used <= limit; used++) {
                for (int requested : new int[]{1, 2, 5, 64}) {
                    for (long coins = 0; coins <= 40; coins++) {
                        for (boolean increasing : new boolean[]{false, true}) {
                            int accepted = MarketPricing.affordable(1, 8, used, limit, requested, coins, increasing);
                            assertTrue(accepted >= 0 && accepted <= requested && accepted <= limit - used);
                            long spent = accepted == 0 ? 0 : MarketPricing.total(1, 8, used, limit, accepted, increasing);
                            assertTrue(spent <= coins);
                            if (accepted < requested && accepted < limit - used) {
                                assertTrue(spent + MarketPricing.unit(1, 8, used + accepted, limit, increasing) > coins,
                                        "A refused next item must actually exceed the remaining budget");
                            }
                        }
                    }
                }
            }
        }
    }

    @Test void emptyBudgetExhaustedStockAndPartialRequestsHaveNoSideEffects() {
        assertEquals(0, MarketPricing.affordable(1, 8, 0, 16, 16, 0, false));
        assertEquals(0, MarketPricing.affordable(1, 8, 16, 16, 16, Long.MAX_VALUE, false));
        assertEquals(2, MarketPricing.affordable(1, 8, 6, 8, 64, Long.MAX_VALUE, true));
        assertEquals(1, MarketPricing.affordable(1, 8, 0, 16, 1, Long.MAX_VALUE, true));
    }

    @Test void largeAcceptedTotalsUseLongArithmeticWithoutOverflowingAnInteger() {
        long expected = 2_304_000_000L;
        for (boolean increasing : new boolean[]{false, true}) {
            assertEquals(expected, MarketPricing.total(1_000_000, 1_000_000, 997_696, 1_000_000, 2304, increasing));
            assertEquals(2304, MarketPricing.affordable(1_000_000, 1_000_000, 997_696, 1_000_000, 2304, Long.MAX_VALUE, increasing));
            assertEquals(2303, MarketPricing.affordable(1_000_000, 1_000_000, 997_696, 1_000_000, 2304, expected - 1, increasing));
        }
    }

    @Test void unitRejectsMalformedBoundsAndOverflowSizedInputs() {
        long[][] invalid = {
                {0, 1, 0, 1}, {-1, 1, 0, 1}, {2, 1, 0, 1}, {1, 1_000_001, 0, 1},
                {1, Long.MAX_VALUE, 0, 1}, {Long.MIN_VALUE, 1, 0, 1},
                {1, 8, -1, 16}, {1, 8, Long.MIN_VALUE, 16}, {1, 8, 16, 16},
                {1, 8, Long.MAX_VALUE, 16}, {1, 8, 0, 0}, {1, 8, 0, -1},
                {1, 8, 0, 1_000_001}, {1, 8, 0, Long.MAX_VALUE}
        };
        for (long[] values : invalid) {
            for (boolean increasing : new boolean[]{false, true}) {
                assertThrows(IllegalArgumentException.class,
                        () -> MarketPricing.unit(values[0], values[1], values[2], values[3], increasing));
            }
        }
    }

    @Test void totalRejectsInvalidOrOverQuotaQuantitiesInsteadOfTruncating() {
        for (int quantity : new int[]{Integer.MIN_VALUE, -1, 0, 2305, Integer.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class, () -> MarketPricing.total(1, 8, 0, 1_000_000, quantity, false));
        }
        assertThrows(IllegalArgumentException.class, () -> MarketPricing.total(1, 8, 7, 8, 2, false));
        assertThrows(IllegalArgumentException.class, () -> MarketPricing.total(1, 8, 8, 8, 1, false));
        assertThrows(IllegalArgumentException.class, () -> MarketPricing.total(1, 8, -1, 8, 1, false));
        assertThrows(IllegalArgumentException.class, () -> MarketPricing.total(1, Long.MAX_VALUE, 0, 8, 1, false));
        assertThrows(IllegalArgumentException.class, () -> MarketPricing.total(1, 8, 0, Long.MIN_VALUE, 1, false));
    }

    @Test void affordabilityRejectsMalformedRequestsAndBoundsEvenWhenStockIsExhausted() {
        for (int requested : new int[]{Integer.MIN_VALUE, -1, 0, 2305, Integer.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class, () -> MarketPricing.affordable(1, 8, 0, 8, requested, 100, false));
        }
        assertThrows(IllegalArgumentException.class, () -> MarketPricing.affordable(1, 8, 0, 8, 1, -1, false));
        assertThrows(IllegalArgumentException.class, () -> MarketPricing.affordable(1, 8, -1, 8, 1, 100, false));
        assertThrows(IllegalArgumentException.class, () -> MarketPricing.affordable(1, 8, 9, 8, 1, 100, false));
        assertThrows(IllegalArgumentException.class, () -> MarketPricing.affordable(1, 8, 0, Long.MAX_VALUE, 1, 100, false));
        for (long[] bounds : new long[][]{{0, 1}, {-1, 1}, {2, 1}, {1, 1_000_001}, {1, Long.MAX_VALUE}}) {
            assertThrows(IllegalArgumentException.class, () -> MarketPricing.affordable(bounds[0], bounds[1], 0, 8, 1, 0, false));
            assertThrows(IllegalArgumentException.class, () -> MarketPricing.affordable(bounds[0], bounds[1], 8, 8, 1, 0, false));
        }
    }
}
