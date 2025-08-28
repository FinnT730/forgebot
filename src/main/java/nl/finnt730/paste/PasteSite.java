package nl.finnt730.paste;

import nl.finnt730.UserDB;

public interface PasteSite {

	public static PasteSite MCLOGS = new MCLogs();
	public static PasteSite GNOMEBOT = new GnomeBot();
	public static PasteSite PASTESDEV = new PastesDev();//ByteBin appears to have been deprecated today in favour of pastes.dev
	public static PasteSite SECURELOGGER = new SecureLogger();
	public static PasteSite MMD = new MMDPaste();
	
	
	/**
	 * 
	 * @param userid
	 * @param content
	 * @return
	 */
	public static PasteSite get(String userid, String content) {
		PasteSite defa= getPure(UserDB.pasteSite(userid));
		if(defa.largeEnough(content)) {
			return defa;
		}else {
			return PASTESDEV;
		}
	}
	
	/**
	 * Gets pastesite from id
	 * @param id
	 * @return
	 */
	public static PasteSite getPure(String id) {
		if(id.equals("mclogs")) {
			return MCLOGS;
		}else if(id.equals("gnomebot")) {
			return GNOMEBOT;
		}else if(id.equals("pastesdev")) {
			return PASTESDEV;
		}else if(id.equals("securelogger")) {
			return SECURELOGGER;
		}else if(id.equals("mmd")) {
			return MMD;
		}
		return MCLOGS;
	}
	
	public boolean largeEnough(String content);
	
	public boolean supportsGZip();
	
	public String getResultURL(String content);
}
