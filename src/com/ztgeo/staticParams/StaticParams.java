package com.ztgeo.staticParams;
//本类处理静态变量
public class StaticParams {
	//oldDatabase params
	public static String username1;
	public static String password1;
	public static String url1; 
	
	//newDatabase params
	public static String username2;
	public static String password2;
	public static String url2; 
	
	//log path
	public static String logpath ;
	
	//xml read path
	public static String xmlPath;
	
	//xml is use Quartz
	public static String isUseQuartz;
	
	//xml read Quartz time
	public static String QuartzTime; 
	
	//rollbackType 
	public static String rollbackType;//true=自动回滚 false=手动回滚
	
	//提前日期
	public static int advanceTime;
	
	//下午中间间隔时间段
	public static int PmHourTime;
	
	//邮件数据字符串
	public static StringBuffer sbForEmail= new StringBuffer();
	
	//sqlserver库的地址
	public static String sqlServer;
}
