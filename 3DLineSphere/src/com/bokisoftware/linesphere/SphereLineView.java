package com.bokisoftware.linesphere;
/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;

/**
 * A 3D line sphere view.
 * 
 * @author Kaleb
 * @version 1.0
 */
public class SphereLineView extends GLSurfaceView
{
	private final SphereLineRenderer renderer;
	private final String tag = "Sphere View";
	private float mPreviousX;
	private float mPreviousY;
	private float distance = 0;

	/**
	 * Create a new instance.
	 * @param context the context of the activity.
	 */
	public SphereLineView(Context context)
	{
		super(context);
		setEGLContextClientVersion(2); // This is the important line
		renderer = new SphereLineRenderer();
		setRenderer(renderer);
	}

	@Override
	public boolean onTouchEvent(MotionEvent e)
	{
		// MotionEvent reports input details from the touch screen
		// and other input controls. In this case, you are only
		// interested in events where the touch position changed.

		float x = e.getX();
		float y = e.getY();
		float newDist = 0;

		switch (e.getAction())
		{

		case MotionEvent.ACTION_MOVE:

			// rotate
			if (e.getPointerCount() == 1)
			{
				float dx = x - mPreviousX;
				float dy = y - mPreviousY;

				Log.d(tag, "dx: " + dx);
				Log.d(tag, "dy: " + dy);

				renderer.setDx(dx);
				renderer.setDy(dy);
			}

			// pinch to zoom
			if (e.getPointerCount() == 2)
			{
				if (distance == 0)
				{
					distance = fingerDist(e);
				}
				newDist = fingerDist(e);
				float d = distance / newDist;
				renderer.zoom(d);
				distance = newDist;
			}

			requestRender();
		}

		distance = newDist;
		mPreviousX = x;
		mPreviousY = y;
		return true;
	}

	protected final float fingerDist(MotionEvent event)
	{
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return (float) Math.sqrt(x * x + y * y);
	}

}
