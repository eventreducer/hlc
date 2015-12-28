package org.eventreducer.hlc;


/**
 * PhysicalTimeProvider interface allows connecting different implementations
 * of NTP 64-bit timestamps
 */
public interface PhysicalTimeProvider {

    /**
     * @return Current timestamp as an NTP 64-bit value.
     */
    long getPhysicalTime();
}
