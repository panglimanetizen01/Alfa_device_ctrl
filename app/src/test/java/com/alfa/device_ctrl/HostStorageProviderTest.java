package com.alfa.device_ctrl;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class HostStorageProviderTest {
    @Test public void providerBoundaryExposesOnlyProviderNeutralOperations() throws Exception {
        assertTrue(HostStorageProvider.class.isInterface());
        assertTrue(Modifier.isPublic(HostStorageProvider.class.getModifiers()));

        Method providerName = HostStorageProvider.class.getMethod("providerName");
        Method supports = HostStorageProvider.class.getMethod(
                "supports", HostStorageExportContract.DestinationMode.class);
        Method begin = HostStorageProvider.class.getMethod(
                "begin",
                HostStorageExportContract.ExportRequest.class,
                HostStorageProvider.ProviderDestination.class);

        assertEquals(String.class, providerName.getReturnType());
        assertEquals(boolean.class, supports.getReturnType());
        assertEquals(HostStorageProvider.ExportSession.class, begin.getReturnType());
        assertEquals(HostStorageBridge.HostStorageException.class, begin.getExceptionTypes()[0]);
    }

    @Test public void destinationBoundaryCarriesCanonicalProviderMetadataOnly() throws Exception {
        assertTrue(HostStorageProvider.ProviderDestination.class.isInterface());
        assertEquals(String.class,
                HostStorageProvider.ProviderDestination.class.getMethod("providerName").getReturnType());
        assertEquals(String.class,
                HostStorageProvider.ProviderDestination.class.getMethod("canonicalUri").getReturnType());
        assertEquals(String.class,
                HostStorageProvider.ProviderDestination.class.getMethod("documentId").getReturnType());
        assertEquals(String.class,
                HostStorageProvider.ProviderDestination.class.getMethod("displayName").getReturnType());
    }

    @Test public void exportSessionRemainsLifecycleBoundaryWithoutWriterMethods() {
        Method[] methods = HostStorageProvider.ExportSession.class.getDeclaredMethods();
        for (Method method : methods) {
            assertTrue(method.getName().equals("requestId")
                    || method.getName().equals("request")
                    || method.getName().equals("snapshot")
                    || method.getName().equals("writeEntries")
                    || method.getName().equals("cancel"));
        }
    }
}
