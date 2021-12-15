package com.haxzz.tl.objects;

public class Colors {
	
	public static final String	RESET				= "\u001B[0m";

	public static final String	HIGH_INTENSITY		= "\u001B[1m";
	public static final String	LOW_INTENSITY		= "\u001B[2m";

	public static final String	ITALIC				= "\u001B[3m";
	public static final String	UNDERLINE			= "\u001B[4m";
	public static final String	BLINK				= "\u001B[5m";
	public static final String	RAPID_BLINK			= "\u001B[6m";
	public static final String	REVERSE_VIDEO		= "\u001B[7m";
	public static final String	INVISIBLE_TEXT		= "\u001B[8m";

	public static final String	BLACK				= LOW_INTENSITY + "\u001B[30m";
	public static final String	RED					= LOW_INTENSITY + "\u001B[31m";
	public static final String	GREEN				= LOW_INTENSITY + "\u001B[32m";
	public static final String	YELLOW				= LOW_INTENSITY + "\u001B[33m";
	public static final String	BLUE				= LOW_INTENSITY + "\u001B[34m";
	public static final String	MAGENTA				= LOW_INTENSITY + "\u001B[35m";
	public static final String	CYAN				= LOW_INTENSITY + "\u001B[36m";
	public static final String	GRAY				= LOW_INTENSITY + "\u001B[37m";
	
	public static final String	BRIGHT_GRAY			= HIGH_INTENSITY + "\u001B[30m";
	public static final String	BRIGHT_RED			= HIGH_INTENSITY + "\u001B[31m";
	public static final String	BRIGHT_GREEN		= HIGH_INTENSITY + "\u001B[32m";
	public static final String	BRIGHT_YELLOW		= HIGH_INTENSITY + "\u001B[33m";
	public static final String	BRIGHT_BLUE			= HIGH_INTENSITY + "\u001B[34m";
	public static final String	BRIGHT_MAGENTA		= HIGH_INTENSITY + "\u001B[35m";
	public static final String	BRIGHT_CYAN			= HIGH_INTENSITY + "\u001B[36m";
	public static final String	WHITE				= HIGH_INTENSITY + "\u001B[37m";

	public static final String	BACKGROUND_BLACK	= "\u001B[40m";
	public static final String	BACKGROUND_RED		= "\u001B[41m";
	public static final String	BACKGROUND_GREEN	= "\u001B[42m";
	public static final String	BACKGROUND_YELLOW	= "\u001B[43m";
	public static final String	BACKGROUND_BLUE		= "\u001B[44m";
	public static final String	BACKGROUND_MAGENTA	= "\u001B[45m";
	public static final String	BACKGROUND_CYAN		= "\u001B[46m";
	public static final String	BACKGROUND_WHITE	= "\u001B[47m";
	
	public static String parseColors(String input){
		return input.replaceAll("%rst_", RESET)
				.replaceAll("%hi_", HIGH_INTENSITY)
				.replaceAll("%li_", LOW_INTENSITY)
				.replaceAll("%it_", ITALIC)
				.replaceAll("%ul_", UNDERLINE)
				.replaceAll("%nk_", BLINK)
				.replaceAll("%rnk_", RAPID_BLINK)
				.replaceAll("%rv_", REVERSE_VIDEO)
				.replaceAll("%inv_", INVISIBLE_TEXT)
				.replaceAll("%bl_", BLACK)
				.replaceAll("%r_", RED)
				.replaceAll("%g_", GREEN)
				.replaceAll("%y_", YELLOW)
				.replaceAll("%b_", BLUE)
				.replaceAll("%m_", MAGENTA)
				.replaceAll("%c_", CYAN)
				.replaceAll("%gr_", GRAY)
				.replaceAll("%bgr_", BRIGHT_GRAY)
				.replaceAll("%br_", BRIGHT_RED)
				.replaceAll("%bg_", BRIGHT_GREEN)
				.replaceAll("%by_", BRIGHT_YELLOW)
				.replaceAll("%bb_", BRIGHT_BLUE)
				.replaceAll("%bm_", BRIGHT_MAGENTA)
				.replaceAll("%bc_", BRIGHT_CYAN)
				.replaceAll("%w_", WHITE)
				.replaceAll("%bgbl_", BACKGROUND_BLACK)
				.replaceAll("%bgr_", BACKGROUND_RED)
				.replaceAll("%bgg_", BACKGROUND_GREEN)
				.replaceAll("%bgy_", BACKGROUND_YELLOW)
				.replaceAll("%bgb_", BACKGROUND_BLUE)
				.replaceAll("%bgm_", BACKGROUND_MAGENTA)
				.replaceAll("%bgc_", BACKGROUND_CYAN)
				.replaceAll("%bgw_", BACKGROUND_WHITE)
				.concat(RESET);
	}
	
	public static String parseWebColors(String input){
		return input.replaceAll("%rst_", "")
				.replaceAll("%hi_", "")
				.replaceAll("%li_", "")
				.replaceAll("%it_", "</span><span style='font-style: italic;'>")
				.replaceAll("%ul_", "</span><span style='text-decoration: underline;'>")
				.replaceAll("%nk_", "")
				.replaceAll("%rnk_", "")
				.replaceAll("%rv_", "")
				.replaceAll("%inv_", "")
				.replaceAll("%bl_", "</span><span style='color: #000'>")
				.replaceAll("%r_", "</span><span style='color: #A00'>")
				.replaceAll("%g_", "</span><span style='color: #0A0'>")
				.replaceAll("%y_", "</span><span style='color: #AA0'>")
				.replaceAll("%b_", "</span><span style='color: #00A'>")
				.replaceAll("%m_", "</span><span style='color: #A0A'>")
				.replaceAll("%c_", "</span><span style='color: #0AA'>")
				.replaceAll("%gr_", "</span><span style='color: #AAA'>")
				.replaceAll("%bgr_", "</span><span style='color: #EEE'>")
				.replaceAll("%br_", "</span><span style='color: #F00'>")
				.replaceAll("%bg_", "</span><span style='color: #0F0'>")
				.replaceAll("%by_", "</span><span style='color: #FF0'>")
				.replaceAll("%bb_", "</span><span style='color: #00F'>")
				.replaceAll("%bm_", "</span><span style='color: #F0F'>")
				.replaceAll("%bc_", "</span><span style='color: #0FF'>")
				.replaceAll("%w_",  "</span><span style='color: #FFF'>")
				.replaceAll("%bgbl_", "</span><span style='background-color: #000'>")
				.replaceAll("%bgr_", "</span><span style='background-color: #F00'>")
				.replaceAll("%bgg_", "</span><span style='background-color: #0F0'>")
				.replaceAll("%bgy_", "</span><span style='background-color: #FF0'>")
				.replaceAll("%bgb_", "</span><span style='background-color: #00F'>")
				.replaceAll("%bgm_", "</span><span style='background-color: #F0F'>")
				.replaceAll("%bgc_", "</span><span style='background-color: #0FF'>")
				.replaceAll("%bgw_", "</span><span style='background-color: #FFF'>")
				.replaceAll("(\r\n|\n\r|\r|\n)", "<br>")
				.concat("</span>");
	}
	
	public static String stripColors(String input){
		return input.replaceAll("\u001B\\[[;\\d]*m", "");
	}
	
}
