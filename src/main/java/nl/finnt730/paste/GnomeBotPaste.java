package nl.finnt730.paste;

public final class GnomeBotPaste extends MCLogs{

    @Override
    public String getResultURL(String content) {
    	String sup=super.getResultURL(content);
    	return sup.replace("mclo.gs/", "gnomebot.dev/paste/mclogs/");
    }
	
	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return "gnomebot";
	}
}
