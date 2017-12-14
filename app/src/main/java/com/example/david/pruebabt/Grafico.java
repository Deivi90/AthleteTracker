package com.example.david.pruebabt;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class Grafico extends AppCompatActivity {

    LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>();  //Serie de datos a graficar
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graficos);
        GraphView graph = (GraphView) findViewById(R.id.Id_grafico);

        // Recuperos los datos de la intent anterior
        double[] datos = ( double[]) getIntent().getSerializableExtra("DATA");

        series = new LineGraphSeries<DataPoint>();
        //Log.d("Grafico","llega a OnCreate");
        double tiempo = -0.1;
        double aceleracion = 0;
        int i=0;
        // Recorro el vector de detos y armo la serie a graficar
        while(i<10){
            aceleracion=datos[i];
            tiempo= tiempo + 0.1;
            series.appendData(new DataPoint(tiempo, aceleracion), true, 100);
            i++;
        }
        graph.addSeries(series);    //grafica la serie
    }
}