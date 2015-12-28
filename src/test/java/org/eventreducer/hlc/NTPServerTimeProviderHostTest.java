package org.eventreducer.hlc;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest(InetAddress.class)
public class NTPServerTimeProviderHostTest {

    @Test(expected = UnknownHostException.class)
    public void testUnknownHost() throws UnknownHostException {
        mockStatic(InetAddress.class);
        Mockito.when(InetAddress.getAllByName("host")).thenThrow(new UnknownHostException("host"));
        new NTPServerTimeProvider(new String[]{"host"});
    }
}