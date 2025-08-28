package nl.finnt730.paste;

/**
 * A Wrapper around MCLogs which makes some changes and allows better zooming on mobile. Used by Modded MC Discord and Crash Assistant
 */
public final class GnomeBot extends MCLogs {

	@Override
	public String getResultURL(String content) {
		return super.getResultURL(content).replace("mclo.gs", "gnomebot.dev/paste/mclogs");
	}

}
