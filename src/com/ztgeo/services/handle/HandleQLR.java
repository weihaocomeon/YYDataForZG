package com.ztgeo.services.handle;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.ztgeo.dao.DoDatabase;
import com.ztgeo.entity.QLR;
import com.ztgeo.utils.FormateData;
import com.ztgeo.utils.StringToDic;
import com.ztgeo.utils.WriteLog;
import com.ztgeo.utils.setIsRight;

public class HandleQLR {
	static Logger log = Logger.getLogger(HandleQLR.class);
	private StringBuffer rollBacksql = new StringBuffer();//回滚语句
	private boolean ZLZT;//增量状态 用来判定是否更新状态位
	private List<QLR> qlrList;
	public void handleQLR(){
		//连接老库
		DoDatabase.getConnOld();
		//查询增量数据 SELECT 增量 qlr
		String sqlSZLqlr = "select * from FC_QLR t where t.zlzt = 1";
		//set集合进行保存到list中
		ResultSet setSSZLqlr;
		setSSZLqlr = DoDatabase.getData(sqlSZLqlr);
		qlrSaveAsList(setSSZLqlr);
		//关闭set 和prep
		DoDatabase.closeResource();
		DoDatabase.closeConn();
//分级处理  
		toHandleQLR();
		
	}
	
	public void toHandleQLR(){		
		for (QLR qlr : qlrList) {
			rollBacksql.setLength(0);//回滚语句的清空
			String SLBH = "";
			ZLZT=true;
			log.info("");
			log.info("");
			log.info("※※※※FC_QLR ID:"+qlr.getID()+"※※※※");
			System.out.println("\n\n※※※※※※※※※※※※FC_QLR ID:"+qlr.getID()+"※※※※※※※※※※※※\n");
			//查询房证信息： 有则插入 无则不插入（状态值要改正）
			String YWZH = PublicDo.getYWZH(qlr.getSLBH());
			//查看是否是ywzh本身为空
			if("".equals(YWZH)||YWZH==null){
				//本身为空
				log.error("※检测到FC_QLR对应的YWZH为空，导致FC_QLR导入失败※");
				System.out.println("※ERROR:检测到FC_QLR对应的YWZH为空，导致FC_QLR导入失败※");
				
				ZLZT = false;
				//写入日志
				PublicDo.writeLogT("FC_QLR", qlr.getID(), "DJ_QLR", "FC_QLR中该条信息对应的YWZH为空，数据不规范！");
			}else{
				//生成slbh的方法 需要参数slbh
				SLBH =PublicDo.getSLBHWithDJB(YWZH);
				//查询对于属性库中是否有业务对应
				if(PublicDo.isHaveData("dj_djb", "slbh", SLBH)==0){
					//说明无权属信息 ，暂时不需要导入
					log.error("※检测到FC_QLR的SLBH在DJ_DJB中无对应，无权证信息，导致导入失败...※");
					System.out.println("※ERROR:检测到FC_QLR的SLBH在DJ_DJB中无对应，无权证信息，导致导入失败...※");
					ZLZT = false;
					PublicDo.writeLogT("FC_QLR", qlr.getID(), "DJ_QLR", "FC_QLR的SLBH在DJ_DJB中无对应，无权证信息，导致导入失败。。。");
				}else{
					//有权证信息。可以导入
					insertQLR(qlr);
					//判断现在的状态值 是否可以导入联系人
					if(ZLZT){
						log.info("※正在准备导入DJ_QLRGL※");
						System.out.println("※INFO:正在准备导入DJ_QLRGL※");
						insertQLRGL(qlr,SLBH);
					}else{
						log.error("※DJ_QLR导入失败，已终止了其他表的导入工作※");
						System.out.println("※ERROR:DJ_QLR导入失败，已终止了其他表的导入工作※");
					}
				}
			}
			//判断状态码 进行改变数据状态
			PublicDo.changeState("fc_qlr", "id", qlr.getID(), ZLZT);
			log.info("※INFO:回滚语句："+rollBacksql.toString());
			System.out.println("※INFO:回滚语句："+rollBacksql.toString());
			//根据回滚语句和回滚方式进行自动回滚
			PublicDo.rollbackNewD(rollBacksql.toString(),ZLZT);
			
			//数据状态
			log.info("※该条数据导入结果:※"+ (ZLZT==true?"成功!!!":"失败!!!"));
			System.out.println("※INFO:该条数据导入结果:※"+ (ZLZT==true?"成功!!!":"失败!!!"));
			
		}
	}
	
	private void insertQLRGL(QLR qlr,String SLBH) {
			String[] insQLRGLparams = new String[7];
			//拿出ywzh使用	
			//受理编号 赋值
			
			
			String GLBM = UUID.randomUUID().toString();
			//slbh
			String QLRID = qlr.getID();//权利人id-id
			String GYFS =StringToDic.decodeGYFS(qlr.getGYQK());//共有方式--共有情况
			String GYFE = qlr.getGYFE();//共有份额--共有份额
			String QLR = qlr.getQLR();//权利人名称--权利人
			
			String QLRLX = "权利人";
			//赋值
			insQLRGLparams[0] = GLBM;
			insQLRGLparams[1] = SLBH;
			insQLRGLparams[2] =QLRID;
			insQLRGLparams[3] = GYFS;
			insQLRGLparams[4] = GYFE;
			insQLRGLparams[5] = QLR;
			
			insQLRGLparams[6] = QLRLX;
			
			
			String insQLRGL = "insert into dj_qlrgl (glbm,slbh,qlrid,gyfs,gyfe,qlrmc,qlrlx,transnum) values(?,?,?,?,?,?,?,57)";
			
			//开启新库连接并插入
			DoDatabase.getConnNew();
			try {
				DoDatabase.doExecuteUpdate(insQLRGL, insQLRGLparams);
				log.info("※QL_QLRGL信息导入成功生成的GLBM(主键)为:"+GLBM+"※");
				System.out.println("※INFO:QL_QLRGL信息导入成功生成的GLBM(主键)为:"+GLBM+"※");
				rollBacksql.append("delete from dj_qlrgl where glbm ='"+GLBM+"';\n");
			} catch (SQLException e) {
				ZLZT = false;
				//sql有问题时进行捕捉并保存到数据库 
				log.info("※QL_QLRGL信息导入时发生错误:"+GLBM+e.getLocalizedMessage()+"导致导入失败※");
				System.out.println("※ERROR:QL_QLRGL信息导入时发生错误:"+GLBM+e.getLocalizedMessage()+"导致导入失败※");
				PublicDo.writeLogT("FC_QLR",GLBM,"DJ_QLRGL",e.getLocalizedMessage());
			}finally {
				DoDatabase.closeResource();
				DoDatabase.closeConn();
			}
	}

	public void insertQLR(QLR qlr){
		log.info("※准备导入DJ_QLR...※");
		System.out.println("※INFO:准备导入DJ_QLR...※");
		String insQLRsql =""; 
		String[] insQLRparams = new String[4];
		insQLRsql = "";
		//证件类型的转移的转译
		String zjlx = StringToDic.decodeZJLX(qlr.getZJLX());
		//参数赋值
		insQLRparams[0] = qlr.getID();//权利人id 使用id进行赋值 
		insQLRparams[1] = qlr.getQLR();//权利人名称--权利人
		insQLRparams[2] = zjlx;//证件类别--证件类型（需要转字典）
		insQLRparams[3] = qlr.getZJH();//证件号码--证件号
		DoDatabase.getConnNew();
		insQLRsql="insert into dj_qlr (qlrid,qlrmc,zjlb,zjhm,transnum) values(?,?,?,?,57)";
		try {
			DoDatabase.doExecuteUpdate(insQLRsql, insQLRparams);
			log.info("※QL_QLR信息导入成功生成的QLRID(主键)为:"+qlr.getID()+"※");
			System.out.println("※INFO:QL_QLR信息导入成功生成的QLRID(主键)为:"+qlr.getID()+"※");
			rollBacksql.append("delete from dj_qlr where qlrid ='"+qlr.getID()+"';\n");
			//后期更改 状态
		} catch (SQLException e) {
			ZLZT=false;
			//可能出现的异常 fj插不进去超过值范围 gyfe(共有份额 插不进去 值太大)//或者主键冲突
			log.error("※FC_QLR信息导入时发生错误"+e.getLocalizedMessage()+"导致导入失败,主键:"+qlr.getID()+"※");
			System.out.println("※ERROR:FC_QLR信息导入时发生错误"+e.getLocalizedMessage()+"导致导入失败,主键:"+qlr.getID()+"※");
			PublicDo.writeLogT("FC_QLR",qlr.getID(),"DJ_QLR",e.getLocalizedMessage());
		}finally{
			DoDatabase.closeResource();
			DoDatabase.closeConn();
		}
		
		
	}
	
	
	public void qlrSaveAsList(ResultSet set){
		qlrList = new ArrayList<>();
		try {
			while(set.next()){
				//取值
				QLR qlr = new QLR();
				qlr.setID(set.getString("ID"));
				qlr.setQLR(set.getString("QLR"));
				qlr.setZJLX(set.getString("ZJLX"));
				qlr.setZJH(set.getString("ZJH"));
				qlr.setGYQK(set.getString("GYQK"));
				qlr.setGYFE(set.getString("GYFE"));
				qlr.setDYCS(set.getString("DYCS"));
				qlr.setSLBH(set.getString("SLBH"));
				qlr.setFCSLBH(set.getString("FCSLBH"));
				qlr.setZLZT(set.getInt("ZLZT"));
				//加入list
				qlrList.add(qlr);
			}
			
		} catch (SQLException e) {
			System.out.println("--获取qlr增量数据处理set集合异常");
			e.printStackTrace();
		}
		
	}
}
