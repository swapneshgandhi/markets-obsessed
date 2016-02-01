package utils;

import java.util.Random;

/**
 * Creates a randomly generated price based on the previous price
 */
public class FakeStockQuote implements StockQuote {

    public Float newPrice(Float lastPrice) {
        // todo: this trends towards zero
        return new Float(lastPrice * (0.95 + (0.1 * new Random().nextFloat()))); // lastPrice * (0.95 to 1.05)
    }

}
