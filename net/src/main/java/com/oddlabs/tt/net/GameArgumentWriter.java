package com.oddlabs.tt.net;

import com.oddlabs.net.DefaultARMIArgumentWriter;
import com.oddlabs.tt.simulation.model.Distributable;
import com.oddlabs.tt.simulation.model.DistributableTable;
import com.oddlabs.tt.simulation.model.Target;
import com.oddlabs.util.ByteBufferOutputStream;

import java.io.IOException;

/** Serializer for custom ARMI game command parameters. */
public final class GameArgumentWriter extends DefaultARMIArgumentWriter {
    private final DistributableTable distributable_table;

    public GameArgumentWriter(DistributableTable table) {
        this.distributable_table = table;
    }

    @Override
    public void writeArgument(Class<?> type, Object arg, ByteBufferOutputStream out)
            throws IOException {
        if (Distributable.class.isAssignableFrom(type) || Target.class.isAssignableFrom(type)) {
            if (arg instanceof Distributable distributable) {
                int name = distributable_table.getName(distributable);
                out.buffer().putInt(name);
            } else {
                throw new IllegalArgumentException("Cannot serialize non-distributable argument of type "
                        + type.getName() + ": " + arg);
            }
        } else if (Distributable[].class.isAssignableFrom(type) || Target[].class.isAssignableFrom(type)) {
            Object[] items = (Object[]) arg;
            out.buffer().putShort((short) items.length);
            for (Object item : items) {
                if (item instanceof Distributable distributable) {
                    int name = distributable_table.getName(distributable);
                    out.buffer().putInt(name);
                } else {
                    throw new IllegalArgumentException("Cannot serialize non-distributable array element: " + item);
                }
            }
        } else {
            super.writeArgument(type, arg, out);
        }
    }
}
