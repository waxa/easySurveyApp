package waxa.easysurvey;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

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
import java.net.URI;
import java.net.URL;


public class MainActivity extends ActionBarActivity {

    private Button enviar;
    private EnviarId tarea;

    // Url del servicio REST que se invoca para el envio del identificador de
// registro a la aplicación jee
    public static final String URL_REGISTRO_ID = "http://gui.uva.es:22/guardarId/";

    // Seña númerica que se utiliza cuando se verifica la disponibilidad de los
// google play services
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    // Una simple Tag utilizada en los logs
    private static final String TAG = "Demo GCM";

    public static final String EXTRA_MESSAGE = "message";
    // Clave que permite recuperar de las preferencias compartidas de la
// aplicación el dentificador de registro en GCM
    private static final String PROPERTY_REG_ID = "registration_id";
    // Clave que permite recuperar de las preferencias compartidas de la
// aplicación el dentificador de la versión de la aplicación
    private static final String PROPERTY_APP_VERSION = "appVersion";
    // Identificador de la instancia del servicio de GCM al cual accedemos
    private static final String SENDER_ID = "27998784559";
    // Clase que da acceso a la api de GCM
    private GoogleCloudMessaging gcm;
    // Identificador de registro
    private String regid;
    // Contexto de la aplicación
    private Context contexto;

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

        contexto = this;


    }

    /**
     * En este método se procede al registro de la aplicación obteniendo el
     * identificador de registro que se almacena en la tarjeta de memoria para
     * no tener que repetir el mismo proceso la próxima vez. Adicionalmente se
     * envía el identificador de registro al a la aplicación jee , invocando un
     * servicio REST.
     */
    private void registroEnSegundoPlano() {
        new AsyncTask<Object, Object, Object>() {
            @Override
            protected void onPostExecute(final Object result) {
                Log.i(TAG, result.toString());
            }

            @Override
            protected String doInBackground(final Object... params) {
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(contexto);
                    }
                    // En este metodo se invoca al servicio de registro de los
                    // servicios GCM
                    regid = gcm.register(SENDER_ID);
                    msg = "Dispositivo3 registrado, registration ID=" + regid;
                    Log.i(TAG, msg);
                    // Una vez se tiene el identificador de registro se manda a
                    // la aplicacion jee
                    // ya que para que esta envie el mensaje de la notificación
                    // a los servidores
                    // de GCM es necesario dicho identificador
                    enviarIdentificadorRegistroALaAplicacionJ2ee();
                    //
                    // Se persiste el identificador de registro para que no sea
                    // necesario repetir el proceso de
                    // registro la proxima vez
                    almacenarElIdentificadorDeRegistro(regid);
                } catch (Exception e) {
                    msg = "Error :" + e.getMessage();
                    e.printStackTrace();
                }
                return msg;
            }

        }.execute(this, null, null);
    }

    /**
     * Se almacena el identificador de registro de "Google Cloud Message" y la
     * versión de la aplicación
     *
     * @param regId identificador de registro en GCM
     */
    private void almacenarElIdentificadorDeRegistro(String regId) {
        final SharedPreferences prefs = getPreferenciasCompartidas();
        int appVersion = getVersionDeLaAplicacion();
        Log.i(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }

    /**
     * Metodo que recupera el registration ID que fue almacenado la ultima vez
     * que la aplicación se registro, En caso que la aplicación este
     * desactualizada o no se haya registrado previamente no se recuperara
     * ningón registration ID
     *
     * @return identificador del registro, o vacio("") si no existe o esta
     *         desactualizado dicho registro
     */
    private String obtenerIdentificadorDeRegistroAlmacenado() {
        final SharedPreferences prefs = getPreferenciasCompartidas();
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        // Comprueba si la aplicación esta actualizada
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION,
                Integer.MIN_VALUE);
        int currentVersion = getVersionDeLaAplicacion();
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }

    /**
     * Metodo que sirve para recupera las preferencias compartidas en modo privado
     *
     * @return Application's {@code SharedPreferences}.
     */
    private SharedPreferences getPreferenciasCompartidas() {
        return getSharedPreferences(MainActivity.class.getSimpleName(),
                Context.MODE_PRIVATE);
    }

    private void enviarIdentificadorRegistroALaAplicacionJ2ee()
            throws Exception {
        JSONObject requestRegistrationId = new JSONObject();
        requestRegistrationId.put("registrationId", regid);
        BufferedReader in = null;
        try {
            HttpClient client = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost();
            httpPost.setURI(new URI(URL_REGISTRO_ID));
            httpPost.setEntity(new StringEntity(requestRegistrationId
                    .toString(), "UTF-8"));
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("content-type", "application/json");

            HttpResponse response = client.execute(httpPost);
            InputStreamReader lectura = new InputStreamReader(response
                    .getEntity().getContent());
            in = new BufferedReader(lectura);
            StringBuffer sb = new StringBuffer("");
            String line = "";
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }
            in.close();
            Log.i("INFO", sb.toString());
        } catch (Exception e) {
            Log.e("ERROR", e.getMessage(), e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Recupera la versión aplicación que identifica a cada una de las
     * actualizaciones de la misma.
     *
     * @return La versión del codigo de la aplicación
     */
    private int getVersionDeLaAplicacion() {
        try {
            PackageInfo packageInfo = contexto.getPackageManager()
                    .getPackageInfo(contexto.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    /**
     * Este metodo comprueba si Google Play Services esta disponible, ya que
     * este requiere que el terminal este asociado a una cuenta de google.Esta
     * verificación es necesaria porque no todos los dispositivos Android estan
     * asociados a una cuenta de Google ni usan sus servicios, por ejemplo, el
     * Kindle fire de Amazon, que es una tablet Android pero no requiere de una
     * cuenta de Google.
     *
     * @return Indica si Google Play Services esta disponible.
     */
    private boolean chequearPlayServices() {
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(contexto);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "Dispositivo no soportado.");
                finish();
            }
            return false;
        }
        return true;
    }

    private void lanzarPeticion(){
        // Se comprueba que Play Services APK estan disponibles, Si lo esta se
        // proocede con el registro en GCM
        if (chequearPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(contexto);
            // Se recupera el "registration Id" almacenado en caso que la
            // aplicación ya se hubiera registrado previamente
            regid = obtenerIdentificadorDeRegistroAlmacenado();

            // Si no se ha podido recuperar el id del registro procedemos a
            // obtenerlo mediante el proceso de registro
            //if (regid.isEmpty()) {
            if (true) {
                // Se inicia el proceso de registro
                registroEnSegundoPlano();
            }
        } else {
            Log.i(TAG, "No valid Google Play Services APK found.");
        }
        //tarea = new EnviarId();
       // tarea.execute();
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
        HttpPost post = new HttpPost(MainActivity.URL_REGISTRO_ID);

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
