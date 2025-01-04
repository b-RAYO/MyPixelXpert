package sh.siava.pixelxpert.modpacks.utils;

import android.graphics.Color;
import android.graphics.Paint;

/** When setting a paint's color, alpha gets reset... naturally. So this is kind of paint that remembers its alpha and keeps it intact
 */
public class AlphaConsistantPaint extends Paint {
	int mAlpha = super.getAlpha();
	int mColor = super.getColor();
	public AlphaConsistantPaint(int flag) {
		super(flag);
	}

	@Override
	public void setAlpha(int alpha)
	{
		mAlpha = alpha;
		refreshColor();
	}

	@Override
	public void setColor(int color)
	{
		mColor = color;
		refreshColor();
	}

	private void refreshColor() {
		super.setColor(
				Color.argb(combinedAlpha(mAlpha, Color.alpha(mColor)),
						Color.red(mColor),
						Color.green(mColor),
						Color.blue(mColor)));
	}

	private int combinedAlpha(int alpha1, int alpha2)
	{
		return Math.round((alpha1 / 255f) * (alpha2 / 255f) * 255f);
	}
}
