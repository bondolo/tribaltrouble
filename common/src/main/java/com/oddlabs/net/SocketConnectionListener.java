package com.oddlabs.net;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * NIO TCP socket implementation of {@link ConnectionListener}.
 */
public final class SocketConnectionListener extends AbstractConnectionListener<InetAddress> implements Handler {
    private static final Logger logger = Logger.getLogger(SocketConnectionListener.class.getSimpleName());

    private final NetworkSelector network;
    private @Nullable SelectionKey key;

    private final Deque<SocketChannel> incoming_connections = new ArrayDeque<>();

    private static SelectionKey createServerSocket(NetworkSelector network, InetSocketAddress address)
            throws IOException {
        ServerSocketChannel server_channel = ServerSocketChannel.open();
        server_channel.configureBlocking(false);
        server_channel.socket().setReuseAddress(true);
        server_channel.socket().bind(address);
        return server_channel.register(network.getSelector(), SelectionKey.OP_ACCEPT);
    }

    public SocketConnectionListener(NetworkSelector network, InetSocketAddress address,
            ConnectionListenerInterface<? super InetAddress> connection_listener_interface) {
        super(connection_listener_interface);
        this.network = network;
        IOException exception;
        try {
            if (!network.getDeterministic().isPlayback()) {
                key = createServerSocket(network, address);
            }
            exception = null;
        } catch (IOException e) {
            exception = e;
        }
        if (network.getDeterministic().log(exception != null))
            error(network.getDeterministic().log(exception));
        else
            network.attachToKey(key, this);
    }

    public SocketConnectionListener(NetworkSelector network, @Nullable InetAddress ip, int port,
            ConnectionListenerInterface<? super InetAddress> connection_listener_interface) {
        this(network, new InetSocketAddress(ip, port), connection_listener_interface);
    }

    public int getPort() {
        return network.getDeterministic().log(key != null ? ((ServerSocketChannel) key.channel()).socket()
                .getLocalPort() : -1);
    }

    @Override
    public void handle() throws IOException {
        IOException exception = null;
        SocketChannel channel = null;
        if (!network.getDeterministic().isPlayback()) {
            ServerSocketChannel server_channel = (ServerSocketChannel) key.channel();
            channel = server_channel.accept();
            try {
                Connection.configureChannel(channel);
            } catch (IOException e) {
                try {
                    channel.close();
                } catch (IOException e2) {
                    logger.log(Level.WARNING, "Failed to close channel after connection error", e2);
                }
                exception = e;
            }
        }
        if (network.getDeterministic().log(exception != null))
            throw network.getDeterministic().log(exception);
        incoming_connections.add(channel);
        notifyIncomingConnection();
    }

    public void error(IOException e) {
        notifyError(e);
    }

    public void incoming(InetAddress remote_address) {
        notifyIncomingConnection(remote_address);
    }

    private void notifyIncomingConnection() {
        InetAddress remote_inet_address = null;
        if (!network.getDeterministic().isPlayback()) {
            SocketChannel channel = incoming_connections.getFirst();
            if (channel.socket().getRemoteSocketAddress() instanceof InetSocketAddress remote_address) {
                remote_inet_address = remote_address.getAddress();
            }
        }
        incoming(network.getDeterministic().log(remote_inet_address));
    }

    private SocketChannel removeNextChannel() {
        return incoming_connections.removeFirst();
    }

    private SocketChannel getNextConnection() {
        SocketChannel channel = removeNextChannel();
        if (!incoming_connections.isEmpty())
            notifyIncomingConnection();
        return channel;
    }

    private SelectionKey getNextConnectionKey() {
        try {
            SocketChannel channel = getNextConnection();
            SelectionKey socket_key = channel.register(network.getSelector(), SelectionKey.OP_READ);
            return socket_key;
        } catch (ClosedChannelException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public AbstractConnection acceptConnection(@Nullable ConnectionInterface conn_interface) {
        SelectionKey socket_key;
        if (!network.getDeterministic().isPlayback())
            socket_key = getNextConnectionKey();
        else
            socket_key = null;
        return new Connection(network, socket_key, conn_interface);
    }

    @Override
    public void rejectConnection() {
        try {
            SocketChannel channel = getNextConnection();
            if (!network.getDeterministic().isPlayback())
                channel.close();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error while rejecting connection", e);
        }
    }

    @Override
    public void close() {
        if (key != null && key.isValid()) {
            try {
                key.channel().close();
                while (!incoming_connections.isEmpty())
                    removeNextChannel().close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error while closing listener", e);
            }
        }
        if (network.getDeterministic().log(key != null))
            network.cancelKey(key, this);
    }

    @Override
    public void handleError(IOException e) throws IOException {
        error(e);
    }
}
