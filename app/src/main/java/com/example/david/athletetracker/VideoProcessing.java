package com.example.david.athletetracker;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

public class VideoProcessing extends Activity implements Runnable
{
    Mat mBgra,imgHSV, imgThresholded, finalMat, curveMat;
    int frameIndex=0;

    VideoWriter cameraVideo;
    VideoCapture videoToProcess;

    // https://docs.opencv.org/3.2.0/df/d9d/tutorial_py_colorspaces.html
    // Rango de colores a filtrar (Azul)
    Scalar lowHSV = new Scalar(89, 126, 167);
    Scalar highHSV = new Scalar(126, 255, 255);

    // Inicializaciones para graficar contornos
    Double maxArea, Area;
    int index = 0 ;
    int maxAreaIndex = 0;
    Scalar CONTOUR_COLOR = new Scalar(255,155,235);

    // Inicianilaciones para graficar lineas
    Moments contourMoments;
    Double centerX;
    Double centerY;
    Scalar LINE_COLOR = new Scalar(50,255,255);
    int CURVE_LENGHT = 30;
    Deque<Point> centerPoint = new ArrayDeque<>();
    Deque<Double> velDataDeque = new ArrayDeque<>();
    Iterator<Double> velDataIterator;

    Point initialPoint = new Point();
    Point finalPoint = new Point();
    int indexList = 0;
    boolean firstIteration = false;
    String filePath;
    ArrayList<Double> velDataList = new ArrayList<Double>();
    ArrayList<Integer> velDataIndex = new ArrayList<>();


    //A ProgressDialog View
    private ProgressDialog progressDialog;
    //A thread, that will be used to execute code in parallel with the UI thread
    private Thread thread;
    //Create a Thread handler to queue code execution on a thread
    private Handler handler;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        filePath = getIntent().getStringExtra("path");
        velDataList = (ArrayList<Double>) getIntent().getSerializableExtra("velData");
        velDataIndex = getIntent().getIntegerArrayListExtra("Index");

        //Create a new progress dialog.
        progressDialog = new ProgressDialog(VideoProcessing.this);
        //Set the progress dialog to display a horizontal bar .
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        //Set the dialog title to 'Loading...'.
        progressDialog.setTitle("Procesando Video...");
        //Set the dialog message to 'Loading application View, please wait...'.
        progressDialog.setMessage("Por favor espere mientras su video es procesado");
        //This dialog can't be canceled by pressing the back key.
        progressDialog.setCancelable(false);
        //This dialog isn't indeterminate.
        progressDialog.setIndeterminate(false);
        //The maximum number of progress items is 100.
        progressDialog.setMax(100);
        //Set the current progress to zero.
        progressDialog.setProgress(0);
        //Display the progress dialog.
        progressDialog.show();

        //Initialize the handler
        handler = new Handler();
        //Initialize the thread
        thread = new Thread(this, "ProgressDialogThread");
        //start the thread
        thread.start();
    }

    //Initialize a counter integer to zero
    int counter = 0;
    @Override
    public void run()
    {
        processVideo();
        try
        {
            //Obtain the thread's token
            synchronized (thread)
            {
                //While the counter is smaller than four
                while(counter <= 4)
                {
                    //Wait 850 milliseconds
                    thread.wait(850);
                    //Increment the counter
                    counter++;

                    //update the changes to the UI thread
                    handler.post(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            //Set the current progress.
                            progressDialog.setProgress(counter*25);
                        }
                    });
                }
            }
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }

        //This works just like the onPostExecute method from the AsyncTask class
        handler.post(new Runnable()
        {
            @Override
            public void run()
            {
                //Close the progress dialog
                progressDialog.dismiss();
                //Call the application's main View
                finish();
            }
        });

        //Try to "kill" the thread, by interrupting its execution
        synchronized (thread)
        {
            thread.interrupt();
        }
    }

    private  void processVideo(){

        Double maxVel = 0.0;
        mBgra = new Mat(480, 640, CvType.CV_8UC4);
        imgHSV = new Mat(480, 640, CvType.CV_8UC4);
        imgThresholded = new Mat(480, 640, CvType.CV_8UC4);
        curveMat = new Mat(480, 200, CvType.CV_8UC4);
        finalMat = new Mat(mBgra.rows(), mBgra.cols() +  curveMat.cols(), mBgra.type());

        int aCols = mBgra.cols();
        int aRows = mBgra.rows();

        Imgproc.putText(curveMat,"Max Acel:aa", new Point(20,50),Core.FONT_HERSHEY_COMPLEX_SMALL,
                1,new Scalar(170, 0, 180),2 );


        int centerIndex;
        if (!velDataList.isEmpty()) {
            maxVel = Collections.max(velDataList);
        }
        Double velValue = 0.0;
        Double velValueColor;
        int thickness;



        cameraVideo = new VideoWriter(filePath +".avi", VideoWriter.fourcc('M', 'J', 'P', 'G'), 15.0, mBgra.size());
        cameraVideo.open(filePath +".avi", VideoWriter.fourcc('M', 'J', 'P', 'G'), 15.0, mBgra.size());

        videoToProcess = new VideoCapture(filePath + "TEMP.avi");
        while (videoToProcess.read(mBgra))      // mBgra esta en BGR
        {
            Log.i("VideoProcessing", "Lee un Mat");

            //https://docs.opencv.org/3.1.0/dd/d49/tutorial_py_contour_features.html
            // Se carga la imagen en la matriz mBgra
            // Se convierte a escala de colores HSV
            Imgproc.cvtColor(mBgra, imgHSV, Imgproc.COLOR_BGR2HSV);
            // Se filtran los colores que no pertenecen al rango HSV indicado
            Core.inRange(imgHSV, lowHSV, highHSV, imgThresholded);
            // Erosion y dilatacion
            // Imgproc.erode(imgThresholded, imgErode, new Mat() );
            // Imgproc.erode(imgErode, imgErode, new Mat() );
            // Imgproc.dilate(imgErode, imgDilat, new Mat() );
            // Lista de los contornos detectados
            List<MatOfPoint> contours = new ArrayList<>();
            // Imgproc.findContours(imgDilat, contours, new Mat(), Imgproc.RETR_EXTERNAL,Imgproc.CHAIN_APPROX_SIMPLE);
            Imgproc.findContours(imgThresholded, contours, new Mat(), Imgproc.RETR_EXTERNAL,Imgproc.CHAIN_APPROX_SIMPLE);
            // Se busca y grafica el mayor contorno de todos los detectados
            index = 0;
            maxAreaIndex = index;
            if(!contours.isEmpty()) {    // Si no hay contornos no se grafica nada
                maxArea = Imgproc.contourArea(contours.get(index));
                if (contours.size() > 1) {
                    while (contours.size() > index + 1) {
                        index++;
                        Area = Imgproc.contourArea(contours.get(index));
                        if (maxArea < Area) {
                            maxAreaIndex = index;
                            maxArea = Area;
                        }
                    }
                }
                Imgproc.drawContours(mBgra, contours, maxAreaIndex, CONTOUR_COLOR, 4);
                // Se calcula el centro de masa del contorno para poder dibujar la trayectoria del mismo
                contourMoments = Imgproc.moments(contours.get(maxAreaIndex));
                // El centro es igual a Cx = (int(M["m10"] / M["m00"]),  Cy = int(M["m01"] / M["m00"]))
                centerX = contourMoments.get_m10() / contourMoments.get_m00();
                centerY = contourMoments.get_m01() / contourMoments.get_m00();

                // En la curva solo se grafican los ultimos "CURVE_LENGHT" puntos
                // Estos puntos se guardan en la cola "centerPoint"

                //Almacenamiento en la cola
                if (indexList < CURVE_LENGHT) {     // Se almacenan los puntos hasta que se llene la cola
                    centerPoint.addFirst(new Point(centerX, centerY));
                    indexList++;
                    if (!velDataList.isEmpty())
                        velDataDeque.addFirst(velDataList.get(frameIndex));
                        //velDataDeque.addFirst(velDataList.get(velDataIndex.get(frameIndex)));

                } else {    // Si la cola esta llena, se borra el elemento mas viejo y luego se agrega uno nuevo
                    centerPoint.removeLast();
                    centerPoint.addFirst(new Point(centerX, centerY));
                    if (!velDataList.isEmpty()) {
                        velDataDeque.removeLast();
                        velDataDeque.addFirst(velDataList.get(frameIndex));
                        //velDataDeque.addFirst(velDataList.get(velDataIndex.get(frameIndex)));
                    }
                }

            }
            centerIndex = 0 ;
            if( ! velDataList.isEmpty())
                velDataIterator = velDataDeque.iterator();
            // Graficos de los puntos en la cola
            for (Point center : centerPoint) {
                finalPoint = center;
                if ( firstIteration ){
                    initialPoint = center;
                    firstIteration = false;
                }
                else {
                    thickness = (int)(Math.sqrt( CURVE_LENGHT / (centerIndex + 1)) * 3);
                            if( velDataList.isEmpty())
                                Imgproc.line(mBgra, initialPoint, finalPoint, LINE_COLOR, thickness);
                            else{
                                // El color de la curva va de amarillo a rojo a medida que aumenta la velocidad
                                velValue = velDataIterator.next();
                                velValueColor = velValue * 255/maxVel; // La velocidad va de 0 a 255
                                velValueColor = Math.abs(velValueColor - 255); // La velocidad va de 255 a 0
                                Imgproc.line(mBgra, initialPoint, finalPoint, new Scalar(50,velValueColor,255), thickness);
                            }
                    initialPoint = finalPoint;
                }
                centerIndex++;
            }
            if (!velDataDeque.isEmpty())
                velValue = velDataDeque.getFirst();
            Imgproc.putText(mBgra,"Acelerometer Value:  ".concat(velValue.toString()) ,new Point(20,55),Core.FONT_HERSHEY_SIMPLEX,
                    1,new Scalar(255, 255, 255),2 );

            Imgproc.putText(mBgra,"Max Acel:  ".concat(maxVel.toString()),new Point(20,75),Core.FONT_HERSHEY_SIMPLEX,
                    1,new Scalar(255, 255, 255),2 );
            firstIteration = true;


            //List<Mat> src = Arrays.asList(mBgra, curveMat);
           // Core.vconcat(src, finalMat);
            // Se graba el video a un archivo
            //cameraVideo.write(mBgra);

          //  finalMat = new Mat(mBgra.rows(), mBgra.cols() +  curveMat.cols(), mBgra.type());
          //  mBgra.copyTo(finalMat.rowRange(0, aRows-1).colRange(0, aCols-1));
            //curveMat.copyTo(finalMat.rowRange(0, aRows-1).colRange(aCols, finalMat.cols()));

            // Se graba el video a un archivo
            cameraVideo.write(mBgra);
            mBgra.release();
            finalMat.release();
            frameIndex++;
        }
        Log.i("VideoProcessing", "Termino el proc");
        cameraVideo.release();
        File tempFile = new File(filePath + "TEMP.avi");
        tempFile.delete();
        imgHSV.release();
        imgThresholded.release();
        finalMat.release();
        curveMat.release();
    }


}