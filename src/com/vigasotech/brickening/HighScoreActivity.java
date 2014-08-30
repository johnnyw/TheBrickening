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

import com.vigasotech.brickening.R;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class HighScoreActivity extends Activity {
	public static int game = DBLocalHighScores.TABLE_BRICKENING;
	boolean local = true;
	static boolean downloading = false;
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.highscore_layout);
	    TextView gameLabel = (TextView)findViewById(R.id.gametitle);
	    gameLabel.setText("Local High Scores");
	    TableLayout table = (TableLayout)findViewById(R.id.highscoretable);
	    DBLocalHighScores db = new DBLocalHighScores(HighScoreActivity.this);
	    db.open();
	    List<String> list = db.getScores(game);
	    db.close();
	    for(int i = 0; i < list.size(); i++) {
	    	String current = list.get(i);
	    	String[] strs = current.split(" ");
	    	TableRow row = new TableRow(HighScoreActivity.this);
	    	TextView name = new TextView(HighScoreActivity.this);
	    	TextView score = new TextView(HighScoreActivity.this);
	    	name.setTextSize(20);
	    	score.setTextSize(20);
	    	name.setText(Integer.toString(i + 1) + ") " + strs[0]);
	    	score.setText(strs[strs.length-1]);
	    	score.setGravity(Gravity.RIGHT);
	    	row.addView(name);
	    	row.addView(score);
	    	table.addView(row);
	    }
	}
	
	void addMoreButton(TableLayout t) {
		Button btn = new Button(HighScoreActivity.this);
		btn.setText("More...");
		btn.setGravity(Gravity.CENTER);
		btn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				getNext50();
			}
		});
		TableRow row = new TableRow(HighScoreActivity.this);
		row.setGravity(Gravity.CENTER);
		row.addView(btn);
		t.addView(row);
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();
		menu.add(0, 0, 0, "Local High Scores");
		menu.add(0, 1, 0, "Online High Scores");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case 0:
			// set content to local high scores
			useLocalScores(true);
			return true;
		case 1:
			// set content to online high scores
			useLocalScores(false);
			return true;
		}
		return false;
	}
	
	int nextStartIndex = 0;
	boolean hitEnd = false;
	
	void getNext50() {
		if(hitEnd) return;
		
		String title = "TheBrickening";
		switch(game) {
	    case DBLocalHighScores.TABLE_BRICKENING:
	    	title = "TheBrickening";
	    	break;
	    case DBLocalHighScores.TABLE_BUBBLE:
	    	title = "BubbleBuhBoom";
	    	break;
	    case DBLocalHighScores.TABLE_ELECTROSNAKE:
	    	title = "Electrosnake";
	    	break;
	    case DBLocalHighScores.TABLE_JELLYATTACK:
	    	title = "JellyAttack";
	    	break;
	    case DBLocalHighScores.TABLE_ROIDRAGE:
	    	title = "RoidRage";
	    	break;
	    }
		
		GetScoresTask getScores = new GetScoresTask(this, title);
		getScores.execute();
	}
	
	void useLocalScores(boolean b) {
		local = b;
	    String title = "Game";
	    nextStartIndex = 0;
	    hitEnd = false;
	    switch(game) {
	    case DBLocalHighScores.TABLE_BRICKENING:
	    	title = "The Brickening";
	    	break;
	    case DBLocalHighScores.TABLE_BUBBLE:
	    	title = "Bubble Buh-Boom";
	    	break;
	    case DBLocalHighScores.TABLE_ELECTROSNAKE:
	    	title = "Electrosnake";
	    	break;
	    case DBLocalHighScores.TABLE_JELLYATTACK:
	    	title = "Jelly Attack";
	    	break;
	    case DBLocalHighScores.TABLE_ROIDRAGE:
	    	title = "Roid Rage";
	    	break;
	    }
	    
	    setContentView(R.layout.highscore_layout);
	    TableLayout table = (TableLayout)findViewById(R.id.highscoretable);
	    TextView gameLabel = (TextView)findViewById(R.id.gametitle);
		if(b) {
			gameLabel.setText(title + " Local High Scores");
		    DBLocalHighScores db = new DBLocalHighScores(HighScoreActivity.this);
		    db.open();
		    List<String> list = db.getScores(game);
		    db.close();
		    for(int i = 0; i < list.size(); i++) {
		    	String current = list.get(i);
		    	String[] strs = current.split(" ");
		    	TableRow row = new TableRow(HighScoreActivity.this);
		    	TextView name = new TextView(HighScoreActivity.this);
		    	TextView score = new TextView(HighScoreActivity.this);
		    	name.setTextSize(20);
		    	score.setTextSize(20);
		    	name.setText(Integer.toString(i + 1) + ") " + strs[0]);
		    	score.setText(strs[strs.length-1]);
		    	score.setGravity(Gravity.RIGHT);
		    	row.addView(name);
		    	row.addView(score);
		    	table.addView(row);
		    }
		} else {
			gameLabel.setText("Online High Scores");
		    getNext50();
		}
	}
	
	class GetScoresTask extends AsyncTask<Void, Void, HttpResponse> {
		String result;
		Context mContext;
		String mTitle = null;
		ProgressDialog mProgress;
		
		public GetScoresTask(Context ctx, String title) {
			super();
			mContext = ctx;
			mTitle = title;
		}
		
		@Override
		protected HttpResponse doInBackground(Void... params) {
			HostnameVerifier hostnameVerifier = org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;

	        DefaultHttpClient client = new DefaultHttpClient();

	        SchemeRegistry registry = new SchemeRegistry();
	        SSLSocketFactory socketFactory = SSLSocketFactory.getSocketFactory();
	        socketFactory.setHostnameVerifier((X509HostnameVerifier) hostnameVerifier);
	        registry.register(new Scheme("https", socketFactory, 443));
	        SingleClientConnManager mgr = new SingleClientConnManager(client.getParams(), registry);
	        DefaultHttpClient http = new DefaultHttpClient(mgr, client.getParams());

	        HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);
	        HttpPost httppost = new HttpPost("https://p8.secure.hostingprod.com/@vigasotech.com/ssl/party/getscores.php");
	        try {  
	            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);  
	            nameValuePairs.add(new BasicNameValuePair("game", mTitle));  
	            nameValuePairs.add(new BasicNameValuePair("start", Integer.toString(nextStartIndex)));
	            nameValuePairs.add(new BasicNameValuePair("end", Integer.toString(nextStartIndex + 49)));
	            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));  
	            nextStartIndex += 50;
	            HttpResponse response = http.execute(httppost);
	            return response;
	              
	        } catch (ClientProtocolException e) {  
	        	result = "Failed to connect to server. Please try again later.";
	        } catch (IOException e) {  
	        	result = "Failed to connect to server. Please try again later.";  
	        }  
			return null;
		}
		
		protected void onPreExecute() {
			mProgress = ProgressDialog.show(mContext, "", "Loading...", true);
			downloading = true;
		}
		
		protected void onPostExecute(HttpResponse response) {
			if(this.isCancelled()) return;
	        try {  
	            int status = response.getStatusLine().getStatusCode();

	            if (status != HttpStatus.SC_OK) {
	                ByteArrayOutputStream ostream = new ByteArrayOutputStream();
	                response.getEntity().writeTo(ostream);
	                Log.e("HTTP CLIENT", ostream.toString());
	            } else {
	                InputStream content = response.getEntity().getContent();
	                BufferedReader reader = new BufferedReader(new InputStreamReader(content));
	                String line;
	                int i = nextStartIndex - 50;
	                TableLayout table = (TableLayout)findViewById(R.id.highscoretable);
	                if(i != 0) table.removeViewAt(table.getChildCount() - 1);
	                while((line = reader.readLine()) != null) {
	                	if(line.contains("--end--")) {
	                		hitEnd = true;
	                		continue;
	                	}
	                	
	                	if(line.contains("--more--")) continue;
	                	
	                	String[] strs = line.split(" ");
	    		    	TableRow row = new TableRow(HighScoreActivity.this);
	    		    	TextView name = new TextView(HighScoreActivity.this);
	    		    	TextView score = new TextView(HighScoreActivity.this);
	    		    	name.setTextSize(20);
	    		    	score.setTextSize(20);
	    		    	name.setText(Integer.toString(i + 1) + ") " + strs[0]);
	    		    	score.setText(strs[strs.length-1]);
	    		    	score.setGravity(Gravity.RIGHT);
	    		    	row.addView(name);
	    		    	row.addView(score);
	    		    	table.addView(row);
	    		    	i++;
	                }
	                content.close();
	                if(!hitEnd) addMoreButton(table);
	            }
	              
	        } catch (IOException e) {  
	        	result = "Failed to connect to server. Please try again later.";  
	        }  catch (Exception e) {
	        	result = "Connection to server timed out. Please try again later.";
	        }
			if(mProgress != null) mProgress.dismiss();
			if(result != null) Toast.makeText(mContext, result, Toast.LENGTH_LONG).show();
			downloading = false;
		}
	}
}
