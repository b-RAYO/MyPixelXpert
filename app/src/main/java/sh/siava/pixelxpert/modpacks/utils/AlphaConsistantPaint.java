package sh.siava.pixelxpert.modpacks.utils;

import android.graphics.Color;
import android.graphics.Paint;

/** When setting a paint's color, alpha gets reset... naturally. So this is kind of paint that remembers its alpha and keeps it intact
 */
public class AlphaConsistantPaint extends Paint {
	int mAlpha = 255;
	public AlphaConsistantPaint(int flag) {
		super(flag);
	}

	@Override
	public void setAlpha(int alpha)
	{
		mAlpha = alpha;

		super.setAlpha(combinedAlpha(alpha, getAlpha()));
	}

	@Override
	public void setColor(int color)
	{
		super.setColor(color);

		super.setAlpha(combinedAlpha(Color.alpha(color), mAlpha));
	}

	private int combinedAlpha(int alpha1, int alpha2)
	{
		return Math.round((alpha1 / 255f) * (alpha2 / 255f) * 255f);
	}
}
