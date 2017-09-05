package com.ztgeo.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.ztgeo.staticParams.StaticParams;
public class Dao {
	static Logger log = Logger.getLogger(Dao.class);
	private static Connection conn;
	private static ResultSet set;
	private static PreparedStatement prep;
	private static int resultCount;
	
	public static Connection getConnO(String url,String username, String password){
		//获得驱动
		try {
			Class.forName("oracle.jdbc.driver.OracleDriver");
			conn = DriverManager.getConnection(url, username, password);
		} catch (ClassNotFoundException e) {
			log.error("----未发现数据库的驱动类---");
			System.out.println("----未发现数据库驱动类");
			System.out.println(e.getLocalizedMessage());
			StaticParams.sbForEmail.append("----未发现数据库驱动类\n");
			StaticParams.sbForEmail.append(e.getLocalizedMessage()+"\n");
		} catch (SQLException e) {
			log.error("----数据库连接异常") ;
			System.out.println("----数据库连接异常") ;
			System.out.println(e.getLocalizedMessage());
			StaticParams.sbForEmail.append("----数据库连接异常\n") ;
			StaticParams.sbForEmail.append(e.getLocalizedMessage()+"\n");
		}
		return conn;
	}

	public static Connection getConnS(String url,String username, String password){
		//获得驱动
		try {
			Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
			conn = DriverManager.getConnection(url, username, password);
			System.out.println("数据库s:"+conn);
			log.info("数据库sqlserver:"+conn);
			StaticParams.sbForEmail.append("数据库sqlserver:"+conn+"\n");
		} catch (ClassNotFoundException e) {
			log.error("----未发现数据库的驱动类---");
			System.out.println("----未发现数据库驱动类");
			System.out.println(e.getLocalizedMessage());
		} catch (SQLException e) {
			log.error("----数据库连接异常") ;
			System.out.println("----数据库连接异常") ;
			System.out.println(e.getLocalizedMessage());
			log.error("----数据库连接异常") ;
			StaticParams.sbForEmail.append("----数据库连接异常\n") ;
			StaticParams.sbForEmail.append(e.getLocalizedMessage()+"\n");
		}
		return conn;
	}
	
	//无预设的查询
	public static ResultSet getData(String sql) {
		//获得连接
			try {
				prep = conn.prepareStatement(sql);
				set = prep.executeQuery();
			} catch (SQLException e) {
				System.out.println(e.getLocalizedMessage());
			}
			
		return set;
	}
		
	//获得条数
	public static int getCount(String sql){
		int total = 0;
		//获得连接
		try {
			prep = conn.prepareStatement(sql);
			set = prep.executeQuery();
			while(set.next()){
				total = set.getInt(1);
			}
		} catch (SQLException e) {
			System.out.println("----预编译sql语句有误");
			System.out.println(e.getLocalizedMessage());
			return -1;
		}finally {
		}
		return total;
	}
	
	public static ResultSet getDataByParams(String baseSql,String[] params){
		//遍历params进行预设prep
		try {
			prep = conn.prepareStatement(baseSql);
			//循环赋值预设
			for (int i = 0; i < params.length; i++) {
				prep.setObject(i+1, params[i]);
			}
			set = prep.executeQuery();
		} catch (SQLException e) {
			System.out.println("----预编译sql语句有误");
			System.out.println(e.getLocalizedMessage());
		}
		return set;
	};
		

	
	   //批量提交的执行增删改的方法
	   //执行增改查的工作 
		public static int doExecuteUpdate(String baseSql,Object[] params) throws SQLException{
			//使用本方法记得获得连接 设置连接 关闭连接
			
				prep = conn.prepareStatement(baseSql);
				//循环赋值预设
				for (int i = 0; i < params.length; i++) {
					prep.setObject(i+1, params[i]);
				}
				resultCount=prep.executeUpdate();
			return resultCount;
		} 
		
		
	public static void closeConn(){
		try {
			if(!conn.isClosed()){
				conn.close();
			}
		} catch (SQLException e) {
			System.out.println("-----关闭连接时遇到问题");
			System.out.println(e.getLocalizedMessage());
			log.error("-----关闭连接时遇到问题");
			log.info("-----关闭连接时遇到问题");
			StaticParams.sbForEmail.append("-----关闭连接时遇到问题\n");
			StaticParams.sbForEmail.append(e.getLocalizedMessage()+"\n");
		}
		
	} 
	
	//关闭资源文件
	public static void closeResource(){
		try {
			
			if(prep!=null){
				prep.close();
			}
			if(set!=null){
				set.close();
			}
		} catch (SQLException e) {
			System.out.println("----关闭资源文件遇到问题");
			System.out.println(e.getLocalizedMessage());
			log.error("----关闭资源文件遇到问题");
			log.error(e.getLocalizedMessage());
			StaticParams.sbForEmail.append("----关闭资源文件遇到问题\n");
			StaticParams.sbForEmail.append(e.getLocalizedMessage()+"\n");
		}
	}

}
