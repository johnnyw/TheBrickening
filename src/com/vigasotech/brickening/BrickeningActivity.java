package com.vigasotech.brickening;

import java.util.Random;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import com.vigasotech.brickening.BrickeningGame;

public class BrickeningActivity extends Activity implements OnClickListener {
	BrickeningGame gameView;
	String[] names = { "Sid", "Nathaniel", "Antonio", "Jeff", "Kurt", "Rocco", "Jason", "Armand", "Felix" };
	@Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if(gameView != null) {
        	menu.clear();
        	return gameView.onPrepareOptionsMenu(menu);
        }
        return false;
    }
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if(gameView != null) {
    		return gameView.onOptionsItemSelected(item);
    	}
        return false;
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent e) {
    	if(keyCode == KeyEvent.KEYCODE_BACK) {
    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    		builder.setTitle("Exit Game");
    		builder.setMessage("Are you sure you want to leave?");
    		final Activity activity = this;
    		builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					gameView.saveGameState(getPreferences(0));
		    		activity.finish();
		    		android.os.Process.killProcess(android.os.Process.myPid());
				}
    		});
    		builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
    		});
    		gameView.gameStarted = false;
    		builder.create().show();
    		return true;
    	}
    	
    	if(gameView != null) {
    		return gameView.onKeyDown(keyCode, e);
    	}
    	return false;
    }
	
    @Override
    protected void onPause() {
    	super.onPause();
    	gameView.pauseGame();
    	gameView.gameStarted = false;
    	// gameView.saveGameState(getPreferences(0));
    }
    
    @Override
    protected void onStop() {
    	super.onStop();
    	// gameView.saveGameState(getPreferences(0));
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	gameView.resumeGame();
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        Random rnd = new Random();
        DBLocalHighScores db = new DBLocalHighScores(this);
        for(int i = 0; i < DBLocalHighScores.DATABASE_TABLES.length; i++) {
        	db.open();
        	if(db.getScores(i).isEmpty()) {
        		for(int n = 1; n <= 20; n++) {
        			db.add(i, names[rnd.nextInt(names.length)], n * 1000);
        		}
        	}
        	db.close();
        }
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        		WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.brickening_layout);
        gameView = (BrickeningGame) findViewById(R.id.brickeninggame);
        gameView.loadSaveData(getPreferences(0));
    }
	
	@Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // gameView.saveGameState(getPreferences(0));
    }
	
	@Override
	protected void onRestoreInstanceState(Bundle dummy) {
		super.onRestoreInstanceState(dummy);
		// gameView.loadSaveData(getPreferences(0));
		
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		gameView.saveGameState(getPreferences(0));
		android.os.Process.killProcess(android.os.Process.myPid());
	}

	public void onClick(View v) {
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(gameView != null) {
			gameView.onActivityResult(requestCode, resultCode, data);
		}
	}
}