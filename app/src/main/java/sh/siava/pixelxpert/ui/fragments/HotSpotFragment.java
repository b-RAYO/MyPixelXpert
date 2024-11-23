package sh.siava.pixelxpert.ui.fragments;

import sh.siava.pixelxpert.R;
import sh.siava.pixelxpert.utils.ControlledPreferenceFragmentCompat;

public class HotSpotFragment extends ControlledPreferenceFragmentCompat {
	@Override
	public String getTitle() {
		return getString(R.string.hotspot_header);
	}

	@Override
	public int getLayoutResource() {
		return R.xml.hotspot_prefs;
	}
}