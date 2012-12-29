package com.bokisoftware.linesphere;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.SystemClock;

/**
 * Draw a line sphere. This class will draw the latitude and longitude lines of
 * the sphere.
 * 
 * @author Kaleb
 * @version 1.0
 */
public class SphereLine
{

	private ArrayList<Float> sphereCoords = new ArrayList<Float>();
	private ArrayList<Float> sphereNormals = new ArrayList<Float>();
	private ArrayList<Float> sphereColor = new ArrayList<Float>();

	private int spherePositionHandle;
	private int sphereColorHandle;
	private int sphereNormalHandle;

	private int spherePoints;

	private FloatBuffer sphereVertexBuffer;
	private FloatBuffer sphereNormalsBuffer;
	private FloatBuffer sphereColorBuffer;

	// Determine a degree
	private double degree = Math.PI / 180;
	// The radius of the sphere.
	private double sphereRaduis;
	// The step of the sphere, or the number of facets.
	private double sphereStep;

	/** How many bytes per float. */
	private final int bytesPerFloat = 4;

	/** Size of the position data in elements. */
	private final int positionDataSize = 3;

	/** Size of the color data in elements. */
	private final int colorDataSize = 4;

	/** Size of the normal data in elements. */
	private final int normalsDataSize = 3;

	/** This will be used to pass in the transformation matrix. */
	private int mvpMatrixHandle;

	/** This will be used to pass in the modelview matrix. */
	private int mvMatrixHandle;

	/** This will be used to pass in the light position. */
	private int lightPosHandle;

	/** This is a handle to our per-vertex sphere shading program. */
	private int perVertexProgramHandle;

	/**
	 * Create a line sphere.
	 * 
	 * @param radius
	 *            the radius of the sphere.
	 * @param step
	 *            the number of steps, or facets.
	 */
	public SphereLine(float radius, double step)
	{
		this.sphereRaduis = radius;
		this.sphereStep = step;

		// Generate the sphere points.
		spherePoints = buildSphere();

		float[] sphereCoordsArray = new float[sphereCoords.size()];
		for (int i = 0; i < sphereCoords.size(); i++)
		{
			sphereCoordsArray[i] = sphereCoords.get(i);
		}

		sphereVertexBuffer = ByteBuffer
				.allocateDirect(sphereCoordsArray.length * bytesPerFloat)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		sphereVertexBuffer.put(sphereCoordsArray).position(0);

		float[] sphereNormalsArray = new float[sphereNormals.size()];
		for (int i = 0; i < sphereNormals.size(); i++)
		{
			sphereNormalsArray[i] = sphereNormals.get(i);
		}

		sphereNormalsBuffer = ByteBuffer
				.allocateDirect(sphereNormalsArray.length * bytesPerFloat)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		sphereNormalsBuffer.put(sphereNormalsArray).position(0);

		float[] sphereColorsArray = new float[sphereColor.size()];
		for (int i = 0; i < sphereColor.size(); i++)
		{
			sphereColorsArray[i] = sphereColor.get(i);
		}

		sphereColorBuffer = ByteBuffer
				.allocateDirect(sphereColorsArray.length * bytesPerFloat)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		sphereColorBuffer.put(sphereColorsArray).position(0);
	}

	/**
	 * Set the handles.
	 */
	public void setHandles()
	{
		// Set our per-vertex lighting program.
		GLES20.glUseProgram(perVertexProgramHandle);

		// Set program handles for sphere drawing.
		mvpMatrixHandle = (GLES20.glGetUniformLocation(perVertexProgramHandle,
				"u_MVPMatrix"));
		mvMatrixHandle = (GLES20.glGetUniformLocation(perVertexProgramHandle,
				"u_MVMatrix"));
		lightPosHandle = (GLES20.glGetUniformLocation(perVertexProgramHandle,
				"u_LightPos"));
		spherePositionHandle = (GLES20.glGetAttribLocation(
				perVertexProgramHandle, "a_Position"));
		sphereColorHandle = (GLES20.glGetAttribLocation(perVertexProgramHandle,
				"a_Color"));
		sphereNormalHandle = (GLES20.glGetAttribLocation(
				perVertexProgramHandle, "a_Normal"));
	}

	
	/**
	 * Draw the sphere..
	 * 
	 * @param mvpMatrix
	 *            the model-view-perspective matrix
	 * @param viewMatrix
	 *            the view matrix
	 * @param projectionMatrix
	 *            the projection matrix
	 * @param modelMatrix
	 *            the model matrix
	 * @param lightPosInEyeSpace
	 *            the light position in eye space matrix
	 */
	public void drawSphere(float[] mvpMatrix, float[] viewMatrix,
			float[] projectionMatrix, float[] modelMatrix,
			float[] lightPosInEyeSpace)
	{
		// Set our per-vertex lighting program.
		GLES20.glUseProgram(perVertexProgramHandle);

		// Pass in the position information
		sphereVertexBuffer.position(0);
		GLES20.glVertexAttribPointer(spherePositionHandle, positionDataSize,
				GLES20.GL_FLOAT, false, 0, sphereVertexBuffer);

		GLES20.glEnableVertexAttribArray(spherePositionHandle);

		// Pass in the color information
		sphereColorBuffer.position(0);
		GLES20.glVertexAttribPointer(sphereColorHandle, colorDataSize,
				GLES20.GL_FLOAT, false, 0, sphereColorBuffer);

		GLES20.glEnableVertexAttribArray(sphereColorHandle);

		// Pass in the normal information
		sphereNormalsBuffer.position(0);
		GLES20.glVertexAttribPointer(sphereNormalHandle, normalsDataSize,
				GLES20.GL_FLOAT, false, 0, sphereNormalsBuffer);

		GLES20.glEnableVertexAttribArray(sphereNormalHandle);

		// This multiplies the view matrix by the model matrix, and stores the
		// result in the MVP matrix
		// (which currently contains model * view).
		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);

		// Pass in the modelview matrix.
		GLES20.glUniformMatrix4fv(mvMatrixHandle, 1, false, mvpMatrix, 0);

		// This multiplies the modelview matrix by the projection matrix, and
		// stores the result in the MVP matrix
		// (which now contains model * view * projection).
		Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);

		// Pass in the combined matrix.
		GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

		// Pass in the light position in eye space.
		GLES20.glUniform3f(lightPosHandle, lightPosInEyeSpace[0],
				lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

		// Draw the sphere.
		GLES20.glDrawArrays(GLES20.GL_LINES, 0, spherePoints);
	}

	public void setPerVertexProgramHandle(int perVertexProgramHandle)
	{
		this.perVertexProgramHandle = perVertexProgramHandle;
	}

	/**
	 * Generate the sphere.
	 * @return the number of points.
	 */
	private int buildSphere()
	{
		/**
		 * x = p * sin(phi) * cos(theta) y = p * sin(phi) * sin(theta) z = p *
		 * cos(phi)
		 */

		double dTheta = sphereStep * degree;
		double dPhi = dTheta;
		int points = 0;

		for (double phi = -(Math.PI); phi <= Math.PI; phi += dPhi)
		{
			// for each stage calculating the slices
			for (double theta = 0.0; theta <= (Math.PI * 2); theta += dTheta)
			{
				float xCoord = (float) (sphereRaduis * Math.sin(phi) * Math
						.cos(theta));
				float yCoord = (float) (sphereRaduis * Math.sin(phi) * Math
						.sin(theta));
				float zCoord = (float) (sphereRaduis * Math.cos(phi));

				sphereCoords.add(xCoord);
				sphereCoords.add(yCoord);
				sphereCoords.add(zCoord);

				float magnitude = (float) Math.sqrt(Math.pow(xCoord, 2)
						+ Math.pow(yCoord, 2) + Math.pow(zCoord, 2));

				sphereNormals.add(xCoord / magnitude);
				sphereNormals.add(yCoord / magnitude);
				sphereNormals.add(zCoord / magnitude);

				sphereColor.add(0.63671875f);
				sphereColor.add(0.76953125f);
				sphereColor.add(0.22265625f);
				sphereColor.add(1.0f);

				points++;
			}
		}
		return points;
	}
}
