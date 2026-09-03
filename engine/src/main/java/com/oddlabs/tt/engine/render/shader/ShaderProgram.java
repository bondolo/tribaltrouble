package com.oddlabs.tt.engine.render.shader;

import com.oddlabs.tt.engine.render.state.ScopedState;
import com.oddlabs.tt.base.resource.NativeResource;
import com.oddlabs.util.Color;
import org.joml.Matrix4fc;
import org.joml.Vector2fc;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL32;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Holds native state for shaders including vertex, fragment, and optionally geometry shader.
 */
public abstract class ShaderProgram extends NativeResource<ShaderProgram.Program> implements Shader {
    private static final Logger logger = Logger.getLogger(ShaderProgram.class.getSimpleName());
    /**
     * the currently active shader or null
     */
    private static final AtomicReference<@Nullable ShaderProgram> inUse = new AtomicReference<>();

    static final class Program extends NativeResource.NativeState {
        final int programId;
        private final int vertexShaderId;
        private final int fragmentShaderId;
        private final int geometryShaderId;
        private final Map<String, Integer> uniformLocations = new HashMap<>();
        private final Map<String, Integer> attributeLocations = new HashMap<>();
        private final Map<Integer, float[]> mat4Uniforms = new HashMap<>();

        Program(int vertexShaderId, int fragmentShaderId, int geometryShaderId) {
            this.vertexShaderId = vertexShaderId;
            this.fragmentShaderId = fragmentShaderId;
            this.geometryShaderId = geometryShaderId;
            this.programId = GL20.glCreateProgram();

            GL20.glAttachShader(programId, vertexShaderId);
            GL20.glAttachShader(programId, fragmentShaderId);
            if (geometryShaderId != 0) {
                GL20.glAttachShader(programId, geometryShaderId);
            }
        }

        /**
         * complete linking after (optionally) setting up shader layouts
         */
        void link() {
            GL20.glLinkProgram(programId);
            if (GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                throw new IllegalArgumentException("Shader link failed: " + GL20.glGetProgramInfoLog(programId, 1024));
            }
            bindGlobalStateBlock();
        }

        private void bindGlobalStateBlock() {
            int blockIndex = GL31.glGetUniformBlockIndex(programId, "GlobalState");
            if (blockIndex != -1) {
                GL31.glUniformBlockBinding(programId, blockIndex, 0);
            }
        }

        @Override
        public void close() {
            GL20.glDetachShader(programId, vertexShaderId);
            GL20.glDetachShader(programId, fragmentShaderId);
            GL20.glDeleteShader(vertexShaderId);
            GL20.glDeleteShader(fragmentShaderId);
            if (geometryShaderId != 0) {
                GL20.glDetachShader(programId, geometryShaderId);
                GL20.glDeleteShader(geometryShaderId);
            }
            GL20.glDeleteProgram(programId);
        }
    }

    public ShaderProgram(String vertexSource, String fragmentSource) throws IllegalArgumentException {
        this(vertexSource, fragmentSource, null);
    }

    public ShaderProgram(String vertexSource, String fragmentSource, @Nullable String geometrySource)
            throws IllegalArgumentException {
        super(new Program(
                compileShader(GL20.GL_VERTEX_SHADER, vertexSource),
                compileShader(GL20.GL_FRAGMENT_SHADER, fragmentSource),
                geometrySource != null ? compileShader(GL32.GL_GEOMETRY_SHADER, geometrySource) : 0
        ));
    }

    protected void bindFragDataLocation(int colorNumber, String name) {
        GL30.glBindFragDataLocation(state.programId, colorNumber, name);
    }

    /**
     * complete linking after (optionally) setting up shader layouts
     */
    protected void link() {
        state.link();
    }

    private static int compileShader(int type, String source) throws IllegalArgumentException {
        int shaderId = GL20.glCreateShader(type);
        GL20.glShaderSource(shaderId, source);
        GL20.glCompileShader(shaderId);

        if (GL20.glGetShaderi(shaderId, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetShaderInfoLog(shaderId, 1024);
            GL20.glDeleteShader(shaderId);
            throw new IllegalArgumentException("Shader compilation failed: " + log);
        }
        return shaderId;
    }

    private boolean closed = false;

    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            super.close();
        }
    }

    public ScopedState use() {
        if (closed) {
            throw new IllegalStateException("Attempting to use a closed ShaderProgram: " + getClass().getName());
        }
        var active = inUse.compareAndExchange(null, this);
        if (null == active) {
            GL20.glUseProgram(state.programId);
            return this::disuse;
        }
        var ise = new IllegalStateException("Shader already in use; active=" + active);
        logger.log(Level.SEVERE, ise.getMessage(), ise);
        throw ise; // Throw the exception
    }

    public static @Nullable ShaderProgram activeShader() {
        return inUse.get();
    }

    @Override
    public boolean inUse() {
        return this == inUse.get();
    }

    @Override
    public int getAttributeLocation(String name) {
        return state.attributeLocations.computeIfAbsent(name, n -> GL20.glGetAttribLocation(state.programId, n));
    }

    @Override
    public int getUniformLocation(String name) {
        return state.uniformLocations.computeIfAbsent(name, n -> GL20.glGetUniformLocation(state.programId, n));
    }

    @Override
    public void setUniform(String name, int[] values) {
        setUniform(getUniformLocation(name), values);
    }

    @Override
    public void setUniform(int loc, int[] values) {
        if (loc == -1) return;
        GL20.glUniform1iv(loc, values);
    }

    @Override
    public void setUniform(String name, int value) {
        setUniform(getUniformLocation(name), value);
    }

    @Override
    public void setUniform(int loc, int value) {
        if (loc == -1) return;
        GL20.glUniform1i(loc, value);
    }

    @Override
    public void setUniform(String name, float value) {
        setUniform(getUniformLocation(name), value);
    }

    @Override
    public void setUniform(int loc, float value) {
        if (loc == -1) return;
        GL20.glUniform1f(loc, value);
    }

    @Override
    public void setUniform(String name, boolean value) {
        setUniform(getUniformLocation(name), value);
    }

    @Override
    public void setUniform(int loc, boolean value) {
        setUniform(loc, value ? 1 : 0);
    }

    @Override
    public void setUniform(String name, float x, float y) {
        setUniform(getUniformLocation(name), x, y);
    }

    @Override
    public void setUniform(int loc, float x, float y) {
        if (loc == -1) return;
        GL20.glUniform2f(loc, x, y);
    }

    public void setUniform(String name, Vector2fc value) {
        setUniform(getUniformLocation(name), value);
    }

    public void setUniform(int loc, Vector2fc value) {
        if (loc == -1) return;
        GL20.glUniform2f(loc, value.x(), value.y());
    }

    @Override
    public void setUniform(String name, float x, float y, float z) {
        setUniform(getUniformLocation(name), x, y, z);
    }

    @Override
    public void setUniform(int loc, float x, float y, float z) {
        if (loc == -1) return;
        GL20.glUniform3f(loc, x, y, z);
    }

    public void setUniform(String name, Vector3fc value) {
        setUniform(getUniformLocation(name), value);
    }

    public void setUniform(int loc, Vector3fc value) {
        if (loc == -1) return;
        GL20.glUniform3f(loc, value.x(), value.y(), value.z());
    }

    @Override
    public void setUniform(String name, Color value) {
        setUniform(getUniformLocation(name), value);
    }

    @Override
    public void setUniform(int loc, Color value) {
        if (loc == -1) return;
        var linearColor = value instanceof Color.Linear linear ? linear : new Color.Linear(value);
        GL20.glUniform4f(loc, linearColor.r(), linearColor.g(), linearColor.b(), linearColor.a());
    }

    public void setUniformColor3(String name, Color value) {
        setUniformColor3(getUniformLocation(name), value);
    }

    public void setUniformColor3(int loc, Color value) {
        if (loc == -1) return;
        var linearColor = value instanceof Color.Linear linear ? linear : new Color.Linear(value);
        GL20.glUniform3f(loc, linearColor.r(), linearColor.g(), linearColor.b());
    }

    @Override
    public void setUniform(String name, Matrix4fc matrix) {
        setUniform(getUniformLocation(name), false, matrix);
    }

    @Override
    public void setUniform(int loc, Matrix4fc matrix) {
        setUniform(loc, false, matrix);
    }

    @Override
    public void setUniform(String name, boolean transpose, Matrix4fc matrix) {
        setUniform(getUniformLocation(name), transpose, matrix);
    }

    @Override
    public void setUniform(int loc, boolean transpose, Matrix4fc matrix) {
        if (loc == -1) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            setUniformMatrix4(loc, transpose, matrix.get(stack.mallocFloat(16)));
        }
    }

    private void setUniformMatrix4(int loc, boolean transpose, FloatBuffer matrix) {
        float[] cached = state.mat4Uniforms.get(loc);
        int pos = matrix.position();
        if (cached == null) {
            cached = new float[16];
            state.mat4Uniforms.put(loc, cached);
        } else {
            boolean matches = true;
            for (int i = 0; i < 16; i++) {
                if (cached[i] != matrix.get(pos + i)) {
                    matches = false;
                    break;
                }
            }
            if (matches) return;
        }

        for (int i = 0; i < 16; i++) {
            cached[i] = matrix.get(pos + i);
        }

        GL20.glUniformMatrix4fv(loc, transpose, matrix);
    }


    private void disuse() {
        var active = inUse.compareAndExchange(this, null);
        if (this == active) {
            GL20.glUseProgram(0);
        } else {
            var ise = new IllegalStateException("Shader not in use; active=" + active);
            logger.log(Level.SEVERE, ise.getMessage(), ise);
            throw ise; // Throw the exception
        }
    }
}
