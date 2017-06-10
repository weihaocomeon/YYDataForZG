package com.ztgeo.services.handle;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


import org.apache.log4j.Logger;
import org.omg.CORBA.OBJ_ADAPTER;

import com.ztgeo.dao.DoDatabase;
import com.ztgeo.entity.ZS;
import com.ztgeo.utils.FormateData;
import com.ztgeo.utils.StringToDic;

public class HandleZS {
	static Logger log = Logger.getLogger(HandleZS.class);
	private StringBuffer rollBacksql = new StringBuffer();//回滚语句
	private boolean ZLZT;//增量状态 用来判定是否更新状态位
	private List<ZS> ZSList;
	public void handleZS() {
		//连接老库
		DoDatabase.getConnOld();
		//查询增量数据 SELECT 增量 qlr
		String sqlSZLZS = "select * from FC_ZSXX t where t.zlzt = 1";
		//set集合进行保存到list中
		ResultSet setSZLFJ = DoDatabase.getData(sqlSZLZS);
		zsSaveAsList(setSZLFJ);
		
		//关闭set 和prep
		DoDatabase.closeResource();
		DoDatabase.closeConn();
		//分级处理 
				toHandleZS();
	}

	private void toHandleZS() {
		for (ZS zs : ZSList) {
			rollBacksql.setLength(0);//回滚语句的清空
			ZLZT=true;
			log.info("");
			log.info("");
			log.info("※※※※FC_ZSXX 受理编号:"+zs.getSlbh()+"※※※※");
			System.out.println("\n\n※※※※※※※※※※※※FC_ZSXX 受理编号:"+zs.getSlbh()+"※※※※※※※※※※※※\n");
			
			
			
			//1.导入djb
			//逻辑判断是否可以导入 
			isCanInsDj_djb(zs);
			
			
			//如果存在tstybm则进行保存到dj_tsgl
			if(ZLZT){
				log.info("※DJ_DJB导入成功,正在准备其他表的导入工作※");
				System.out.println("※INFO:DJ_DJB导入成功,正在准备其他表的导入工作※");
				//查看是否有幢信息 不存在 则导入幢信息 使用gwbh去查询代表已落宗的幢信息
				/*log.info("※正在检测幢信息是否存在※");
				if(PublicDo.isHaveData("FC_Z_QSDC", "FWBH", zs.getFwbh())==0){
					
					//2.导入fc_Z_qsdc
					log.info("※幢信息不存在,正在准备幢信息的导入※");
					toInsertFC_Z_QSDC(zs);
				}else{
					log.info("※检测到幢信息存在,无需进行幢信息的导入※");
				}*/
				
				
				log.info("※正在查询是否有户信息存在※");
				System.out.println("※INFO:正在查询是否有户信息存在※");
				if(PublicDo.isHaveData("FC_H_QSDC", "TSTYBM", zs.getHouseinfoId())==0){
					log.error("※未查询到户信息，已停止了对其他表的导入※");
					System.out.println("※ERROR:未查询到户信息，已停止了对其他表的导入※");
					PublicDo.writeLogT("DJ_DJB",zs.getSlbh(),"DJ_DJB","未查询到户信息，已停止了对其他表的导入");
					ZLZT=false;
					//toInsFC_H_QSDC(zs);
				}else{
					//4.导入DJ_TSGL
					log.info("※正在准备DJ_TSGL的导入工作※");
					System.out.println("※INFO:正在准备DJ_TSGL的导入工作※");
					toInsertDJ_TSGL(zs);
					
					//5.导入DJ_SJD
					if(ZLZT){
					log.info("※正在准备DJ_SJD的导入工作※");
					System.out.println("※INFO:正在准备DJ_SJD的导入工作※");
					toInsertDJ_SJD(zs);
					}
					
					//6.导入ql_tdxg
					if(ZLZT){
					log.info("※正在准备QL_TDXG的导入工作※");
					System.out.println("※INFO:正在准备QL_TDXG的导入工作※");
					toInsertQL_TDXG(zs);
					}
					
					//7.导入ql_fwxg
					if(ZLZT){
					log.info("※正在准备QL_FWXG的导入工作※");
					System.out.println("※INFO:正在准备QL_FWXG的导入工作※");
					toInsertQL_FWXG(zs);
					}
				}
				
				
			}
			//有数据失败 给出回滚语句并把增量状态进行改变
			PublicDo.changeState("fc_zsxx", "slbh", zs.getSlbh(), ZLZT);
			log.error("※数据回滚语句:"+rollBacksql.toString()+"※");
			System.out.println("※INFO:数据回滚语句:\n"+rollBacksql.toString()+"※");
			//根据回滚语句和回滚方式进行自动回滚
			PublicDo.rollbackNewD(rollBacksql.toString(),ZLZT);
			//数据状态
			log.info("※该条数据导入结果:※"+ (ZLZT==true?"成功!!!":"失败!!!"));
			System.out.println("※INFO:该条数据导入结果:※"+ (ZLZT==true?"成功!!!":"失败!!!"));
		}
	}
	

	private void isCanInsDj_djb(ZS zs) {
		
		if(zs.getHouseinfoId()==null||"".equals(zs.getHouseinfoId())){
			log.error("※该信息对应的HOUSEINFO_ID为空值，数据不规范，无法关联房子信息，导致导入失败...※");
			System.out.println("※ERROR:该信息对应的HOUSEINFO_ID为空值，数据不规范，无法关联房子信息，导致导入失败...※");
			ZLZT =false;
			PublicDo.writeLogT("FC_ZSXX",zs.getSlbh(),"DJ_DJB","该信息对应的HOUSEINFO_ID为空值，数据不规范，无法关联房子信息，导致导入失败...");
		}else{
			//查询是否有历史数据
			String sql = "select slbh from dj_djb d where slbh in (select slbh from dj_tsgl where tstybm='"+zs.getHouseinfoId()+"') and (d.lifecycle<>1 or d.lifecycle is null)";
			//执行获得slbhs
			List<String> SLBHS = PublicDo.getSLBHS(sql);
			if(SLBHS.size()==0){
				log.info("※DJ_DJB中未发现现实状态的权属信息，准备导入DJ_DJB※");
				System.out.println("※INFO:DJ_DJB中未发现现实状态的权属信息，准备导入DJ_DJB※");
				toInsertDJ_DJB(zs);
			};//没有有效信息 可以直接导入
			
			if(SLBHS.size()==1){//有一条现实状态的权属信息，需要被置为历史 tsgl中也要被置为历史
				log.info("※DJ_DJB中发现有一条现实权属信息，准备将该条权属信息置为历史※");
				System.out.println("※INFO:DJ_DJB中发现有一条现实权属信息，准备将该条权属信息置为历史※");
				//需要判断是否是多户办理业务
				isManyHinfo(SLBHS.get(0),zs.getHouseinfoId());
				if(ZLZT){
					toInsertDJ_DJB(zs); 
					//插入dj_xgdjgl
					if(ZLZT){
					toInsertDJ_XGDJGL(SLBHS.get(0),zs);
					}
				}
			}
			if(SLBHS.size()>1){//出现这种情况 xgdjgl无法进行导入
				log.error("※警告：DJ_DJB中发现有多条现实权属信息，需要人工干涉处理，DJB继续导入。。※");
				System.out.println("※WARRING：DJ_DJB中发现有多条现实权属信息，需要人工干涉处理，DJB继续导入。。但会忽略DJ_XGDJGL的导入※");
				toInsertDJ_DJB(zs);
			}
			
			
		}
		
			
	}

	private void toInsertDJ_XGDJGL(String FSLBH, ZS zs) {
		log.info("※准备导入DJ_XGDJGL※");
		System.out.println("※INFO:准备导入DJ_XGDJGL※");
		String BGBM = UUID.randomUUID().toString();//变更编码 随机生成
		String ZSLBH =PublicDo.getSLBHWithDJB(zs.getYwzh());
		String BGRQ = FormateData.subTime(zs.getDbrq());//变更日期--登簿日期
		String BGLX = FormateData.getBGLX(zs.getDjlx());//变更类型--收件-登记类型并转译
		//相关证号是上一首业务的bdczh
		String XGZH = PublicDo.getDataNew("bdczh", "dj_djb", "slbh", FSLBH);//上一手业务的bdczh不动产证号
		String XGZLX = "房屋不动产证";
		Object insDJ_XGDJGL[] = new Object[7];
		insDJ_XGDJGL[0] =BGBM;
		insDJ_XGDJGL[1] =ZSLBH;
		insDJ_XGDJGL[2] =FSLBH;
		insDJ_XGDJGL[3] =BGRQ;
		insDJ_XGDJGL[4] =BGLX;
		insDJ_XGDJGL[5] =XGZH;
		insDJ_XGDJGL[6] =XGZLX;
		
		String insDJ_XGDJGL_Sql = "insert into dj_xgdjgl(bgbm,zslbh,fslbh,bgrq,bglx,xgzh,xgzlx,transnum)values(?,?,?,to_date(?,'yyyy/mm/dd HH24:MI:SS'),?,?,?,57)";

		try {
			DoDatabase.getConnNew();
			DoDatabase.doExecuteUpdate(insDJ_XGDJGL_Sql, insDJ_XGDJGL);
			log.info("※DJ_XGDJGL信息导入成功生成的BGBM(主键)为:"+BGBM+"※");
			System.out.println("※DJ_XGDJGL信息导入成功生成的BGBM(主键)为:"+BGBM+"※");
			rollBacksql.append("delete from dj_xgdjgl where bgbm ='"+BGBM+"';\n");
		} catch (SQLException e) {
			ZLZT=false;
			log.error("※DJ_XGDJGL信息导入时发生错误"+e.getLocalizedMessage()+"导致导入失败※");
			System.out.println("※ERROR:DJ_XGDJGL信息导入时发生错误"+e.getLocalizedMessage()+"导致导入失败※");
			PublicDo.writeLogT("FC_ZSXX",zs.getSlbh(),"DJ_XGDJGL",e.getLocalizedMessage());
		}finally {
			DoDatabase.closeResource();
			DoDatabase.closeConn();
		}
		
	}

	private void isManyHinfo(String slbh,String tstybm) {
		log.info("※正在查询是否是多户权属※");
		System.out.println("※INFO:正在查询是否是多户权属※");
		DoDatabase.getConnNew();
		int count =PublicDo.isHaveDataByL("dj_tsgl", "slbh",slbh);
		if(count>1){//是多户
			log.info("※查询到是多户权属※");
			System.out.println("※INFO:查询到是多户权属※");
			toSetLifeCycle_tsgl(slbh,tstybm);//这边有一个遗留问题 如果是地下室 如何处理
		}else{//单户 可以将dj_djb置为历史
			log.info("※查询到是单户权属※");
			System.out.println("※INFO:查询到是单户权属※");
			//将djb直接置为历史
			toSetLifeCycle(slbh);
			toSetLifeCycle_tsgl(slbh,tstybm);
		}
	}

	private void toSetLifeCycle(String SLBH) {
		DoDatabase.getConnNew();
		String sql = "update dj_djb set lifecycle=1 where slbh='"+SLBH+"'";
		try {
			DoDatabase.doExecuteUpdate(sql, new Object[0]);
			log.info("※DJ_DJB信息更新历史状态成功，SLBH(主键)为:"+SLBH+"※");
			System.out.println("※INFO:DJ_DJB信息更新历史状态成功，SLBH(主键)为:"+SLBH+"※");
			rollBacksql.append("update dj_djb set lifecycle=0 where slbh='"+SLBH+"';\n");
		} catch (SQLException e) {
			ZLZT=false;
			//可能出现的异常 fj插不进去超过值范围 gyfe(共有份额 插不进去 值太大)//或者主键冲突
			log.error("※DJ_DJB现实状态置为历史是发生错误"+e.getLocalizedMessage()+"导致更新历史状态失败※");
			System.out.println("※ERROR:DJ_DJB现实状态置为历史是发生错误"+e.getLocalizedMessage()+"导致更新历史状态失败※");
			PublicDo.writeLogT("DJ_DJB",SLBH,"DJ_DJB(SLBH)","DJ_DJB现实状态置为历史是发生错误"+e.getLocalizedMessage());
		}finally {
			DoDatabase.closeResource();
			DoDatabase.closeConn();
		}
		
		
	}
	
	
	private void toSetLifeCycle_tsgl(String SLBH,String TSTYBM){
		DoDatabase.getConnNew();
		String sql = "update dj_tsgl set lifecycle=1 where slbh='"+SLBH+"' and tstybm = '"+TSTYBM+"'";
		try {
			DoDatabase.doExecuteUpdate(sql, new Object[0]);
			log.info("※DJ_TSGL信息更新历史状态成功，SLBH为:"+SLBH+",TSTYBM为:"+TSTYBM+"※");
			System.out.println("※INFO:DJ_TSGL信息更新历史状态成功，SLBH为:"+SLBH+",TSTYBM为:"+TSTYBM+"※");
			rollBacksql.append("update dj_tsgl set lifecycle=0 where slbh='"+SLBH+"' and tstybm = '"+TSTYBM+"';\n");
		} catch (SQLException e) {
			ZLZT=false;
			//可能出现的异常 fj插不进去超过值范围 gyfe(共有份额 插不进去 值太大)//或者主键冲突
			log.error("※DJ_TSGL现实状态置为历史时发生错误"+e.getLocalizedMessage()+"导致更新历史状态失败※");
			System.out.println("※ERROR:DJ_TSGL现实状态置为历史时发生错误"+e.getLocalizedMessage()+"导致更新历史状态失败※");
			PublicDo.writeLogT("DJ_TSGL","SLBH:"+SLBH+"TSTYBM:"+TSTYBM,"DJ_TSGL","DJ_DJB现实状态置为历史时发生错误"+e.getLocalizedMessage());
		}finally {
			DoDatabase.closeResource();
			DoDatabase.closeConn();
		}
	} 

	private void toInsertQL_FWXG(ZS zs) {
		String QLBH =UUID.randomUUID().toString();
		String SLBH = PublicDo.getSLBHWithDJB(zs.getYwzh());//受理编号 
		String QLLX = "4";//权利类型 4代表房屋所有权
		String QLXZ = zs.getFwxz();//权利性质--房屋性质(需要转译)
		String JZMJ = zs.getJzmj();//建筑面积--建筑面积
		String TNJZMJ = zs.getTnmj();//套内面积--套内面积
		String FTJZMJ = zs.getFtmj();//分摊面积--分摊面积
		String QDFS = zs.getQdfs();//取得方式--取得方式
		String QDJG = zs.getQdjg();//取得价格--取得价格
		String GHYT = zs.getGhyt();//规划用途(字典表)--规划用途(字符表)需要转译
		
		Object insQL_FWXG_Params[] = new Object[10];
		insQL_FWXG_Params[0]=QLBH;
		insQL_FWXG_Params[1]=SLBH;
		insQL_FWXG_Params[2]=QLLX;
		insQL_FWXG_Params[3]=QLXZ;
		insQL_FWXG_Params[4]=JZMJ;
		insQL_FWXG_Params[5]=TNJZMJ;
		insQL_FWXG_Params[6]=FTJZMJ;
		insQL_FWXG_Params[7]=QDFS;
		insQL_FWXG_Params[8]=QDJG;
		insQL_FWXG_Params[9]=GHYT;
		
		String insQL_FWXG_Sql = "insert into ql_fwxg (qlbh,slbh,qllx,qlxz,jzmj,tnjzmj,ftjzmj,qdfs,qdjg,ghytms,transnum) values(?,?,?,?,?,?,?,?,?,?,57)";
		try {
			DoDatabase.getConnNew();
			DoDatabase.doExecuteUpdate(insQL_FWXG_Sql, insQL_FWXG_Params);
			log.info("※QL_FWXG信息导入成功生成的QLBH(主键)为:"+QLBH+"※");
			System.out.println("※INFO:QL_FWXG信息导入成功生成的QLBH(主键)为:"+QLBH+"※");
			rollBacksql.append("delete from ql_fwxg where qlbh ='"+QLBH+"';\n");
		} catch (SQLException e) {
			ZLZT=false;
			//可能出现的异常 fj插不进去超过值范围 gyfe(共有份额 插不进去 值太大)//或者主键冲突
			log.error("※QL_FWXG信息导入时发生错误"+e.getLocalizedMessage()+"导致导入失败※");
			System.out.println("※ERROR:QL_FWXG信息导入时发生错误"+e.getLocalizedMessage()+"导致导入失败※");
			PublicDo.writeLogT("FC_ZSXX",zs.getSlbh(),"QL_FWXG",e.getLocalizedMessage());
		}finally {
			DoDatabase.closeResource();
			DoDatabase.closeConn();
		}
		
	}

	private void toInsertDJ_TSGL(ZS zs) {
		String GLBM = UUID.randomUUID().toString();//GLBM 
		String SLBH = PublicDo.getSLBHWithDJB(zs.getYwzh());//SLBH
		String BDCLX = "房屋";
		//获取lsztybm
		String TSTYBM = zs.getHouseinfoId();
		String BDCDYH = zs.getBdcdyh();//不动产单元号--不动产单元号
		String DJZL = "权属";//登记种类
		String CSSJ = FormateData.subTime(zs.getDbrq());//产生时间
		Object insDJ_TSGL_Params[] = new Object[7];
		insDJ_TSGL_Params[0] =GLBM;
		insDJ_TSGL_Params[1] =SLBH;
		insDJ_TSGL_Params[2] =BDCLX;
		insDJ_TSGL_Params[3] =TSTYBM;
		insDJ_TSGL_Params[4] =BDCDYH;
		insDJ_TSGL_Params[5] =DJZL;
		insDJ_TSGL_Params[6] =CSSJ;
		
		String insDJ_TSGL = "insert into dj_tsgl (glbm,slbh,bdclx,tstybm,bdcdyh,djzl,cssj,transnum) values(?,?,?,?,?,?,to_date(?,'yyyy/mm/dd HH24:MI:SS'),57)";
		try {
			DoDatabase.getConnNew();
			DoDatabase.doExecuteUpdate(insDJ_TSGL, insDJ_TSGL_Params);
			log.info("※DJ_TSGL信息导入成功生成的GLBM(主键)为:"+GLBM+"※");
			System.out.println("※INFO:DJ_TSGL信息导入成功生成的GLBM(主键)为:"+GLBM+"※");
			rollBacksql.append("delete from dj_tsgl where glbm ='"+GLBM+"';\n");
		} catch (SQLException e) {
			ZLZT=false;
			//可能出现的异常 fj插不进去超过值范围 gyfe(共有份额 插不进去 值太大)//或者主键冲突
			log.error("※DJ_TSGL信息导入时发生错误"+e.getLocalizedMessage()+"导致导入失败※");
			System.out.println("※ERROR:DJ_TSGL信息导入时发生错误"+e.getLocalizedMessage()+"导致导入失败※");
			PublicDo.writeLogT("FC_ZSXX",zs.getSlbh(),"DJ_TSGL",e.getLocalizedMessage());
		}finally {
			DoDatabase.closeResource();
			DoDatabase.closeConn();
		}
	}

	private void toInsertFC_Z_QSDC(ZS zs) {
		
		String TSTYBM = UUID.randomUUID().toString();//tstybm 随机生成
		String ZDTYBM = zs.getZdtybm();//宗地统一编码(19)--宗地统一编码(20)
		String FWBH = zs.getFwbh();//房屋编号(24)--房屋编号(36)
		String BDCDYH = zs.getBdcdyh();//不动产单元号(28)--不动产单元号(500)
		String FWZL = zs.getYhtdzl();//房屋坐落(100)--土地-用户土地坐落
		String GHYT = zs.getGhyt();//规划用途(30)--规划用途(30)
		String FWJG = zs.getFwjg();//房屋结构(20)--房屋结构(500)
		
		Object insFC_Z_QSDC_Params[] = new Object[7];
		insFC_Z_QSDC_Params[0]=TSTYBM;
		insFC_Z_QSDC_Params[1]=ZDTYBM;
		insFC_Z_QSDC_Params[2]=FWBH;
		insFC_Z_QSDC_Params[3]=BDCDYH;
		insFC_Z_QSDC_Params[4]=FWZL;
		insFC_Z_QSDC_Params[5]=GHYT;
		insFC_Z_QSDC_Params[6]=FWJG;
		
		String insFC_Z_QSDC_Sql = "insert into fc_z_qsdc (tstybm,zdtybm,fwbh,bdcdyh,fwzl,ghyt,fwjg) values(?,?,?,?,?,?,?)";
		try {
			DoDatabase.getConnNew();
			DoDatabase.doExecuteUpdate(insFC_Z_QSDC_Sql, insFC_Z_QSDC_Params);
			log.info("※幢信息导入成功,对应的TSTYBM(主键)值为:"+TSTYBM+"※");
			System.out.println("※幢信息导入成功,对应的TSTYBM(主键)值为:"+TSTYBM+"※");
			rollBacksql.append("delete from fc_z_qsdc where tstybm ='"+TSTYBM+"';\n");
		} catch (SQLException e) {
			ZLZT=false;
			//可能出现的异常 fj插不进去超过值范围 gyfe(共有份额 插不进去 值太大)//或者主键冲突
			log.error("※导入幢信息时出现错误:"+e.getLocalizedMessage()+"导致幢信息导入失败※");
			System.out.println("※导入幢信息时出现错误:"+e.getLocalizedMessage()+"导致幢信息导入失败※");
			PublicDo.writeLogT("FC_ZSXX",zs.getSlbh(),"FC_Z_QSDC",e.getLocalizedMessage());
		}finally {
			DoDatabase.closeResource();
			DoDatabase.closeConn();
		}
		
	}

	private void toInsFC_H_QSDC(ZS zs) {
		String TSTYBM = "FCCS"+zs.getHouseinfoId().substring(0,32);//图属统一编码--houseinfo-id
		String ZDTYBM = zs.getZdtybm();//宗地统一编码(19)--宗地统一编码
		String ZH = zs.getZh();//幢号(4)--幢号(20)
		String BDCDYH = zs.getBdcdyh();//不动产单元号--不动产单元号
		String LSFWBM = zs.getFwbh();//隶属房屋编号--房屋编号
		String QLLX = "4";//权利类型 4 代表房屋所有权
		String QLXZ = zs.getFwxz();//权利性质--房屋性质
		String GHYT = zs.getGhyt();//规划用途(30)--规划用途(500) 需转译
		String ZL = zs.getFwzl()==null||"".equals(zs.getFwzl())?zs.getYhtdzl():zs.getFwzl();//房屋坐落以房屋坐落为准
		String SJC = zs.getSzc();//实际层--所在层
		String MYC = zs.getMyc();//名义层--名义层
		String DYH = zs.getDyh();//单元号(3)--单元号(500)
		String FJH = zs.getFjh();//房间号(8)--房间号(500)
		String JZMJ = zs.getJzmj();//实测建筑面积--建筑面积
		String TNJZMJ = zs.getTnmj();//实测套内建筑面积--套内面积
		String FTJZMJ = zs.getFtmj();//实测分摊建筑面积--分摊面积
		String TTZZRQ =FormateData.subTime(zs.getZzrq());//土地终止日期--终止日期
		String TDYT = zs.getYhtdyt();//土地用途(字符串)--土地-用户土地用途(字典表)
		String FTTDMJ = zs.getFttdmj();//分摊土地面积--分摊土地面积
		//获取lsztybm
		String LSZTYBM = PublicDo.getZtstybm(zs.getFwbh());
		
		Object insFC_H_QSDC_Params[] = new Object[20];
		insFC_H_QSDC_Params[0]=TSTYBM;
		insFC_H_QSDC_Params[1]=ZDTYBM;
		insFC_H_QSDC_Params[2]=ZH;
		insFC_H_QSDC_Params[3]=BDCDYH;
		insFC_H_QSDC_Params[4]=LSFWBM;
		insFC_H_QSDC_Params[5]=QLLX;
		insFC_H_QSDC_Params[6]=QLXZ;
		insFC_H_QSDC_Params[7]=GHYT;
		insFC_H_QSDC_Params[8]=ZL;
		insFC_H_QSDC_Params[9]=SJC;
		insFC_H_QSDC_Params[10]=MYC;
		insFC_H_QSDC_Params[11]=DYH;
		insFC_H_QSDC_Params[12]=FJH;
		insFC_H_QSDC_Params[13]=JZMJ;
		insFC_H_QSDC_Params[14]=TNJZMJ;
		insFC_H_QSDC_Params[15]=FTJZMJ;
		insFC_H_QSDC_Params[16]=TTZZRQ;
		insFC_H_QSDC_Params[17]=TDYT;
		insFC_H_QSDC_Params[18]=FTTDMJ;
		insFC_H_QSDC_Params[19]=LSZTYBM;
		String insFC_H_QSDC_Sql = "insert into fc_h_qsdc (tstybm,zdtybm,zh,bdcdyh,lsfwbh,qllx,qlxz,ghyt,zl,sjc,myc,dyh,fjh,jzmj,tnjzmj,ftjzmj,tdzzrq,tdyt,fttdmj,lsztybm) values( "
				+ " ?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,to_date(?,'yyyy/mm/dd HH24:MI:SS'),?,?,?)";

		try {
			DoDatabase.getConnNew();
			DoDatabase.doExecuteUpdate(insFC_H_QSDC_Sql, insFC_H_QSDC_Params);
			log.info("※户信息导入成功,生成的TSTYBM(主键)值为:"+zs.getHouseinfoId()+"※");
			System.out.println("※户信息导入成功,生成的TSTYBM(主键)值为:"+zs.getHouseinfoId()+"※");
			rollBacksql.append("delete from fc_h_qsdc where tstybm='"+zs.getHouseinfoId()+"';\n");
		} catch (SQLException e) {
			ZLZT=false;
			//可能出现的异常 fj插不进去超过值范围 gyfe(共有份额 插不进去 值太大)//或者主键冲突
			log.error("※户信息导入时发生错误:"+e.getLocalizedMessage()+"导致户信息导入失败");
			System.out.println("※户信息导入时发生错误:"+e.getLocalizedMessage()+"导致户信息导入失败");
			PublicDo.writeLogT("FC_ZSXX",zs.getSlbh(),"FC_H_QSDC",e.getLocalizedMessage());
		}finally {
			DoDatabase.closeResource();
			DoDatabase.closeConn();
		}
		
	}

	private void toInsertDJ_DJB(ZS zs) {
		log.info("※正在进行YWZH非空的检查※");
		System.out.println("※INFO:正在进行YWZH非空的检查※");
	if(!"".equals(zs.getYwzh())&&zs.getYwzh()!=null){
		String SLBH = PublicDo.getSLBHWithDJB(zs.getYwzh());
		log.info("※生成的SLBH："+SLBH);
		System.out.println("※生成的SLBH："+SLBH);
		String SQRQ =FormateData.subTime(zs.getSjrq());//申请日期--收件日期
		String BDCZH = zs.getBdczh();//不动产证号--不动产证号
		String ZSH = zs.getZsh();//证书号--证书号
		String GYFS = StringToDic.decodeGYFS(zs.getGyqk());
		String GYFE = zs.getGyfe();//共有份额(256)--共有份额(5000)
		String DJRQ =FormateData.subTime(zs.getDbrq());//登记日期--登簿日期
		String DJR = zs.getDbr();//登簿人--登簿人
		String FZJG = zs.getFzjg();//发证机关--bdc-发证机关
		String FZRQ = FormateData.subTime(zs.getFzrq());//发证日期--bdc发证日期
		String ZSXLH = zs.getZsxlh();//证书序列号--bdc证书序列号
		String FJ = zs.getFj();//附记(1024)--附记(2000)
	/*	String XGZH = zs.getTdzh();//相关证号-- 土地证号
		String GLZH = zs.getQzbh();//关联证号-- 权证编号 房产证号
*/		String BDCDYH = zs.getBdcdyh();//不动产单元号--不动产单元号
		
		String szr = zs.getDzr();//缮证人 --打证人
		String qt = zs.getQt();//其他--其他
		
		String SSJC = zs.getSsjc();//省市简称--省市简称
		String JGJC = zs.getJgjc();//机构简称--机构简称
		String FZND = zs.getFznd();//发证年度--发证年度
		String ZSLX = "房屋不动产证";//证书类型
		String DYCS = zs.getDycs();//打印次数--打印次数
		//String XGZH ="";//待定////////////////////////////////////////////////////////////////
		
		Object[] insDJ_DJBParams = new String[20];
		
		insDJ_DJBParams[0]=SLBH;
		insDJ_DJBParams[1]=SQRQ;
		insDJ_DJBParams[2]=BDCZH;
		insDJ_DJBParams[3]=ZSH;
		insDJ_DJBParams[4]=GYFS;
		insDJ_DJBParams[5]=GYFE;
		insDJ_DJBParams[6]=DJRQ;
		insDJ_DJBParams[7]=DJR;
		insDJ_DJBParams[8]=FZJG;
		insDJ_DJBParams[9]=FZRQ;
		insDJ_DJBParams[10]=ZSXLH;
		insDJ_DJBParams[11]=FJ;
		insDJ_DJBParams[12]=BDCDYH;
		insDJ_DJBParams[13]=szr;
		insDJ_DJBParams[14]=qt;
		insDJ_DJBParams[15]=SSJC;
		insDJ_DJBParams[16]=JGJC;
		insDJ_DJBParams[17]=FZND;
		insDJ_DJBParams[18]=ZSLX;
		insDJ_DJBParams[19]=DYCS;
		log.info("※准备导入DJ_DJB※");
		System.out.println("※INFO:准备导入DJ_DJB※");
		
		String insDJ_DJB = "insert into dj_djb (slbh,sqrq,bdczh,zsh,gyfs,gyfe,djrq,dbr,fzjg,fzrq,zsxlh,fj,bdcdyh,szr,qt,ssjc,jgjc,fznd,zslx,dycs,transnum) values(?,to_date(?,'yyyy/mm/dd HH24:MI:SS'),?,?,?,?,to_date(?,'yyyy/mm/dd HH24:MI:SS'),?,?,to_date(?,'yyyy/mm/dd HH24:MI:SS'),?,?,?,?,?,?,?,?,?,?,57)";
		try {
			DoDatabase.getConnNew();
			DoDatabase.doExecuteUpdate(insDJ_DJB, insDJ_DJBParams);
			log.info("※导入DJ_DJB成功!生成的SLBH为:"+SLBH+"※");
			System.out.println("※INFO:导入DJ_DJB成功!生成的SLBH为:"+SLBH+"※");
			//回滚语句
			rollBacksql.append("delete from dj_djb where slbh ='"+SLBH+"';\n");
		} catch (SQLException e) {
			ZLZT=false;
			//可能出现的异常 fj插不进去超过值范围 gyfe(共有份额 插不进去 值太大)//或者主键冲突
			log.error("※导入DJ_DJB时出现错误:"+e.getLocalizedMessage()+"导致导入DJ_DJB失败※");
			System.out.println("※ERROR:导入DJ_DJB时出现错误:"+e.getLocalizedMessage()+"导致导入DJ_DJB失败※");
			PublicDo.writeLogT("FC_ZSXX",zs.getSlbh(),"DJ_DJB",e.getLocalizedMessage());
		}finally {
			DoDatabase.closeResource();
			DoDatabase.closeConn();
		}
		
	}else{
		log.error("※检测到对应的YWZH为空,无法生成DJB主键,DJB导入失败※");
		System.out.println("※ERROR:检测到对应的YWZH为空,无法生成DJB主键,DJB导入失败※");
		PublicDo.writeLogT("FC_ZSXX",zs.getSlbh(),"dj_djb","YWZH为空,无法生成为dj_djb的主键");
		ZLZT=false;
	}
	}

	private void toInsertQL_TDXG(ZS zs) {
		//按照属性库中无权利数据进行处理
		String QLBH = UUID.randomUUID().toString();//权利编号
		String SLBH = PublicDo.getSLBHWithDJB(zs.getYwzh());//slbh
		String  QLLX= zs.getQsxz();//权利类型
		String  QLXZ= zs.getTdsyqlx();//权利性质
		String TDSYQR = zs.getTdsyqr();//土地使用权人
		String DYTDMJ = zs.getDytdmj();//独有土地面积
		String FTTDMJ = zs.getFttdmj();//分摊土地面积
		String QSRQ = FormateData.subTime(zs.getQsrq());//启始时间--起始日期
		String ZZRQ = FormateData.subTime(zs.getZzrq());//终止时间--终止日期
		String TDYT = zs.getYhtdyt();//土地用途--用户土地用途
		Object[] insQL_TDXG_Params = new String[10];
		insQL_TDXG_Params[0]=QLBH;
		insQL_TDXG_Params[1]=SLBH;
		insQL_TDXG_Params[2]=QLLX;
		insQL_TDXG_Params[3]=QLXZ;
		insQL_TDXG_Params[4]=TDSYQR;
		insQL_TDXG_Params[5]=DYTDMJ;
		insQL_TDXG_Params[6]=FTTDMJ;
		insQL_TDXG_Params[7]=QSRQ;
		insQL_TDXG_Params[8]=ZZRQ;
		insQL_TDXG_Params[9]=TDYT;
		
		String insQL_TDXG = "insert into ql_tdxg (qlbh,slbh,qllx,qlxz,tdsyqr,dytdmj,fttdmj,qsrq,zzrq,tdyt,transnum) values(?,?,bdczk.f_ParseO2NZY(?,'徐州土地权利类型转译'),bdczk.f_ParseO2NZY(?,'徐州土地权利性质转译'),?,?,?,to_date(?,'yyyy/mm/dd HH24:MI:SS'),to_date(?,'yyyy/mm/dd HH24:MI:SS'),bdczk.f_ParseO2NZY(?,' 徐州土地用途转译'),57)";
		try {
			DoDatabase.getConnNew();
			DoDatabase.doExecuteUpdate(insQL_TDXG, insQL_TDXG_Params);
			log.info("※QL_TDXG的导入成功,保存的QLBH(主键)值为:"+QLBH+"※");
			System.out.println("※INFO:QL_TDXG的导入成功,保存的QLBH(主键)值为:"+QLBH+"※");
			rollBacksql.append("delete from ql_tdxg where qlbh ='"+QLBH+"';\n");
		} catch (SQLException e) {
			ZLZT=false;
			//sql有问题时进行捕捉并保存到数据库 
			//e.printStackTrace();
			log.info(e.getLocalizedMessage());
			log.error("※QL_TDXG导入时发生错误:"+e.getLocalizedMessage()+"导致导入QL_TDXG失败※");
			System.out.println("※ERROR:QL_TDXG导入时发生错误:"+e.getLocalizedMessage()+"导致导入QL_TDXG失败※");
			PublicDo.writeLogT("FC_ZSXX",zs.getSlbh(),"QL_TDXG",e.getLocalizedMessage());
		}finally {
			DoDatabase.closeResource();
			DoDatabase.closeConn();
		}
		
	}

	private void toInsertDJ_SJD(ZS zs) {
			log.info("※准备导入DJ_SJD※");
			System.out.println("※INFO:准备导入DJ_SJD※");
			Object[] insDJ_SJD_Params = new String[7];
			String SLBH = PublicDo.getSLBHWithDJB(zs.getYwzh());
			String DJXL = zs.getDjlx();//登记类型赋值给登记小类
			String TZRYDDH =zs.getDhhm();//通知人电话
			String TZRXM = zs.getLxr();//通知人姓名
			//坐落 需要确定土地是否为空不为空 已房屋坐落为准 为空 以土地坐落为准
			String ZL = zs.getFwzl()==null||"".equals(zs.getFwzl())?zs.getYhtdzl():zs.getFwzl();
			String SJR = zs.getSjr();//收件人
			//收件日期暂时没有空值 不做空值判断
			String SJSJ = FormateData.subTime(zs.getSjrq());//收件日期
			
			//拼接参数
			insDJ_SJD_Params[0] = SLBH;//受理编号
			insDJ_SJD_Params[1] = DJXL;//登记类型
			insDJ_SJD_Params[2] = TZRYDDH;
			insDJ_SJD_Params[3] = TZRXM;
			insDJ_SJD_Params[4] = ZL;
			insDJ_SJD_Params[5] = SJR;
			insDJ_SJD_Params[6] = SJSJ;
			
			String insDJ_SJD = "insert into dj_sjd (slbh,djxl,tzryddh,tzrxm,zl,sjr,sjsj,transnum) values(?,?,?,?,?,?,to_date(?,'yyyy/mm/dd HH24:MI:SS'),57)";
			//导入值注意 slbh为可能重复 要捕捉异常
			try {
				DoDatabase.getConnNew();
				DoDatabase.doExecuteUpdate(insDJ_SJD, insDJ_SJD_Params);
				log.info("导DJ_SJD成功,生成的SLBH(主键)为:"+SLBH);
				System.out.println("※INFO:导DJ_SJD成功,生成的SLBH(主键)为:"+SLBH);
				rollBacksql.append("delete from dj_sjd where slbh ='"+SLBH+"';\n");
			} catch (SQLException e) {
				ZLZT=false;
				//sql有问题时进行捕捉并保存到数据库 
				log.error("※导入DJ_SJD时发生错误"+e.getLocalizedMessage()+"导致导入失败※");
				System.out.println("※ERROR:导入DJ_SJD时发生错误"+e.getLocalizedMessage()+"导致导入失败※");
				PublicDo.writeLogT("FC_ZSXX",zs.getSlbh(),"dj_sjd",e.getLocalizedMessage());
			}finally {
				DoDatabase.closeResource();
				DoDatabase.closeConn();
			}
	}

	private void zsSaveAsList(ResultSet set) {
		ZSList = new ArrayList<>();
		try {
			while(set.next()){
				ZS zs = new ZS(
						set.getString("slbh"),
						set.getString("bdcdyh"),
						set.getString("fwzl"),
						set.getString("syqr"),
						set.getString("qlr_zh"),
						set.getString("ywr_zh"),
						set.getString("qlr"),
						set.getString("zjlx"),
						set.getString("zjh"),
						set.getString("gyqk"),
						set.getString("gyfe"),
						set.getString("qllx"),
						set.getString("fwxz"),
						set.getString("ghyt"),
						set.getString("fwyt"),
						set.getString("jzmj"),
						set.getString("tnmj"),
						set.getString("ftmj"),
						set.getString("qdfs"),
						set.getString("qdjg"),
						set.getString("fwjg"),
						set.getString("syqx"),
						set.getString("qsrq"),
						set.getString("zzrq"),
						set.getString("zcs"),
						set.getString("szc"),
						set.getString("myc"),
						set.getString("zh"),
						set.getString("dyh"),
						set.getString("fjh"),
						set.getString("jzrq"),
						set.getString("jgrq"),
						set.getString("fj"),
						set.getString("ywzh"),
						set.getString("qzbh"),
						set.getString("djlx"),
						set.getString("sqr"),
						set.getString("lxr"),
						set.getString("dhhm"),
						set.getString("lxrzjh"),
						set.getString("jjr"),
						set.getString("sjr"),
						set.getString("sjrq"),
						set.getString("bdczh"),
						set.getString("fzjg"),
						set.getString("fzrq"),
						set.getString("zsxlh"),
						set.getString("jgjc"),
						set.getString("fznd"),
						set.getString("zsh"),
						set.getString("ssjc"),
						set.getString("qsxz"),
						set.getString("tdsyqlx"),
						set.getString("yhtdyt"),
						set.getString("syqmj"),
						set.getString("yhtdzl"),
						set.getString("qlslqk"),
						set.getString("zdtybm"),
						set.getString("jzrjl"),
						set.getString("jzmd"),
						set.getString("jzxg"),
						set.getString("tdsyqr"),
						set.getString("tdsyfe"),
						set.getString("djh"),
						set.getString("hh"),
						set.getString("yzh"),
						set.getString("ysyqx"),
						set.getString("dwxz"),
						set.getString("tdslbh"),
						set.getString("dycs"),
						set.getString("fwbh"),
						set.getString("dbr"),
						set.getString("dbrq"),
						set.getString("fwdm"),
						set.getString("zbh"),
						set.getString("zdlx"),
						set.getString("fzmj"),
						set.getString("dytdmj"),
						set.getString("fttdmj"),
						set.getString("zddj"),
						set.getString("zdjg"),
						set.getString("houseinfo_id"),
						set.getString("prprtcert_id"),
						set.getString("datafrom"),
						set.getString("tdzh"),
						set.getString("zsczr"),
						set.getString("zh2"),
						set.getString("qzbhqc"),
						set.getString("dataflag"),
						set.getString("dyfs"),
						set.getString("zssl"),
						set.getString("dzr"),
						set.getString("dzrq"),
						set.getString("qt"),
						set.getString("fcfj"),
						set.getString("zhqymc"),
						set.getString("fcmjms"),
						set.getString("hxzt"),
						set.getString("lifecycle"),
						set.getString("sbzt"),
						set.getString("bdcdyh_old"),
						set.getString("zdtybm_old"),
						set.getString("djh_old"),
						set.getString("fwbh_old"),
						set.getString("bmtz201612")
						
						);
				ZSList.add(zs);
			}
			
		} catch (SQLException e) {
			log.error("※获取qlr增量数据处理set集合异常※");
			System.out.println("※ERROR:获取qlr增量数据处理set集合异常※");
			e.printStackTrace();
		}
	
			
	}
}
