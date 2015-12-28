package org.eventreducer.hlc;

import com.google.common.util.concurrent.ServiceManager;
import com.googlecode.junittoolbox.ParallelParameterized;
import org.apache.commons.net.ntp.TimeStamp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

@RunWith(ParallelParameterized.class)
public class NTPServerTimeProviderTest {

    private ServiceManager serviceManager;

    @Parameterized.Parameters(name = "{index}: delay={0}")
    public static Collection<Integer> delays() {
        return IntStream.generate(() -> new Random().nextInt(3000)).
                limit(100).
                boxed().collect(Collectors.toList());
    }

    @Parameterized.Parameter
    public int delay;

    private NTPServerTimeProvider provider;

    @Before
    public void setup() throws UnknownHostException, ExecutionException, InterruptedException {
        provider = new NTPServerTimeProvider(new String[]{"localhost"}); // use localhost to avoid delays and usage caps
        serviceManager = new ServiceManager(Arrays.asList(provider));
        serviceManager.startAsync().awaitHealthy();
    }

    @After
    public void teardown() throws ExecutionException, InterruptedException {
        serviceManager.stopAsync().awaitStopped();
    }

    @Test(timeout = 4000)
    public void secondsPassed() throws UnknownHostException, InterruptedException {
        TimeStamp ts1 = provider.getTimestamp();
        Thread.sleep(delay);
        TimeStamp ts2 = provider.getTimestamp();
        long seconds = delay / 1000;
        // Verify that seconds passed were calculated properly
        // since the last time NTP timestamp was fetched. Measuring fractions
        // is pointless as there's a gap between sleeping and requesting the timestamp.
        assertEquals("Delay=" + delay + " time_diff=" + (ts2.getTime() - ts1.getTime()), seconds, (ts2.getTime() - ts1.getTime()) / 1000);
    }

}