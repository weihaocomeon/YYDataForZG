package com.ztgeo.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.Test;

import com.ztgeo.staticParams.StaticParams;

public class FormateData {
	public  String getYYBH() {
		 Date date=new Date();//取时间
	      Calendar calendar = new GregorianCalendar();
	      calendar.setTime(date);
	      calendar.add(calendar.DATE,+StaticParams.advanceTime+1);//+1是因为日历获取的本来就比实际少一天
	      date=calendar.getTime(); 
	     SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
	     System.out.println(formatter.format(date));
	     return formatter.format(date);
	}
	
	
	//获取当前的系统时间
	public static String getNowTime(){
		Date date = new Date();
		SimpleDateFormat fm = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS");
		String dateS = fm.format(date);
		return dateS;
	}
	
	@Test
	public  void dod(){
		FormateData fm = new FormateData();
		System.out.println(fm.getYYBH());
	}
	
}

