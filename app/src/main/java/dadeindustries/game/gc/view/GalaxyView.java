package dadeindustries.game.gc.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.gc.R;

import java.util.ArrayList;

import dadeindustries.game.gc.mechanics.units.UnitActions;
import dadeindustries.game.gc.model.GlobalGameData;
import dadeindustries.game.gc.model.enums.SpacecraftOrder;
import dadeindustries.game.gc.model.factionartifacts.ColonyShip;
import dadeindustries.game.gc.model.factionartifacts.CombatShip;
import dadeindustries.game.gc.model.factionartifacts.Spaceship;
import dadeindustries.game.gc.model.players.Player;
import dadeindustries.game.gc.model.stellarphenomenon.Sector;
import dadeindustries.game.gc.model.stellarphenomenon.phenomena.System;
import dadeindustries.game.gc.model.stellarphenomenon.phenomena.Wormhole;

import static dadeindustries.game.gc.model.GlobalGameData.galaxySizeY;
import static dadeindustries.game.gc.model.GlobalGameData.isHumanPlayer;

public class GalaxyView extends View implements OnTouchListener, OnKeyListener {

	// Display details
	private static int NUM_SQUARES_IN_ROW = 4;
	private static int NUM_SQUARES_IN_COLUMN = 0;
	private static int SQUARE_SIZE;

	public Sector[][] sectors;

	// co-ordinates of the top left of the viewport
	// in real world co-ordinates
	protected Point viewPort = new Point(2, 2);
	private Context ctxt; // needed for Toast debugging
	private Paint paint = new Paint();

	// Global Bitmaps
	private Bitmap up, mo, p1, p2, wh; // Bitmap variables

	private GestureDetector gestureDetector;

	private MediaPlayer sound_yessir, sound_reporting, sound_setting_course;

	private GlobalGameData globalGameData;

	private int currentX, currentY;
	private Rect r = new Rect();

	private int SELECT_MODE = 0;
	private boolean CURRENTLY_ORDERING = false;

	private Spaceship selectedShip;

	private final int PADDING = 10;
	private final int THE_UNDISCOVERED_COUNTRY = Color.rgb(51, 0, 51);

	ScaleGestureDetector pinchDetector;


	public GalaxyView(Context context) {
		super(context);
		init(context);
	}

	/* This constructor is needed for "inflating" the UI from the XML layout */
	public GalaxyView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public void init(Context context) {
		GlobalGameData globalGameData = new GlobalGameData(10, 10);
		ctxt = context;

		/* Load sound effects */
		sound_yessir = MediaPlayer.create(context, R.raw.yessir);
		sound_reporting = MediaPlayer.create(context, R.raw.reporting);
		sound_setting_course = MediaPlayer.create(context, R.raw.setting_course);

		/* Get the size of the screen */
		DisplayMetrics metrics = getResources().getDisplayMetrics();
		int displayWidth = metrics.widthPixels;
		int displayHeight = metrics.heightPixels;

		/* Based on the screen size... determine how many sector squares can be seen at a time */
		NUM_SQUARES_IN_ROW = NUM_SQUARES_IN_ROW + ((displayWidth / 500) * 2);
		SQUARE_SIZE = displayWidth / NUM_SQUARES_IN_ROW;
		NUM_SQUARES_IN_COLUMN = displayHeight / SQUARE_SIZE;

		/* Set the background colour of the view to black and the drawn grid to white */
		setBackgroundColor(THE_UNDISCOVERED_COUNTRY);
		paint.setColor(Color.WHITE);
		paint.setStrokeWidth(3);
		loadBitmaps();

		this.globalGameData = globalGameData;
		sectors = globalGameData.getSectors();

		// Enable gesture detection
		gestureDetector = new GestureDetector(ctxt, new GestureListener());
		pinchDetector = new ScaleGestureDetector(context, new PinchListener());

		setOnTouchListener(this);

		// Enable keyboard detection
		setOnKeyListener(this);

		setFocusable(true);
		requestFocus();

		setViewPortPosition(0, 0);
		invalidate(); // Force repaint of the screen
	}

	public GlobalGameData getGlobalGameData() {
		return globalGameData;
	}

	/**
	 * Load the sprites from their files into variables (planets, ships, etc.)
	 */
	private void loadBitmaps() {
		up = BitmapFactory.decodeResource(getResources(), R.drawable.up);
		mo = BitmapFactory.decodeResource(getResources(), R.drawable.morphers);
		p1 = BitmapFactory.decodeResource(getResources(), R.drawable.system1);
		p2 = BitmapFactory.decodeResource(getResources(), R.drawable.system2);
		wh = BitmapFactory.decodeResource(getResources(), R.drawable.wormhole);
	}

	/**
	 * Moves the galaxy view to the specified coordinates.
	 *
	 * @param x X coordinate to set the top left of the screen to
	 * @param y Y coordinate to set the top left of the screen to
	 */
	public void setViewPortPosition(int x, int y) {

		if (x < 0) {
			x = 0;
		} else if (x > (GlobalGameData.galaxySizeX - NUM_SQUARES_IN_ROW)) {
			x = GlobalGameData.galaxySizeX - NUM_SQUARES_IN_ROW;
		}

		if (y < 0) {
			y = 0;

		} else if (y > (GlobalGameData.galaxySizeY - NUM_SQUARES_IN_COLUMN)) {
			y = GlobalGameData.galaxySizeY - NUM_SQUARES_IN_COLUMN;

		}

		viewPort.x = x;
		viewPort.y = y;
		invalidate(); // Forces the screen to repaint
	}

	public void drawSystem(Sector sector, Canvas canvas) {

		Player human = globalGameData.getHumanPlayer();

		if (sector.hasSystem() && (human.hasDiscovered(sector) || human.isVisible(sector))) {

			int x, y;

			int systemX = sector.getX();
			int systemY = sector.getY();

			if ((systemX >= viewPort.x)
					&& (systemX <= viewPort.x + NUM_SQUARES_IN_ROW)
					&& (systemY >= viewPort.y)) {

				x = (systemX - viewPort.x) * SQUARE_SIZE;
				y = (systemY - viewPort.y) * SQUARE_SIZE;
				r.left = x + (SQUARE_SIZE / 2);
				r.top = y + (SQUARE_SIZE / 2);
				r.right = x + (SQUARE_SIZE / 2) * 2;
				r.bottom = y + (SQUARE_SIZE / 2) * 2;

				canvas.drawBitmap(p2, null, r, paint);
			}
		}
	}

	public void drawSystemLabel(Canvas canvas, Sector sector) {

		if (sector.hasSystem()) {

			if ((sector.getX() >= viewPort.x)
					&& (sector.getX() <= viewPort.x + NUM_SQUARES_IN_ROW)
					&& (sector.getY() >= viewPort.y)
					&& globalGameData.getHumanPlayer().hasDiscovered(sector) == true
					) {

				int x = (sector.getX() - viewPort.x) * SQUARE_SIZE;
				int y = (sector.getY() - viewPort.y) * SQUARE_SIZE;

				int savedColor = paint.getColor();
				paint.setColor(Color.CYAN);
				paint.setTextSize(16 * getResources().getDisplayMetrics().density);
				canvas.drawText(sector.getSystem().getName(),
						(x) + PADDING,
						y + (SQUARE_SIZE / 2),
						paint);
				paint.setColor(savedColor);
			}
		}
	}

	public void drawGrid(Canvas canvas) {

		// Save colour before using it
		int savedColor = paint.getColor();
		paint.setColor(Color.WHITE);

		// Draw vertical lines
		for (int i = viewPort.x; i <= viewPort.x + getResources().getDisplayMetrics().widthPixels;
			 i = i + SQUARE_SIZE) {
			canvas.drawLine(i + SQUARE_SIZE, 0, i + SQUARE_SIZE,
					getResources().getDisplayMetrics().heightPixels,
					paint);
		}

		// Draw horizontal lines
		for (int k = viewPort.y; k < viewPort.y + getResources().getDisplayMetrics().heightPixels;
			 k = k + SQUARE_SIZE) {
			canvas.drawLine(0, k + SQUARE_SIZE,
					getResources().getDisplayMetrics().heightPixels,
					k + SQUARE_SIZE,
					paint);
		}

		// highlight current selection with a red square
		if (currentX >= 0 && currentY >= 0) {
			paint.setColor(Color.RED);
			paint.setStyle(Paint.Style.STROKE);

			int a = currentX * SQUARE_SIZE;
			int b = currentY * SQUARE_SIZE;
			paint.setStrokeWidth(6);
			canvas.drawRect(a, b, a + SQUARE_SIZE, b + SQUARE_SIZE, paint);
			paint.setStrokeWidth(3);
			paint.setStyle(Paint.Style.FILL);
			paint.setColor(Color.WHITE);
		}

		// Restore saved colour
		paint.setColor(savedColor);
	}

	/**
	 * Draws the text in the top left of the GalaxyView
	 * This text includes information such as:
	 * - credits
	 * - the number of turns
	 *
	 * @param canvas
	 */
	public void drawTopLeftInformation(Canvas canvas) {

		// Save colour before using it
		int savedColor = paint.getColor();
		paint.setColor(Color.WHITE);
		// Put text in top left corner indicating the current turn number
		canvas.drawText("Turn " + globalGameData.getTurn(),
				viewPort.x + PADDING, viewPort.y + PADDING * 3, paint);
		canvas.drawText("Credits " + globalGameData.getHumanPlayerCredits(),
				viewPort.x + PADDING, viewPort.y + PADDING * 3 * 2, paint);
		paint.setColor(savedColor);
	}

	/**
	 * Draws the bitmap of a ship at a particular sector
	 *
	 * @param canvas
	 * @param sector
	 */
	public void drawShip(Canvas canvas, Sector sector) {

		int x, y;
		int shipx = sector.getX();
		int shipy = sector.getY();

		if ((shipx >= viewPort.x)
				&& (shipx <= viewPort.x + NUM_SQUARES_IN_ROW)
				&& (shipy >= viewPort.y)
				&& globalGameData.getHumanPlayer().isVisible(sector) == true
				) {

			x = (shipx - viewPort.x) * SQUARE_SIZE;
			y = (shipy - viewPort.y) * SQUARE_SIZE;
			r.left = x;
			r.top = y;
			r.right = x + SQUARE_SIZE / 2;
			r.bottom = y + SQUARE_SIZE / 2;

			// TODO: Need to handle case where multiple units in the same system

			ArrayList<Spaceship> ships = sector.getUnits();
			for (Spaceship ship : ships) {

				switch (ship.getOwner().getIntelligence()) {

					case HUMAN:
						getGlobalGameData().getHumanPlayer().discover(sector);
						canvas.drawBitmap(up, null, r, paint);
						break;

					case ARTIFICIAL:
						canvas.drawBitmap(mo, null, r, paint);
						break;

					default:
						// do nothing
				}
			}
		}
	}

	public void drawShipLabel(Canvas canvas, Sector sector) {

		ArrayList<Spaceship> ships = sector.getUnits(globalGameData.getHumanPlayer());

		if (ships.size() > 0) {

			int savedColor = paint.getColor(); // Save paint colour

			paint.setColor(Color.WHITE);
			paint.setTextSize(16 * getResources().getDisplayMetrics().density);

			String text = "";

			if (ships.size() > 1) {
				text = ships.size() + " ships";

			} else {
				text = ships.get(0).getShipName();
			}

			int x = (sector.getX() - viewPort.x) * SQUARE_SIZE;
			int y = (sector.getY() - viewPort.y) * SQUARE_SIZE;

			canvas.drawText(text,
					(x) + PADDING,
					y + (PADDING * 3),
					paint);

			paint.setColor(savedColor); // Restore paint colour
		}

	}


	/**
	 * This is triggered every time the screen refreshes/repaints
	 *
	 * @param canvas
	 */
	@Override
	public void onDraw(Canvas canvas) {

		/* Paint all sectors */
		for (int i = viewPort.x; i < globalGameData.galaxySizeX; i++) {
			for (int j = viewPort.y; j < globalGameData.galaxySizeY; j++) {

				try {

					Sector sector = sectors[i][j];

					// Draw a purple square if unexplored
					if (globalGameData.getHumanPlayer().isVisible(sector)) {
						paint.setColor(Color.BLACK);
					} else {
						paint.setColor(THE_UNDISCOVERED_COUNTRY);
					}

					int x = (sector.getX() - viewPort.x) * SQUARE_SIZE;
					int y = (sector.getY() - viewPort.y) * SQUARE_SIZE;
					r.left = x;
					r.top = y;
					r.right = x + (SQUARE_SIZE);
					r.bottom = y + (SQUARE_SIZE);
					canvas.drawRect(r, paint);

					// Draw wormhole
					drawWormhole(sector, canvas);
					// Draw System bitmaps
					drawSystem(sector, canvas);
					// Draw Ship bitmaps
					drawShip(canvas, sector);

				} catch (ArrayIndexOutOfBoundsException e) {

				}
			}
		}

		drawGrid(canvas);

		/* Labels are drawn on top of sectors once all the sectors have been painted  */
		for (Object s : globalGameData.getHumanPlayer().getDiscoveredSectors()) {
			drawSystem((Sector) s, canvas);
			drawSystemLabel(canvas, (Sector) s);
			drawShipLabel(canvas, (Sector) s);
		}

		drawTopLeftInformation(canvas);
	}

	private void drawWormhole(Sector sector, Canvas canvas) {

		int x, y;

		if (sector instanceof Wormhole) {
			int systemX = sector.getX();
			int systemY = sector.getY();

			if ((systemX >= viewPort.x)
					&& (systemX <= viewPort.x + NUM_SQUARES_IN_ROW)
					&& (systemY >= viewPort.y)
					&& (globalGameData.getHumanPlayer().isVisible(sector) == true ||
					globalGameData.getHumanPlayer().hasDiscovered(sector) == true)
					) {

				x = (systemX - viewPort.x) * SQUARE_SIZE;
				y = (systemY - viewPort.y) * SQUARE_SIZE;
				r.left = x;
				r.top = y;
				r.right = x + (SQUARE_SIZE / 2) * 2;
				r.bottom = y + (SQUARE_SIZE / 2) * 2;
				canvas.drawBitmap(wh, null, r, paint);
			}
		}
	}

	private float startX, startY = 0; // Co-ordinates of where the finger starteed
	private float moveX, moveY = 0; // Co-ordinates of where the finger ended

	/**
	 * When the player touches the screen this is called
	 *
	 * @param view
	 * @param motion The type of motion the player made
	 * @return
	 */
	@Override
	public boolean onTouch(View view, MotionEvent motion) {

		gestureDetector.onTouchEvent(motion);
		pinchDetector.onTouchEvent(motion);

		// when finger lifts off screen
		if (motion.getAction() == 1) {

			int x = (int) (motion.getX() / SQUARE_SIZE);
			int y = (int) (motion.getY() / SQUARE_SIZE);
			currentX = x;
			currentY = y;

			if (SELECT_MODE == 1) {
				Point gameCoods = this.translateViewCoodsToGameCoods(x, y);
				UnitActions.setCourse(selectedShip, gameCoods.x, gameCoods.y);
				SELECT_MODE = 0;
				makeToast("Set a course for " + currentX + "," + currentY + "!");
				sound_setting_course.start();
			}

			// Debugging telemetry
			if (isShipSelected(currentX, currentY)) {
				Log.wtf("Ship selected", currentX + " " + currentY);
			}

		}

		invalidate(); // Force redraw of the screen

		return true;
	}

	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event) {

		return false;
	}

	void moveGridUp() {
		if (viewPort.y > 0) {
			viewPort.y--;
		}
	}

	void moveGridDown() {
		Log.e("Number of squares ", "" + NUM_SQUARES_IN_COLUMN);
		if (viewPort.y < (GlobalGameData.galaxySizeY - (NUM_SQUARES_IN_COLUMN + 1))) {
			viewPort.y++;
		}
	}

	void moveGridLeft() {
		if (viewPort.x > 0) {
			viewPort.x--;
		}
	}

	void moveGridRight() {
		if (viewPort.x < (GlobalGameData.galaxySizeX - NUM_SQUARES_IN_ROW)) {
			viewPort.x++;
		}
	}

	void zoomOut() {
		if (NUM_SQUARES_IN_ROW < sectors.length) {
			NUM_SQUARES_IN_ROW++;
			SQUARE_SIZE = getResources().getDisplayMetrics().widthPixels / NUM_SQUARES_IN_ROW;
		}
	}

	void zoomIn() {
		/* Limit the maximum zoom to 3 squares in width */
		if (NUM_SQUARES_IN_ROW > 3) {
			NUM_SQUARES_IN_ROW--;
			SQUARE_SIZE = getResources().getDisplayMetrics().widthPixels / NUM_SQUARES_IN_ROW;
		}
	}

	public void makeToast(String s) {
		Toast.makeText(ctxt, s, Toast.LENGTH_SHORT).show();
	}

	private Point translateViewCoodsToGameCoods(int viewx, int viewy) {

		Point p = new Point(viewx + viewPort.x, viewy + viewPort.y);

		if (p.x >= GlobalGameData.galaxySizeX) {
			p.x = GlobalGameData.galaxySizeX - 1;
		}

		if (p.y >= GlobalGameData.galaxySizeY) {
			p.y = GlobalGameData.galaxySizeY - 1;
		}

		if (p.x < 0) {
			p.x = 0;
		}

		if (p.y < 0) {
			p.y = 0;
		}

		return p;
	}

	boolean isShipSelected(int x, int y) {

		Point gameCoods = translateViewCoodsToGameCoods(x, y);

		if (sectors[gameCoods.x][gameCoods.y].hasShips()) {
			for (Spaceship u : sectors[gameCoods.x][gameCoods.y].getUnits()) {
				if (isHumanPlayer((u.getOwner()))) {
					return true;
				}
			}
		}
		return false;
	}

	Sector getSelectedSector(int x, int y) {
		Point gameCoods = this.translateViewCoodsToGameCoods(x, y);
		return sectors[gameCoods.x][gameCoods.y];
	}

	Spaceship getSelectedShip(int x, int y) {
		Point gameCoods = this.translateViewCoodsToGameCoods(x, y);

		return sectors[gameCoods.x][gameCoods.y].getUnits().get(0);
	}

	ArrayList<Spaceship> getSelectedShips(int x, int y) {
		Point gameCoods = this.translateViewCoodsToGameCoods(x, y);
		return sectors[gameCoods.x][gameCoods.y].getUnits();
	}

	ArrayList<Spaceship> getSelectedPlayerShips(int x, int y, Player player) {
		Point gameCoods = this.translateViewCoodsToGameCoods(x, y);
		ArrayList list = new ArrayList();
		for (Spaceship ship : sectors[gameCoods.x][gameCoods.y].getUnits()) {
			if (ship.getOwner() == player) {
				list.add(player);
			}
		}

		return list;
	}

	System getSelectedSystem(int x, int y) {
		Point gameCoods = this.translateViewCoodsToGameCoods(x, y);
		return sectors[gameCoods.x][gameCoods.y].getSystem();
	}

	boolean isSystemSelected(int x, int y) {
		Point gameCoods = this.translateViewCoodsToGameCoods(x, y);
		Log.e("Co-od translation", "view coods (" + x + "," + y + ")" + "-> (" +
				gameCoods.x + "," + gameCoods.y + ")");

		if (gameCoods.x > GlobalGameData.galaxySizeX || gameCoods.y > GlobalGameData.galaxySizeY) {
			return false;
		}

		if (sectors[gameCoods.x][gameCoods.y].hasSystem()) {
			if (globalGameData.getHumanPlayer().hasDiscovered(sectors[gameCoods.x][gameCoods.y])) {
				return true;
			}
		}

		return false;
	}


	boolean isSystemSelectedMine(Player player, int x, int y) {
		if (isSystemSelected(x, y) == false) {
			return false;
		}

		if (globalGameData.getSectors()[x][y].getSystem().getOwner() == player) {
			return true;
		}

		return false;
	}

	/**
	 * When the keyboard is pressed (if one is available)
	 *
	 * @param keyCode
	 * @param event
	 * @return
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		Log.wtf("Co-ordinates before pressing ", "" + viewPort.x + " " + viewPort.y);
		switch (keyCode) {

			case KeyEvent.KEYCODE_DPAD_UP:
				moveGridUp();
				break;

			case KeyEvent.KEYCODE_DPAD_DOWN:
				moveGridDown();
				break;

			case KeyEvent.KEYCODE_DPAD_LEFT:
				moveGridLeft();
				break;

			case KeyEvent.KEYCODE_DPAD_RIGHT:
				moveGridRight();
				break;

			default:
				return false;
		}

		invalidate(); // Repaint the screen
		return true;
	}


	private void setSelectedShipForOnClick() {
		selectedShip = (isHumanPlayer(
				getSelectedShip(currentX, currentY).getOwner()) ?
				getSelectedShip(currentX, currentY) :
				null);
	}

	/**
	 * Show error message in a dialog box to the player
	 *
	 * @param message
	 */
	public void showError(String message) {
		AlertDialog.Builder builder =
				new AlertDialog.Builder(ctxt).
						setTitle("Error").
						setMessage(message).
						setIcon(android.R.drawable.ic_dialog_info).
						setPositiveButton("OK", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
							}
						});
		builder.create().show();
	}

	/**
	 * Graphical menu for giving a ship an order
	 *
	 * @param ship The ship object to give an order to
	 */
	public void showShipMenu(final Spaceship ship) {

		ArrayList<CharSequence> validOrders = new ArrayList<CharSequence>();
		validOrders.add(SpacecraftOrder.MOVE.name());
		validOrders.add(SpacecraftOrder.ATTACK.name());

		Sector s = globalGameData.getSectors()[ship.getX()][ship.getY()];

		if (s.hasSystem()) {
			if (s.getSystem().hasOwner() == false) {
				if (ship instanceof ColonyShip) {
					validOrders.add(SpacecraftOrder.COLONISE.name());
				}
			}
		}

		if (s instanceof Wormhole) {
			validOrders.add(SpacecraftOrder.ENTER_WORMHOLE.name());
		}

		final CharSequence orders[] = validOrders.toArray(new CharSequence[validOrders.size()]);

		if (ship != null) {
			AlertDialog.Builder builder = new AlertDialog.Builder(ctxt);
			builder.setTitle(ship.getClass().getSimpleName());
			builder.setItems(orders, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					orderShip(ship, SpacecraftOrder.valueOf((String) orders[which]));
				}
			});

			AlertDialog shipDialog = builder.create();
			int WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT;
			shipDialog.getWindow().setLayout(100, WRAP_CONTENT);
			shipDialog.show();
			sound_yessir.start();
		}
	}

	/**
	 * Given an order to a ship
	 *
	 * @param ship  The ship object to give an order to
	 * @param order The order to give the ship
	 *              0 - MOVE
	 *              1 - ATTACK
	 *              2 - COLONISE
	 */
	public void orderShip(final Spaceship ship, SpacecraftOrder order) {
		switch (order) {

			case MOVE:
				selectedShip = ship; // Does this need to be changed to setSelectedShipForOnClick()?

				SELECT_MODE = (selectedShip != null) ? 1 : 0;
				CURRENTLY_ORDERING = true;
				break;

			case ATTACK:
				if (ship instanceof ColonyShip) {
					// do nothing
				} else {
					setSelectedShipForOnClick();
					UnitActions.attackSystem(selectedShip, globalGameData);
				}
				break;

			case COLONISE:
				setSelectedShipForOnClick();
				//UnitActions.coloniseSystem(selectedShip, globalGameData);
				if (selectedShip instanceof ColonyShip) {
					if (sectors[currentX][currentY].hasSystem()) {
						if (!sectors[currentX][currentY].getSystem().hasOwner()) {
							selectedShip.clearCourse();
							((ColonyShip) selectedShip).colonise();
							makeToast("Colonising...");
						} else {
							showError("This system is already colonised " +
									sectors[currentX][currentY].getSystem().getOwner());
						}
					} else {
						showError("This system cannot be colonised");
					}
				}
				break;
        
			case ENTER_WORMHOLE:

				setSelectedShipForOnClick();

				Sector selectedSector = getSelectedSector(currentX, currentY);

				if (selectedSector instanceof Wormhole) {
					selectedShip.enterWormhole();
					makeToast("Entering wormhole");
				} else {
					makeToast("No wormhole to enter");
				}
				break;

			default:
				Log.wtf("Clicked ", "" + order);
		}
	}

	public void showMultipleShipMenu(final Sector sector) {

		Player humanPlayer = globalGameData.getHumanPlayer();

		CharSequence items[] = new CharSequence[sector.getUnits(humanPlayer).size()];

		for (int i = 0; i < sector.getUnits(humanPlayer).size(); i++) {
			if (globalGameData.isHumanPlayer(sector.getUnits().get(i).getOwner())) {
				items[i] = sector.getUnits().get(i).getShipName();
			}
		}

		AlertDialog.Builder menu = new AlertDialog.Builder(ctxt);
		menu.setTitle("Select a ship");
		menu.setItems(items, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int option) {
				dialog.dismiss();
				showShipMenu(sector.getUnits().get(option));
			}
		});

		if (items.length > 0) {
			menu.show();
		}
	}

	public void showSystemMenu(final System system) {
		CharSequence items[] = new CharSequence[]{
				"Build CombatShip", "Build ColonyShip"};
		AlertDialog.Builder sysMenu = new AlertDialog.Builder(ctxt);
		String title = system.getName();

		if (system.hasOwner()) {
			title = title + " (" + system.getOwner().getIntelligence() + " intelligence)";
		} else {
			title = title + " (no player)";
		}

		sysMenu.setTitle(title);

		/* If the system is owned by the human player then show build options */
		if (isHumanPlayer(system.getOwner())) {

			sysMenu.setItems(items, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int option) {
					dialog.dismiss();
					switch (option) {
						case 0:
							// Build combatship
							CombatShip combat = new CombatShip(
									system.getOwner(),
									globalGameData.getSectors()[system.getX()][system.getY()],
									system.getOwner().getFaction(),
									"New CombatShip",
									2,
									4);
							system.addToQueue(combat);
							makeToast("Building combat ship");
							break;
						case 1:
							// Build Colonyship
							ColonyShip colony = new ColonyShip(
									system.getOwner(),
									globalGameData.getSectors()[system.getX()][system.getY()],
									system.getOwner().getFaction(),
									"New ColonyShip",
									0,
									4);
							system.addToQueue(colony);
							makeToast("Building colony ship");
							break;
					}
				}
			});

		} else {
			sysMenu.setMessage("No information to display");
		}
		sysMenu.show();
	}

	/**
	 * A popup menu that gives the player some options about the entity
	 * TODO: This should eventually be generic to handle units, planets, and other things.
	 */
	public void showMenu() {

		if (isShipSelected(currentX, currentY) &&
				isSystemSelected(currentX, currentY)) {

			CharSequence menuOptions[] = new CharSequence[]{
					"SYSTEM",
					"SHIPS"};

			AlertDialog.Builder topMenu = new AlertDialog.Builder(ctxt);
			topMenu.setTitle("Menu");
			topMenu.setItems(menuOptions, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int option) {

					switch (option) {
						// SYSTEM
						case 0:
							Log.wtf("Clicked", "Selected system menu!");
							showSystemMenu(getSelectedSystem(currentX, currentY));
							break;

						// SHIPS
						case 1:
							if (getSelectedPlayerShips(currentX, currentY,
									globalGameData.getHumanPlayer()).size() > 1) {
								showMultipleShipMenu(getSelectedSector(currentX, currentY));
							} else {
								showShipMenu(getSelectedShip(currentX, currentY));
							}
							break;
						default:
							Log.wtf("Clicked ", "" + option + " on global menu");
					}
				}
			}).show();
		} else if (isShipSelected(currentX, currentY)) {
			Log.wtf("GUI", "ship selected");
			if (getSelectedPlayerShips(currentX, currentY, globalGameData.getHumanPlayer()).size() > 1) {
				showMultipleShipMenu(getSelectedSector(currentX, currentY));
			} else {
				showShipMenu(getSelectedShip(currentX, currentY));
			}
		} else if (isSystemSelected(currentX, currentY)) {
			showSystemMenu(getSelectedSystem(currentX, currentY));
			Log.wtf("GUI", "system selected");
		}
	}

	class GestureListener extends SimpleOnGestureListener {

		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			int x = (int) (e.getX() / SQUARE_SIZE);
			int y = (int) (e.getY() / SQUARE_SIZE);
			currentX = x;
			currentY = y;

			/* Don't show menu if player is in the middle of ordering a unit somewhere */
			if (CURRENTLY_ORDERING == false) {
				showMenu();
			} else {
				CURRENTLY_ORDERING = false;
			}
			return true;
		}


		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
							   float velocityY) {

			final int SWIPE_MIN_DISTANCE = 10;
			final int SWIPE_MAX_OFF_PATH = 250;
			final int SWIPE_THRESHOLD_VELOCITY = 200;

			try {

				if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH) {

					// up - down swipe
					if (e1.getY() - e2.getY() > SWIPE_MIN_DISTANCE
							&& Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
						Log.i("Gesture", "Up");
						moveGridDown();
					} else if (e2.getY() - e1.getY() > SWIPE_MIN_DISTANCE
							&& Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
						Log.i("Gesture", "Down");
						moveGridUp();
					}

				}

				// right - left swipe
				else if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE
						&& Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
					Log.i("Gesture", "Left");
					moveGridRight();
				} else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
						&& Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
					Log.i("Gesture", "Right");
					moveGridLeft();
				}

			} catch (Exception e) {
				Log.e("ERR", e.getLocalizedMessage());
			}
			return false;
		}
	}

	class PinchListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			if (detector.getCurrentSpan() > detector.getPreviousSpan()) {
				zoomIn();

			} else {
				zoomOut();
			}
			return true;
		}
	}
}
