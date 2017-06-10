package com.ztgeo.utils;

public class FormateData {
	//精确秒 截取秒后的.0字符
	public static String subTime(String time) {
		if(time!=null&&!"".equals(time)){
			return time.substring(0, 19);
		}
		else{
			return "";
		}
	}
	
	public static String getLSFWBM(String BDCDYH){
		if(BDCDYH.length()>=24){
			return BDCDYH.substring(0,24);
		}else{
			return BDCDYH;
		}
		
	}
	
	
	public static String getBGLX(String str){
		str = str.trim();
		if("期权后办产权".equals(str)||"按揭后办产权_二手房".equals(str)||"按揭后办产权".equals(str)){
			return "转移含抵押";
		}else if("".equals(str)||str==null){
			return "";
		}else{
			return "权属变更";
		}
	}
}

