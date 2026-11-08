package com.oddlabs.tt.net;

import com.oddlabs.net.ByteBufferInputStream;
import com.oddlabs.net.DefaultARMIArgumentReader;
import com.oddlabs.tt.simulation.model.Distributable;
import com.oddlabs.tt.simulation.model.DistributableTable;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Target;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Deserializer for custom ARMI game command parameters. */
public final class GameArgumentReader extends DefaultARMIArgumentReader {
    private final DistributableTable distributable_table;

    public GameArgumentReader(DistributableTable table) {
        this.distributable_table = table;
    }

    @Override
    public @Nullable Object readArgument(Class<?> type, ByteBufferInputStream in) throws IOException,
            ClassNotFoundException {
        if (Distributable.class.isAssignableFrom(type) || Target.class.isAssignableFrom(type)) {
            int name = in.buffer().getInt();
            return distributable_table.getDistributable(name);
        } else if (Distributable[].class.isAssignableFrom(type) || Target[].class.isAssignableFrom(type)) {
            short length = in.buffer().getShort();
            Object[] array = (Object[]) Array.newInstance(type.getComponentType(), length);
            for (int j = 0; j < array.length; j++) {
                int name = in.buffer().getInt();
                array[j] = distributable_table.getDistributable(name);
            }
            return array;
        } else if (List.class.isAssignableFrom(type) || Collection.class.isAssignableFrom(type)) {
            Object[] array = (Object[]) readArgument(Distributable[].class, in);
            if (array == null) {
                return List.of();
            }
            List<Selectable<?>> list = new ArrayList<>(array.length);
            for (Object d : array) {
                if (d instanceof Selectable<?> selectable) {
                    list.add(selectable);
                }
            }
            return list;
        } else {
            return super.readArgument(type, in);
        }
    }
}
