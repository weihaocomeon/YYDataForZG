package com.ztgeo.utils;


//字典转义	
public class StringToDic {
	//证件类型的转库
	public static String decodeZJLX(String zjlx){
	if("".equals(zjlx)||zjlx==null){
		return "";
	}else{	
		//去空格
		switch (zjlx.trim()) {
		case "组织机构代码":
			return "6";
		case "营业执照":
			return "7";
		case "护照":
			return "3";
		case "户口本":
			return "4";
		case "身份证":
		case "居民身份证":
			return "1";
		case "军官证":
		case "士官证":
		case "士兵证":
		case "警官证":
			return "5";
		case "其他":
		case "驾驶证":
			return "99";
		case "港澳台身份证":
		case "港澳台地区身份证明":
			return "2";
		case "统一社会信用代码证":
			return "8";
		default:
			return "99";
		}
	}
	}
	
	public static String decodeGYFS(String gyfs){
		switch (gyfs.trim()) {
		case "共同共有":
		case "共同所有":
		case "共同共有.":
		case "共占房产份额.":
			return "1";
		case "单独所有":
			return "0";
		case "按份共有":
			return "2";
		default://其他共有
			return "3";
		}
	}
}
