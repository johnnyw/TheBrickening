package com.vigasotech.brickening;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.LinkedList;
import java.util.Queue;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuff;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import com.vigasotech.brickening.BrickeningActivity;
import com.vigasotech.brickening.BrickeningBrowseLevelsActivity;
import com.vigasotech.brickening.BrickeningLevelEditorActivity;
import com.vigasotech.brickening.DBLocalHighScores;
import com.vigasotech.brickening.GameView;
import com.vigasotech.brickening.HighScoreActivity;
import com.vigasotech.brickening.UploadScoresTask;
import com.vigasotech.brickening.R;

class Brick {
	public int phase = 0;
	public int powerUp = 0;
	
	public Brick() {
	}
}

class Ball {
	public int x;
	public int y;
	public int xspeed;
	public int yspeed;
}

class PowerUp {
	public int x;
	public int y;
	public int type;
}

public class BrickeningGame extends GameView {
	boolean soundEnabled = true;
	SoundPool soundPool;
	
	// bitmap objects
	Bitmap
	oneup,
	add3,
	ball,
	barricade,
	block,
	dbl,
	dblship,
	ghost,
	half,
	halfship,
	laser,
	lasership,
	ship,
	sticky,
	laserbeam,
	touchscreen,
	background,
	fog,
	msg_barricade,
	msg_double,
	msg_extraballs,
	msg_extralife,
	msg_ghost,
	msg_half,
	msg_laser,
	msg_sticky,
	msg_fail,
	curmsg = null;
	
	// sound effects
	int ballShipHit,
	ballColoredBrick,
	ballSteelBrick,
	die,
	sfxNextLevel,
	laserBlast,
	sfxBarricade,
	sfxDoubleShip,
	sfxExtraBalls,
	sfxExtraLife,
	sfxGhost,
	sfxHalfShip,
	sfxLaser,
	sfxSticky;
	
	// game constants
	final int GRID_WIDTH = 8;
	final int GRID_HEIGHT = 22;
	final int BLOCK_WIDTH = 40;
	final int BLOCK_HEIGHT = 15;
	
	final int SCREEN_WIDTH = 320;
	final int SCREEN_HEIGHT = 480;
	
	AlertDialog gameOver;
	
	// game variables
	public boolean gameStarted = false;
	int score = 0;
	int curLevel = 0;
	Brick[][] grid = new Brick[GRID_WIDTH][GRID_HEIGHT];
	int horizGoal = 160; // start off paddle in middle of screen
	ArrayList<Ball> balls = new ArrayList<Ball>();
	boolean touchedLastFrame = false;
	int curItem = 0;
	int numLives = 3;
	Bitmap curship = null;
	int shipx = 160;
	Rect rect1 = new Rect();
	Rect rect2 = new Rect();
	int blocksLeft = 0;
	ArrayList<PowerUp> powerUps = new ArrayList<PowerUp>();
	boolean laserFired = false;
	Ball[] laserPoints; // use balls to represent lasers (so we can use the same hit detection code)
	
	// power-ups
	final int POWER_NONE = 0;
	final int POWER_ADD3 = 1;
	final int POWER_LASER = 2;
	final int POWER_BARRICADE = 3;
	final int POWER_STICKY = 4;
	final int POWER_DOUBLE = 5;
	final int POWER_HALF = 6;
	final int POWER_1UP = 7;
	final int POWER_GHOST = 8;
	
	Bitmap[] powerUpIcon = new Bitmap[9];
	
	// brick phases
	final int PHASE_EMPTY = 0;
	final int PHASE_RED = 1;
	final int PHASE_YELLOW = 2;
	final int PHASE_GREEN = 3;
	final int PHASE_STEEL = 4;
	
	final int NUM_LEVELS = 100;
	final int SHIP_Y = 405;
	final int SHIP_SPEED = 1200;
	final int OFFSET = 25;
	
	final int SCORE_BRICKHIT = 50;
	final int SPEED_CAP = 250;
	
	Paint paint = new Paint();
	PorterDuffColorFilter[] filters = new PorterDuffColorFilter[4];
	Context mContext;
	Random rnd = new Random();
	Menu menu;
	
	boolean initialized = false;

	private boolean isSticky = false;
	private boolean stuck = false;
	boolean ghostEnabled = false;
	boolean firstStrike = false;
	
	Queue<Integer> shipPosQueue = new LinkedList<Integer>();
	int ghostx = shipx;

	private boolean over9000 = false;
	
	public BrickeningGame(Context context) {
		super(context);
		mContext = context;
		initialize();
	}
	
	public BrickeningGame(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;

		// initialize bitmap objects
		Resources res = getResources();
		oneup = BitmapFactory.decodeResource(res, R.drawable.brick_1up);
		add3 = BitmapFactory.decodeResource(res, R.drawable.brick_add3);
		ball = BitmapFactory.decodeResource(res, R.drawable.brick_ball);
		barricade = BitmapFactory.decodeResource(res, R.drawable.brick_barricade);
		block = BitmapFactory.decodeResource(res, R.drawable.brick_block);
		dbl = BitmapFactory.decodeResource(res, R.drawable.brick_double);
		ghost = BitmapFactory.decodeResource(res, R.drawable.brick_ghost);
		half = BitmapFactory.decodeResource(res, R.drawable.brick_half);
		laser = BitmapFactory.decodeResource(res, R.drawable.brick_laser);
		sticky = BitmapFactory.decodeResource(res, R.drawable.brick_sticky);
		laserbeam = BitmapFactory.decodeResource(res, R.drawable.brick_laserbeam);
		touchscreen = BitmapFactory.decodeResource(res, R.drawable.brick_touchscreen);
		msg_barricade = BitmapFactory.decodeResource(res, R.drawable.brick_msg_barricade);
		msg_double = BitmapFactory.decodeResource(res, R.drawable.brick_msg_double);
		msg_extraballs = BitmapFactory.decodeResource(res, R.drawable.brick_msg_extraballs);
		msg_extralife = BitmapFactory.decodeResource(res, R.drawable.brick_msg_extralife);
		msg_ghost = BitmapFactory.decodeResource(res, R.drawable.brick_msg_ghost);
		msg_half = BitmapFactory.decodeResource(res, R.drawable.brick_msg_half);
		msg_laser = BitmapFactory.decodeResource(res, R.drawable.brick_msg_laser);
		msg_sticky = BitmapFactory.decodeResource(res, R.drawable.brick_msg_sticky);
		levelcleared = BitmapFactory.decodeResource(res, R.drawable.brick_levelcleared);
		msg_fail = BitmapFactory.decodeResource(res, R.drawable.brick_msg_failed);
		
		soundPool = new SoundPool(3, android.media.AudioManager.STREAM_MUSIC, 0);
		
		// load sounds
		ballShipHit = soundPool.load(mContext, R.raw.ball_ship_hit, 1);
		ballColoredBrick = soundPool.load(mContext, R.raw.ball_colored_brick, 1);
		ballSteelBrick = soundPool.load(mContext, R.raw.ball_steel_brick, 1);
		die = soundPool.load(mContext, R.raw.die, 1);
		sfxNextLevel = soundPool.load(mContext, R.raw.sfx_next_level, 1);
		laserBlast = soundPool.load(mContext, R.raw.laser_blast, 1);
		sfxBarricade = soundPool.load(mContext, R.raw.sfx_barricade, 1);
		sfxDoubleShip = soundPool.load(mContext, R.raw.sfx_doubleship, 1);
		sfxExtraBalls = soundPool.load(mContext, R.raw.sfx_extraballs, 1);
		sfxExtraLife = soundPool.load(mContext, R.raw.sfx_extralife, 1);
		sfxGhost = soundPool.load(mContext, R.raw.sfx_ghost, 1);
		sfxHalfShip = soundPool.load(mContext, R.raw.sfx_halfship, 1);
		sfxLaser = soundPool.load(mContext, R.raw.sfx_laser, 1);
		sfxSticky = soundPool.load(mContext, R.raw.sfx_sticky, 1);
		
		initialize();
	}
	
	@Override
	public void initialize() {
		Resources res = getResources();
		Display display = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay(); 
		int width = display.getWidth(); 
		int height = display.getHeight();
		int bg = R.drawable.brick_background;
		DBLocalHighScores db = new DBLocalHighScores(mContext);
		db.open();
		List<String> scores = db.getScores(DBLocalHighScores.TABLE_BRICKENING);
		db.close();
		String topScoreName = scores.get(0);
		String[] split = topScoreName.split(" ");
		topScoreName = split[0];
		if(topScoreName.equalsIgnoreCase("markdriscoll")) {
			bg = R.drawable.brick_driscoll_background;
		}
		if(topScoreName.equalsIgnoreCase("over9000")) {
			over9000 = true;
		}
		int regShip = R.drawable.brick_ship;
		int longShip = R.drawable.brick_doubleship;
		int shortShip = R.drawable.brick_halfship;
		int laserShip = R.drawable.brick_lasership;
		if(over9000) {
			regShip = R.drawable.brick_ship_9000;
			longShip = R.drawable.brick_doubleship_9000;
			shortShip = R.drawable.brick_halfship_9000;
			laserShip = R.drawable.brick_lasership_9000;
		}
		
		lasership = BitmapFactory.decodeResource(res, laserShip);
		ship = BitmapFactory.decodeResource(res, regShip);
		halfship = BitmapFactory.decodeResource(res, shortShip);
		dblship = BitmapFactory.decodeResource(res, longShip);
		
		background = Bitmap.createScaledBitmap(
				BitmapFactory.decodeResource(res, bg),
				width, height, true);
		
		fog = Bitmap.createScaledBitmap(
				BitmapFactory.decodeResource(res, R.drawable.brick_fog),
				width, height, true);
		
		curship = ship;
		
		powerUpIcon[POWER_ADD3] = add3;
		powerUpIcon[POWER_LASER] = laser;
		powerUpIcon[POWER_BARRICADE] = barricade;
		powerUpIcon[POWER_STICKY] = sticky;
		powerUpIcon[POWER_DOUBLE] = dbl;
		powerUpIcon[POWER_HALF] = half;
		powerUpIcon[POWER_1UP] = oneup;
		powerUpIcon[POWER_GHOST] = ghost;
		
		
		// initialize laser "balls"
		laserPoints = new Ball[2];
		laserPoints[0] = new Ball();
		laserPoints[1] = new Ball();
		
		laserPoints[0].x = (lasership.getWidth() / 2) - (lasership.getWidth() / 4);
		laserPoints[0].y = SHIP_Y;
		laserPoints[0].xspeed = 0;
		laserPoints[0].yspeed = SPEED_CAP;
		laserPoints[1].x = (lasership.getWidth() / 2) + (lasership.getWidth() / 4);
		laserPoints[1].y = SHIP_Y;
		laserPoints[1].xspeed = 0;
		laserPoints[1].yspeed = SPEED_CAP;
		
		initGrid();
		loadNextLevel();
		
		// initialize color filter objects
		filters[0] = new PorterDuffColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);
		filters[1] = new PorterDuffColorFilter(Color.YELLOW, PorterDuff.Mode.MULTIPLY);
		filters[2] = new PorterDuffColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);
		filters[3] = new PorterDuffColorFilter(Color.LTGRAY, PorterDuff.Mode.MULTIPLY);
		
		// initialize ball
		Ball b = new Ball();
		b.x = SCREEN_WIDTH / 2;
		b.y = SHIP_Y - 50;
		b.xspeed = 0;
		b.yspeed = SPEED_CAP;
		balls.add(b);
		
		// init game over alert dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		builder.setMessage("Game Over. Play again?")
		       .setCancelable(false)
		       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   resetGame();
		           }
		       })
		       .setNegativeButton("No", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   com.vigasotech.brickening.BrickeningActivity act =
		        		   (com.vigasotech.brickening.BrickeningActivity)mContext;
		        	   resetGame();
		        	   act.finish();
		           }
		       });
		
		gameOver = builder.create();
		paint.setTypeface(
				Typeface.createFromAsset(((Activity)mContext).getAssets(),
						"fonts/orbitron-medium.otf"));
		super.initialize();
		
		builder = new AlertDialog.Builder(mContext);
		builder.setMessage("Would you like to enable sounds?")
	       .setCancelable(false)
	       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
	        	   soundEnabled = true;
	           }
	       })
	       .setNegativeButton("No", new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
	        	   soundEnabled = false;
	           }
	       });
		final AlertDialog d = builder.create();
		((Activity)mContext).runOnUiThread(new Runnable() {
			@Override
			public void run() {
				d.show();
			}
		});
		initialized = true;
	}
	
	void resetGame() {
		for(int i = 0; i < GRID_WIDTH; i++) {
			grid[i][GRID_HEIGHT - 1].phase = 0;
		}
		blocksLeft = 0;
		curLevel = 0;
		score = 0;
		numLives = 3;
		resetBall();
		loadNextLevel();
	}
	
	int diff = 0;
	boolean lagged = false;
	boolean highScoreShown = false;
	Dialog highScoreDialog = null;
	UploadScoresTask uploadScoresTask = null;
	int accumulator = 0;
	int framerate = 0;
	int frameCounter = 0;
	boolean newInstance = true;
	boolean menuShowing = false;
	
	@Override
	public void updateGame(long elapsed) {
		float delta = (float) elapsed / 1000.0f;
		if(newInstance) accumulator += (int)elapsed;
		if(accumulator > 3000) {
			accumulator = 0;
			if(newInstance && !menuShowing) {
				newInstance = false;
				final Activity activity = (Activity)mContext;
				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(activity,
								"Tip: Press the MENU button for more options.",
								Toast.LENGTH_LONG).show();
					}
				});
			}
		}
		if(highScoreShown) {
			if(highScoreDialog != null) {
				if(!highScoreDialog.isShowing()) {
					if(uploadScoresTask == null) {
						highScoreShown = false;
						Activity act = (Activity)mContext;
						act.runOnUiThread(new Runnable() {
							public void run() {
								gameOver.show();
							}
						});
					} else {
						if(uploadScoresTask.getStatus() == AsyncTask.Status.FINISHED) {
							highScoreShown = false;
							Activity act = (Activity)mContext;
							act.runOnUiThread(new Runnable() {
								public void run() {
									gameOver.show();
								}
							});
							uploadScoresTask = null;
						}
					}
				}
			}
		}
		
		if(gameStarted) {
			if(numLives == 0 || gameEnded) {
				gameEnded = false;
				gameStarted = false;
				Activity act = (Activity)mContext;
				DBLocalHighScores db = new DBLocalHighScores(mContext);
				db.open();
				List<String> entries = db.getScores(DBLocalHighScores.TABLE_BRICKENING);
				db.close();
				boolean newHighScore = false;
				for(int i = 0; i < entries.size(); i++) {
					String[] strs = entries.get(i).split(" ");
					int highScore = Integer.parseInt(strs[strs.length-1]);
					if(highScore < score && curLevel != 0) newHighScore = true;
				}
				if(newHighScore) {
					act.runOnUiThread(new Runnable() {
						public void run() {
							final Dialog d = new Dialog(mContext);
							final UploadScoresTask upload = new UploadScoresTask(mContext);
							d.setContentView(R.layout.highscore_dialog);
							Button uploadButton = (Button)d.findViewById(R.id.uploadbutton);
							Button postButton = (Button)d.findViewById(R.id.postbutton);
							Button cancelButton = (Button)d.findViewById(R.id.cancelbutton);
							final EditText edit = (EditText)d.findViewById(R.id.playername);
							edit.setSingleLine(true);
							
							uploadButton.setOnClickListener(new OnClickListener() {
								public void onClick(View v) {
									// get entered player name
									String name = edit.getText().toString();
									// remove whitespace
									name = name.replaceAll("\\W", "");
									// verify that name is appropriate
									if(!DBLocalHighScores.isNameAppropriate(name, mContext)) {
										edit.setText("");
										Toast.makeText(mContext,
												"Please try another name.", Toast.LENGTH_LONG).show();
										return;
									}
									// upload score to server
									upload.execute("TheBrickening", name, Integer.toString(score));
									
									// add to local database as well
									DBLocalHighScores db = new DBLocalHighScores(mContext);
									db.open();
									db.add(DBLocalHighScores.TABLE_BRICKENING, name, score);
									db.close();
									d.dismiss();
									uploadScoresTask = upload;
								}
								
							});
							
							postButton.setOnClickListener(new OnClickListener() {
								public void onClick(View v) {
									// get entered player name
									String name = edit.getText().toString();
									// remove whitespace
									name = name.replaceAll("\\W", "");
									// verify that name is appropriate
									if(!DBLocalHighScores.isNameAppropriate(name, mContext)){
										edit.setText("");
										Toast.makeText(mContext,
												"Please try another name.", Toast.LENGTH_LONG).show();
										return;
									}
									// add score to local database
									DBLocalHighScores db = new DBLocalHighScores(mContext);
									db.open();
									db.add(DBLocalHighScores.TABLE_BRICKENING, name, score);
									db.close();
									d.dismiss();
								}
								
							});
							
							cancelButton.setOnClickListener(new OnClickListener() {
								public void onClick(View v) {
									d.dismiss();
								}
								
							});
							
							d.setTitle("New High Score!!!");
							d.setCancelable(true);
							d.show();
							highScoreDialog = d;
							highScoreShown = true;
						}
					});
				} else {
					act.runOnUiThread(new Runnable() {
						public void run() {
							gameOver.show();
						}
					});
				}
			}
			
			if(shipPosQueue.size() == 20) {
				ghostx = shipPosQueue.remove();
			}
			
			if(Math.abs(horizGoal - shipx) <= SHIP_SPEED * delta)
				shipx = horizGoal;
			
			if(horizGoal > shipx) shipx += SHIP_SPEED * delta;
			if(horizGoal < shipx) shipx -= SHIP_SPEED * delta;
			
			shipx = Math.max(curship.getWidth() / 2, shipx);
			shipx = Math.min(SCREEN_WIDTH - (curship.getWidth() / 2), shipx);
			
			shipPosQueue.add(shipx);
			
			// process deployed power-ups
			for(int i = 0; i < powerUps.size(); i++) {
				PowerUp p = powerUps.get(i);
				int halfwidth = oneup.getWidth() / 2;
				int halfheight = oneup.getHeight() / 2;
				p.y += 180 * delta;
				Rect ship = rect1;
				Rect item = rect2;
				ship.set(shipx - (curship.getWidth() / 2), SHIP_Y,
						shipx + (curship.getWidth() / 2), SHIP_Y + curship.getHeight());
				item.set(p.x - halfwidth, p.y - halfheight, p.x + halfwidth, p.y + halfheight);
				
				if(Rect.intersects(ship, item)) {
					curItem = p.type;
					powerUps.remove(i);
				}
				
				if(p.y >= SCREEN_HEIGHT) powerUps.remove(i);
			}
			
			if(laserPoints[0].y < OFFSET) laserFired = false;
			
			// handle laser points
			if(laserFired) {
				laserPoints[0].y -= laserPoints[0].yspeed * delta;
				laserPoints[1].y -= laserPoints[1].yspeed * delta;
				
				for(int x = 0; x < GRID_WIDTH; x++) {
					for(int y = 0; y < GRID_HEIGHT; y++) {
						Rect point = rect1;
						Rect block = rect2;
						
						int h = laserPoints[0].y + laserbeam.getHeight() < SHIP_Y ?
								laserPoints[0].y + laserbeam.getHeight() : SHIP_Y;
						// check for collision with first laser beam
						point.set(laserPoints[0].x, laserPoints[0].y,
								laserPoints[0].x + laserbeam.getWidth(),
								h);
						block.set(x * BLOCK_WIDTH, y * BLOCK_HEIGHT + OFFSET,
								x * BLOCK_WIDTH + BLOCK_WIDTH, y * BLOCK_HEIGHT + OFFSET + BLOCK_HEIGHT);
						boolean hit = false;
						if(Rect.intersects(point, block) && y != GRID_HEIGHT - 1 && laserFired) {
							if(grid[x][y].phase != PHASE_EMPTY && gameStarted) {
								if(grid[x][y].phase != PHASE_STEEL) {
									flashGrid[x][y] = 255;
									grid[x][y].phase = PHASE_EMPTY;
								}
								hit = true;
							}
						}
						// check for 2nd laser beam
						h = laserPoints[1].y + laserbeam.getHeight() < SHIP_Y ?
							laserPoints[1].y + laserbeam.getHeight() : SHIP_Y;
							
						point.set(laserPoints[1].x, laserPoints[1].y,
								laserPoints[1].x + laserbeam.getWidth(),
								h);
						if(Rect.intersects(point, block) && y != GRID_HEIGHT - 1 && laserFired) {
							if(grid[x][y].phase != PHASE_EMPTY && gameStarted) {
								if(grid[x][y].phase != PHASE_STEEL) {
									flashGrid[x][y] = 255;
									grid[x][y].phase--;
								}
								hit = true;
							}
						}
						
						if((grid[x][y].phase == PHASE_EMPTY ||
								grid[x][y].phase == PHASE_STEEL) && hit) {
							laserFired = false;
							
							if(grid[x][y].phase != PHASE_STEEL) {
								score += SCORE_BRICKHIT;
								blocksLeft--;
							}
							
							if(grid[x][y].powerUp != POWER_NONE) {
								// deploy power-up
								PowerUp p = new PowerUp();
								p.type = grid[x][y].powerUp;
								p.x = x * BLOCK_WIDTH + (BLOCK_WIDTH / 2);
								p.y = y * BLOCK_HEIGHT + (BLOCK_HEIGHT / 2) + OFFSET;
								powerUps.add(p);
							}
							laserPoints[0].x = shipx - (lasership.getWidth() / 4) + 3;
							laserPoints[0].y = SHIP_Y;
							laserPoints[1].x = shipx + (lasership.getWidth() / 4) - 3;
							laserPoints[1].y = SHIP_Y;
						}
					}
				}
			}
			else {
				laserPoints[0].x = shipx - (lasership.getWidth() / 4);
				laserPoints[0].y = SHIP_Y;
				laserPoints[1].x = shipx + (lasership.getWidth() / 4);
				laserPoints[1].y = SHIP_Y;
			}
			
			if(curItem != POWER_NONE) {
				// initialize power up message sequence
				showMsg = true;
				if(msgTimer > 250) msgTimer = 250;
			}
			
			int itemNoise = -1;
			
			switch(curItem) {
			case POWER_NONE:
				break;
			case POWER_ADD3:
				for(int i = 0; i < 3; i++) {
					Ball b = new Ball();
					b.x = shipx;
					b.y = SHIP_Y - 50;
					b.xspeed = rnd.nextInt(SPEED_CAP * 2) - SPEED_CAP;
					b.yspeed = -SPEED_CAP;
					balls.add(b);
				}
				curItem = POWER_NONE;
				curmsg = msg_extraballs;
				isSticky = false;
				score += 30;
				itemNoise = sfxExtraBalls;
				break;
			case POWER_LASER:
				curship = lasership;
				ghostEnabled = false;
				isSticky = false;
				curmsg = msg_laser;
				curItem = POWER_NONE;
				itemNoise = sfxLaser;
				break;
			case POWER_BARRICADE:
				for(int i = 0; i < GRID_WIDTH; i++) {
					if(grid[i][GRID_HEIGHT - 1].phase == PHASE_EMPTY) {
						grid[i][GRID_HEIGHT - 1].phase = PHASE_RED;
						flashGrid[i][GRID_HEIGHT - 1] = 255;
					}
				}
				score += SCORE_BRICKHIT * GRID_WIDTH;
				curItem = POWER_NONE;
				curmsg = msg_barricade;
				itemNoise = sfxBarricade;
				break;
			case POWER_DOUBLE:
				curship = dblship;
				ghostEnabled = false;
				isSticky = false;
				curmsg = msg_double;
				curItem = POWER_NONE;
				itemNoise = sfxDoubleShip;
				break;
			case POWER_HALF:
				curship = halfship;
				ghostEnabled = false;
				isSticky = false;
				curmsg = msg_half;
				curItem = POWER_NONE;
				itemNoise = sfxHalfShip;
				break;
			case POWER_1UP:
				numLives++;
				curItem = POWER_NONE;
				curmsg = msg_extralife;
				itemNoise = sfxExtraLife;
				break;
			case POWER_STICKY:
				isSticky = true;
				curship = ship;
				curItem = POWER_NONE;
				curmsg = msg_sticky;
				ghostEnabled = false;
				itemNoise = sfxSticky;
				break;
			case POWER_GHOST:
				isSticky = false;
				stuck = false;
				ghostEnabled = true;
				curship = ship;
				curmsg = msg_ghost;
				curItem = POWER_NONE;
				itemNoise = sfxGhost;
				break;
			}
			
			if(itemNoise != -1 && soundEnabled) {
				soundPool.play(itemNoise, 1.0f, 1.0f, 2, 0, 1.0f);
			}
			
			// update ball positions
			for(int i = 0; i < balls.size(); i++) {
				Ball b = balls.get(i);
				
				boolean wallCollision = false;
				// do ball/wall collision
				if(b.x - (ball.getWidth()/2) < 0){  b.xspeed = Math.abs(b.xspeed); wallCollision = true; }
				if(b.x + (ball.getWidth()/2) > SCREEN_WIDTH) { b.xspeed = -Math.abs(b.xspeed); wallCollision = true; }
				if(b.y - (ball.getHeight()/2) < OFFSET) { b.yspeed = Math.abs(b.yspeed); wallCollision = true; }
				if(wallCollision && soundEnabled && !stuck) {
					soundPool.play(ballShipHit, 1.0f, 1.0f, 1, 0, 1.0f);
				}
				if(b.y + (ball.getHeight()/2) > SCREEN_HEIGHT) {
					balls.remove(i);
					if(!balls.isEmpty()) score -= 10;
				}
				
				float fourthWidth = ship.getWidth() / 4;
				
				// collision with ghost ship
				if(ghostEnabled) {
					rect1.set(b.x - (ball.getWidth()), b.y - (ball.getHeight()),
							b.x + (ball.getWidth()), b.y + (ball.getHeight()));
					rect2.set(ghostx - (curship.getWidth() / 2), SHIP_Y,
							ghostx + (curship.getWidth() / 2), SHIP_Y + curship.getHeight());
					if(Rect.intersects(rect1, rect2)) {
						b.yspeed = -Math.abs(b.yspeed);
						if(b.xspeed == 0) {
							b.xspeed = -(shipx - b.x);
							b.xspeed = (int) ((b.xspeed / fourthWidth) * SPEED_CAP);
						} else {
							b.xspeed = (b.xspeed >= 0 ? Math.abs(ghostx - b.x) / 2: -Math.abs(ghostx - b.x) / 2);
							b.xspeed = (int) ((b.xspeed / fourthWidth) * SPEED_CAP);
						}
					}
				}
				
				// ball/ship collision
				rect1.set(b.x - (ball.getWidth()) + (int)(b.xspeed * delta), b.y - (ball.getHeight()) + (int)(b.yspeed * delta),
						b.x + (ball.getWidth()) + (int)(b.xspeed * delta), b.y + (ball.getHeight()) + (int)(b.yspeed * delta));
				rect2.set(shipx - (curship.getWidth() / 2), SHIP_Y,
						shipx + (curship.getWidth() / 2), SHIP_Y + curship.getHeight());
				
				if(Rect.intersects(rect1, rect2)) {
					if(isSticky && !stuck) {
						stuck = true;
						diff = b.x - shipx;
						balls.clear();
						balls.add(b);
					}
					
					if(!stuck && soundEnabled) {
						soundPool.play(ballShipHit, 1.0f, 1.0f, 1, 0, 1.0f);
					}
					
					b.yspeed = -Math.abs(b.yspeed);
					if(b.xspeed == 0) {
						if(!firstStrike) {
							firstStrike = true;
							b.xspeed = rnd.nextInt(SPEED_CAP * 2) - SPEED_CAP;
						} else {
							b.xspeed = -(shipx - b.x);
							b.xspeed = (int) ((b.xspeed / fourthWidth) * SPEED_CAP);
						}
					}
					else {
						b.xspeed = (b.xspeed >= 0 ? Math.abs(shipx - b.x) / 2 : -Math.abs(shipx - b.x) / 2);
						b.xspeed = (int) ((b.xspeed / fourthWidth) * SPEED_CAP);
					}
				}
				
				if(!isSticky) stuck = false;
				
				if(balls.isEmpty()) {
					numLives--;
					curmsg = msg_fail;
					levelClearedMsg = true;
					
					if(soundEnabled) {
						soundPool.play(die, 1.0f, 1.0f, 1, 0, 1.0f);
					}
					if(numLives != 0)
						resetBall();
					score -= 100;
				}
				
				if(blocksLeft == 0) {
					if(soundEnabled) soundPool.play(sfxNextLevel, 1.0f, 1.0f, 1, 0, 1.0f);
					if(curLevel == 0) resetGame();
					else {
						if(curLevel + 1 > NUM_LEVELS) gameEnded = true;
						loadNextLevel();
					}
					resetBall();
					if(gameEnded) gameStarted = true;
					else {
						curmsg = levelcleared;
						levelClearedMsg = true;
					}
				}
				
				if(!stuck) {
					b.xspeed = Math.min(b.xspeed, SPEED_CAP);
					b.xspeed = Math.max(b.xspeed, -SPEED_CAP);
					b.x += (int)(b.xspeed * delta);
					b.y += (int)(b.yspeed * delta);
				} else {
					b.x = shipx + diff;
					b.y = SHIP_Y - ball.getHeight();
				}
				doBallBlockCollision(b, delta);
			}
		}
	}
	
	// point scratch variables (allocated beforehand for efficiency)
	Point point1 = new Point();
	Point point2 = new Point();
	Point point3 = new Point();
	Point point4 = new Point();
	Point point5 = new Point();
	
	boolean hitLastFrame = false;
	
	void doBallBlockCollision(Ball b, float delta) {
		if(hitLastFrame) {
			hitLastFrame = false;
			return;
		}
		
		// rect1 = ball rectangle
		rect1.set(b.x - 0 + (int)(b.xspeed * delta), b.y - 0 + (int)(b.yspeed * delta),
				b.x + 0 + (int)(b.xspeed * delta), b.y + 0 + (int)(b.yspeed * delta));
		
		int found = -1;
		
		// rect2 = block rectangle
		for(int ix = 0; ix < GRID_WIDTH; ix++) {
			for(int iy = 0; iy < GRID_HEIGHT; iy++) {
				int x = ix;
				int y = iy;
				
				if(b.xspeed < 0) x = GRID_WIDTH - ix - 1;
				if(b.yspeed < 0) y = GRID_HEIGHT - iy - 1;
				
				if(grid[x][y].phase != PHASE_EMPTY) {
					int vert = y == GRID_HEIGHT - 1 ? SCREEN_HEIGHT - BLOCK_HEIGHT * 3 :
						y * BLOCK_HEIGHT + OFFSET;
					rect2.set(x * BLOCK_WIDTH - ball.getWidth(), vert - ball.getHeight(),
							x * BLOCK_WIDTH + BLOCK_WIDTH + ball.getWidth(), vert + BLOCK_HEIGHT + ball.getHeight());
					if(Rect.intersects(rect1, rect2)) {
						hitLastFrame = true;
						
						// generate a line segment from current ball position
						// to next ball position
						Point next = point1; next.set(b.x + (int)(b.xspeed * delta), (int)(b.y + b.yspeed * delta));
						Point cur = point2; cur.set(b.x, b.y);
						
						Point p1 = point3;
						Point p2 = point4;
						int shortestDist = 100;
						int closest = 0;
						int dist = 0;
						final int TOP = 0;
						final int BOTTOM = 1;
						final int LEFT = 2;
						final int RIGHT = 3;
						
						
						p1.set(rect2.left, rect2.top);
						p2.set(rect2.left, rect2.bottom);
						if(linesIntersect(cur, next, p1, p2, point5)) {
							shortestDist = lineDist(cur, point5);
							closest = LEFT;
						}
						
						p1.set(rect2.left, rect2.top);
						p2.set(rect2.right, rect2.top);
						if(linesIntersect(cur, next, p1, p2, point5)) {
							dist = lineDist(cur, point5);
							if(dist < shortestDist) {
								closest = TOP;
								shortestDist = dist;
							}
						}
						
						p1.set(rect2.right, rect2.top);
						p2.set(rect2.right, rect2.bottom);
						if(linesIntersect(cur, next, p1, p2, point5)) {
							dist = lineDist(cur, point5);
							if(dist < shortestDist) {
								closest = RIGHT;
								shortestDist = dist;
							}
						}
						
						p1.set(rect2.left, rect2.bottom);
						p2.set(rect2.right, rect2.bottom);
						if(linesIntersect(cur, next, p1, p2, point5)) {
							dist = lineDist(cur, point5);
							if(dist < shortestDist) {
								closest = BOTTOM;
								shortestDist = dist;
							}
						}
						
						if(grid[x][y].phase != PHASE_STEEL && gameStarted && found == -1) {
							if(y != GRID_HEIGHT - 1) {
								score += curship == halfship ? SCORE_BRICKHIT * 2 : SCORE_BRICKHIT;
							}
							else score -= SCORE_BRICKHIT;
							flashGrid[x][y] = 255;
							grid[x][y].phase--;
							if(soundEnabled) {
								soundPool.play(ballColoredBrick, 1.0f, 1.0f, 1, 0, 1.0f);
							}
						}
						
						if(grid[x][y].phase == PHASE_STEEL && soundEnabled) {
							soundPool.play(ballSteelBrick, 1.0f, 1.0f, 1, 0, 1.0f);
						}
						switch(closest) {
						case LEFT:
							if(found != LEFT) {
								b.xspeed = -Math.abs(b.xspeed);
								found = LEFT;
							}
							break;
						case RIGHT:
							if(found != RIGHT) {
								b.xspeed = Math.abs(b.xspeed);
								found = RIGHT;
							}
							break;
						case TOP:
							if(found != TOP && found != LEFT && found != RIGHT) {
								b.yspeed = b.y < y * BLOCK_HEIGHT + OFFSET ?
										-Math.abs(b.yspeed) : Math.abs(b.yspeed);
								found = TOP;
							}
							break;
						case BOTTOM:
							if(found != BOTTOM && found != LEFT && found != RIGHT) {
								b.yspeed = b.y > y * BLOCK_HEIGHT + OFFSET +
								BLOCK_HEIGHT ? Math.abs(b.yspeed) :
									-Math.abs(b.yspeed);
								found = BOTTOM;
							}
							break;
						}
						
						if(y == GRID_HEIGHT - 1) b.yspeed = -Math.abs(b.yspeed);
						
						if(grid[x][y].phase == PHASE_EMPTY && y != GRID_HEIGHT - 1) {
							blocksLeft--;
							if(grid[x][y].powerUp != POWER_NONE) {
								// deploy power-up
								PowerUp p = new PowerUp();
								p.type = grid[x][y].powerUp;
								p.x = x * BLOCK_WIDTH + (BLOCK_WIDTH / 2);
								p.y = y * BLOCK_HEIGHT + (BLOCK_HEIGHT / 2) + OFFSET;
								powerUps.add(p);
							}
						}
					}
				}
			}
		}
	}
	
	void resetBall() {
		for(int i = 0; i < GRID_WIDTH; i++) {
			grid[i][GRID_HEIGHT - 1].phase = 0;
		}
		showMsg = false;
		firstStrike = false;
		ghostEnabled = false;
		isSticky = false;
		laserFired = false;
		curship = ship;
		horizGoal = 160;
		gameStarted = false;
		shipx = 160;
		Ball b = new Ball();
		b.x = SCREEN_WIDTH / 2;
		b.y = SHIP_Y - 50;
		b.xspeed = 0;
		b.yspeed = SPEED_CAP;
		balls.clear();
		powerUps.clear();
		balls.add(b);
		curItem = POWER_NONE;
	}
	
	int osc = 255;
	int dir = -480;
	
	int [][]flashGrid = new int[GRID_WIDTH][GRID_HEIGHT];
	float fogoffset = 0;
	PorterDuffColorFilter multiplyFilter = new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);
	long msgTimer = 0;
	boolean showMsg = false;
	Rect msgRect = new Rect();

	private boolean levelClearedMsg = false;

	private Bitmap levelcleared;
	
	@Override
	public void drawGame(Canvas canvas, long elapsed) {
		float delta = (float) elapsed / 1000.0f;
		if(!initialized) return;
		
		fogoffset += 60 * delta;
		if(fogoffset >= fog.getWidth()) fogoffset = 0;
		
		// clear screen
		canvas.drawColor(Color.BLACK);
		canvas.drawBitmap(background, 0, 0, paint);
		paint.setAlpha(25);
		canvas.drawBitmap(fog, fogoffset, 0, paint);
		canvas.drawBitmap(fog, fogoffset - fog.getWidth(), 0, paint);
		paint.setColor(Color.WHITE);
		
		// draw grid of blocks
		for(int x = 0; x < GRID_WIDTH; x++) {
			for(int y = 0; y < GRID_HEIGHT; y++) {
				if(grid[x][y].phase != 0) {
					paint.setColorFilter(filters[grid[x][y].phase - 1]);
					int vert = y == GRID_HEIGHT - 1 ? SCREEN_HEIGHT - BLOCK_HEIGHT * 3 :
						y * BLOCK_HEIGHT + OFFSET;
					canvas.drawBitmap(block, x * BLOCK_WIDTH, vert,
							paint);
				}
			}
		}
		
		// remove color filter for other game objects
		paint.setColorFilter(null);
		
		// draw flash grid
		for(int x = 0; x < GRID_WIDTH; x++) {
			for(int y = 0; y < GRID_HEIGHT; y++) {
				if(flashGrid[x][y] > 0) {
					int vert = y == GRID_HEIGHT - 1 ? SCREEN_HEIGHT - BLOCK_HEIGHT * 3 :
						y * BLOCK_HEIGHT + OFFSET;
					paint.setColor(Color.WHITE);
					paint.setAlpha(flashGrid[x][y]);
					canvas.drawBitmap(block, x * BLOCK_WIDTH, vert,
							paint);
					flashGrid[x][y] -= Math.abs(dir * delta);
				}
			}
		}
		
		paint.setColor(Color.WHITE);
		
		// draw laser if applicable
		if(laserFired) {
			for(int i = 0; i < 2; i++) {
				int h = laserPoints[i].y + laserbeam.getHeight() < SHIP_Y ?
						laserPoints[i].y + laserbeam.getHeight() : SHIP_Y;
						
				rect1.set(0, 0, laserbeam.getWidth(),
						laserbeam.getHeight() > laserPoints[i].y - SHIP_Y ?
								laserbeam.getHeight() : laserPoints[i].y - SHIP_Y);
				rect2.set(laserPoints[i].x, laserPoints[i].y,
					laserPoints[i].x + laserbeam.getWidth(),
					h);
				canvas.drawBitmap(laserbeam, rect1, rect2, paint);
			}
		}
		
		// draw balls
		int ballWidth = ball.getWidth();
		int ballHeight = ball.getHeight();
		for(int i = 0; i < balls.size(); i++) {
			Ball b = balls.get(i);
			canvas.drawBitmap(ball, b.x - (ballWidth / 2), b.y - (ballHeight / 2), paint);
		}
		
		// draw power-ups
		for(int i = 0; i < powerUps.size(); i++) {
			PowerUp p = powerUps.get(i);
			canvas.drawBitmap(powerUpIcon[p.type], p.x - 10,
					p.y - 10, paint);
		}
		
		// draw ship
		if(ghostEnabled) {
			paint.setAlpha(128);
			canvas.drawBitmap(curship, ghostx - (curship.getWidth() / 2), SHIP_Y - 5, paint);
			paint.setAlpha(255);
		}
		canvas.drawBitmap(curship, shipx - (curship.getWidth() / 2), SHIP_Y - 5, paint);
		
		// draw HUD info
		paint.setStyle(Paint.Style.FILL);
		paint.setTextSize(15);
		paint.setColor(Color.WHITE);
		paint.setTextAlign(Paint.Align.LEFT);
		
		String hudtxt = "Score: " + Integer.toString(score) + " Lives: " + numLives +
		" Level: " + curLevel;
		canvas.drawText(hudtxt, 5, 15, paint);
		
		if(!gameStarted && !levelClearedMsg) {
			if(showMsg) {
				showMsg = false;
				msgTimer = 0;
			}
			osc += (int)(dir * delta);
			if(osc <= 0) {
				osc = 0;
				dir = 480;
			}
			
			if(osc >= 255) {
				osc = 255;
				dir = -480;
			}
			
			// draw message "Touch Screen to Begin"
			float width = touchscreen.getWidth();
			paint.setAlpha(osc);
			canvas.drawBitmap(touchscreen, (SCREEN_WIDTH / 2) - (width / 2),
					SCREEN_HEIGHT / 2, paint);
			paint.setColor(Color.WHITE);
		} else {
			if(levelClearedMsg) {
				if(showMsg) {
					showMsg = false;
					msgTimer = 0;
				}
				// curmsg = levelcleared;
				
				if(msgTimer < 250) {
					int alpha = (int)(((float)msgTimer/250.0f)*255);
					float invdelta = 1.0f - ((float)msgTimer/250.0f);
					int vertical = (SCREEN_HEIGHT / 2) + (int)(invdelta * curmsg.getHeight());
					paint.setAlpha(alpha);
					if(curmsg != null) {
						canvas.drawBitmap(curmsg, (SCREEN_WIDTH / 2) - (curmsg.getWidth() / 2),
								vertical, paint);
					}
					paint.setColor(Color.WHITE);
				}
				
				if(msgTimer >= 250 && msgTimer < 750) {
					if(curmsg != null) {
						canvas.drawBitmap(curmsg, (SCREEN_WIDTH / 2) - (curmsg.getWidth() / 2),
								SCREEN_HEIGHT / 2, paint);
					}
				}
				
				if(msgTimer >= 750 && msgTimer < 1000) {
					float d = (float)(msgTimer - 750) / 250.0f;
					float s = 1.0f + d;
					int a = (int)((1.0f - d) * 255.0f);
					int w = (int)(curmsg.getWidth() * s);
					msgRect.left = (SCREEN_WIDTH / 2) - (w / 2);
					msgRect.top = SCREEN_HEIGHT / 2;
					msgRect.right = msgRect.left + (int)(curmsg.getWidth() * s);
					msgRect.bottom = msgRect.top + (int)(curmsg.getHeight() * s);
					paint.setAlpha(a);
					if(curmsg != null) {
						canvas.drawBitmap(curmsg, null, msgRect, paint);
					}
					paint.setColor(Color.WHITE);
				}
				
				msgTimer += elapsed;
				if(msgTimer > 1000) {
					msgTimer = 0;
					levelClearedMsg = false;
					osc = 0;
				}
			}
			if(showMsg) {
				if(msgTimer < 250) {
					int alpha = (int)(((float)msgTimer/250.0f)*255);
					float invdelta = 1.0f - ((float)msgTimer/250.0f);
					int vertical = (SCREEN_HEIGHT / 2) + (int)(invdelta * curmsg.getHeight());
					paint.setAlpha(alpha);
					if(curmsg != null) {
						canvas.drawBitmap(curmsg, (SCREEN_WIDTH / 2) - (curmsg.getWidth() / 2),
								vertical, paint);
					}
					paint.setColor(Color.WHITE);
				}
				
				if(msgTimer >= 250 && msgTimer < 500) {
					if(curmsg != null) {
						canvas.drawBitmap(curmsg, (SCREEN_WIDTH / 2) - (curmsg.getWidth() / 2),
								SCREEN_HEIGHT / 2, paint);
					}
				}
				
				if(msgTimer >= 500 && msgTimer < 750) {
					float d = (float)(msgTimer - 500) / 250.0f;
					float s = 1.0f + d;
					int a = (int)((1.0f - d) * 255.0f);
					int w = (int)(curmsg.getWidth() * s);
					msgRect.left = (SCREEN_WIDTH / 2) - (w / 2);
					msgRect.top = SCREEN_HEIGHT / 2;
					msgRect.right = msgRect.left + (int)(curmsg.getWidth() * s);
					msgRect.bottom = msgRect.top + (int)(curmsg.getHeight() * s);
					paint.setAlpha(a);
					if(curmsg != null) {
						canvas.drawBitmap(curmsg, null, msgRect, paint);
					}
					paint.setColor(Color.WHITE);
				}
				
				msgTimer += elapsed;
				if(msgTimer > 750) {
					msgTimer = 0;
					showMsg = false;
				}
			}
		}
	}
	
	void initGrid() {
		for(int x = 0; x < GRID_WIDTH; x++) {
			for(int y = 0; y < GRID_HEIGHT; y++) {
				grid[x][y] = new Brick();
			}
		}
	}
	
	boolean gameEnded = false;

	private int levelStartScore;
	void loadNextLevel() {
		levelStartScore = score;
		laserFired = false;
		curLevel++;
		if(curLevel > NUM_LEVELS) curLevel = 1;
		if(curLevel > 1) score += 1000;
		String name = "level" + Integer.toString(curLevel);
		int id = getResources().getIdentifier(name, "raw",
				"com.vigasotech.brickening");
		
		InputStream is = getResources().openRawResource(id);
		InputStreamReader reader = new InputStreamReader(is);
		
		int len = 0;
		try {
			len = is.available();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		char[] data = new char[len];
		
		try {
			reader.read(data);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		int cur = 0;
		for(int x = 0; x < GRID_WIDTH; x++) {
			for(int y = 0; y < GRID_HEIGHT - 1; y++) {
					grid[x][y].phase = Integer.parseInt(Character.toString(data[cur]));
					cur++;
					grid[x][y].powerUp = Integer.parseInt(Character.toString(data[cur]));
					cur++;
					
					if(grid[x][y].phase != PHASE_EMPTY &&
							grid[x][y].phase != PHASE_STEEL) blocksLeft++;
					flashGrid[x][y] = 0;
			}
		}
		
		for(int i = 0; i < GRID_WIDTH; i++) {
			flashGrid[i][GRID_HEIGHT - 1] = 0;
		}
		try {
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		gameStarted = false;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent e) {
		if(!gameStarted && !touchedLastFrame) {
			if(e.getAction() == MotionEvent.ACTION_UP) {
				horizGoal = 160;
				gameStarted = true;
			}
		} else {
			horizGoal = (int)e.getX();
			if(!laserFired && curship == lasership) {
				laserFired = true;
				if(soundEnabled) {
					soundPool.play(laserBlast, 1.0f, 1.0f, 1, 0, 1.0f);
				}
			}
			
			if(e.getAction() == MotionEvent.ACTION_DOWN) {
				touchedLastFrame = true;
			}
			
			if(e.getAction() == MotionEvent.ACTION_UP) {
				touchedLastFrame = false;
				stuck = false;
			}
		}
		return true;
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent e) {
		if(keyCode == KeyEvent.KEYCODE_MENU) {
			return true;
		}
		return false;
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent e) {
		if(keyCode == KeyEvent.KEYCODE_BACK){
			com.vigasotech.brickening.BrickeningActivity act =
     		   (com.vigasotech.brickening.BrickeningActivity)mContext;
			act.finish();
     	   return true;
		}
		return false;
	}
	
	boolean linesIntersect(Point line1p1, Point line1p2, Point line2p1, Point line2p2, Point output) {
		output.set(-100, -100);
	    int d   =   (line2p2.y - line2p1.y)*(line1p2.x-line1p1.x) -
	                (line2p2.x - line2p1.x)*(line1p2.y-line1p1.y);
	    
	    if(d == 0) return false;
	    
	    int n_a =   (line2p2.x - line2p1.x)*(line1p1.y-line2p1.y) - 
	                (line2p2.y - line2p1.y)*(line1p1.x-line2p1.x);
	                
	    int n_b =   (line1p2.x - line1p1.x)*(line1p1.y - line2p1.y) -
	                (line1p2.y - line1p1.y)*(line1p1.x - line2p1.x);
	    
	    int ua = (n_a << 14) / d;
	    int ub = (n_b << 14) / d;
	    
	    if(ua >=0 && ua <= (1<<14) && ub >= 0 && ub <= (1<<14))
	    {
	    	output.set(line1p1.x + ((ua * (line1p2.x - line1p1.x))>>14),
	    			line1p1.y + ((ua * (line1p2.y - line1p1.y))>>14));
	        return true;
	    }
		return false;
	}
	
	int lineDist(Point p1, Point p2) {
		int a = (p2.x - p1.x) * (p2.x - p1.x);
		int b = (p2.y - p1.y) * (p2.y - p1.y); 
		return (int)Math.sqrt(a + b);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		gameStarted = false;
		menuShowing = true;
		menu.add(0, 0, 0, "Resume");
		menu.add(0, 1, 0, "Reset Level");
		menu.add(0, 2, 0, "New Game");
		menu.add(0, 3, 0, "Level Editor");
		menu.add(0, 4, 0, "Browse Custom Levels");
		menu.add(0, 5, 0, "High Scores");
		menu.add(0, 6, 0, soundEnabled ? "Turn Sound Off" : "Turn Sound On");
		menu.add(0, 7, 0, "How to Play");
		menu.add(0, 8, 0, "About");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		menuShowing = false;
		final BrickeningActivity act = (BrickeningActivity) mContext;
		switch(item.getItemId()) {
		case 0:
			gameStarted = true;
			return true;
		case 1:
			if(curLevel != 0) {
				curLevel--;
				blocksLeft = 0;
				resetBall();
				score = levelStartScore;
				loadNextLevel();
			} else {
				if(customLevelData != null) {
					onActivityResult(0, Activity.RESULT_OK, customLevelData);
				} else if(customLevelLayout != null) {
					int n = 0;
					blocksLeft = 0;
					for(int x = 0; x < GRID_WIDTH; x++) {
						for(int y = 0; y < GRID_HEIGHT - 1; y++) {
							grid[x][y].phase = customLevelLayout[n];
							if(customLevelLayout[n] != PHASE_EMPTY) blocksLeft++;
							n++;
							grid[x][y].powerUp = customLevelLayout[n];
							n++;
						}
						resetBall();
						score = 0;
					}
				}
			}
			return true;
		case 2:
			AlertDialog.Builder dbuilder = new AlertDialog.Builder(mContext);
			dbuilder.setTitle("Attention!");
			dbuilder.setMessage("Are you sure you want to reset the game?");
			dbuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					resetGame();
				}
			});
			dbuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			dbuilder.show();
			return true;
		case 3:
			// launch level editor
			act.startActivity(new Intent(mContext, BrickeningLevelEditorActivity.class));
			return true;
		case 4:
			BrickeningBrowseLevelsActivity.SHOW_MENU = true;
			act.startActivityForResult(new Intent(mContext, BrickeningBrowseLevelsActivity.class), 0);
			return true;
		case 5:
			HighScoreActivity.game = DBLocalHighScores.TABLE_BRICKENING;
			act.startActivity(new Intent(mContext, HighScoreActivity.class));
			return true;
		case 6:
			soundEnabled = !soundEnabled;
			return true;
		case 7:
			act.startActivity(new Intent(mContext, HowtoActivity.class));
			return true;
		case 8:
			act.startActivity(new Intent(mContext, AboutActivity.class));
			return true;
		}
		return false;
	}
	
	Intent customLevelData = null;
	int[] customLevelLayout = null;
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode == Activity.RESULT_OK) {
			customLevelData = data;
			resetGame();
			curLevel = 0;
			blocksLeft = 0;
			customLevelLayout = data.getIntArrayExtra("bricklayout");
			int cur = 0;
    		for(int x = 0; x < 8; x++) {
    			for(int y = 0; y < 21; y++) {
    				int phase = customLevelLayout[cur];
    				cur++;
    				int powerUp = customLevelLayout[cur];
    				cur++;
    				grid[x][y].phase = phase;
    				grid[x][y].powerUp = powerUp;
    				if(phase != PHASE_EMPTY) {
    					if(phase != PHASE_STEEL) blocksLeft++;
    				}
    			}
    		}
		}
	}
	
	@Override
	public void saveGameState(SharedPreferences prefs) {
		SharedPreferences.Editor data = prefs.edit();
		data.clear();
		data.putBoolean("brick_somethingSaved", true);
		data.putInt("brick_score", score);
		data.putInt("brick_curLevel", curLevel);
		for(int x = 0; x < GRID_WIDTH; x++) {
			for(int y = 0; y < GRID_HEIGHT; y++) {
				data.putInt("brick_grid" + Integer.toString(x) + Integer.toString(y) + "phase", grid[x][y].phase);
				data.putInt("brick_grid" + Integer.toString(x) + Integer.toString(y) + "powerUp", grid[x][y].powerUp); 
			}
		}
		data.putInt("brick_horizGoal", horizGoal);
		data.putInt("brick_numBalls", balls.size());
		for(int i = 0; i < balls.size(); i++) {
			String curIndex = Integer.toString(i);
			Ball ball = balls.get(i);
			data.putInt("brick_ball" + curIndex + "x", ball.x);
			data.putInt("brick_ball" + curIndex + "y", ball.y);
			data.putInt("brick_ball" + curIndex + "xspeed", ball.xspeed);
			data.putInt("brick_ball" + curIndex + "yspeed", ball.yspeed);
		}
		data.putInt("brick_curItem", curItem);
		data.putInt("brick_numLives", numLives);
		if(curship == ship) {
			data.putString("brick_curship", "default");
		}
		if(curship == lasership) {
			data.putString("brick_curship", "laser");
		}
		
		if(curship == dblship) {
			data.putString("brick_curship", "dblship");
		}
		
		if(curship == halfship) {
			data.putString("brick_curship", "halfship");
		}
		data.putInt("brick_shipx", shipx);
		data.putInt("brick_blocksLeft", blocksLeft);
		data.putInt("numPowerUps", powerUps.size());
		for(int i = 0; i < powerUps.size(); i++) {
			String curIndex = Integer.toString(i);
			PowerUp power = powerUps.get(i);
			data.putInt("brick_powerUp" + curIndex + "type", power.type);
			data.putInt("brick_powerUp" + curIndex + "x", power.x);
			data.putInt("brick_powerUp" + curIndex + "y", power.y);
		}
		for(int i = 0; i < 2; i++) {
			String curIndex = Integer.toString(i);
			data.putInt("brick_laser" + curIndex + "x", laserPoints[i].x);
			data.putInt("brick_laser" + curIndex + "y", laserPoints[i].y);
			data.putInt("brick_laser" + curIndex + "xspeed", laserPoints[i].xspeed);
			data.putInt("brick_laser" + curIndex + "yspeed", laserPoints[i].yspeed);
		}
		data.putBoolean("brick_isSticky", isSticky);
		data.putBoolean("brick_stuck", stuck);
		data.putBoolean("brick_ghostEnabled", ghostEnabled);
		data.putBoolean("brick_firstStrike", firstStrike);
		data.putInt("brick_shipPosQueueSize", shipPosQueue.size());
		@SuppressWarnings("unchecked")
		List<Integer> shipPosList = (List<Integer>)shipPosQueue;
		for(int i = 0; i < shipPosQueue.size(); i++) {
			String curIndex = Integer.toString(i);
			data.putInt("brick_shipPosQueue" + curIndex, shipPosList.get(i));
		}
		data.putInt("brick_ghostx", ghostx);
		data.putBoolean("brick_soundEnabled", soundEnabled);
		if(customLevelLayout != null)
			for(int i = 0; i < customLevelLayout.length; i++) {
				data.putInt("customLevelLayout" + Integer.toString(i),
						customLevelLayout[i]);
			}
		data.commit();
	}

	@Override
	public void loadSaveData(SharedPreferences data) {
		if(data == null) return;
		gameStarted = false;
		newInstance = true;
		if(!data.getBoolean("brick_somethingSaved", false)) return;
		// newInstance = false;
		score = data.getInt("brick_score", score);
		curLevel = data.getInt("brick_curLevel", curLevel);
		for(int x = 0; x < GRID_WIDTH; x++) {
			for(int y = 0; y < GRID_HEIGHT; y++) {
				grid[x][y].phase = data.getInt("brick_grid" + Integer.toString(x) + Integer.toString(y) + "phase", 0);
				grid[x][y].powerUp = data.getInt("brick_grid" + Integer.toString(x) + Integer.toString(y) + "powerUp", 0); 
			}
		}
		horizGoal = data.getInt("brick_horizGoal", 0);
		int numBalls = data.getInt("brick_numBalls", 0);
		balls.clear();
		for(int i = 0; i < numBalls; i++) {
			String curIndex = Integer.toString(i);
			Ball ball = new Ball();
			ball.x = data.getInt("brick_ball" + curIndex + "x", 0);
			ball.y = data.getInt("brick_ball" + curIndex + "y", 0);
			ball.xspeed = data.getInt("brick_ball" + curIndex + "xspeed", 0);
			ball.yspeed = data.getInt("brick_ball" + curIndex + "yspeed", 0);
			balls.add(ball);
		}
		curItem = data.getInt("brick_curItem", 0);
		numLives = data.getInt("brick_numLives", 0);
		String curShip = data.getString("brick_curship", "default");
		if(curShip == "default") {
			curship = ship;
		}
		if(curShip == "laser") {
			curship = lasership;
		}
		
		if(curShip == "dblship") {
			curship = dblship;
		}
		
		if(curShip == "halfship") {
			curship = halfship;
		}
		shipx = data.getInt("brick_shipx", 0);
		blocksLeft = data.getInt("brick_blocksLeft", 0);
		int numPowerUps = data.getInt("numPowerUps", 0);
		powerUps.clear();
		for(int i = 0; i < numPowerUps; i++) {
			String curIndex = Integer.toString(i);
			PowerUp power = new PowerUp();
			power.type = data.getInt("brick_powerUp" + curIndex + "type", 0);
			power.x = data.getInt("brick_powerUp" + curIndex + "x", 0);
			power.y = data.getInt("brick_powerUp" + curIndex + "y", 0);
			powerUps.add(power);
		}
		for(int i = 0; i < 2; i++) {
			String curIndex = Integer.toString(i);
			laserPoints[i].x = data.getInt("brick_laser" + curIndex + "x", 0);
			laserPoints[i].y = data.getInt("brick_laser" + curIndex + "y", 0);
			laserPoints[i].xspeed = data.getInt("brick_laser" + curIndex + "xspeed", 0);
			laserPoints[i].yspeed = data.getInt("brick_laser" + curIndex + "yspeed", 0);
		}
		isSticky = data.getBoolean("brick_isSticky", false);
		stuck = data.getBoolean("brick_stuck", false);
		ghostEnabled = data.getBoolean("brick_ghostEnabled", false);
		firstStrike = data.getBoolean("brick_firstStrike", false);
		int shipPosQueueSize = data.getInt("brick_shipPosQueueSize", 0);
		@SuppressWarnings("unchecked")
		List<Integer> shipPosList = (List<Integer>)shipPosQueue;
		for(int i = 0; i < shipPosQueueSize; i++) {
			String curIndex = Integer.toString(i);
			shipPosList.add(data.getInt("brick_shipPosQueue" + curIndex, 0));
		}
		ghostx = data.getInt("brick_ghostx", 0);
		soundEnabled = data.getBoolean("brick_soundEnabled", true);
		customLevelLayout = new int[GRID_WIDTH * (GRID_HEIGHT - 1) * 2];
		for(int i = 0; i < customLevelLayout.length; i++) {
			customLevelLayout[i] = data.getInt("customLevelLayout" + Integer.toString(i), 0);
		}
	}
}
