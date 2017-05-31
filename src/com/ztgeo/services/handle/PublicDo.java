package com.ztgeo.services.handle;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.ztgeo.dao.DoDatabase;

public class PublicDo {
	static Logger log = Logger.getLogger(PublicDo.class);
	//获取业务宗号的方法
	public static  String  getYWZH(String slbh){
		String selFczh = "select zs.ywzh from fc_zsxx zs where zs.slbh = '"+slbh+"'";
		DoDatabase.getConnOld();
		//在zsxx中没有两个slbh的信息 因为在zsxx中slbh是主键  但有可能qlr查不到zsxx(可能性小)
		ResultSet set = DoDatabase.getData(selFczh);
		String YWZH = "";
		try {
			while(set.next()){
				YWZH=set.getString("YWZH");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}finally {
			DoDatabase.closeResource();
			DoDatabase.closeConn();
		}
		return YWZH;
	}
	
	//属性库中查看是否存在特定的信息 新库中是否有信息
	public static int isHaveData(String tableName,String columName,String value){
		String sql = "select count(1) from "+tableName+" where "+columName+" = '"+value+"'";
		DoDatabase.getConnNew();
		int count = DoDatabase.getCount(sql);
		DoDatabase.closeResource();
		DoDatabase.closeConn();
		return count;
	}
	
	//老库中是否有信息
	public static int isHaveDataOld(String tableName,String columName,String value){
		String sql = "select count(1) from "+tableName+" where "+columName+" = '"+value+"'";
		DoDatabase.getConnNew();
		int count = DoDatabase.getCount(sql);
		DoDatabase.closeResource();
		DoDatabase.closeConn();
		return count;
	}

	
	public static void writeLogT(String seriestname, String fieldId, String propertyname, String localizedMessage) {
		//关闭连接
		DoDatabase.closeResource();
		DoDatabase.closeConn();
		//重新获取连接
		DoDatabase.getConnNew();
		String sql = "insert into zlztinfo t (seriestname,fieldid,propertytname,errorinfo) values(?,?,?,?)";
		Object[] params = {seriestname,fieldId,propertyname,localizedMessage};
		try {
			DoDatabase.doExecuteUpdate(sql, params);
		} catch (SQLException e) {
			e.printStackTrace();
		}finally {
			DoDatabase.closeResource();
			DoDatabase.closeConn();
		}
		
	}
	
	//获取幢的tstybm
	public static String getZtstybm(String fwbh){
		//关闭连接
		String lsztybm="";
		DoDatabase.closeResource();
		DoDatabase.closeConn();
		DoDatabase.getConnNew();
		String sql = "select tstybm from fc_z_qsdc where fwbh = '"+fwbh+"'";
		ResultSet set = DoDatabase.getData(sql);
		try {
			while(set.next()){
				lsztybm= set.getString("TSTYBM");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return lsztybm;
	}
	
	//修改状态值
	public static void changeState(String tableName,String colName,String colValue,boolean state){
		Integer ZLZT = state==true?2:3;
		String sql = "update "+tableName+" set zlzt = "+ZLZT+" where "+colName+" = '"+colValue+"'";
		DoDatabase.getConnOld();
		try {
			DoDatabase.doExecuteUpdate(sql, new Object[0]);
			log.info("增量状态修改成功！状态值为："+ZLZT);
			log.info("※状态回滚语句：※"+"update "+tableName+" set zlzt = 1 where "+colName+" = '"+colValue+"'");
			System.out.println("※INFO:增量状态修改成功！状态值为："+ZLZT);
			System.out.println("※INFO:状态回滚语句：※"+"update "+tableName+" set zlzt = 1 where "+colName+" = '"+colValue+"'");
		} catch (SQLException e) {
			log.error("增量状态修改失败，错误信息为："+e.getLocalizedMessage());
			System.out.println("ERROR:增量状态修改失败，错误信息为："+e.getLocalizedMessage());
			//写入状态表
			PublicDo.writeLogT(tableName, colValue, "修改增量状态", e.getLocalizedMessage());
		}finally {
			
			DoDatabase.closeResource();
			DoDatabase.closeConn();
		
		}
		
	}
	
	public static String getDataNew(String colName,String tableName,String selColName,String selColValue){
		String sql = "select "+colName+" from "+tableName+" where "+selColName+" = '"+selColValue+"'";
		DoDatabase.getConnNew();
		ResultSet set = DoDatabase.getData(sql);
		try {
			while(set.next()){
				colName = set.getString(colName);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}finally {
			DoDatabase.closeResource();
			DoDatabase.closeConn();
		}
		
		return colName;
	}
	
	public static String getDataOld(String colName,String tableName,String selColName,String selColValue){
		String sql = "select "+colName+" from "+tableName+" where "+selColName+" = '"+selColValue+"'";
		DoDatabase.getConnOld();
		ResultSet set = DoDatabase.getData(sql);
		try {
			while(set.next()){
				colName = set.getString(colName);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}finally {
			DoDatabase.closeResource();
			DoDatabase.closeConn();
		}
		
		return colName;
	}
	
	
	//fc_zsxx生成slbh的方法
	public static String getSLBHWithDJB(String fc_ywzh){
		String SLBH= "";
		if("新登记系统".equals(PublicDo.getDataOld("datafrom","fc_zsxx", "ywzh", fc_ywzh))){
			SLBH = "FC"+ fc_ywzh+"_1";
		}else{
			SLBH = "OFC"+ fc_ywzh;
		}
		return SLBH;
	}
	
	//fc_dyxx生成slbh的方法
	//fc_zsxx生成slbh的方法
	public static String getSLBHWithDY(String DATAFROM,String FCDYSLBH){
		String SLBH= "";
		if("新登记系统".equals(DATAFROM)){
			SLBH = "FC"+ FCDYSLBH+"_1";
		}else{
			SLBH = "OFC"+ FCDYSLBH;
		}
		return SLBH;
	}
	
	//获得有效的slbhs
	public static List<String> getSLBHS(String sql){
		List<String> SLBHS = new ArrayList<>();
		DoDatabase.getConnNew();
		ResultSet set = DoDatabase.getData(sql);
		try {
			while(set.next()){
				SLBHS.add(set.getString("slbh"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}finally {
			DoDatabase.closeResource();
			DoDatabase.closeConn();
		}
		return SLBHS;
	}
	
	//获得相关证号
	public static String getXGZH(String bdczh,String fczh,String tdzh){
		if(!"".equals(bdczh)&&bdczh!=null){
			return bdczh;
		}
		else{
			if((!"".equals(fczh)&&fczh!=null)&&(!"".equals(tdzh)&&tdzh!=null)){
				return fczh+"、"+tdzh;
			}else{
				if(!"".equals(fczh)&&fczh!=null){
					return fczh;
				}else{
					return tdzh;
				}
				
			}
		}
	}
}
