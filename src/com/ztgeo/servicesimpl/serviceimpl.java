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

import com.ztgeo.dao.Dao;
import com.ztgeo.entity.YYINFO;
import com.ztgeo.services.Services;
import com.ztgeo.staticParams.StaticParams;
public class serviceimpl implements Services {
	
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
	    List<YYINFO> cyyList =	getDataByYYbh(keyWord);
		//将结果进行保存
		insertSqlServer(cyyList);
	}

	private void insertSqlServer(List<YYINFO> cyyList) {
		Connection conn = Dao.getConnS(StaticParams.url2, StaticParams.username2,  StaticParams.password2);
		
		//设置手动提交
		try {
			conn.setAutoCommit(false);
			//遍历集合
		 	
			for (int i = 0; i < cyyList.size(); i++) {
				Object[] params = new Object[5];
				//数据赋值
				params[0] = cyyList.get(i).getQNO();//顺序号
				params[1] = params[0].toString().contains("XF")?"二手房交易":"登记受理";//是否是二手房业务 否则普通C
				params[2] = cyyList.get(i).getNAME();//姓名
				params[3] = cyyList.get(i).getIDENTITYCARDNUM();//身份证
				params[4] = cyyList.get(i).getCARDID();//联系方式
				//准备sql语句
				String sql ="insert into [dbo].[AUDIT_QUEUE_TEST] \n" +
						"(rowguid,taskname,qno,name,identitycardnum,cardid,appointfromtime)\n" +
						"VALUES\n" +
						"(NEWID(),?,?,?,?,?,dateadd(day,"+ StaticParams.advanceTime +",dateadd(Minute,"+(i+1)+",GETDATE())))";
				//插入表 
					Dao.doExecuteUpdate(sql, params);
					
				//释放资源 防止 溢出
					
					Dao.closeResource();
			}
			//均执行成功没有发生异常 说明 可以批量提交
			conn.commit();
			System.out.println("导入前置库成功!数据已提交!!共导入数据"+cyyList.size()+"条!");
		} catch (SQLException e) {
			//数据回滚
			try {
				conn.rollback();
			} catch (SQLException e1) {
				System.out.println("数据回滚异常!");
				e1.printStackTrace();
			}
			System.out.println("执行插入时发生异常 数据已经回滚!!");
			e.printStackTrace();
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
						"select yyxh, yyrxm, yyrdhhm, yyrzjhm\n" +
						"  from ww_zxyy t\n" + 
						" where (yybh like 'C"+yybh+"'\n" + 
						" or yybh like 'XF"+yybh+"')\n" + 
						"  and t.yyzt is null\n" + 
						" order by yyxh";
				System.out.println(sql);
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
			YYINFO yy = new YYINFO(setforC.getString("yyxh"), setforC.getString("yyrxm"), setforC.getString("yyrdhhm"), setforC.getString("yyrzjhm"));
				//YYINFO yy = new YYINFO(setforC.getString("QNO"), setforC.getString("NAME"), setforC.getString("IDENTITYCARDNUM"), setforC.getString("CARDID"));
				list.add(yy);
			//System.out.println(yy.toString());
			}
		} catch (SQLException e) {
			System.out.println("处理结果集异常!");
			e.printStackTrace();
		}
		return list;
	}

}
