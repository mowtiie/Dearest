package com.mowtiie.dearest.backup;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class BackupManagerTest {

    @Test
    public void readsASmallStreamInFull() throws Exception {
        byte[] data = new byte[20_000];
        for (int i = 0; i < data.length; i++) data[i] = (byte) i;

        assertArrayEquals(data, BackupManager.readLimited(new ByteArrayInputStream(data), 20_000));
    }

    @Test
    public void refusesAStreamOverTheLimit() {
        ByteArrayInputStream tooBig = new ByteArrayInputStream(new byte[20_001]);

        assertThrows(IOException.class, () -> BackupManager.readLimited(tooBig, 20_000));
    }
}
