package waxa.easysurvey;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;


public class MainActivity extends ActionBarActivity {

    private Button enviar;
    private EnviarId tarea;

    public static String URL = "http://gui.uva.es:22/guardarId/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        enviar = (Button)findViewById(R.id.send);

        enviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lanzarPeticion();
            }
        });
    }


    private void lanzarPeticion(){
        tarea = new EnviarId();
        tarea.execute();
    }

    private void uploadUrl() throws Exception {
/*
        URL url = new URL(MainActivity.URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(15000 *//* milliseconds *//*);
        conn.setConnectTimeout(17000 *//* milliseconds *//*);
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setDoInput(false);
        */

        HttpClient c = new DefaultHttpClient();
        HttpPost post = new HttpPost(MainActivity.URL);

        JSONObject obj = new JSONObject();
        obj.put("id","waxaMovil");

        StringEntity se = new StringEntity(obj.toString());

        post.setEntity(se);

        HttpResponse httpresponse = c.execute(post);
/*
        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());



        wr.write(obj.toString());
        wr.flush();
        // Start the query
        conn.connect();
        conn.disconnect();*/
    }

    private class EnviarId extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... urls) {
            try{
                if (checkNetworkConnection()){
                    uploadUrl();
                }else{
                    Log.d("WAXATAG", "no hay conexion2 a internet");
                }
            }catch (Exception e){
                Log.d("WAXATAG", "NO SE QUE COÑO PASA " + e.getMessage());
            }
            return null;
        }

        /**
         * Uses the logging framework to display the output of the fetch
         * operation in the log fragment.
         */
    }


    private boolean checkNetworkConnection() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();
        if (activeInfo != null && activeInfo.isConnected()) {
            /*Datos.wifiConnected = activeInfo.getType() == ConnectivityManager.TYPE_WIFI;
            Datos.mobileConnected = activeInfo.getType() == ConnectivityManager.TYPE_MOBILE;
            if(Datos.wifiConnected) {
                Log.i(Datos.TAG, getString(R.string.wifi_connection));
            } else if (Datos.mobileConnected){
                Log.i(Datos.TAG, getString(R.string.mobile_connection));
            }*/
            return true;
        } else {
            //Log.i(Datos.TAG, getString(R.string.no_wifi_or_mobile));
            return false;
        }
    }
/*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }*/
}
