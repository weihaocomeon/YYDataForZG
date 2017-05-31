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
}

