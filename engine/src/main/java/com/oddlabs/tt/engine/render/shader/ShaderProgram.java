package com.oddlabs.tt.engine.render.shader;

import com.oddlabs.tt.engine.render.state.ScopedState;
import com.oddlabs.tt.base.resource.NativeResource;
import com.oddlabs.util.Color;
import org.joml.Matrix4fc;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL40;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
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
        private final Map<Integer, Integer> intUniforms = new HashMap<>();
        private final Map<Integer, Float> floatUniforms = new HashMap<>();
        private final Map<Integer, Vector2fc> vec2Uniforms = new HashMap<>();
        private final Map<Integer, Vector3fc> vec3Uniforms = new HashMap<>();
        private final Map<Integer, Color> colorUniforms = new HashMap<>();
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
            bindStandardAttributes();
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

        private void bindStandardAttributes() {
            GL20.glBindAttribLocation(programId, POSITION_LOC, POSITION);
            GL20.glBindAttribLocation(programId, NORMAL_LOC, NORMAL);
            GL20.glBindAttribLocation(programId, TEX_COORD_LOC, TEX_COORD);
            GL20.glBindAttribLocation(programId, COLOR_LOC, COLOR);
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
        int loc = getUniformLocation(name);
        if (loc == -1) return;
        GL20.glUniform1iv(loc, values);
    }

    @Override
    public void setUniform(String name, int value) {
        int loc = getUniformLocation(name);
        if (loc == -1) return;
        Integer lastValue = state.intUniforms.get(loc);
        if (lastValue != null && lastValue == value) return;
        GL20.glUniform1i(loc, value);
        state.intUniforms.put(loc, value);
    }

    @Override
    public void setUniform(String name, float value) {
        int loc = getUniformLocation(name);
        if (loc == -1) return;
        Float lastValue = state.floatUniforms.get(loc);
        if (lastValue != null && lastValue == value) return;
        GL20.glUniform1f(loc, value);
        state.floatUniforms.put(loc, value);
    }

    @Override
    public void setUniform(String name, boolean value) {
        setUniform(name, value ? 1 : 0);
    }

    @Override
    public void setUniform(String name, float x, float y) {
        setUniform(name, new Vector2f(x, y));
    }

    public void setUniform(String name, Vector2fc value) {
        int loc = getUniformLocation(name);
        if (loc == -1) return;
        Vector2fc lastValue = state.vec2Uniforms.get(loc);
        if (lastValue != null && lastValue.equals(value)) return;
        GL20.glUniform2f(loc, value.x(), value.y());
        state.vec2Uniforms.put(loc, new Vector2f(value));
    }

    @Override
    public void setUniform(String name, float x, float y, float z) {
        setUniform(name, new Vector3f(x, y, z));
    }

    public void setUniform(String name, Vector3fc value) {
        int loc = getUniformLocation(name);
        if (loc == -1) return;
        Vector3fc lastValue = state.vec3Uniforms.get(loc);
        if (lastValue != null && lastValue.equals(value)) return;
        GL20.glUniform3f(loc, value.x(), value.y(), value.z());
        state.vec3Uniforms.put(loc, new Vector3f(value));
    }

    @Override
    public void setUniform(String name, Color value) {
        int loc = getUniformLocation(name);
        if (loc == -1) return;
        Color lastValue = state.colorUniforms.get(loc);
        var linearColor = value instanceof Color.Linear linear ? linear : new Color.Linear(value);
        if (lastValue != null && lastValue.equals(linearColor)) return;
        GL20.glUniform4f(loc, linearColor.r(), linearColor.g(), linearColor.b(), linearColor.a());
        state.colorUniforms.put(loc, linearColor);
    }

    public void setUniformColor3(String name, Color value) {
        int loc = getUniformLocation(name);
        if (loc == -1) return;

        // We reuse the colorUniforms cache but only upload 3 components
        Color lastValue = state.colorUniforms.get(loc);
        if (lastValue != null && lastValue.equals(value)) return;

        var linearColor = value instanceof Color.Linear linear ? linear : new Color.Linear(value);
        GL20.glUniform3f(loc, linearColor.r(), linearColor.g(), linearColor.b());
        state.colorUniforms.put(loc, linearColor);
    }

    @Override
    public void setUniform(String name, Matrix4fc matrix) {
        setUniform(name, false, matrix);
    }

    @Override
    public void setUniform(String name, boolean transpose, Matrix4fc matrix) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            setUniformMatrix4(name, transpose, matrix.get(stack.mallocFloat(16)));
        }
    }

    private void setUniformMatrix4(String name, boolean transpose, FloatBuffer matrix) {
        int loc = getUniformLocation(name);
        if (loc == -1) return;

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

    /**
     * Binds a set of subroutines for the fragment shader.
     *
     * @param uniformToSubroutine A map where the key is the subroutine uniform name and the value is the subroutine
     *            function name.
     */
    protected void setFragmentSubroutines(Map<String, String> uniformToSubroutine) {
        int count = GL40.glGetProgramStagei(state.programId, GL20.GL_FRAGMENT_SHADER,
                GL40.GL_ACTIVE_SUBROUTINE_UNIFORM_LOCATIONS);
        if (count <= 0) return;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer indices = stack.callocInt(count); // initialized to 0
            for (Map.Entry<String, String> entry : uniformToSubroutine.entrySet()) {
                int loc = GL40.glGetSubroutineUniformLocation(state.programId, GL20.GL_FRAGMENT_SHADER, entry.getKey());
                if (loc >= 0) {
                    int index = GL40.glGetSubroutineIndex(state.programId, GL20.GL_FRAGMENT_SHADER, entry.getValue());
                    if (index != GL31.GL_INVALID_INDEX) {
                        indices.put(loc, index);
                    }
                }
            }
            GL40.glUniformSubroutinesuiv(GL20.GL_FRAGMENT_SHADER, indices);
        }
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
