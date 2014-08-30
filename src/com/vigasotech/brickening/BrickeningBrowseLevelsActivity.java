package com.vigasotech.brickening;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class BrickeningBrowseLevelsActivity extends Activity {
	boolean useLocal = true;
	boolean downloading = false;
	int nextStartIndex = 0;
	boolean anyReturned = false;
	boolean hitEnd = false;
	
	public static boolean SHOW_MENU = true;
	public static boolean DELETE = false;
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(isFinishing()) DELETE = false;
	}
	
	@Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if(SHOW_MENU && !DELETE) {
	        menu.clear();
	        menu.add(0, 0, 0, "Browse Local Levels");
			menu.add(0, 1, 0, "Browse Online Levels");
	        return true;
        } else
        	return false;
    }
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	TextView tv;
    	anyReturned = false;
    	switch(item.getItemId()) {
		case 0:
			useLocal = true;
			hitEnd = false;
			nextStartIndex = 0;
			setContentView(R.layout.brickbrowselevels_layout);
			tv = (TextView)findViewById(R.id.brickbrowselabel);
			tv.setMinimumWidth(8 * 40);
			tv.setText("Local Custom Levels");
			populateList();
			return true;
		case 1:
			useLocal = false;
			hitEnd = false;
			nextStartIndex = 0;
			setContentView(R.layout.brickbrowselevels_layout);
			tv = (TextView)findViewById(R.id.brickbrowselabel);
			tv.setMinimumWidth(8 * 40);
			tv.setText("Online Custom Levels");
			populateList();
			return true;
		}
		return false;
    }
    
	void addMoreButton(TableLayout t) {
		Button btn = new Button(BrickeningBrowseLevelsActivity.this);
		btn.setText("More...");
		btn.setGravity(Gravity.CENTER);
		btn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				populateList();
			}
		});
		TableRow row = new TableRow(BrickeningBrowseLevelsActivity.this);
		row.setGravity(Gravity.CENTER);
		row.addView(btn);
		t.addView(row);
	}
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.brickbrowselevels_layout);
		populateList();
	}
	
	void populateList() {
		if(useLocal) {
			File dir = getDir("bricklevels", 0);
			String[] children = dir.list();
			if(children != null) {
				if(children.length != 0) anyReturned = true;
				for(int i = 0; i < children.length; i++) {
					String filename = children[i];
					try {
						BufferedReader reader = new BufferedReader(
								new InputStreamReader(
										new FileInputStream(new File(dir, filename))));
						String data = reader.readLine();
						TableRow displayRow = new TableRow(BrickeningBrowseLevelsActivity.this);
						DispLevelView disp = new DispLevelView(BrickeningBrowseLevelsActivity.this);
						disp.setName(filename.substring(0, filename.lastIndexOf(".")));
    		    		disp.setMinimumWidth(8 * 40);
    		    		disp.setMinimumHeight(21 * 15);
    		    		int cur = 0;
    		    		for(int x = 0; x < 8; x++) {
    		    			for(int y = 0; y < 21; y++) {
    		    				int phase = Integer.parseInt(Character.toString(data.charAt(cur)));
    		    				cur++;
    		    				int powerUp = Integer.parseInt(Character.toString(data.charAt(cur)));
    		    				cur++;
    		    				disp.setGridState(x, y, phase, powerUp);
    		    			}
    		    		}
    		    		reader.close();
    		    		displayRow.setGravity(Gravity.CENTER);
    		    		displayRow.addView(disp);
    		    		TableLayout table = (TableLayout) findViewById(R.id.brickbrowselevelstable);
    		    		table.addView(displayRow);
    		    		TableRow nameRow = new TableRow(BrickeningBrowseLevelsActivity.this);
    		    		TextView levelName = new TextView(BrickeningBrowseLevelsActivity.this);
    		    		levelName.setText(filename.substring(0, filename.lastIndexOf(".")));
    		    		levelName.setTextSize(20);
    		    		levelName.setGravity(Gravity.CENTER);
    		    		nameRow.setGravity(Gravity.CENTER);
    		    		nameRow.addView(levelName);
    		    		table.addView(nameRow);
					} catch (FileNotFoundException e) {
						Toast.makeText(this, "Failed miserably to open file.", Toast.LENGTH_LONG).show();
					} catch (IOException e) {
						Toast.makeText(this, "Failed miserably to read file.", Toast.LENGTH_LONG).show();
					}
				}
			}
			
			if(!anyReturned) {
				View barrier = new View(BrickeningBrowseLevelsActivity.this);
				barrier.setBackgroundColor(Color.DKGRAY);
				barrier.setMinimumHeight(2);
				TextView label = new TextView(BrickeningBrowseLevelsActivity.this);
				if(SHOW_MENU)
					label.setText("You haven't created any custom levels yet.\nPress the Menu button to browse levels online,\nor press the Back button to create your own.");
				else
					label.setText("You haven't created any custom levels yet.");
				TableLayout table = (TableLayout) findViewById(R.id.brickbrowselevelstable);
				TableRow row = new TableRow(BrickeningBrowseLevelsActivity.this);
				label.setMinimumWidth(8 * 40);
				label.setGravity(Gravity.CENTER);
				row.setGravity(Gravity.CENTER);
				row.addView(label);
				table.addView(barrier);
				table.addView(row);
			}
		} else {
			GetLevelsTask getLevels = new GetLevelsTask(BrickeningBrowseLevelsActivity.this);
			getLevels.execute();
		}
	}
	
	class GetLevelsTask extends AsyncTask<Void, Void, HttpResponse> {
		String result;
		Context mContext;
		ProgressDialog mProgress;
		
		public GetLevelsTask(Context ctx) {
			super();
			mContext = ctx;
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
	        HttpPost httppost = new HttpPost("https://p8.secure.hostingprod.com/@vigasotech.com/ssl/party/brickgetlevels.php");
	        try {  
	            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);  
	            nameValuePairs.add(new BasicNameValuePair("start", Integer.toString(nextStartIndex)));
	            nameValuePairs.add(new BasicNameValuePair("end", Integer.toString(nextStartIndex + 10)));
	            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));  
	            nextStartIndex += 10;
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
	                int i = nextStartIndex - 10;
	                TableLayout table = (TableLayout)findViewById(R.id.brickbrowselevelstable);
	                if(i != 0) table.removeViewAt(table.getChildCount() - 1);
	                boolean dataline = true;
	                while((line = reader.readLine()) != null) {
	                	if(line.contains("--end--")) {
	                		hitEnd = true;
	                		continue;
	                	}
	                	anyReturned = true;
	                	if(line.contains("--more--")) continue;
	                	
	    		    	TableRow row = new TableRow(BrickeningBrowseLevelsActivity.this);
	    		    	if(dataline) {
	    		    		DispLevelView disp = new DispLevelView(BrickeningBrowseLevelsActivity.this);
	    		    		disp.setMinimumWidth(8 * 40);
	    		    		disp.setMinimumHeight(21 * 15);
	    		    		int cur = 0;
	    		    		for(int x = 0; x < 8; x++) {
	    		    			for(int y = 0; y < 21; y++) {
	    		    				int phase = Integer.parseInt(Character.toString(line.charAt(cur)));
	    		    				cur++;
	    		    				int powerUp = Integer.parseInt(Character.toString(line.charAt(cur)));
	    		    				cur++;
	    		    				disp.setGridState(x, y, phase, powerUp);
	    		    			}
	    		    		}
	    		    		row.setGravity(Gravity.CENTER);
	    		    		row.addView(disp);
	    		    	} else {
	    		    		TextView levelName = new TextView(BrickeningBrowseLevelsActivity.this);
	    		    		levelName.setText(line);
	    		    		levelName.setTextSize(20);
	    		    		levelName.setGravity(Gravity.CENTER);
	    		    		row.setGravity(Gravity.CENTER);
	    		    		row.addView(levelName);
	    		    	}
	    		    	table.addView(row);
	    		    	i++;
	    		    	dataline = !dataline;
	                }
	                content.close();
	                if(!hitEnd) addMoreButton(table);
	            }
	              
	        } catch (IOException e) {  
	        	result = "Failed to connect to server. Please try again later.";  
	        }  catch (Exception e) {
	        	result = "Connection to server timed out. Please try again later.";
	        }
	        
	        if(!anyReturned) {
	        	View barrier = new View(BrickeningBrowseLevelsActivity.this);
				barrier.setBackgroundColor(Color.DKGRAY);
				barrier.setMinimumHeight(2);
				TextView label = new TextView(BrickeningBrowseLevelsActivity.this);
				label.setText("No custom levels found.");
				TableLayout table = (TableLayout) findViewById(R.id.brickbrowselevelstable);
				TableRow row = new TableRow(BrickeningBrowseLevelsActivity.this);
				label.setMinimumWidth(8 * 40);
				label.setGravity(Gravity.CENTER);
				row.setGravity(Gravity.CENTER);
				row.addView(label);
				table.addView(barrier);
				table.addView(row);
	        }
			if(mProgress != null) mProgress.dismiss();
			if(result != null) Toast.makeText(mContext, result, Toast.LENGTH_LONG).show();
			downloading = false;
		}
	}
	
	class DispLevelView extends View {
		// brick phases
		public final int PHASE_EMPTY = 0;
		public final int PHASE_RED = 1;
		public final int PHASE_YELLOW = 2;
		public final int PHASE_GREEN = 3;
		public final int PHASE_STEEL = 4;
		
		// power-ups
		public final int POWER_NONE = 0;
		public final int POWER_ADD3 = 1;
		public final int POWER_LASER = 2;
		public final int POWER_BARRICADE = 3;
		public final int POWER_STICKY = 4;
		public final int POWER_DOUBLE = 5;
		public final int POWER_HALF = 6;
		public final int POWER_1UP = 7;
		public final int POWER_GHOST = 8;
		
		final int GRID_WIDTH = 8;
		final int GRID_HEIGHT = 22;
		final int BLOCK_WIDTH = 40;
		final int BLOCK_HEIGHT = 15;
		
		int[][] grid = new int[GRID_WIDTH][GRID_HEIGHT];
		int[][] powerUps = new int[GRID_WIDTH][GRID_HEIGHT];
		
		Context mContext;
		
		public DispLevelView(Context context) {
			super(context);
			mContext = context;
		}
		
		void setGridState(int x, int y, int phase, int powerUp) {
			grid[x][y] = phase;
			powerUps[x][y] = powerUp;
		}
		
		String mName = null;
		void setName(String name) {
			mName = name;
		}
		
		PorterDuffColorFilter[] filters = new PorterDuffColorFilter[4];
		
		protected void onDraw(Canvas canvas) {
			Bitmap block = BitmapFactory.decodeResource(getResources(), R.drawable.brick_block);
			filters[0] = new PorterDuffColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);
			filters[1] = new PorterDuffColorFilter(Color.YELLOW, PorterDuff.Mode.MULTIPLY);
			filters[2] = new PorterDuffColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);
			filters[3] = new PorterDuffColorFilter(Color.LTGRAY, PorterDuff.Mode.MULTIPLY);
			canvas.drawColor(Color.DKGRAY);
			Paint p = new Paint();
			for(int x = 0; x < GRID_WIDTH; x++) {
				for(int y = 0; y < GRID_HEIGHT; y++) {
					if(grid[x][y] == PHASE_EMPTY) continue;
					p.setColorFilter(filters[grid[x][y] - 1]);
					canvas.drawBitmap(block, x * BLOCK_WIDTH, y * BLOCK_HEIGHT, p);
				}
			}
		}
		
		@Override
		public boolean onTouchEvent(MotionEvent e) {
			super.onTouchEvent(e);
			if(e.getAction() == MotionEvent.ACTION_UP) {
				if(DELETE) {
					AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
					builder.setMessage("Are you sure you want to delete this map?");
					builder.setTitle("Attention!");
					builder.setCancelable(false);
					final Activity activity = (Activity)mContext;
					builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							// delete selected map
							File file = new File(getDir("bricklevels", 0), mName + ".txt");
							file.delete();
							dialog.dismiss();
							activity.setResult(Activity.RESULT_OK, activity.getIntent());
							activity.finish();
						}
					});
					builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
					builder.create().show();
					return true;
				}
				AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
				builder.setMessage("Loading a custom map will cause you to lose your current game progress. Is this okay?")
					   .setTitle("Warning")
				       .setCancelable(false)
				       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				        	   Activity act = (Activity)mContext;
								Intent intent = act.getIntent();
								int[] data = new int[GRID_WIDTH * (GRID_HEIGHT-1) * 2];
								int cur = 0;
								for(int x = 0; x < GRID_WIDTH; x++) {
									for(int y = 0; y < GRID_HEIGHT - 1; y++) {
										data[cur] = grid[x][y];
										cur++;
										data[cur] = powerUps[x][y];
										cur++;
									}
								}
								intent.putExtra("bricklayout", data);
								intent.putExtra("mapname", mName);
								act.setResult(Activity.RESULT_OK, intent);
								act.finish();
				           }
				       })
				       .setNegativeButton("No", new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				        	   dialog.dismiss();
				           }
				       });
				
				if(SHOW_MENU) builder.create().show();
				else {
					Activity act = (Activity)mContext;
					Intent intent = act.getIntent();
					int[] data = new int[GRID_WIDTH * (GRID_HEIGHT-1) * 2];
					int cur = 0;
					for(int x = 0; x < GRID_WIDTH; x++) {
						for(int y = 0; y < GRID_HEIGHT - 1; y++) {
							data[cur] = grid[x][y];
							cur++;
							data[cur] = powerUps[x][y];
							cur++;
						}
					}
					intent.putExtra("bricklayout", data);
					intent.putExtra("mapname", mName);
					act.setResult(Activity.RESULT_OK, intent);
					act.finish();
				}
			}
			return true;
		}
	}
}
