package com.bokisoftware.linesphere;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

/**
 * Renders the 3D line sphere.
 * 
 * @author Kaleb
 * @version 1.0
 */
public class SphereLineRenderer implements GLSurfaceView.Renderer
{
	// Draw a line sphere.
	private SphereLine sphere;
	// Draw the lighting.
	private Lighting lighting;
	// Primitives for the touch to rotate and pinch to zoom.
	private float dx = 0, dy = 0, zoom = 1;

	// Create a new perspective projection matrix. The height will stay the
	// same while the width will vary as per aspect ratio.
	private float ratio = 0;
	private float left = 0;
	private float right = 0;
	private float bottom = 0;
	private float top = 0;
	private float near = 0;
	private float far = 0;

	/**
	 * Create a new instance.
	 */
	public SphereLineRenderer()
	{
		sphere = new SphereLine(1, 5);
		lighting = new Lighting();
	}

	/**
	 * Set the change in the x-axis for the touch to rotate.
	 * 
	 * @param dx
	 *            the derivative of the x-axis touch.
	 */
	public void setDx(float dx)
	{
		this.dx = dx;
	}

	/**
	 * Set the change in the y-axis for the touch to rotate.
	 * 
	 * @param dy
	 *            the derivative of the y-axis touch.
	 */
	public void setDy(float dy)
	{
		this.dy = dy;
	}

	/** {@inheritDoc} */
	@Override
	public void onDrawFrame(GL10 gl)
	{
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

		// Set the handles for the shaders.
		sphere.setHandles();

		// Prepare the lighting with the shaders.
		lighting.renderLighting(dx, dy);

		// Draw the sphere.
		sphere.drawSphere(lighting.getMvpMatrix(), lighting.getViewMatrix(),
				lighting.getProjectionMatrix(), lighting.getModelMatrix(),
				lighting.getLightPosInEyeSpace());

		// lighting.drawLight();
	}

	/** {@inheritDoc} */
	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height)
	{
		// Set the OpenGL viewport to the same size as the surface.
		GLES20.glViewport(0, 0, width, height);

		// Create a new perspective projection matrix. The height will stay the
		// same while the width will vary as per aspect ratio.
		ratio = (float) width / height;
		left = -ratio;
		right = ratio;
		bottom = -1.0f;
		top = 1.0f;
		near = 1.0f;
		far = 10.0f;

		Matrix.frustumM(lighting.getProjectionMatrix(), 0, left, right, bottom,
				top, near, far);
	}

	/**
	 * The zoom factor for the touch to zoom.
	 * 
	 * @param mult the zoom factor.
	 */
	public final void zoom(float mult)
	{
		zoom *= mult;
		Matrix.frustumM(lighting.getProjectionMatrix(), 0, zoom * left, zoom
				* right, zoom * bottom, zoom * top, near, far);
	}

	/** {@inheritDoc} */
	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config)
	{
		// Set the background clear color to black.
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

		// Use culling to remove back faces.
		GLES20.glEnable(GLES20.GL_CULL_FACE);

		// Enable depth testing
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);

		GLES20.glDepthMask(false);

		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

		// Position the eye in front of the origin.
		final float eyeX = 0.0f;
		final float eyeY = 0.0f;
		final float eyeZ = -0.5f;

		// We are looking toward the distance
		final float lookX = 0.0f;
		final float lookY = 0.0f;
		final float lookZ = -5.0f;

		// Set our up vector. This is where our head would be pointing were we
		// holding the camera.
		final float upX = 0.0f;
		final float upY = 1.0f;
		final float upZ = 0.0f;

		// Set the view matrix. This matrix can be said to represent the camera
		// position.
		// NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination
		// of a model and
		// view matrix. In OpenGL 2, we can keep track of these matrices
		// separately if we choose.
		Matrix.setLookAtM(lighting.getViewMatrix(), 0, eyeX, eyeY, eyeZ, lookX,
				lookY, lookZ, upX, upY, upZ);

		final String vertexShader = getVertexShader();
		final String fragmentShader = getFragmentShader();

		final int vertexShaderHandle = compileShader(GLES20.GL_VERTEX_SHADER,
				vertexShader);
		final int fragmentShaderHandle = compileShader(
				GLES20.GL_FRAGMENT_SHADER, fragmentShader);

		// Draw the sphere.
		sphere.setPerVertexProgramHandle(createAndLinkProgram(
				vertexShaderHandle, fragmentShaderHandle, new String[]
				{ "a_Position", "a_Color", "a_Normal" }));


		// Define a simple shader program for our lighting.
		final String lightVertexShader = "uniform mat4 u_MVPMatrix;      \n"
				+ "attribute vec4 a_Position;     \n"
				+ "void main()                    \n"
				+ "{                              \n"
				+ "   gl_Position = u_MVPMatrix   \n"
				+ "               * a_Position;   \n"
				+ "   gl_PointSize = 5.0;         \n"
				+ "}                              \n";

		final String lightFragmentShader = "precision mediump float;       \n"
				+ "void main()                    \n"
				+ "{                              \n"
				+ "   gl_FragColor = vec4(1.0,    \n"
				+ "   1.0, 1.0, 1.0);             \n"
				+ "}                              \n";

		final int lightVertexShaderHandle = compileShader(
				GLES20.GL_VERTEX_SHADER, lightVertexShader);
		final int lightFragmentShaderHandle = compileShader(
				GLES20.GL_FRAGMENT_SHADER, lightFragmentShader);

		lighting.setPointProgramHandle(createAndLinkProgram(
				lightVertexShaderHandle, lightFragmentShaderHandle,
				new String[]
				{ "a_Position" }));

	}

	/**
	 * Helper function to compile a shader.
	 * 
	 * @param shaderType
	 *            The shader type.
	 * @param shaderSource
	 *            The shader source code.
	 * @return An OpenGL handle to the shader.
	 */
	private int compileShader(final int shaderType, final String shaderSource)
	{
		int shaderHandle = GLES20.glCreateShader(shaderType);

		if (shaderHandle != 0)
		{
			// Pass in the shader source.
			GLES20.glShaderSource(shaderHandle, shaderSource);

			// Compile the shader.
			GLES20.glCompileShader(shaderHandle);

			// Get the compilation status.
			final int[] compileStatus = new int[1];
			GLES20.glGetShaderiv(shaderHandle, GLES20.GL_COMPILE_STATUS,
					compileStatus, 0);

			// If the compilation failed, delete the shader.
			if (compileStatus[0] == 0)
			{
				Log.e("ShadedSphere",
						"Error compiling shader: "
								+ GLES20.glGetShaderInfoLog(shaderHandle));
				GLES20.glDeleteShader(shaderHandle);
				shaderHandle = 0;
			}
		}

		if (shaderHandle == 0)
		{
			throw new RuntimeException("Error creating shader.");
		}

		return shaderHandle;
	}

	/**
	 * Helper function to compile and link a program.
	 * 
	 * @param vertexShaderHandle
	 *            An OpenGL handle to an already-compiled vertex shader.
	 * @param fragmentShaderHandle
	 *            An OpenGL handle to an already-compiled fragment shader.
	 * @param attributes
	 *            Attributes that need to be bound to the program.
	 * @return An OpenGL handle to the program.
	 */
	private int createAndLinkProgram(final int vertexShaderHandle,
			final int fragmentShaderHandle, final String[] attributes)
	{
		int programHandle = GLES20.glCreateProgram();

		if (programHandle != 0)
		{
			// Bind the vertex shader to the program.
			GLES20.glAttachShader(programHandle, vertexShaderHandle);

			// Bind the fragment shader to the program.
			GLES20.glAttachShader(programHandle, fragmentShaderHandle);

			// Bind attributes
			if (attributes != null)
			{
				final int size = attributes.length;
				for (int i = 0; i < size; i++)
				{
					GLES20.glBindAttribLocation(programHandle, i, attributes[i]);
				}
			}

			// Link the two shaders together into a program.
			GLES20.glLinkProgram(programHandle);

			// Get the link status.
			final int[] linkStatus = new int[1];
			GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS,
					linkStatus, 0);

			// If the link failed, delete the program.
			if (linkStatus[0] == 0)
			{
				Log.e("Shaded Shpere",
						"Error compiling program: "
								+ GLES20.glGetProgramInfoLog(programHandle));
				GLES20.glDeleteProgram(programHandle);
				programHandle = 0;
			}
		}

		if (programHandle == 0)
		{
			throw new RuntimeException("Error creating program.");
		}

		return programHandle;
	}

	protected String getVertexShader()
	{
		// TODO: Explain why we normalize the vectors, explain some of the
		// vector math behind it all. Explain what is eye space.
		final String vertexShader = "uniform mat4 u_MVPMatrix;      \n" 
				+ "uniform mat4 u_MVMatrix;       \n" 
				+ "uniform vec3 u_LightPos;       \n" 

				+ "attribute vec4 a_Position;     \n" 
				+ "attribute vec4 a_Color;        \n" 
				+ "attribute vec3 a_Normal;       \n" 
				+ "varying vec4 v_Color;          \n" 
				+ "void main()                    \n" 
				+ "{                              \n"
				// Transform the vertex into eye space.
				+ "   vec3 modelViewVertex = vec3(u_MVMatrix * a_Position);              \n"
				// Transform the normal's orientation into eye space.
				+ "   vec3 modelViewNormal = vec3(u_MVMatrix * vec4(a_Normal, 0.0));     \n"
				// Will be used for attenuation.
				+ "   float distance = length(u_LightPos - modelViewVertex);             \n"
				// Get a lighting direction vector from the light to the vertex.
				+ "   vec3 lightVector = normalize(u_LightPos - modelViewVertex);        \n"
				// Calculate the dot product of the light vector and vertex
				// normal. If the normal and light vector are
				// pointing in the same direction then it will get max
				// illumination.
				+ "   float diffuse = max(dot(modelViewNormal, lightVector), 0.9);       \n"
				// Attenuate the light based on distance.
				+ "   diffuse = diffuse * (1.0 / (1.0 + (0.1 * distance * distance)));  \n"
				// Multiply the color by the illumination level. It will be
				// interpolated across the triangle.
				+ "   v_Color = a_Color * diffuse;                                       \n"
				// gl_Position is a special variable used to store the final
				// position.
				// Multiply the vertex by the matrix to get the final point in
				// normalized screen coordinates.
				+ "   gl_Position = u_MVPMatrix * a_Position;                            \n"
				+ "}                                                                     \n";

		return vertexShader;
	}

	protected String getFragmentShader()
	{
		final String fragmentShader = "precision mediump float;       \n" 
				+ "varying vec4 v_Color;          \n"
				+ "void main()                    \n" 									
				+ "{                              \n"
				+ "   gl_FragColor = v_Color;     \n" 
														
				+ "}                              \n";

		return fragmentShader;
	}
}
