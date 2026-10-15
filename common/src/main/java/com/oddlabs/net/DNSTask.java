package com.oddlabs.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Callable;

/**
 * Resolves a DNS hostname asynchronously on a virtual thread and connects the socket on completion.
 */
record DNSTask(String dnsName, int port, Connection connection)
        implements Callable<InetSocketAddress>, TaskCallback<InetSocketAddress> {

    @Override
    public InetSocketAddress call() throws IOException {
        InetAddress inetAddress = InetAddress.getByName(dnsName);
        return new InetSocketAddress(inetAddress, port);
    }

    @Override
    public void taskCompleted(InetSocketAddress result) {
        connection.connect(result);
    }

    @Override
    public void taskFailed(Throwable e) {
        connection.dnsError(e instanceof IOException ioException ? ioException : new IOException(e));
    }
}
