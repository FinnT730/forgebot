package nl.finnt730.paste;

import nl.finnt730.UserDB;

public sealed interface PasteSite permits MCLogs,PastesDev,SecureLogger,MMDPaste{

	public final static PasteSite MCLOGS = new MCLogs();
	public final static PasteSite GNOMEBOT = new GnomeBot();
	public final static PasteSite PASTESDEV = new PastesDev();//ByteBin appears to have been deprecated today in favour of pastes.dev
	public final static PasteSite SECURELOGGER = new SecureLogger();
	public final static PasteSite MMD = new MMDPaste();
	
	
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
	    switch (id) {
	        case "mclogs":
	            return MCLOGS;
	        case "gnomebot":
	            return GNOMEBOT;
	        case "pastesdev":
	            return PASTESDEV;
	        case "securelogger":
	            return SECURELOGGER;
	        case "mmd":
	            return MMD;
	        default:
	            return MCLOGS;
	    }
	}
	
	public boolean largeEnough(String content);
	
	public boolean supportsGZip();
	
	public String getResultURL(String content);
}
