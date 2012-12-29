package com.bokisoftware.linesphere;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.SystemClock;

/**
 * Draw the lighting for the scatter plot.
 * 
 * @author Kaleb
 * @version 1.0
 */
public class Lighting
{
	/**
	 * A scaling factor for the touch to rotate.
	 */
	private static float TOUCH_SCALE_FACTOR = 180.0f / 320;

	/**
	 * Stores a copy of the model matrix specifically for the light position.
	 */
	private float[] lightModelMatrix = new float[16];

	/**
	 * Used to hold the current position of the light in world space (after
	 * transformation via model matrix).
	 */
	private final float[] lightPosInWorldSpace = new float[4];

	/**
	 * Used to hold a light centered on the origin in model space. We need a 4th
	 * coordinate so we can get translations to work when we multiply this by
	 * our transformation matrices.
	 */
	private final float[] lightPosInModelSpace = new float[]
	{ 0.0f, 0.0f, 0.0f, 1.0f };

	/**
	 * Used to hold the transformed position of the light in eye space (after
	 * transformation via modelview matrix)
	 */
	private final float[] lightPosInEyeSpace = new float[4];

	/**
	 * Store the model matrix. This matrix is used to move models from object
	 * space (where each model can be thought of being located at the center of
	 * the universe) to world space.
	 */
	private float[] modelMatrix = new float[16];
	/**
	 * Allocate storage for the final combined matrix. This will be passed into
	 * the shader program.
	 */
	private float[] mvpMatrix = new float[16];
	/**
	 * Store the view matrix. This can be thought of as our camera. This matrix
	 * transforms world space to eye space; it positions things relative to our
	 * eye.
	 */
	private float[] viewMatrix = new float[16];

	/**
	 * Store the projection matrix. This is used to project the scene onto a 2D
	 * viewport.
	 */
	private float[] projectionMatrix = new float[16];

	/** This is a handle to our light point program. */
	private int pointProgramHandle;

	// The parameters for the touch to rotate.
	private float dx = 1;
	private float dy = 1;
	private float dz = 1;
	private float xAngle = 0;
	private float yAngle = 0;

	/**
	 * Render the lighting and the rotation angle of the axes.
	 * 
	 * @param dx
	 *            the change in the x-axis.
	 * @param dy
	 *            the change in the y-axis.
	 */
	public void renderLighting(float dx, float dy)
	{
		// Integrate the derivative of the coordinates.
		this.dx += -Math.abs(dy);
		this.dy = 0;
		this.dz += Math.abs(dx);

		// Integrate the derivative of the rotation angles in the x and y axis.
		xAngle += dy;
		yAngle += dx;

		// Calculate position of the light. Rotate and then push into the
		// distance. This is used to light the sphere.
		Matrix.setIdentityM(lightModelMatrix, 0);
		Matrix.translateM(lightModelMatrix, 0, 0.0f, 0.0f, -5.0f);
		Matrix.rotateM(lightModelMatrix, 0, 35, -1.7f, -1.5f, 0.0f);
		Matrix.translateM(lightModelMatrix, 0, 0.0f, 0.0f, 2.0f);

		Matrix.multiplyMV(lightPosInWorldSpace, 0, lightModelMatrix, 0,
				lightPosInModelSpace, 0);
		Matrix.multiplyMV(lightPosInEyeSpace, 0, viewMatrix, 0,
				lightPosInWorldSpace, 0);

		// This rotates the axis of the scatter plot.
		Matrix.setIdentityM(modelMatrix, 0);
		Matrix.translateM(modelMatrix, 0, 0.0f, 0.0f, -5.0f);
		Matrix.rotateM(modelMatrix, 0, xAngle * TOUCH_SCALE_FACTOR, this.dx
				* TOUCH_SCALE_FACTOR, this.dy * TOUCH_SCALE_FACTOR, 0);
		Matrix.rotateM(modelMatrix, 0, yAngle * TOUCH_SCALE_FACTOR, 0, this.dy
				* TOUCH_SCALE_FACTOR, this.dz * TOUCH_SCALE_FACTOR);
	}

	/**
	 * Draws a point representing the position of the light.
	 */
	public void drawLight()
	{
		// Draw a point to indicate the light.
		GLES20.glUseProgram(pointProgramHandle);

		final int pointMVPMatrixHandle = GLES20.glGetUniformLocation(
				pointProgramHandle, "u_MVPMatrix");
		final int pointPositionHandle = GLES20.glGetAttribLocation(
				pointProgramHandle, "a_Position");

		// Pass in the position.
		GLES20.glVertexAttrib3f(pointPositionHandle, lightPosInModelSpace[0],
				lightPosInModelSpace[1], lightPosInModelSpace[2]);

		// Since we are not using a buffer object, disable vertex arrays for
		// this attribute.
		GLES20.glDisableVertexAttribArray(pointPositionHandle);

		// Pass in the transformation matrix.
		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, lightModelMatrix, 0);
		Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
		GLES20.glUniformMatrix4fv(pointMVPMatrixHandle, 1, false, mvpMatrix, 0);

		// Draw the point.
		GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);
	}

	/**
	 * Get the light model matrix.
	 * 
	 * @return light model matrix.
	 */
	public float[] getLightModelMatrix()
	{
		return lightModelMatrix;
	}

	/**
	 * Get the light position in eye space matrix.
	 * 
	 * @return light position in eye space matrix.
	 */
	public float[] getLightPosInEyeSpace()
	{
		return lightPosInEyeSpace;
	}

	/**
	 * Get the light position in model space matrix.
	 * 
	 * @return light position in model space matrix.
	 */
	public float[] getLightPosInModelSpace()
	{
		return lightPosInModelSpace;
	}

	/**
	 * Get the model matrix.
	 * 
	 * @return model matrix.
	 */
	public float[] getModelMatrix()
	{
		return modelMatrix;
	}

	/**
	 * Get the mvp matrix.
	 * 
	 * @return mvp matrix.
	 */
	public float[] getMvpMatrix()
	{
		return mvpMatrix;
	}

	/**
	 * Get the view matrix.
	 * 
	 * @return view matrix.
	 */
	public float[] getViewMatrix()
	{
		return viewMatrix;
	}

	/**
	 * Get the projection matrix.
	 * 
	 * @return projection matrix.
	 */
	public float[] getProjectionMatrix()
	{
		return projectionMatrix;
	}

	/**
	 * Set point program handle.
	 * 
	 * @param pointProgramHandle
	 */
	public void setPointProgramHandle(int pointProgramHandle)
	{
		this.pointProgramHandle = pointProgramHandle;
	}
}
