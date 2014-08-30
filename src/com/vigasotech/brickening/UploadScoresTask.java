package com.vigasotech.brickening;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.message.BasicNameValuePair;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class UploadScoresTask extends AsyncTask<String, Void, Void> {
	Context mContext;
	ProgressDialog mProgress;
	String result;
	
	public UploadScoresTask(Context ctx) {
		super();
		mContext = ctx;
	}
	@Override
	protected Void doInBackground(String... params) {
		if(params.length != 3) throw new IllegalArgumentException();
		// note: argument 0: game, argument 1: player name, argument 2: score
		// upload score to server
		HostnameVerifier hostnameVerifier = org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;

        DefaultHttpClient client = new DefaultHttpClient();

        SchemeRegistry registry = new SchemeRegistry();
        SSLSocketFactory socketFactory = SSLSocketFactory.getSocketFactory();
        socketFactory.setHostnameVerifier((X509HostnameVerifier) hostnameVerifier);
        registry.register(new Scheme("https", socketFactory, 443));
        SingleClientConnManager mgr = new SingleClientConnManager(client.getParams(), registry);
        DefaultHttpClient http = new DefaultHttpClient(mgr, client.getParams());

        HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);
        HttpPost httppost = new HttpPost("https://p8.secure.hostingprod.com/@vigasotech.com/ssl/party/submit.php");
        try {  
            // Add your data  
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);  
            nameValuePairs.add(new BasicNameValuePair("name", params[1]));  
            nameValuePairs.add(new BasicNameValuePair("score", params[2]));
            nameValuePairs.add(new BasicNameValuePair("game", params[0]));
            nameValuePairs.add(new BasicNameValuePair("password", "asdn324jks09FFSAION92asl9jj874"));
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));  
      
            // Execute HTTP Post Request  
            HttpResponse response = http.execute(httppost);
            int status = response.getStatusLine().getStatusCode();

            if (status != HttpStatus.SC_OK) {
                ByteArrayOutputStream ostream = new ByteArrayOutputStream();
                response.getEntity().writeTo(ostream);
                Log.e("HTTP CLIENT", ostream.toString());
            } else {
                InputStream content = response.getEntity().getContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(content));
                String line = reader.readLine();
                line = line.replace(params[1], "");
                result = line;
                content.close();
            }
              
        } catch (ClientProtocolException e) {  
        	result = "Failed to connect to server.";
        } catch (IOException e) {  
        	result = "Connection to server timed out.";  
        }  
		return null;
	}
	
	protected void onPreExecute() {
		mProgress = ProgressDialog.show(mContext, "", "Uploading...", true);
	}
	
	protected void onPostExecute(Void dummy) {
		if(mProgress != null) mProgress.dismiss();
		Toast.makeText(mContext, result, Toast.LENGTH_LONG).show();
	}
}
