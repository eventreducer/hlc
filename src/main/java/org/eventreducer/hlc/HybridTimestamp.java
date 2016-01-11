package org.eventreducer.hlc;

import lombok.Getter;
import org.apache.commons.net.ntp.TimeStamp;

/**
 * HybridTimestamp implements <a href="http://www.cse.buffalo.edu/tech-reports/2014-04.pdf">Hybrid Logical Clock</a>,
 * currently heavily inspired by a corresponding <a href="https://github.com/tschottdorf/hlc-rs">Rust library</a>.
 */
public class HybridTimestamp implements Timestamped {

    private final PhysicalTimeProvider physicalTimeProvider;

    @Getter
    long logicalTime = 0;
    @Getter
    long logicalCounter = 0;

    public HybridTimestamp(PhysicalTimeProvider physicalTimeProvider) {
        this(physicalTimeProvider, 0, 0);
    }

    public HybridTimestamp(PhysicalTimeProvider physicalTimeProvider, long timestamp) {
        this(physicalTimeProvider, timestamp >> 16, timestamp << 48 >> 48);
    }

    public HybridTimestamp(PhysicalTimeProvider physicalTimeProvider, long logicalTime, long logicalCounter) {
        this.physicalTimeProvider = physicalTimeProvider;
        this.logicalTime = logicalTime;
        this.logicalCounter = logicalCounter;
    }

    /**
     * Creates a new instance of HybridTimestamp with the same data
     * @return a new object instance
     */
    public HybridTimestamp clone() {
        return new HybridTimestamp(physicalTimeProvider, logicalTime, logicalCounter);
    }

    /**
     * Compares two NTP timestamps (non-numerically)
     * @param time1
     * @param time2
     * @return 0 if equal, less than 0 if time1 &lt; time2, more than 0 if time1 &gt; time2
     */
    private int compare(long time1, long time2) {
        TimeStamp t1 = new TimeStamp(time1);
        TimeStamp t2 = new TimeStamp(time2);
        if (t1.getSeconds() == t2.getSeconds() && t1.getFraction() == t2.getFraction()) {
            return 0;
        }
        if (t1.getSeconds() == t2.getSeconds()) {
            return t1.getFraction() < t2.getFraction() ? -1 : 1;
        }
        return t1.getSeconds() < t2.getSeconds() ? -1 : 1;
    }


    /**
     * Updates timestamp for local or send events
     * @return updated timestamp
     */
    public long update() {
        long physicalTime = physicalTimeProvider.getPhysicalTime();
        if (compare(logicalTime, physicalTime) < 0) {
            logicalTime = physicalTime;
            logicalCounter = 0;
        } else {
            logicalCounter++;
        }
        return timestamp();
    }

    /**
     * Updates timestamp for a received event
     * @param timestamped Object that implements Timestamped interface
     * @return updated timestamp
     */
    public long update(Timestamped timestamped) {
        long timestamp = timestamped.timestamp();
        return update(timestamp >> 16, timestamp << 48 >> 48);
    }

    /**
     * Updates timestamp for a received event
     * @param eventLogicalTime Received event logical time
     * @param eventLogicalCounter Received event logical counter
     * @return updated timestamp
     */
    public long update(long eventLogicalTime, long eventLogicalCounter) {
        long physicalTime = physicalTimeProvider.getPhysicalTime();
        if (compare(physicalTime, eventLogicalTime) > 0 &&
            compare(physicalTime, logicalTime) > 0) {
            logicalTime = physicalTime;
            logicalCounter = 0;
        } else if (compare(eventLogicalTime, logicalTime) > 0) {
            logicalTime = eventLogicalTime;
            logicalCounter++;
        } else if (compare(logicalTime, eventLogicalTime) > 0) {
            logicalCounter++;
        } else {
            if (eventLogicalCounter > logicalCounter) {
                logicalCounter = eventLogicalCounter;
            }
            logicalCounter++;
        }
        return timestamp();
    }

    /**
     * @return 64-bit timestamp
     */
    public long timestamp() {
        return (logicalTime >> 16 << 16) | (logicalCounter << 48 >> 48);
    }


    public String toString() {
        String logical = TimeStamp.getNtpTime(logicalTime).toUTCString();
        String ntpValue = TimeStamp.getNtpTime(timestamp()).toUTCString();
        return "<[" + logical + "]," + logicalCounter + ": " + ntpValue + ">";
    }
}
