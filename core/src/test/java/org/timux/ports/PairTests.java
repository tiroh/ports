package org.timux.ports;

import org.junit.jupiter.api.Test;
import org.timux.ports.types.Pair;
import org.timux.ports.types.PairX;
import org.timux.ports.types.Triple;
import org.timux.ports.types.TripleX;
import org.timux.ports.types.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PairTests {

    @Test
    public void pairTripleEquals() {
        TripleX<Integer> t1 = Tuple.ofX(1, 2, 3);
        TripleX<Integer> t2 = Tuple.ofX(1, 2, 3);

        assertEquals(t1, t2);

        TripleX<Integer> t3 = Tuple.ofX(0, 2, 3);
        TripleX<Integer> t4 = Tuple.ofX(1, 0, 3);
        TripleX<Integer> t5 = Tuple.ofX(1, 2, 0);

        assertNotEquals(t1, t3);
        assertNotEquals(t1, t4);
        assertNotEquals(t1, t5);

        Pair<Integer, Integer> p1 = Tuple.of(1, 2);
        Pair<Integer, Integer> p2 = Tuple.of(1, 2);

        assertEquals(p1, p2);

        Pair<Integer, Integer> p3 = Tuple.of(0, 2);
        Pair<Integer, Integer> p4 = Tuple.of(1, 0);

        assertNotEquals(p1, p3);
        assertNotEquals(p1, p4);
    }

    @Test
    public void pairTripleContains() {
        TripleX<Integer> t1 = Tuple.ofX(1, 2, 3);

        PairX<Integer> p1 = Tuple.ofX(1, 2);
        PairX<Integer> p2 = Tuple.ofX(1, 3);
        PairX<Integer> p3 = Tuple.ofX(2, 3);
        PairX<Integer> p4 = Tuple.ofX(2, 1);
        PairX<Integer> p5 = Tuple.ofX(3, 1);
        PairX<Integer> p6 = Tuple.ofX(3, 2);

        assertTrue(t1.containsDistinct(p1));
        assertTrue(t1.containsDistinct(p2));
        assertTrue(t1.containsDistinct(p3));
        assertTrue(t1.containsDistinct(p4));
        assertTrue(t1.containsDistinct(p5));
        assertTrue(t1.containsDistinct(p6));

        Pair<Integer, Integer> p7 = Tuple.of(0, 2);
        Pair<Integer, Integer> p8 = Tuple.of(1, 0);

        assertFalse(t1.containsDistinct(p7));
        assertFalse(t1.containsDistinct(p8));

        Pair<Integer, Integer> p9 = Tuple.of(1, 1);
        Pair<Integer, Integer> p10 = Tuple.of(2, 2);
        Pair<Integer, Integer> p11 = Tuple.of(2, null);

        assertFalse(t1.containsDistinct(p9));
        assertFalse(t1.containsDistinct(p10));
        assertFalse(t1.containsDistinct(p11));
    }

    @Test
    public void pairTripleContainsRandomized() {
        Random random = new Random(0L);

        for (int i = 0; i < 10000; i++) {
            Integer a = random.nextInt(3) > 0 ? random.nextInt(10) : null;
            Integer b = random.nextInt(3) > 0 ? random.nextInt(10) : null;
            Integer c = random.nextInt(3) > 0 ? random.nextInt(10) : null;

            TripleX<Integer> triple = Tuple.ofX(a, b, c);

            int x = random.nextInt(3);
            int y;

            do {
                y = random.nextInt(3);
            } while (y == x);

            boolean shallBeEqual = random.nextBoolean();

            if (shallBeEqual) {
                Pair<Integer, Integer> pair = Tuple.of(triple.get(x), triple.get(y));
                assertTrue(triple.containsDistinct(pair));
            } else {
                Pair<Integer, Integer> pair = Tuple.of(Integer.MAX_VALUE, triple.get(y));
                assertFalse(triple.containsDistinct(pair));

                pair = Tuple.of(triple.get(x), Integer.MAX_VALUE);
                assertFalse(triple.containsDistinct(pair));
            }
        }
    }

    @Test
    public void forEach() {
        Pair<Integer, Double> pair = Tuple.of(1, 2.5);
        Triple<Integer, Float, Double> triple = Tuple.of(1, 2.5f, 3.5);

        List<String> results = new ArrayList<>();

        pair.forEach(s -> results.add(s + "p"));
        triple.forEach(s -> results.add(s + "t"));

        assertArrayEquals(new String[] {"1p", "2.5p", "1t", "2.5t", "3.5t"}, results.toArray());
    }

    @Test
    public void forEachNotNull() {
        Pair<Integer, Double> pair = Tuple.of(1, 2.5);
        Triple<Integer, Float, Double> triple = Tuple.of(1, 2.5f, 3.5);

        Pair<Integer, Double> pairWithNull = Tuple.of(1, null);
        Triple<Integer, Float, Double> tripleWithNull = Tuple.of(1, null, 3.5);

        List<String> results = new ArrayList<>();
        List<String> resultsWithNull = new ArrayList<>();

        pair.forEachNotNull(s -> results.add(s + "p"));
        triple.forEachNotNull(s -> results.add(s + "t"));

        pairWithNull.forEachNotNull(s -> resultsWithNull.add(s + "p"));
        tripleWithNull.forEachNotNull(s -> resultsWithNull.add(s + "t"));

        assertArrayEquals(new String[] {"1p", "2.5p", "1t", "2.5t", "3.5t"}, results.toArray());
        assertArrayEquals(new String[] {"1p", "1t", "3.5t"}, resultsWithNull.toArray());
    }

    @Test
    public void forEachX() {
        PairX<Integer> pair = Tuple.ofX(1, 2);
        TripleX<Integer> triple = Tuple.ofX(1, 2, 3);

        List<String> results = new ArrayList<>();

        pair.forEachX(s -> results.add(s + "p"));
        triple.forEachX(s -> results.add(s + "t"));

        assertArrayEquals(new String[] {"1p", "2p", "1t", "2t", "3t"}, results.toArray());
    }

    @Test
    public void forEachXNotNull() {
        PairX<Integer> pair = Tuple.ofX(1, 2);
        TripleX<Integer> triple = Tuple.ofX(1, 2, 3);

        PairX<Integer> pairWithNull = Tuple.ofX(1, null);
        TripleX<Integer> tripleWithNull = Tuple.ofX(1, null, 3);

        List<String> results = new ArrayList<>();
        List<String> resultsWithNull = new ArrayList<>();

        pair.forEachXNotNull(s -> results.add(s + "p"));
        triple.forEachXNotNull(s -> results.add(s + "t"));

        pairWithNull.forEachXNotNull(s -> resultsWithNull.add(s + "p"));
        tripleWithNull.forEachXNotNull(s -> resultsWithNull.add(s + "t"));

        assertArrayEquals(new String[] {"1p", "2p", "1t", "2t", "3t"}, results.toArray());
        assertArrayEquals(new String[] {"1p", "1t", "3t"}, resultsWithNull.toArray());
    }

    @Test
    public void reverse() {
        Pair<Integer, Double> pair = Tuple.of(1, 2.5);
        Triple<Integer, Float, Double> triple = Tuple.of(1, 2.5f, 3.5);

        PairX<Integer> pairX = Tuple.ofX(1, 2);
        TripleX<Integer> tripleX = Tuple.ofX(1, 2, 3);

        Pair<Double, Integer> pairRev = pair.reverse();
        Triple<Double, Float, Integer> tripleRev = triple.reverse();

        PairX<Integer> pairXRev = pairX.reverse();
        TripleX<Integer> tripleXRev = tripleX.reverse();

        assertEquals(pair.getB(), pairRev.getA());
        assertEquals(pair.getA(), pairRev.getB());

        assertEquals(triple.getC(), tripleRev.getA());
        assertEquals(triple.getB(), tripleRev.getB());
        assertEquals(triple.getA(), tripleRev.getC());

        assertEquals(pairX.getB(), pairXRev.getA());
        assertEquals(pairX.getA(), pairXRev.getB());

        assertEquals(tripleX.getC(), tripleXRev.getA());
        assertEquals(tripleX.getB(), tripleXRev.getB());
        assertEquals(tripleX.getA(), tripleXRev.getC());
    }

    @Test
    public void reduce() {
        Pair<Integer, Double> pair = Tuple.of(1, 2.5);
        String result1 = pair.reduce((i, d) -> Integer.toString(i) + d);

        Triple<Integer, Float, Double> triple = Tuple.of(1, 2.5f, 3.5);
        String result2 = triple.reduce((i, f, d) -> Integer.toString(i) + f + d);

        assertEquals("12.5", result1);
        assertEquals("12.53.5", result2);
    }
}
