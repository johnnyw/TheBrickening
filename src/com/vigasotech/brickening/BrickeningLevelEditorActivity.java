package com.vigasotech.brickening;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
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
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

public class BrickeningLevelEditorActivity extends Activity {
	static int curTool = 0;
	static boolean changedAfterSave = false;
	static String mapName = null;
	
	class UploadMapTask extends AsyncTask<String, Void, Void> {
		Context mContext = null;
		ProgressDialog mProgress = null;
		String result;
		Dialog mDialog = null;
		
		protected void onPreExecute() {
			mProgress = ProgressDialog.show(mContext, "", "Uploading...", true);
		}
		
		public UploadMapTask(Context ctx, Dialog dialog) {
			super();
			mContext = ctx;
			mDialog = dialog;
		}
		
		@Override
		protected Void doInBackground(String... params) {
			if(params.length != 2) throw new IllegalArgumentException();
			// note: argument 0: name, argument 1: data
			HostnameVerifier hostnameVerifier = org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;

	        DefaultHttpClient client = new DefaultHttpClient();

	        SchemeRegistry registry = new SchemeRegistry();
	        SSLSocketFactory socketFactory = SSLSocketFactory.getSocketFactory();
	        socketFactory.setHostnameVerifier((X509HostnameVerifier) hostnameVerifier);
	        registry.register(new Scheme("https", socketFactory, 443));
	        SingleClientConnManager mgr = new SingleClientConnManager(client.getParams(), registry);
	        DefaultHttpClient http = new DefaultHttpClient(mgr, client.getParams());

	        HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);
	        HttpPost httppost = new HttpPost("https://p8.secure.hostingprod.com/@vigasotech.com/ssl/party/brickuploadlevel.php");
	        try {  
	            // Add your data  
	            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);  
	            nameValuePairs.add(new BasicNameValuePair("name", params[0]));  
	            nameValuePairs.add(new BasicNameValuePair("data", params[1]));
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
		
		protected void onPostExecute(Void dummy) {
			if(mProgress != null) mProgress.dismiss();
			mDialog.dismiss();
			Toast.makeText(mContext, result, Toast.LENGTH_LONG).show();
		}
	}
	
	class MapEditView extends View {
		// brick phases
		public static final int PHASE_EMPTY = 0;
		public static final int PHASE_RED = 1;
		public static final int PHASE_YELLOW = 2;
		public static final int PHASE_GREEN = 3;
		public static final int PHASE_STEEL = 4;
		
		// power-ups
		public static final int POWER_NONE = 0;
		public static final int POWER_ADD3 = 1;
		public static final int POWER_LASER = 2;
		public static final int POWER_BARRICADE = 3;
		public static final int POWER_STICKY = 4;
		public static final int POWER_DOUBLE = 5;
		public static final int POWER_HALF = 6;
		public static final int POWER_1UP = 7;
		public static final int POWER_GHOST = 8;
		
		final int GRID_WIDTH = 8;
		final int GRID_HEIGHT = 22;
		final int BLOCK_WIDTH = 40;
		final int BLOCK_HEIGHT = 15;
		
		public int[][] grid = new int[GRID_WIDTH][GRID_HEIGHT];
		public int[][] powerUps = new int[GRID_WIDTH][GRID_HEIGHT];
		
		Context mContext = null;
		
		public MapEditView(Context context) {
			super(context);
			mContext = context;
		}
		
		public void clear() {
			for(int x = 0; x < GRID_WIDTH; x++) {
				for(int y = 0; y < GRID_HEIGHT; y++) {
					grid[x][y] = powerUps[x][y] = 0;
				}
			}
			invalidate();
		}
		
		PorterDuffColorFilter[] filters = new PorterDuffColorFilter[4];
		
		protected void onDraw(Canvas canvas) {
			Bitmap block = BitmapFactory.decodeResource(getResources(), R.drawable.brick_block);
			filters[0] = new PorterDuffColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);
			filters[1] = new PorterDuffColorFilter(Color.YELLOW, PorterDuff.Mode.MULTIPLY);
			filters[2] = new PorterDuffColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);
			filters[3] = new PorterDuffColorFilter(Color.LTGRAY, PorterDuff.Mode.MULTIPLY);
			canvas.drawColor(Color.BLUE);
			Paint p = new Paint();
			for(int x = 0; x < GRID_WIDTH; x++) {
				for(int y = 0; y < GRID_HEIGHT - 1; y++) {
					Style s = p.getStyle();
					p.setStyle(Style.STROKE);
					p.setColor(Color.CYAN);
					canvas.drawRect(new Rect(x * BLOCK_WIDTH, y * BLOCK_HEIGHT,
							(x * BLOCK_WIDTH + BLOCK_WIDTH), (y * BLOCK_HEIGHT + BLOCK_HEIGHT)), p);
					p.setStyle(s);
					if(grid[x][y] == PHASE_EMPTY) continue;
					p.setColorFilter(filters[grid[x][y] - 1]);
					canvas.drawBitmap(block, x * BLOCK_WIDTH, y * BLOCK_HEIGHT, p);
					p.setColorFilter(null);
					p.setTextSize(BLOCK_HEIGHT);
					p.setTextAlign(Paint.Align.CENTER);
					p.setColor(Color.WHITE);
					if(powerUps[x][y] != POWER_NONE) {
						switch(powerUps[x][y]) {
						case POWER_ADD3:
							canvas.drawText("+3", x * BLOCK_WIDTH + (BLOCK_WIDTH / 2),
									y * BLOCK_HEIGHT + BLOCK_HEIGHT * .8f, p);
							break;
						case POWER_LASER:
							canvas.drawText("L", x * BLOCK_WIDTH + (BLOCK_WIDTH / 2),
									y * BLOCK_HEIGHT + BLOCK_HEIGHT * .8f, p);
							break;
						case POWER_BARRICADE:
							canvas.drawText("B", x * BLOCK_WIDTH + (BLOCK_WIDTH / 2),
									y * BLOCK_HEIGHT + BLOCK_HEIGHT * .8f, p);
							break;
						case POWER_STICKY:
							canvas.drawText("S", x * BLOCK_WIDTH + (BLOCK_WIDTH / 2),
									y * BLOCK_HEIGHT + BLOCK_HEIGHT * .8f, p);
							break;
						case POWER_DOUBLE:
							canvas.drawText("D", x * BLOCK_WIDTH + (BLOCK_WIDTH / 2),
									y * BLOCK_HEIGHT + BLOCK_HEIGHT * .8f, p);
							break;
						case POWER_HALF:
							canvas.drawText("H", x * BLOCK_WIDTH + (BLOCK_WIDTH / 2),
									y * BLOCK_HEIGHT + BLOCK_HEIGHT * .8f, p);
							break;
						case POWER_1UP:
							canvas.drawText("1UP", x * BLOCK_WIDTH + (BLOCK_WIDTH / 2),
									y * BLOCK_HEIGHT + BLOCK_HEIGHT * .8f, p);
							break;
						case POWER_GHOST:
							canvas.drawText("G", x * BLOCK_WIDTH + (BLOCK_WIDTH / 2),
									y * BLOCK_HEIGHT + BLOCK_HEIGHT * .8f, p);
							break;
						}
					}
				}
			}
		}
		
		@Override
		public boolean onTouchEvent(MotionEvent e) {
			super.onTouchEvent(e);
			if(e.getAction() == MotionEvent.ACTION_UP) {
				int x = (int) e.getX() / BLOCK_WIDTH;
				int y = (int) e.getY() / BLOCK_HEIGHT;
				
				int curGrid = grid[x][y];
				int curPowerUp = powerUps[x][y];
				
				if(curTool == 0) return true;
				
				switch(curTool) {
				case PHASE_RED:
					if(curGrid != PHASE_RED) grid[x][y] = PHASE_RED; else { grid[x][y] = 0; powerUps[x][y] = 0; }
            		break;
            	case PHASE_YELLOW:
            		if(curGrid != PHASE_YELLOW) grid[x][y] = PHASE_YELLOW; else { grid[x][y] = 0; powerUps[x][y] = 0; }
            		break;
            	case PHASE_GREEN:
            		if(curGrid != PHASE_GREEN) grid[x][y] = PHASE_GREEN; else { grid[x][y] = 0; powerUps[x][y] = 0; }
            		break;
            	case PHASE_STEEL:
            		if(curGrid != PHASE_STEEL) {
            			grid[x][y] = PHASE_STEEL;
            			powerUps[x][y] = 0;
            		} else { grid[x][y] = 0; powerUps[x][y] = 0; }
            		break;
            	case POWER_ADD3 + 4:
            		if(curPowerUp != POWER_ADD3) {
            			if(grid[x][y] != 0) {
            				powerUps[x][y] = POWER_ADD3;
            			}
            		} else powerUps[x][y] = 0;
            		break;
            	case POWER_LASER + 4:
            		if(curPowerUp != POWER_LASER) {
            			if(grid[x][y] != 0 && grid[x][y] != PHASE_STEEL) {
            				powerUps[x][y] = POWER_LASER;
            			}
            		} else powerUps[x][y] = 0;
            		break;
            	case POWER_BARRICADE + 4:
            		if(curPowerUp != POWER_BARRICADE) {
            			if(grid[x][y] != 0 && grid[x][y] != PHASE_STEEL) {
            				powerUps[x][y] = POWER_BARRICADE;
            			}
            		} else powerUps[x][y] = 0;
            		break;
            	case POWER_STICKY + 4:
            		if(curPowerUp != POWER_STICKY) {
            			if(grid[x][y] != 0 && grid[x][y] != PHASE_STEEL) {
            				powerUps[x][y] = POWER_STICKY;
            			}
            		} else powerUps[x][y] = 0;
            		break;
            	case POWER_DOUBLE + 4:
            		if(curPowerUp != POWER_DOUBLE) {
            			if(grid[x][y] != 0 && grid[x][y] != PHASE_STEEL) {
            				powerUps[x][y] = POWER_DOUBLE;
            			}
            		} else powerUps[x][y] = 0;
            		break;
            	case POWER_HALF + 4:
            		if(curPowerUp != POWER_HALF) {
            			if(grid[x][y] != 0 && grid[x][y] != PHASE_STEEL) {
            				powerUps[x][y] = POWER_HALF;
            			}
            		} else powerUps[x][y] = 0;
            		break;
            	case POWER_1UP + 4:
            		if(curPowerUp != POWER_1UP) {
            			if(grid[x][y] != 0 && grid[x][y] != PHASE_STEEL) {
            				powerUps[x][y] = POWER_1UP;
            			}
            		} else powerUps[x][y] = 0;
            		break;
            	case POWER_GHOST + 4:
            		if(curPowerUp != POWER_GHOST) {
            			if(grid[x][y] != 0 && grid[x][y] != PHASE_STEEL) {
            				powerUps[x][y] = POWER_GHOST;
            			}
            		} else powerUps[x][y] = 0;
            		break;
            	default:
            		break;
				}
				changedAfterSave = true;
				this.invalidate();
			}
			return true;
		}
	}
	
	@Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.clear();
        menu.add(0, 0, 0, "Save");
		menu.add(0, 1, 0, "Save As");
		menu.add(0, 2, 0, "Open");
		menu.add(0, 3, 0, "Clear");
		menu.add(0, 4, 0, "Upload");
		menu.add(0, 5, 0, "Delete");
		menu.add(0, 6, 0, "How-To");
        return true;
    }
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
		boolean anyBlocks = false;
		for(int x = 0; x < 8; x++) for(int y = 0; y < 21; y++) if(mEdit.grid[x][y] != 0 && mEdit.grid[x][y] != 4) anyBlocks = true;
		switch(item.getItemId()) {
		case 0:
			// save...
			if(!anyBlocks) {
				Toast.makeText(this, "Cannot save an empty map!", Toast.LENGTH_LONG).show();
				break;
			}
			if(mapName == null) mSaveDialog.show();
			else {
				// save silently (without dialog box)...
				
				// open custom map directory
				File dir = getDir("bricklevels", 0);
				File file = new File(dir, mapName + ".txt");
				try {
					if(!file.exists()) {
						file.createNewFile();
					}
					
					FileOutputStream output = new FileOutputStream(file);
					OutputStreamWriter writer = new OutputStreamWriter(output);
					for(int x = 0; x < 8; x++) {
						for(int y = 0; y < 21; y++) {
							writer.write(Integer.toString(mEdit.grid[x][y]));
							writer.write(Integer.toString(mEdit.powerUps[x][y]));
						}
					}
					writer.write("\n");
					writer.close();
					changedAfterSave = false;
				} catch(IOException ioe) {
					ioe.printStackTrace();
				}
			}
			break;
		case 1:
			// save as...
			if(!anyBlocks) {
				Toast.makeText(this, "Cannot save an empty map!", Toast.LENGTH_LONG).show();
				break;
			}
			mSaveDialog.show();
			break;
		case 2:
			// open...
			BrickeningBrowseLevelsActivity.SHOW_MENU = false;
			startActivityForResult(new Intent(this, BrickeningBrowseLevelsActivity.class), 0);
			break;
		case 3:
			// clear...
			mEdit.clear();
			break;
		case 4:
			// upload...
			if(!anyBlocks) {
				Toast.makeText(this, "Cannot upload an empty map!", Toast.LENGTH_LONG).show();
				break;
			}
			final Dialog uploadDialog = new Dialog(this);
			final UploadMapTask upload = new UploadMapTask(this, uploadDialog);
			uploadDialog.setContentView(R.layout.uploadmap_layout);
			uploadDialog.setTitle("Upload Map");
			final Button uploadButton = (Button)uploadDialog.findViewById(R.id.uploadmapbutton);
			final Button cancelButton = (Button)uploadDialog.findViewById(R.id.canceluploadbutton);
			final EditText nameEdit = (EditText)uploadDialog.findViewById(R.id.uploadmapname);
			final Activity activity = this;
			String tmpData = "";
			for(int x = 0; x < 8; x++) {
				for(int y = 0; y < 21; y++) {
					tmpData += Integer.toString(mEdit.grid[x][y]);
					tmpData += Integer.toString(mEdit.powerUps[x][y]);
				}
			}
			final String data = tmpData;
			uploadButton.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					String name = nameEdit.getText().toString();
					if(name == "") {
						Toast.makeText(activity, "Please enter a name for your map.", Toast.LENGTH_LONG).show();
						return;
					}
					
					if(!DBLocalHighScores.isNameAppropriate(name, activity)) {
						Toast.makeText(activity, "Please try another name.", Toast.LENGTH_LONG).show();
						return;
					}
					upload.execute(name, data);
				}
			});
			cancelButton.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					uploadDialog.dismiss();
				}
			});
			uploadDialog.show();
			break;
		case 5:
			// delete...
			BrickeningBrowseLevelsActivity.DELETE = true;
			startActivityForResult(new Intent(this, BrickeningBrowseLevelsActivity.class), 0);
			break;
		case 6:
			startActivity(new Intent(this, HowtoEditorActivity.class));
			break;
		default:
			return false;
		}
        return true;
    }
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode == Activity.RESULT_OK){
			if(!BrickeningBrowseLevelsActivity.DELETE){ 
				int[] layout = data.getIntArrayExtra("bricklayout");
				mapName = data.getStringExtra("mapname");
				int cur = 0;
	    		for(int x = 0; x < 8; x++) {
	    			for(int y = 0; y < 21; y++) {
	    				int phase = layout[cur];
	    				cur++;
	    				int powerUp = layout[cur];
	    				cur++;
	    				mEdit.grid[x][y] = phase;
	    				mEdit.powerUps[x][y] = powerUp;
	    			}
	    		}
			}
			BrickeningBrowseLevelsActivity.DELETE = false;
		}
	}
	
	@Override
    public boolean onKeyDown(int keyCode, KeyEvent e) {
		if(keyCode == KeyEvent.KEYCODE_BACK){
			boolean anyBlocks = false;
			for(int x = 0; x < 8; x++) for(int y = 0; y < 21; y++) if(mEdit.grid[x][y] != 0) anyBlocks = true;
			if(!changedAfterSave || !anyBlocks) return super.onKeyDown(keyCode, e);
		    mDialog.show();
		    return true;
		}
		return false;
	}
    
	AlertDialog mDialog = null;
	Dialog mSaveDialog = null;
	MapEditView mEdit = null;
	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Activity activity = this;
        mSaveDialog = new Dialog(this);
        mSaveDialog.setContentView(R.layout.savemap_layout);
        mSaveDialog.setTitle("Save As...");
        Button saveButton = (Button)mSaveDialog.findViewById(R.id.savebutton);
        Button cancelButton = (Button)mSaveDialog.findViewById(R.id.cancelsavebutton);
        final EditText edit = (EditText)mSaveDialog.findViewById(R.id.mapname);
        final Dialog d = mSaveDialog;
        saveButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// get player entered level name
				String name = edit.getText().toString();
				// remove invalid characters
				name = name.replaceAll("\\W", "");
				// verify that name is appropriate
				if(!DBLocalHighScores.isNameAppropriate(name, activity)) {
					edit.setText("");
					Toast.makeText(activity,
							"Please try another name.", Toast.LENGTH_LONG).show();
					return;
				}
				
				// open custom map directory
				File dir = getDir("bricklevels", 0);
				
				// check to see that name isn't duplicated
				for(int i = 0; i < dir.list().length; i++) {
					if(dir.list()[i].equals(name + ".txt")) {
						Toast.makeText(activity,
								"A map with that name already exists. Please try another.",
								Toast.LENGTH_LONG).show();
						return;
					}
				}
				
				// create a new file and write map data to it
				File file = new File(dir, name + ".txt");
				if(!file.exists())
					try {
						file.createNewFile();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				try {
					FileOutputStream output = new FileOutputStream(file);
					OutputStreamWriter writer = new OutputStreamWriter(output);
					for(int x = 0; x < 8; x++) {
						for(int y = 0; y < 21; y++) {
							writer.write(Integer.toString(mEdit.grid[x][y]));
							writer.write(Integer.toString(mEdit.powerUps[x][y]));
						}
					}
					writer.write("\n");
					writer.close();
					mapName = name;
					d.dismiss();
					changedAfterSave = false;
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
        });
        
        cancelButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				d.dismiss();
			}
        });
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Attention");
		builder.setMessage("Do you want to save your changes before you exit the editor?");
		builder.setCancelable(false);
		builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
	        	   // do save
	        	   if(mapName == null) {
	        		   mSaveDialog.setOnDismissListener(new OnDismissListener() {
						public void onDismiss(DialogInterface dialog) {
							activity.finish();
						}
	        		   });
	        		   mSaveDialog.show();
	        	   }
		   			else {
		   				// save silently (without dialog box)...
		   				File dir = getDir("bricklevels", 0);
		   				File file = new File(dir, mapName + ".txt");
		   				try {
		   					if(!file.exists()) {
		   						file.createNewFile();
		   					}
		   					
		   					FileOutputStream output = new FileOutputStream(file);
		   					OutputStreamWriter writer = new OutputStreamWriter(output);
		   					for(int x = 0; x < 8; x++) {
		   						for(int y = 0; y < 21; y++) {
		   							writer.write(Integer.toString(mEdit.grid[x][y]));
		   							writer.write(Integer.toString(mEdit.powerUps[x][y]));
		   						}
		   					}
		   					writer.write("\n");
		   					writer.close();
		   					changedAfterSave = false;
		   					activity.finish();
		   				} catch(IOException ioe) {
		   					ioe.printStackTrace();
		   				}
		   			}
	           }
	       });
	      builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
	        	   activity.finish();
	           }
	       });
	    builder.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
	    	   public void onClick(DialogInterface dialog, int id) {
	    		   dialog.dismiss();
	    	   }
	    });
	    mDialog = builder.create();
        curTool = 0;
        changedAfterSave = false;
        mapName = null;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        		WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.brickeditor_layout);
        mEdit = new MapEditView(BrickeningLevelEditorActivity.this);
        LinearLayout layout = (LinearLayout) findViewById(R.id.brickeditlayout);
        mEdit.setMinimumWidth(8 * 40);
		mEdit.setMinimumHeight(21 * 15);
        mEdit.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
        		21 * 15));
		TextView blank = new TextView(this);
		blank.setText("          ");
		layout.addView(blank);
        layout.addView(mEdit);
        blank = new TextView(this);
		blank.setText("          ");
        layout.addView(blank);
        Spinner spinner = new Spinner(this);
        spinner.setPrompt("Select block/item type");
        spinner.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
        		LinearLayout.LayoutParams.WRAP_CONTENT));
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
        		this, R.array.editoptions, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
            	switch(position) {
            	case 0:
            		curTool = MapEditView.PHASE_RED;
            		break;
            	case 1:
            		curTool = MapEditView.PHASE_YELLOW;
            		break;
            	case 2:
            		curTool = MapEditView.PHASE_GREEN;
            		break;
            	case 3:
            		curTool = MapEditView.PHASE_STEEL;
            		break;
            	case 4:
            		curTool = MapEditView.POWER_ADD3 + 4;
            		break;
            	case 5:
            		curTool = MapEditView.POWER_LASER + 4;
            		break;
            	case 6:
            		curTool = MapEditView.POWER_BARRICADE + 4;
            		break;
            	case 7:
            		curTool = MapEditView.POWER_STICKY + 4;
            		break;
            	case 8:
            		curTool = MapEditView.POWER_DOUBLE + 4;
            		break;
            	case 9:
            		curTool = MapEditView.POWER_HALF + 4;
            		break;
            	case 10:
            		curTool = MapEditView.POWER_1UP + 4;
            		break;
            	case 11:
            		curTool = MapEditView.POWER_GHOST + 4;
            		break;
            	default:
            		break;
            	}
            }

            public void onNothingSelected(AdapterView<?> parentView) {
            	// do nothing
            }

        });
        spinner.setAdapter(adapter);
        layout.addView(spinner);
    }
	
	@Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }
	
	@Override
	protected void onRestoreInstanceState(Bundle dummy) {
		super.onRestoreInstanceState(dummy);
	}
}
