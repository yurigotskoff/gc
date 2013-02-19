package dadeindustries.game.gc;

import java.util.ArrayList;

import com.example.gc.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class GalaxyView extends View implements OnTouchListener {

	Context ctxt;

	private final static int NUM_SQUARES_IN_ROW = 5;
	private Paint paint = new Paint();
	private int displayWidth = 0;
	private int displayHeight = 0;
	private static int SQUARE_SIZE;
	
	private Point viewPosition = new Point(0,0);
	protected static final int MAP_RADIUS = 50;
	
	Bitmap up = null; // temp variable
	Bitmap mo = null; // temp variable

	ArrayList<Ship> ships = new ArrayList<Ship>();

	public GalaxyView(Context context) {
		super(context);
		ctxt = context;
		DisplayMetrics metrics = getResources().getDisplayMetrics();
		displayWidth = metrics.widthPixels;
		displayHeight = metrics.heightPixels;
		SQUARE_SIZE = displayWidth / NUM_SQUARES_IN_ROW;
		this.setBackgroundColor(Color.BLACK);
		paint.setColor(Color.WHITE);
		paint.setStrokeWidth(3);
		up = BitmapFactory.decodeResource(getResources(), R.drawable.up);
		mo = BitmapFactory.decodeResource(getResources(), R.drawable.morphers);
		loadTestShips();
		this.setOnTouchListener(this);

	}

	public void loadTestShips()
	{
		ships.add(new Ship(SQUARE_SIZE * 0, SQUARE_SIZE * 0,
				Ship.Faction.UNITED_PLANETS, "USS Douglas"));
		ships.add(new Ship(SQUARE_SIZE * 2, SQUARE_SIZE * 2,
				Ship.Faction.MORPHERS, "Kdfkljsdf"));
	}
	
	Rect r = new Rect();

	@Override
	public void onDraw(Canvas canvas) {

		System.out.println("Draw!");
		// vertical lines
		for (int i = 1; i <= displayWidth; i++) {
			canvas.drawLine(i * SQUARE_SIZE, 0, i * SQUARE_SIZE, displayHeight,
					paint);
		}

		// horizontal lines
		for (int k = 1; k < displayHeight; k++) {
			canvas.drawLine(0, k * SQUARE_SIZE, displayWidth, k * SQUARE_SIZE,
					paint);
		}

		// ships
		for (int i = 0; i < ships.size(); i++) {
			int x = ships.get(i).x;
			int y = ships.get(i).y;
			r.left = x;
			r.top = y;
			r.right = x + SQUARE_SIZE;
			r.bottom = y + SQUARE_SIZE;

			switch (ships.get(i).side) {

			case UNITED_PLANETS:
				canvas.drawBitmap(up, null, r, paint);
				break;

			case MORPHERS:
				canvas.drawBitmap(mo, null, r, paint);
				break;

			default:
				// do nothing

			}
			System.out.println("End switch!!");
		}

		// highlight current selection
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
		System.out.println("EndDraw!");
	}

	private int currentX = -1;
	private int currentY = -1;

	@Override
	public boolean onTouch(View view, MotionEvent motion) {

		// Use the type of motion event to potentially figure out
		// whether user wishes to scroll map

		if (motion.getActionMasked() == MotionEvent.ACTION_MOVE) {
			// Toast.makeText(ctxt, "Movement!", Toast.LENGTH_SHORT).show();
		}

		int x = (int) (motion.getX() / SQUARE_SIZE);
		int y = (int) (motion.getY() / SQUARE_SIZE);
		currentX = x;
		currentY = y;
		this.invalidate();
		return true;
	}
}