package com.example.david.athletetracker;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.Window;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import org.opencv.videoio.VideoWriter;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.List;



public class CameraRecorder extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static String TAG = "Camera";

    JavaCameraView javaCameraView;  // Instancia a la camara view
    // Matrices usadas para representar la imagen
    Mat mRgba,imgHSV, imgThresholded, imgErode, imgDilat;
    VideoWriter cameraVideo;
    // https://docs.opencv.org/3.2.0/df/d9d/tutorial_py_colorspaces.html
    // H: 0 - 180, S: 0 - 255, V: 0 - 255
    // Now you take [H-10, 100,100] and [H+10, 255, 255] as lower bound and upper bound respectively.

    // Rango de colores a filtrar
    Scalar lowHSV, highHSV;
    int iLowH = 29;
    int iHighH = 64;
    int iLowS = 86;
    int iHighS = 255;
    int iLowV = 103;
    int iHighV = 255;

    // Inicializaciones para graficar contornos
    double maxArea, Area;
    int index = 0 ;
    int maxAreaIndex = 0;
    Scalar CONTOUR_COLOR = new Scalar(255,0,0,255);

    // Inicianilaciones para graficar lineas
    Moments contourMoments;
    double centerX;
    double centerY;
    Scalar LINE_COLOR = new Scalar(0,255,0,255);
    int CURVE_LENGHT = 20;
    Deque<Point> centerPoint = new ArrayDeque<>();
    Point initialPoint = new Point();
    Point finalPoint = new Point();
    int indexList = 0;
    boolean firstIteration = false;
    String filePath;


    BaseLoaderCallback mLoaderCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case BaseLoaderCallback.SUCCESS: {
                    javaCameraView.enableView();
                    break;
                }
                default: {
                    super.onManagerConnected(status);
                    break;
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        this.supportRequestWindowFeature(Window.FEATURE_NO_TITLE); // Para que no aparezca la barra de titulo
        setContentView(R.layout.activity_camera_recorder);

        lowHSV = new Scalar(iLowH, iLowS, iLowV);
        highHSV = new Scalar(iHighH, iHighS, iHighV);

        javaCameraView = (JavaCameraView) findViewById(R.id.java_camera_view);
        //javaCameraView.setCameraIndex(0);     // 0 for rear 1 for front
        javaCameraView.setVisibility(SurfaceView.VISIBLE);  // Set the visibility state of this view
        javaCameraView.setCvCameraViewListener(this);  //**//
    }


    @Override
    protected void onPause(){
        super.onPause();
        // Si esta la camara se deshabilita
        if (javaCameraView != null)
            javaCameraView.disableView();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if (javaCameraView != null)
            javaCameraView.disableView();
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(OpenCVLoader.initDebug()){
            Log.i(TAG,"OpenCv Loaded successfully");
            mLoaderCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);    //LLama al BaseLoaderCallback que se creó antes
        }
        else{
            Log.i(TAG,"OpenCV Failed to load");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_3_0, this, mLoaderCallBack);
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        //inicializamos la matriz mRgba
        mRgba = new Mat(height, width, CvType.CV_16UC4);
        imgHSV = new Mat(height, width, CvType.CV_16UC4);
        imgThresholded = new Mat(width, height, CvType.CV_16UC4);
        imgErode = new Mat(width, height, CvType.CV_16UC4);
        imgDilat = new Mat(width, height, CvType.CV_16UC4);


        // Inicializaciones relativas a la creacion del archivo de video
        // https://stackoverflow.com/questions/44393509/android-opencv-record-video
        // https://stackoverflow.com/questions/41632203/opencv3-2-videowriter-in-android
        // Se crea el directorio y nombre del video
        File sddir = Environment.getExternalStorageDirectory();
        File vrdir = new File(sddir, "Atlethe Tracker");    // Direccion de la carpeta
        // Si la carpeta no existe se crea
        if(!vrdir.exists())
        {
            vrdir.mkdir();
        }
        // El nombre del archivo tendra fecha y hora
        SimpleDateFormat videoTime = new SimpleDateFormat("yyyyMMddHHmmss");
        filePath = vrdir.getAbsolutePath() + "/" + videoTime.format(new Date()) +".avi" ;
        Log.i("Camera", "Direccion:  " + filePath);
    }


    @Override
    public void onCameraViewStopped() {
        mRgba.release();
        cameraVideo.release();

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        //https://docs.opencv.org/3.1.0/dd/d49/tutorial_py_contour_features.html
        //Imgproc.cvtColor(mRgba, imgGray, Imgproc.COLOR_RGB2GRAY);
        //Imgproc.Canny(mRgba, imgCanny, 50 , 100);
        // Se carga la imagen en la matriz mRgba
        mRgba = inputFrame.rgba();
        // Se convierte a escala de colores HSV
        Imgproc.cvtColor(mRgba, imgHSV, Imgproc.COLOR_BGR2HSV);
        // Se filtran los colores que no pertenecen al rango HSV indicado
        Core.inRange(imgHSV, lowHSV, highHSV, imgThresholded);
        // Erosion y dilatacion
        Imgproc.erode(imgThresholded, imgErode, new Mat() );
        Imgproc.erode(imgErode, imgErode, new Mat() );
        Imgproc.dilate(imgErode, imgDilat, new Mat() );
        Imgproc.dilate(imgDilat, imgDilat, new Mat() );
        // Lista de los contornos detectados
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(imgDilat, contours, new Mat(), Imgproc.RETR_EXTERNAL,Imgproc.CHAIN_APPROX_SIMPLE);
        // Se busca y grafica el mayor contorno de todos los detectados
        index = 0;
        maxAreaIndex = index;
        centerY = 0;
        centerX = 0;
        if(!contours.isEmpty()) {    // Si no hay contornos no se grafica nada
            maxArea = Imgproc.contourArea(contours.get(index));
            if (contours.size() > 1) {      //**//
                while (contours.size() > index + 1) {
                    index++;
                    Area = Imgproc.contourArea(contours.get(index));
                    if (maxArea < Area) {
                        maxAreaIndex = index;
                        maxArea = Area;
                    }
                }
            }
            Imgproc.drawContours(mRgba, contours, maxAreaIndex, CONTOUR_COLOR, 4);
            // Se calcula el centro de masa del contorno para poder dibujar la trayectoria del mismo
            contourMoments = Imgproc.moments(contours.get(maxAreaIndex));
            // El centro es igual a Cx = (int(M["m10"] / M["m00"]),  Cy = int(M["m01"] / M["m00"]))
            centerX = contourMoments.get_m10() / contourMoments.get_m00();
            centerY = contourMoments.get_m01() / contourMoments.get_m00();
        }
        // En la curva solo se grafican los ultimos "CURVE_LENGHT" puntos
        // Estos puntos se guardan en la cola "centerPoint"

        //Almacenamiento en la cola
        if (indexList < CURVE_LENGHT) {     // Se almacenan los puntos hasta que se llene la cola
            centerPoint.addFirst(new Point(centerX, centerY));
            indexList++;
        }
        else{    // Si la cola esta llena, se borra el elemento mas viejo y luego se agrega uno nuevo
            centerPoint.removeLast();
            centerPoint.addFirst(new Point(centerX, centerY));
        }

        // Graficos de los puntos en la cola
        for (Point center : centerPoint) {
            finalPoint = center;
            if ( firstIteration ){
                initialPoint = center;
                firstIteration = false;
            }
            else {
                if( !initialPoint.equals(new Point(0,0)) && !finalPoint.equals(new Point(0,0)) )
                    Imgproc.line(mRgba, initialPoint ,finalPoint , LINE_COLOR, 5);
                initialPoint = finalPoint;
            }
        }
        firstIteration = true;

        // Se graba el video a un archivo
        if (cameraVideo == null) {
            cameraVideo = new VideoWriter(filePath, VideoWriter.fourcc('M', 'J', 'P', 'G'), 15, mRgba.size());
            Log.d(TAG,"mVideoWriter good : " + cameraVideo);
            cameraVideo.open(filePath, VideoWriter.fourcc('M', 'J', 'P', 'G'), 15, mRgba.size());
            Log.i(TAG, "onCameraFrame: recordFilePath" + filePath);
        }
        if (!cameraVideo.isOpened()) {
            Log.w(TAG, "onCameraFrame: open");
            cameraVideo.open(filePath, VideoWriter.fourcc('M', 'J', 'P', 'G'), 15, mRgba.size());
        }
        cameraVideo.write(mRgba);
        return mRgba;
    }
}

