package org.eventreducer.hlc;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class HybridTimestampTest {

    class TestPhysicalTimeProvider implements PhysicalTimeProvider {

        private long physicalTime = 0;

        @Override
        public long getPhysicalTime() {
            return physicalTime;
        }

        public void setPhysicalTime(long physicalTime) {
            this.physicalTime = physicalTime;
        }
    };
    private TestPhysicalTimeProvider physicalTimeProvider;

    @Before
    public void setup() {
        physicalTimeProvider = new TestPhysicalTimeProvider();
    }

    @Test
    public void test() {
        long ts, ts_;
        HybridTimestamp timestamp = new HybridTimestamp(physicalTimeProvider);

        ts = (long)1 << 32 | 0;
        physicalTimeProvider.setPhysicalTime(ts);
        timestamp.update();
        assertEquals(ts, timestamp.getLogicalTime());
        assertEquals(0, timestamp.getLogicalCounter());

        // clock didn't move
        ts = (long)1 << 32 | 0;
        physicalTimeProvider.setPhysicalTime(ts);
        timestamp.update();
        assertEquals(ts, timestamp.getLogicalTime());
        assertEquals(1, timestamp.getLogicalCounter());

        // clock moved back
        ts = (long)0 << 32 | 1;
        physicalTimeProvider.setPhysicalTime(ts);
        ts_ = timestamp.getLogicalTime();
        timestamp.update();
        assertEquals(ts_, timestamp.getLogicalTime());
        assertEquals(2, timestamp.getLogicalCounter());

        // clock moved ahead
        ts = (long)2 << 32 | 0;
        physicalTimeProvider.setPhysicalTime(ts);
        timestamp.update();
        assertEquals(ts, timestamp.getLogicalTime());
        assertEquals(0, timestamp.getLogicalCounter());

        // event happens, but wall ahead
        ts = (long)3 << 32 | 0;
        physicalTimeProvider.setPhysicalTime(ts);
        timestamp.update((long)1 << 32 | 2, 3);
        assertEquals(ts, timestamp.getLogicalTime());
        assertEquals(0, timestamp.getLogicalCounter());

        // event happens, wall ahead but unchanged
        ts = (long)3 << 32 | 0;
        physicalTimeProvider.setPhysicalTime(ts);
        timestamp.update((long)1 << 32 | 2, 3);
        assertEquals(ts, timestamp.getLogicalTime());
        assertEquals(1, timestamp.getLogicalCounter());

        //  event happens at wall, which is still unchanged
        ts = (long)3 << 32 | 0;
        physicalTimeProvider.setPhysicalTime(ts);
        timestamp.update((long)3 << 32 | 0, 1);
        assertEquals(ts, timestamp.getLogicalTime());
        assertEquals(2, timestamp.getLogicalCounter());

        //  event with larger logical, wall unchaged
        ts = (long)3 << 32 | 0;
        physicalTimeProvider.setPhysicalTime(ts);
        timestamp.update((long)3 << 32 | 0, 99);
        assertEquals(ts, timestamp.getLogicalTime());
        assertEquals(100, timestamp.getLogicalCounter());

        // event with larger wall, our wall behind
        ts = (long)3 << 32 | 5;
        physicalTimeProvider.setPhysicalTime(ts);
        timestamp.update((long)4 << 32 | 4, 100);
        assertEquals((long) 4 << 32 | 4, timestamp.getLogicalTime());
        assertEquals(101, timestamp.getLogicalCounter());

        // event behind wall, but ahead of previous state
        ts = (long)5 << 32 | 0;
        physicalTimeProvider.setPhysicalTime(ts);
        timestamp.update((long)4 << 32 | 5, 0);
        assertEquals(ts, timestamp.getLogicalTime());
        assertEquals(0, timestamp.getLogicalCounter());

        ts = (long)4 << 32 | 9;
        physicalTimeProvider.setPhysicalTime(ts);
        timestamp.update((long)5 << 32 | 0, 99);
        assertEquals((long) 5 << 32 | 0, timestamp.getLogicalTime());
        assertEquals(100, timestamp.getLogicalCounter());

        // event at state, lower logical than state
        ts = (long)0 << 32 | 0;
        physicalTimeProvider.setPhysicalTime(ts);
        timestamp.update((long)5 << 32 | 0, 50);
        assertEquals((long) 5 << 32 | 0, timestamp.getLogicalTime());
        assertEquals(101, timestamp.getLogicalCounter());

        // another update API
        timestamp.update(timestamp);
        assertEquals((long) 5 << 32 | 0, timestamp.getLogicalTime());
        assertEquals(102, timestamp.getLogicalCounter());

    }

}