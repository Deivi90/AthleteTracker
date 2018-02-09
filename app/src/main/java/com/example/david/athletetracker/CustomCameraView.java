package com.example.david.athletetracker;

import java.io.FileOutputStream;
import java.util.List;
import org.opencv.android.JavaCameraView;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.util.Log;


// https://github.com/opencv/opencv/blob/master/samples/android/tutorial-3-cameracontrol/src/org/opencv/samples/tutorial3/Tutorial3Activity.java

public class CustomCameraView extends JavaCameraView implements PictureCallback {

    private static final String TAG = "Sample::Tutorial3View";
    private String mPictureFileName;

    public CustomCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    // MI METODO
    public void setRecordingHint(){
        Camera.Parameters parms = mCamera.getParameters();
        parms.setRecordingHint(true);
        mCamera.setParameters(parms);
    }

    // MI METODO
    public void setPreviewFPS(){
        Camera.Parameters parms = mCamera.getParameters();
        parms.setRecordingHint(true);
        parms.set("fast-fps-mode", 1); // 1 for 60fps
        parms.setPreviewFpsRange(60000, 60000);
        mCamera.setParameters(parms);
    }

    // MI METODO
    public void setPreviewFPS(double min, double max){
        Camera.Parameters params = mCamera.getParameters();
        params.setRecordingHint(true);
        params.setPreviewFpsRange((int)(min*1000), (int)(max*1000));
        mCamera.setParameters(params);
    }


    public List<int[]> getSupportedRangeFPS(){
        return mCamera.getParameters().getSupportedPreviewFpsRange();
    }


    public List<String> getEffectList() {
        return mCamera.getParameters().getSupportedColorEffects();
    }

    public boolean isEffectSupported() {
        return (mCamera.getParameters().getColorEffect() != null);
    }

    public String getEffect() {
        return mCamera.getParameters().getColorEffect();
    }

    public void setEffect(String effect) {
        Camera.Parameters params = mCamera.getParameters();
        params.setColorEffect(effect);
        mCamera.setParameters(params);
    }

    public List<Size> getResolutionList() {
        return mCamera.getParameters().getSupportedPreviewSizes();
    }

    public void setResolution(Size resolution) {
        disconnectCamera();
        mMaxHeight = resolution.height;
        mMaxWidth = resolution.width;
        connectCamera(getWidth(), getHeight());
    }

    // MI METODO
    public void setResolution(int height, int width) {
        disconnectCamera();
        mMaxHeight = height;
        mMaxWidth = width;
        connectCamera(getWidth(), getHeight());
    }


    public Size getResolution() {
        return mCamera.getParameters().getPreviewSize();
    }

    public void takePicture(final String fileName) {
        Log.i(TAG, "Taking picture");
        this.mPictureFileName = fileName;
        // Postview and jpeg are sent in the same buffers if the queue is not empty when performing a capture.
        // Clear up buffers to avoid mCamera.takePicture to be stuck because of a memory issue
        mCamera.setPreviewCallback(null);

        // PictureCallback is implemented by the current class
        mCamera.takePicture(null, null, this);
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        Log.i(TAG, "Saving a bitmap to file");
        // The camera preview was automatically stopped. Start it again.
        mCamera.startPreview();
        mCamera.setPreviewCallback(this);

        // Write the image in a file (in jpeg format)
        try {
            FileOutputStream fos = new FileOutputStream(mPictureFileName);

            fos.write(data);
            fos.close();

        } catch (java.io.IOException e) {
            Log.e("PictureDemo", "Exception in photoCallback", e);
        }

    }
}