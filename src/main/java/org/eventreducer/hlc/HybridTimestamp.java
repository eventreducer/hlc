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
     * Updates timestamp for local or send events
     * @return updated timestamp
     */
    public long update() {
        long physicalTime = physicalTimeProvider.getPhysicalTime();
        if (logicalTime < physicalTime) {
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
        if (physicalTime > eventLogicalTime && physicalTime > logicalTime) {
            logicalTime = physicalTime;
            logicalCounter = 0;
        } else if (eventLogicalTime > logicalTime) {
            logicalTime = eventLogicalTime;
            logicalCounter++;
        } else if (logicalTime > eventLogicalTime) {
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
