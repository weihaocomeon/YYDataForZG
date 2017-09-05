package com.ztgeo.servicesimpl;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.log4j.Logger;

import com.ztgeo.dao.Dao;
import com.ztgeo.entity.YYINFO;
import com.ztgeo.main.Main;
import com.ztgeo.services.Services;
import com.ztgeo.staticParams.StaticParams;
import com.ztgeo.utils.FormateData;
public class serviceimpl implements Services {
	Logger log = Logger.getLogger(Services.class);
	@Override
	public void ToDo() {
		
		//获取当前日期加提前日期
		 Date date=new Date();//取时间
	      Calendar calendar = new GregorianCalendar();
	      calendar.setTime(date);
	      calendar.add(calendar.DATE,+StaticParams.advanceTime);//+1是因为日历获取的本来就比实际少一天
	      date=calendar.getTime(); 
	     SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
		String keyWord =formatter.format(date)+"%";
		//获取新城区普通类业务
		StaticParams.sbForEmail.append("当前日期:"+FormateData.getNowTime()+"程序启动中\n");
		StaticParams.sbForEmail.append("正在查询::"+keyWord+"日的预约数据\n");
	    List<YYINFO> cyyList =	getDataByYYbh(keyWord);
	    log.info("查询到日期为:"+keyWord+"的有效数据条数为"+cyyList.size()+"条!!");
	    System.out.println("查询到日期为:"+keyWord+"的有效数据条数为"+cyyList.size()+"条!!");
	    StaticParams.sbForEmail.append("查询到日期为:"+keyWord+"的有效数据条数为"+cyyList.size()+"条!!\n");
	    
		//将结果进行保存
		insertSqlServer(cyyList,keyWord);
	}

	private void insertSqlServer(List<YYINFO> cyyList,String keyWord) {
		Connection conn = Dao.getConnS(StaticParams.url2, StaticParams.username2,  StaticParams.password2);
		
		//设置手动提交
		try {
			conn.setAutoCommit(false);
			//遍历集合
			int j =0;
			for (int i = 0; i < cyyList.size(); i++) {
				Object[] params = new Object[5];
				//数据赋值
				params[0] = cyyList.get(i).getNI_NAME();//姓名
				params[1] = cyyList.get(i).getNI_SFZ();//身份证
				params[2] = cyyList.get(i).getNI_PHONE(); //电话号码
				params[3] = cyyList.get(i).getNI_NUMBER();//预约号
				params[4] = cyyList.get(i).getNI_Noon();//预约时间段
				//准备sql语句
				String sql = "";
				
				if("上午".equals(cyyList.get(i).getNI_Noon())){		
					sql="insert into [dbo].["+StaticParams.sqlServer+"] \n" +
						"(ni_name,ni_sfz,ni_phone,ni_number,ni_booktime,ni_noon,ni_type,ni_createtime)\n" +
						"VALUES\n" +
						"(?,?,?,?,dateadd(day,"+ StaticParams.advanceTime +",dateadd(Minute,"+(i+1)+",GETDATE())),?,1,GETDATE())";//DATEADD(HOUR, -15, DATEADD(Minute, -1, GETDATE()))  
				}else{
					j++;
					sql="insert into [dbo].["+StaticParams.sqlServer+"] \n" +
							"(ni_name,ni_sfz,ni_phone,ni_number,ni_booktime,ni_noon,ni_type,ni_createtime)\n" +
							"VALUES\n" +
							"(?,?,?,?,dateadd(day,"+ StaticParams.advanceTime +",DATEADD(HOUR, "+StaticParams.PmHourTime+",dateadd(Minute,"+(j)+",GETDATE()))),?,1,GETDATE())";
					
				}
					//插入表 
					Dao.doExecuteUpdate(sql, params);
					
				//释放资源 防止 溢出
					
					Dao.closeResource();
			}
			//均执行成功没有发生异常 说明 可以批量提交
			conn.commit();
			System.out.println("导入前置库成功!数据已提交!!共导入"+keyWord+"日数据共:"+cyyList.size()+"条!");
			log.info("导入前置库成功!数据已提交!!共导入"+keyWord+"日数据共:"+cyyList.size()+"条!");
			StaticParams.sbForEmail.append("导入前置库成功!数据已提交!!共导入"+keyWord+"日数据共:"+cyyList.size()+"条!\n");
		} catch (SQLException e) {
			//数据回滚
			try {
				conn.rollback();
			} catch (SQLException e1) {
				log.info("ERROR:数据回滚异常!");
				System.out.println("ERROR:数据回滚异常!");
				e1.printStackTrace();
			}
			log.info("执行插入时发生异常 数据已经回滚!!");
			System.out.println("执行插入时发生异常 数据已经回滚!!");
			StaticParams.sbForEmail.append("执行插入时发生异常 数据已经回滚!!\n");
			StaticParams.sbForEmail.append(e.getLocalizedMessage()+"\n");
		}finally{
			Dao.closeResource();
			Dao.closeConn();
		}
		
		
	}

	private List<YYINFO> getDataByYYbh(String yybh) {
		//连接老库
				Dao.getConnO(StaticParams.url1, StaticParams.username1, StaticParams.password1);
				//拿数据的sql
				String sql = 					
						"select yyrxm, yyrzjhm, yyrdhhm, yyxh,yysjd\n" +
						"  from ww_zxyy t\n" + 
						" where (yybh like 'E"+yybh+"'\n" + 
						" or yybh like 'F"+yybh+"')\n" + 
						"  and t.yyzt is null\n" + 
						" order by yysjd,yyxh";
				System.out.println("执行查询的sql语句为:"+sql);
				log.info("执行查询的sql语句为:"+sql);
				StaticParams.sbForEmail.append("执行查询的sql语句为:"+sql+"\n");
				//执行
				ResultSet set = Dao.getData(sql);
				//将结果进行封装
			 	List<YYINFO> list =  getListBySet(set);
			 	Dao.closeResource();
			 	Dao.closeConn();
		return list;
	}

	private List<YYINFO> getListBySet(ResultSet setforC) {
		List<YYINFO> list = new ArrayList<>();
		try {
			while(setforC.next()){
			YYINFO yy = new YYINFO(setforC.getString("yyxh"), setforC.getString("yyrxm"), setforC.getString("yyrzjhm"), setforC.getString("yyrdhhm"),setforC.getString("yysjd"));
				list.add(yy);
			}
		} catch (SQLException e) {
			System.out.println("处理结果集异常!");
			log.error("处理结果集异常!");
			StaticParams.sbForEmail.append("处理结果集异常!\n");
			StaticParams.sbForEmail.append(e.getLocalizedMessage()+"\n");
		}
		return list;
	}

}
