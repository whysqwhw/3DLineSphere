package com.bokisoftware.linesphere;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.app.Activity;

public class MainActivity extends Activity
{
	private GLSurfaceView mGLView;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		// Create a GLSurfaceView instance and set it
		// as the ContentView for this Activity.
		mGLView = new SphereLineView(this);
		setContentView(mGLView);
	}
}
