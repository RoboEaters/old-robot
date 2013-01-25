package com.gbc.andcar;

import java.util.List;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public abstract class CameraView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private static final String TAG = "Sample::SurfaceView";

    private SurfaceHolder       mHolder;
    private VideoCapture        mCamera;
    public SurfaceView 			surfaceView;
    public double verticalViewAngle;
    public double horizontalViewAngle;
    //private SurfaceView sv;
    
    public CameraView(Context context) {
    	  super(context);
    	  
    	  Camera camera;
    	  camera = Camera.open();
    	  verticalViewAngle = camera.getParameters().getVerticalViewAngle();
    	  horizontalViewAngle = camera.getParameters().getHorizontalViewAngle();

    	  camera.release();
    	  
    	//  surfaceView = (SurfaceView)findViewById(R.id.camerapreview);
    	     mHolder = getHolder();
    	     mHolder.addCallback(this);
 
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
	public void surfaceChanged(SurfaceHolder _holder, int format, int width, int height) {
        Log.i(TAG, "surfaceCreated");
        synchronized (this) {
            if (mCamera != null && mCamera.isOpened()) {
                Log.i(TAG, "before mCamera.getSupportedPreviewSizes()");
                List<Size> sizes = mCamera.getSupportedPreviewSizes();
                Log.i(TAG, "after mCamera.getSupportedPreviewSizes()");
                int mFrameWidth = width;
                int mFrameHeight = height;

                // selecting optimal camera preview size
                {
                    double minDiff = Double.MAX_VALUE;
                    for (Size size : sizes) {
                        if (Math.abs(size.height - height) < minDiff) {
                            mFrameWidth = (int) size.width;
                            mFrameHeight = (int) size.height;
                            minDiff = Math.abs(size.height - height);
                        }
                    }
                }
                Log.i("Sizes", "The frame width is" + mFrameWidth);
                Log.i("Sizes", "The frame height is" + mFrameHeight);
                
                mCamera.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, mFrameWidth);
                mCamera.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, mFrameHeight);
            }
        }
    }

    @Override
	public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "surfaceCreated");
        mCamera = new VideoCapture(Highgui.CV_CAP_ANDROID);
        if (mCamera.isOpened()) {
            (new Thread(this)).start();
        } else {
            mCamera.release();
            mCamera = null;
            Log.e(TAG, "Failed to open native camera");
        }
    }

    @Override
	public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "surfaceDestroyed");
        if (mCamera != null) {
            synchronized (this) {
                mCamera.release();
                mCamera = null;
            }
        }
    }

    public void calculatePulseWidth()
    {
    	
    }
    
    protected abstract Bitmap processFrame(VideoCapture capture);

    @Override
	public void run() {
    	float startTime, endTime;
        Log.i(TAG, "Starting processing thread");
        while (true) {
            Bitmap bmp = null;

            synchronized (this) {
                if (mCamera == null)
                    break;

                if (!mCamera.grab()) {
                    Log.e(TAG, "mCamera.grab() failed");
                    break;
                }
//            	startTime = System.currentTimeMillis();
                bmp = processFrame(mCamera);
//                endTime = System.currentTimeMillis();
//                float diff = endTime - startTime;
//                double fps = 1/ (diff * Math.pow(10, -3));
//                Log.d("FPS", "The FPS is " + fps);
            }
            //ELIMIINATE IF YOUR NOT USING, TAKES UP SPACE
            if (bmp != null) {
                Canvas canvas = mHolder.lockCanvas();
                if (canvas != null) {
                    canvas.drawBitmap(bmp, (canvas.getWidth() - bmp.getWidth()) / 2, (canvas.getHeight() - bmp.getHeight()) / 2, null);
                    mHolder.unlockCanvasAndPost(canvas);
                }
                bmp.recycle();
            }
        }

        Log.i(TAG, "Finishing processing thread");
    }
}