package com.ztgeo.services.handle;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.ztgeo.dao.DoDatabase;
import com.ztgeo.entity.DY;
import com.ztgeo.entity.QLR;
import com.ztgeo.utils.FormateData;
import com.ztgeo.utils.StringToDic;

public class HandleDY {
	static Logger log = Logger.getLogger(HandleDY.class);
	private StringBuffer rollBacksql = new StringBuffer();//回滚语句
	private boolean ZLZT;//增量状态 用来判定是否更新状态位
	private List<DY> DYList;
	public void handleDY() {
		DoDatabase.getConnOld();
		//查询增量数据 SELECT 增量 qlr
		String sqlSZLFJ = "select * from FC_DYXX t where t.zlzt = 1";
		//set集合进行保存到list中
		ResultSet setSZLFJ = DoDatabase.getData(sqlSZLFJ);
		dySaveAsList(setSZLFJ);
		//关闭set 和prep
				DoDatabase.closeResource();
				DoDatabase.closeConn();
		//分级处理 
		toHandleDY();
		
	}
	
	
	
	private void toHandleDY() {
		for (DY dy : DYList) {
			rollBacksql.setLength(0);//回滚语句的清空
			ZLZT=true;
			log.info("");
			log.info("");
			log.info("\n※※※※FC_DYXX BDCDYSLBH:"+dy.getFcdyslbh()+"※※※※");
			System.out.println("\n\n※※※※※※※※※※※※FC_DYXX BDCDYSLBH:"+dy.getFcdyslbh()+"※※※※※※※※※※※※\n");
			//查询FC_H_QSDC中是否有该房子 如果有才可以抵押 如果没有 不允许抵押
			//判断houseinfo_id是否为空 如果为空 不允许导入（绝对没有户信息）
			if(PublicDo.isHaveData("FC_H_QSDC", "TSTYBM", dy.getHouseinfo_id())>0){//这边有个诡异的问题，明明不一样却能匹配 后期撤查
				log.info("※查询到该查封在FC_H_QSDC中有户信息存在，准备导入DJ_DY※");
				System.out.println("※INFO:查询到该查封在FC_H_QSDC中有户信息存在，准备导入DJ_DY※");
				//查看是否可以抵押
				isCanInsert_DY(dy);
				//执行结束后再次对状态判断 如果状态正常 则继续导入
				if(ZLZT){
					//导入DJ_SJD
					insertDJ_SJD(dy);
					//导入DJ_TSGL
					if(ZLZT)
					insertDJ_TSGL(dy);
					//导入DJ_XGDJGL
					if(ZLZT)
					insertDJ_XGDJGL(dy);
					//导入DJ_QLR(两张表)
					if(ZLZT)
					insertQLR(dy);
					//导入DJ_QLRGL(两张表)
				}
			}else{
				//说明无权属信息 ，暂时不需要导入
				log.error("※未查询到该查封户在FC_H_QSDC中存在有效信息，可能的情况是：1.确实无权属关系，2.houseinfo_id为空，导致导入失败...※");
				System.out.println("※ERROR:未查询到该查封户在FC_H_QSDC中存在有效信息，可能的情况是：1.确实无权属关系，2.houseinfo_id为空，导致导入失败...※");
				ZLZT = false;
				PublicDo.writeLogT("FC_DYXX", dy.getFcdyslbh(), "DJ_DY", "未查询到该查封户在FC_H_QSDC中存在有效信息，可能的情况是：1.确实无权属关系，2.houseinfo_id为空导致导入失败...");
			}
			//判断状态码 进行改变数据状态
			PublicDo.changeState("fc_dyxx", "BDCDYSLBH", dy.getFcdyslbh(), ZLZT);	
			log.info("※回滚语句："+rollBacksql.toString());
			System.out.println("※INFO:回滚语句："+rollBacksql.toString());
			//根据回滚语句和回滚方式进行自动回滚
			PublicDo.rollbackNewD(rollBacksql.toString(),ZLZT);
			
			//数据状态
			log.info("※该条数据导入结果:※"+ (ZLZT==true?"成功!!!":"失败!!!"));
			System.out.println("※INFO:该条数据导入结果:※"+ (ZLZT==true?"成功!!!":"失败!!!"));
		}
	}
	
	private void isCanInsert_DY(DY dy) {

		if(dy.getHouseinfo_id()==null||"".equals(dy.getHouseinfo_id())){
			log.error("※该信息对应的HOUSEINFO_ID为空值，数据不规范，无法关联房子信息，导致导入失败...※");
			System.out.println("※ERROR:该信息对应的HOUSEINFO_ID为空值，数据不规范，无法关联房子信息，导致导入失败...※");
			ZLZT =false;
			PublicDo.writeLogT("FC_DYXX",dy.getFcdyslbh(),"DJ_DY","该信息对应的HOUSEINFO_ID为空值，数据不规范，无法关联房子信息，导致导入失败...");
		}else{
			//查询是否有历史数据
			String sql = "select slbh from dj_dy d where slbh in (select slbh from dj_tsgl where tstybm='"+dy.getHouseinfo_id()+"') and (d.lifecycle<>1 or d.lifecycle is null)";
			//执行获得slbhs
			List<String> SLBHS = PublicDo.getSLBHS(sql);
			if(SLBHS.size()==0){
				log.info("※DJ_DY中未发现现实状态的抵押信息，准备导入DJ_DY※");
				System.out.println("※INFO:DJ_DY中未发现现实状态的抵押信息，准备导入DJ_DY※");
				insertDJ_DY(dy);
			};//没有有效信息 可以直接导入
			
			if(SLBHS.size()>=1){
				log.error("※DJ_DY中发现存在现实抵押信息，不允许被抵押,SQL:\n"+sql+"※");
				System.out.println("※ERROR:DJ_DY中发现存在现实抵押信息，不允许被抵押,SQL:\n"+sql+"※");
				PublicDo.writeLogT("FC_DYXX",dy.getFcdyslbh(),"DJ_DY","DJ_DY中发现存在现实抵押信息，不允许被抵押");
				ZLZT =false;
			}
		}
		
	}



	public void insertQLR(DY dy){
		boolean insDJ_QLR1= true;//抵押人导入状态
		boolean insDJ_QLR2 = true;//抵押权人导入状态
		String insQLRsql =""; 
		String[] insQLRparams1 = new String[4];
		String[] insQLRparams2 = new String[4];
		insQLRsql = "";
		String QLRID1 = UUID.randomUUID().toString();//权利人id
		String QLRMC1 = dy.getDyrmc();//抵押人名称
		//证件类型的转移的转译
		String ZJLX1 = StringToDic.decodeZJLX(dy.getDyrzjlx());//抵押人证件类型
		String ZJHM1=dy.getDyrzjbh();//抵押人证件号码
		//参数赋值
		insQLRparams1[0] = QLRID1;
		insQLRparams1[1] = QLRMC1;//权利人名称--权利人
		insQLRparams1[2] = ZJLX1;//证件类别--证件类型（需要转字典）
		insQLRparams1[3] = ZJHM1;//证件号码--证件号
		
		String QLRID2 = UUID.randomUUID().toString();//权利人id
		String QLRMC2 = dy.getDyqrmc();//抵押权人名称
		//证件类型的转移的转译
		String ZJLX2 = StringToDic.decodeZJLX(dy.getDyqrzjlx());//抵押权人证件类型
		String ZJHM2=dy.getDyqrzjbh();//抵押权人证件号码
		
		insQLRparams2[0] = QLRID2;
		insQLRparams2[1] = QLRMC2;//权利人名称--权利人
		insQLRparams2[2] = ZJLX2;//证件类别--证件类型（需要转字典）
		insQLRparams2[3] = ZJHM2;//证件号码--证件号
		
		insQLRsql="insert into dj_qlr (qlrid,qlrmc,zjlb,zjhm,transnum) values(?,?,?,?,58)";
		try {
			DoDatabase.getConnNew();
			log.info("※准备导入DJ_QLR(抵押人)...※");
			System.out.println("※INFO:准备导入DJ_QLR(抵押人)...※");
			DoDatabase.doExecuteUpdate(insQLRsql, insQLRparams1);
			log.info("※QL_QLR(抵押人)信息导入成功生成的QLRID(主键)为:"+QLRID1+"※");
			System.out.println("※INFO:QL_QLR(抵押人)信息导入成功生成的QLRID(主键)为:"+QLRID1+"※");
			rollBacksql.append("delete from dj_qlr where qlrid ='"+QLRID1+"';\n");
			//后期更改 状态
		} catch (SQLException e) {
			ZLZT=false;
			insDJ_QLR1=false;
			//可能出现的异常 fj插不进去超过值范围 gyfe(共有份额 插不进去 值太大)//或者主键冲突
			log.error("※FC_QLR(抵押人)信息导入时发生错误"+e.getLocalizedMessage()+"导致导入失败，已停止QLRGL的导入※");
			System.out.println("※ERROR:FC_QLR(抵押人)信息导入时发生错误"+e.getLocalizedMessage()+"导致导入失败，已停止QLRGL的导入※");
			PublicDo.writeLogT("FC_DYXX",dy.getFcdyslbh(),"DJ_QLR",e.getLocalizedMessage());
		}finally{
			DoDatabase.closeResource();
			DoDatabase.closeConn();
		}
		
		try {
			DoDatabase.getConnNew();
			log.info("※准备导入DJ_QLR(抵押权人)...※");
			System.out.println("※INFO:准备导入DJ_QLR(抵押权人)...※");
			DoDatabase.doExecuteUpdate(insQLRsql, insQLRparams2);
			log.info("※QL_QLR(抵押权人)信息导入成功生成的QLRID(主键)为:"+QLRID2+"※");
			System.out.println("※INFO:QL_QLR(抵押权人)信息导入成功生成的QLRID(主键)为:"+QLRID2+"※");
			rollBacksql.append("delete from dj_qlr where qlrid ='"+QLRID2+"';\n");
			//后期更改 状态
		} catch (SQLException e) {
			ZLZT=false;
			insDJ_QLR2=false;
			//可能出现的异常 fj插不进去超过值范围 gyfe(共有份额 插不进去 值太大)//或者主键冲突
			log.error("※FC_QLR(抵押权人)信息导入时发生错误"+e.getLocalizedMessage()+"导致导入失败，已停止QLRGL的导入※");
			System.out.println("※ERROR:FC_QLR(抵押权人)信息导入时发生错误"+e.getLocalizedMessage()+"导致导入失败，已停止QLRGL的导入※");
			PublicDo.writeLogT("FC_DYXX",dy.getFcdyslbh(),"DJ_QLR",e.getLocalizedMessage());
		}finally{
			DoDatabase.closeResource();
			DoDatabase.closeConn();
		}
		
		//判断是否抵押权利和抵押人的dj_qlr导入成功 如果成功才导入qlrgl
		if(insDJ_QLR1){
			insertDJ_QLRGL_DYR(dy,QLRID1);
		}
		if(insDJ_QLR2){
			insertDJ_QLRGL_DYQR(dy, QLRID2);
		}
	}
	
	private void insertDJ_QLRGL_DYR(DY dy, String qLRID1) {
		String GLBM = UUID.randomUUID().toString();//关联编码 随机生成
		String SLBH = PublicDo.getSLBHWithDY(dy.getDatafrom(), dy.getFcdyslbh());
		//权利人id直接使用
		String QLRLX = "抵押人";
		String QLRMC = dy.getDyrmc();
		Object insDJ_QLRGL_Params[] = new Object[5];
		insDJ_QLRGL_Params[0]=GLBM;
		insDJ_QLRGL_Params[1]=SLBH;
		insDJ_QLRGL_Params[2]=qLRID1;
		insDJ_QLRGL_Params[3]=QLRLX;
		insDJ_QLRGL_Params[4]=QLRMC;
		
		String sql = "insert into dj_qlrgl (glbm,slbh,qlrid,qlrlx,qlrmc,transnum)values(?,?,?,?,?,58)";
		
		try {
			DoDatabase.getConnNew();
			log.info("※准备导入DJ_QLRGL(抵押人)...※");
			System.out.println("※INFO:准备导入DJ_QLRGL(抵押人)...※");
			DoDatabase.doExecuteUpdate(sql, insDJ_QLRGL_Params);
			log.info("※QL_QLRGL(抵押人)信息导入成功生成的GLBM(主键)为:"+GLBM+"※");
			System.out.println("※INFO:QL_QLRGL(抵押人)信息导入成功生成的GLBM(主键)为:"+GLBM+"※");
			rollBacksql.append("delete from dj_qlrgl where glbm ='"+GLBM+"';\n");
			//后期更改 状态
		} catch (SQLException e) {
			ZLZT=false;
			log.error("※FC_QLRGL(抵押人)信息导入时发生错误"+e.getLocalizedMessage()+"导致导入失败※");
			System.out.println("※ERROR:FC_QLR(抵押人)信息导入时发生错误"+e.getLocalizedMessage()+"导致导入失败※");
			PublicDo.writeLogT("FC_DYXX",dy.getFcdyslbh(),"DJ_QLRGL(DYR)",e.getLocalizedMessage());
		}finally{
			DoDatabase.closeResource();
			DoDatabase.closeConn();
		}
		
	}

	private void insertDJ_QLRGL_DYQR(DY dy, String qLRID2) {
		String GLBM = UUID.randomUUID().toString();//关联编码 随机生成
		String SLBH = PublicDo.getSLBHWithDY(dy.getDatafrom(), dy.getFcdyslbh());
		//权利人id直接使用
		String QLRLX = "抵押权人";
		String QLRMC = dy.getDyqrmc();
		Object insDJ_QLRGL_Params[] = new Object[5];
		insDJ_QLRGL_Params[0]=GLBM;
		insDJ_QLRGL_Params[1]=SLBH;
		insDJ_QLRGL_Params[2]=qLRID2;
		insDJ_QLRGL_Params[3]=QLRLX;
		insDJ_QLRGL_Params[4]=QLRMC;
		
		String sql = "insert into dj_qlrgl (glbm,slbh,qlrid,qlrlx,qlrmc,transnum)values(?,?,?,?,?,58)";
		
		try {
			DoDatabase.getConnNew();
			log.info("※准备导入DJ_QLRGL(抵押权人)...※");
			System.out.println("※INFO:准备导入DJ_QLRGL(抵押权人)...※");
			DoDatabase.doExecuteUpdate(sql, insDJ_QLRGL_Params);
			log.info("※QL_QLRGL(抵押权人)信息导入成功生成的GLBM(主键)为:"+GLBM+"※");
			System.out.println("※INFO:QL_QLRGL(抵押权人)信息导入成功生成的GLBM(主键)为:"+GLBM+"※");
			rollBacksql.append("delete from dj_qlrgl where glbm ='"+GLBM+"';\n");
			//后期更改 状态
		} catch (SQLException e) {
			ZLZT=false;
			log.error("※FC_QLRGL(抵押权人)信息导入时发生错误"+e.getLocalizedMessage()+"导致导入失败※");
			System.out.println("※ERROR:FC_QLR(抵押权人)信息导入时发生错误"+e.getLocalizedMessage()+"导致导入失败※");
			PublicDo.writeLogT("FC_DYXX",dy.getFcdyslbh(),"DJ_QLRGL(DYQR)",e.getLocalizedMessage());
		}finally{
			DoDatabase.closeResource();
			DoDatabase.closeConn();
		}
		
	}

	private void insertDJ_XGDJGL(DY dy) {
		//需要判断bdczh是否可以在djb中查找到 查找不到 用fwzh和土地证号 去bdczh中查找 
		//查找到几条插入几条 如果都查找不到 插入空数据 后期需要维护
		String SLBH="";
		boolean isNull=false;
		//查找bdczh 前提是bdczh不为空(目前没有为空的数据)
		if(PublicDo.isHaveData("dj_djb", "bdczh", dy.getBdczh())>0){//bdcdyh有数据
			//如果拿到两条怎么办
			SLBH = PublicDo.getDataNew("slbh", "dj_djb", "bdczh", dy.getBdczh());
			log.info("※查询到该DY信息的bdczh在DJ_DJB中有对应信息，准备导入一条DJ_XGDJGL※");
			System.out.println("※INFO:查询到该DY信息的bdczh在DJ_DJB中有对应信息，准备导入一条DJ_XGDJGL※");
			doInsertDJ_XGDJGL(dy, SLBH,"房屋不动产证",dy.getBdczh());
			isNull=true;
		}else{//没数据继续查
			if(PublicDo.isHaveData("dj_djb", "bdczh", dy.getFczh())>0){
				SLBH = PublicDo.getDataNew("slbh", "dj_djb", "bdczh", dy.getFczh());
				log.info("※查询到该DY信息的bdczh(房产证)在DJ_DJB中有对应信息，准备导入一条DJ_XGDJGL※");
				System.out.println("※INFO:查询到该DY信息的bdczh(房产证)在DJ_DJB中有对应信息，准备导入一条DJ_XGDJGL※");
				doInsertDJ_XGDJGL(dy, SLBH,"房屋不动产证",dy.getFczh());
				isNull=true;
			}
			if(PublicDo.isHaveData("dj_djb", "bdczh", dy.getTdzh())>0){//这边用程序能找到的情况下 用语句找不到 很诡异
				System.out.println("bdczh"+dy.getTdzh());
				SLBH = PublicDo.getDataNew("slbh", "dj_djb", "bdczh", dy.getTdzh());
				log.info("※查询到该DY信息的bdczh(土地证)在DJ_DJB中有对应信息，准备导入一条DJ_XGDJGL※");
				System.out.println("※INFO:查询到该DY信息的bdczh(土地证)在DJ_DJB中有对应信息，准备导入一条DJ_XGDJGL※");
				doInsertDJ_XGDJGL(dy, SLBH,"土地不动产证",dy.getTdzh());
				isNull=true;
			}
		}
		//判断如果状态值还是没有有效信息则提示
		if(!isNull){
			log.error("※查询到该DY信息的bdczh在DJ_DJB中无对应信息，导致导入DJ_XGDJGL失败※");
			System.out.println("※ERROR:查询到该DY信息的bdczh在DJ_DJB中无对应信息，导致导入DJ_XGDJGL失败※");
			ZLZT=false;
			PublicDo.writeLogT("FC_DYXX",dy.getFcdyslbh(),"DJ_XGDJGL","查询到该DY信息的bdczh在DJ_DJB中无对应信息，导致导入失败");
		}
		
	}
	
	private void doInsertDJ_XGDJGL(DY dy,String SLBH,String XGZLX, String XGZH){
		log.info("※准备导入DJ_XGDJGL※");
		System.out.println("※INFO:准备导入DJ_XGDJGL※");
		String BGBM = UUID.randomUUID().toString();//变更编码--随机生成
		String ZSLBH = PublicDo.getSLBHWithDY(dy.getDatafrom(), dy.getFcdyslbh());
		//FSLBH使用传过来的值 
		String BGRQ = FormateData.subTime(dy.getDbrq());
		String BGLX = "抵押";
		//相关证号 根据穿过来的值
		//相关证类型使用传过来的值
		Object insDJ_XGDJGL_Params[] = new Object[7];
		insDJ_XGDJGL_Params[0]=BGBM;
		insDJ_XGDJGL_Params[1]=ZSLBH;
		insDJ_XGDJGL_Params[2]=SLBH;
		insDJ_XGDJGL_Params[3]=BGRQ;
		insDJ_XGDJGL_Params[4]=BGLX;
		insDJ_XGDJGL_Params[5]=XGZH;
		insDJ_XGDJGL_Params[6]=XGZLX;
		
		String ins_DJ_XGDJGL_SQL ="insert into dj_xgdjgl (bgbm,zslbh,fslbh,bgrq,bglx,xgzh,xgzlx,transnum) values(?,?,?,to_date(?,'yyyy/mm/dd HH24:MI:SS'),?,?,?,58)";
		
		try {
			DoDatabase.getConnNew();
			DoDatabase.doExecuteUpdate(ins_DJ_XGDJGL_SQL, insDJ_XGDJGL_Params);
			log.info("※DJ_XGDJGL信息导入成功生成的BGBM(主键)为:"+BGBM+"※");
			System.out.println("※INFO:DJ_XGDJGL信息导入成功生成的BGBM(主键)为:"+BGBM+"※");
			rollBacksql.append("delete from dj_xgdjgl where bgbm ='"+BGBM+"';\n");
		} catch (SQLException e) {
			ZLZT=false;
			//可能出现的异常 fj插不进去超过值范围 gyfe(共有份额 插不进去 值太大)//或者主键冲突
			log.error("※DJ_XGDJGL信息导入时发生错误"+e.getLocalizedMessage()+"导致导入失败※");
			System.out.println("※ERROR:DJ_XGDJGL信息导入时发生错误"+e.getLocalizedMessage()+"导致导入失败※");
			PublicDo.writeLogT("FC_DYXX",dy.getFcdyslbh(),"DJ_XGDJGL",e.getLocalizedMessage());
		}finally {
			DoDatabase.closeResource();
			DoDatabase.closeConn();
		}
		
	}
	
	private void insertDJ_TSGL(DY dy) {
		log.info("※准备导入DJ_TSGL※");
		System.out.println("※INFO:准备导入DJ_TSGL※");
		String GLBM = UUID.randomUUID().toString();//GLBM 
		String SLBH =PublicDo.getSLBHWithDY(dy.getDatafrom(), dy.getFcdyslbh());
		String BDCLX = "房屋";
		String TSTYBM = dy.getHouseinfo_id();//图属统一编码--houseinfo-id
		String BDCDYH = dy.getBdcdyh();//不动产单元号--不动产单元号
		String DJZL = "抵押";//登记种类
		String CSSJ = FormateData.subTime(dy.getDbrq());//产生时间
		Object insDJ_TSGL_Params[] = new Object[7];
		insDJ_TSGL_Params[0] =GLBM;
		insDJ_TSGL_Params[1] =SLBH;
		insDJ_TSGL_Params[2] =BDCLX;
		insDJ_TSGL_Params[3] =TSTYBM;
		insDJ_TSGL_Params[4] =BDCDYH;
		insDJ_TSGL_Params[5] =DJZL;
		insDJ_TSGL_Params[6] =CSSJ;
		
		String insDJ_TSGL = "insert into dj_tsgl (glbm,slbh,bdclx,tstybm,bdcdyh,djzl,cssj,transnum) values(?,?,?,?,?,?,to_date(?,'yyyy/mm/dd HH24:MI:SS'),58)";
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
			PublicDo.writeLogT("FC_DYXX",dy.getFcdyslbh(),"DJ_TSGL",e.getLocalizedMessage());
		}finally {
			DoDatabase.closeResource();
			DoDatabase.closeConn();
		}
		
	}
	private void insertDJ_SJD(DY dy) {
			log.info("※准备导入DJ_SJD※");
			System.out.println("※INFO:※准备导入DJ_SJD※");
			Object[] insDJ_SJD_Params = new String[8];
			String SLBH = PublicDo.getSLBHWithDY(dy.getDatafrom(), dy.getFcdyslbh());
			String DJXL = dy.getDjlx();//登记小类--登记类型
			String TZRDH =dy.getDhhm();//通知人电话
			String TZRXM =dy.getLxr();//通知人姓名
			String PRJID = SLBH;//流程实例
			//坐落 需要确定土地是否为空不为空 已房屋坐落为准 为空 以土地坐落为准
			String ZL = dy.getZl();//坐落--bdc坐落
			String SJR = dy.getSjr();//收件人
			//收件日期暂时没有空值 不做空值判断
			String SJSJ = FormateData.subTime(dy.getSjrq());//收件日期
			
			//拼接参数
			insDJ_SJD_Params[0] = SLBH;//受理编号
			insDJ_SJD_Params[1] = DJXL;//登记类型
			insDJ_SJD_Params[2] = TZRDH;
			insDJ_SJD_Params[3] = TZRXM;
			insDJ_SJD_Params[4] = ZL;
			insDJ_SJD_Params[5] = SJR;
			insDJ_SJD_Params[6] = SJSJ;
			insDJ_SJD_Params[7] = PRJID;
			
			String insDJ_SJD = "insert into dj_sjd (slbh,djxl,tzrdh,tzrxm,zl,sjr,sjsj,prjid,transnum) values(?,?,?,?,?,?,to_date(?,'yyyy/mm/dd HH24:MI:SS'),?,58)";
			//导入值注意 slbh为可能重复 要捕捉异常
			try {
				DoDatabase.getConnNew();
				DoDatabase.doExecuteUpdate(insDJ_SJD, insDJ_SJD_Params);
				log.info("※QL_SJD信息导入成功生成的SLBH(主键)为:"+SLBH+"※");
				System.out.println("※INFO:QL_SJD信息导入成功生成的SLBH(主键)为:"+SLBH+"※");
				rollBacksql.append("delete from dj_sjd where slbh ='"+SLBH+"';\n");
			} catch (SQLException e) {
				ZLZT=false;
				//sql有问题时进行捕捉并保存到数据库 
				log.error("※导入DJ_SJD时发生错误"+e.getLocalizedMessage()+"导致导入失败※");
				System.out.println("※ERROR:导入DJ_SJD时发生错误"+e.getLocalizedMessage()+"导致导入失败※");
				PublicDo.writeLogT("FC_DYXX",dy.getFcdyslbh(),"DJ_SJD",e.getLocalizedMessage());
			}finally {
				DoDatabase.closeResource();
				DoDatabase.closeConn();
			}
	}
	private void insertDJ_DY(DY dy) {
		String SLBH = PublicDo.getSLBHWithDY(dy.getDatafrom(), dy.getFcdyslbh());//受理编号--不动产抵押受理 编号
		String DJLX = "房屋抵押";//登记类型
		String DJYY = dy.getDjlx();//登记原因--登记类型
		String XGZH = PublicDo.getXGZH(dy.getBdczh(), dy.getFczh(), dy.getTdzh());
		String SQRQ	= FormateData.subTime(dy.getSjrq());//申请日期--收件日期
		String DYSW = dy.getQlsx();//抵押顺位--权利顺序
		String DYMJ = dy.getFcdymj();//抵押面积--房产抵押面积
		String BDBZZQSE = dy.getDyje();//被担保主债权数额--抵押金额
		String QLQSSJ = FormateData.subTime(dy.getSdrq());//权利起始时间--fc-设定日期
		String QLJSSJ = FormateData.subTime(dy.getDqrq());//权利结束时间-- fc-到期日期
		String BDCZMH = dy.getBdczmh();//不动产证明号--不动产证明号
		String JGJC = dy.getJgjc();//机构简称--bdc机构简称
		String FZND = dy.getFznd();//发证年度--发证年度
		String ZSH = dy.getZsh();//证书号--BDC-证书号
		String DJRQ = FormateData.subTime(dy.getDbrq());//登记日期--BDC登簿日期
		String DBR = dy.getDbr();//登簿人--BDC登簿人
		String FZRQ = FormateData.subTime(dy.getFzrq());//发证日期--BDC发证日期
		String ZSXLH = dy.getZsxlh();//证书序列号--BDC-证书序列号
		String QT = dy.getQt();//其他(1024)--其他(2000);
		String BDCDYH = dy.getBdcdyh();//不动产单元号--不动产单元号
		//新增字段
		String SZR = dy.getDzr();//缮证人--打件人
		String FZJG = dy.getFzjg();//发证机关--发证机关
		String FJ = dy.getFj();//附记（1024）--附记（2000）
		//新增字段
		String DYLX = dy.getDjlx();//抵押类型--登记类型
		String DYFS =("最高额抵押".equals(dy.getDyfs()))?"2":"1";//最高额抵押是2 其余抵押是1 这里面有个共同抵押无法转
		String DYQX = dy.getXcqx();//抵押期限--续存期限
		
		Object[] insDJ_DYparams = new String[26];
		insDJ_DYparams[0]=SLBH;
		insDJ_DYparams[1]=DJLX;
		insDJ_DYparams[2]=DJYY;
		insDJ_DYparams[3]=XGZH;
		insDJ_DYparams[4]=SQRQ;
		insDJ_DYparams[5]=DYSW;
		insDJ_DYparams[6]=DYMJ;
		insDJ_DYparams[7]=BDBZZQSE;
		insDJ_DYparams[8]=QLQSSJ;
		insDJ_DYparams[9]=QLJSSJ;
		insDJ_DYparams[10]=BDCZMH;
		insDJ_DYparams[11]=JGJC;
		insDJ_DYparams[12]=FZND;
		insDJ_DYparams[13]=ZSH;
		insDJ_DYparams[14]=DJRQ;
		insDJ_DYparams[15]=DBR;
		insDJ_DYparams[16]=FZRQ;
		insDJ_DYparams[17]=ZSXLH;
		insDJ_DYparams[18]=QT;
		insDJ_DYparams[19]=BDCDYH;
		insDJ_DYparams[20]=SZR;
		insDJ_DYparams[21]=FZJG;
		insDJ_DYparams[22]=FJ;
		insDJ_DYparams[23]=DYLX;
		insDJ_DYparams[24]=DYFS;
		insDJ_DYparams[25]=DYQX;
		
		String insDJ_DY = "insert into dj_dy (slbh,djlx,djyy,xgzh,sqrq,dysw,dymj,bdbzzqse,qlqssj,qljssj,bdczmh,jgjc, "
				+ " fznd,zsh,djrq,dbr,fzrq,zsxlh,qt,bdcdyh,szr,fzjg,fj,dylx,dyfs,dyqx,transnum) values(?,?,?,?,to_date(?,'yyyy/mm/dd HH24:MI:SS'),"
				+ "?,?,?,to_date(?,'yyyy/mm/dd HH24:MI:SS'),to_date(?,'yyyy/mm/dd HH24:MI:SS'),"
				+ " ?,?,?,?,to_date(?,'yyyy/mm/dd HH24:MI:SS'),?,"
				+ "to_date(?,'yyyy/mm/dd HH24:MI:SS'),?,?,?,?,?,?,?,?,?,58)";
		
		try {
			DoDatabase.getConnNew();
			DoDatabase.doExecuteUpdate(insDJ_DY, insDJ_DYparams);
			log.info("※QL_DY信息导入成功生成的SLBH(主键)为:"+SLBH+"※");
			System.out.println("※INFO:QL_DY信息导入成功生成的SLBH(主键)为:"+SLBH+"※");
			rollBacksql.append("delete from dj_dy where slbh ='"+SLBH+"';\n");
		} catch (SQLException e) {
			//sql有问题时进行捕捉并保存到数据库 
			
			ZLZT = false;
			//sql有问题时进行捕捉并保存到数据库 
			log.error("※DJ_DY信息导入时发生错误:"+e.getLocalizedMessage()+"导致导入失败※");
			System.out.println("※ERROR:DJ_DY信息导入时发生错误:"+e.getLocalizedMessage()+"导致导入失败※");
			log.info("※DJ_DY导入失败，已停止其他DY相关表的导入工作※");
			System.out.println("※ERROR:DJ_DY导入失败，已停止其他DY相关表的导入工作※");
			PublicDo.writeLogT("FC_DYXX",dy.getFcdyslbh(),"DJ_DY",e.getLocalizedMessage());
		}finally {
			DoDatabase.closeResource();
			DoDatabase.closeConn();
		}
	}
	private void dySaveAsList(ResultSet set) {
		DYList = new ArrayList<>();
		try {
			while(set.next()){
				DY dy = new DY(
						set.getString("bdcdyslbh"),
						set.getString("fczh"),
						set.getString("bdczh"),
						set.getString("bdczmh"),
						set.getString("fzrq"),
						set.getString("dbrq"),
						set.getString("dbr"),
						set.getString("zsxlh"),
						set.getString("jgjc"),
						set.getString("fznd"),
						set.getString("zsh"),
						set.getString("bdcdyh"),
						set.getString("dyje"),
						set.getString("djlx"),
						set.getString("djyy"),
						set.getString("fcdymj"),
						set.getString("sdrq"),
						set.getString("dqrq"),
						set.getString("qlsx"),
						set.getString("zl"),
						set.getString("qt"),
						set.getString("sjrq"),
						set.getString("dzr"),
						set.getString("fzjg"),
						set.getString("fj"),
						set.getString("houseinfo_id"),
						set.getString("sjr"),
						set.getString("lxr"),
						set.getString("dhhm"),
						set.getString("tdzh"),
						set.getString("dyrmc"),
						set.getString("dyqrmc"),
						set.getString("dyrzjlx"),
						set.getString("dyqrzjlx"),
						set.getString("dyrzjbh"),
						set.getString("dyqrzjbh"),
						set.getString("datafrom"),
						set.getString("fcdyslbh"),
						set.getString("dyfs"),
						set.getString("xcqx")
						);
				DYList.add(dy);
			}
		} catch (SQLException e) {
			log.error("--获取fj增量数据处理set集合异常");
			System.out.println("--获取fj增量数据处理set集合异常");
			e.printStackTrace();
		}
	}

}
