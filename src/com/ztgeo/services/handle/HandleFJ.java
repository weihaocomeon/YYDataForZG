package com.ztgeo.services.handle;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.ztgeo.dao.DoDatabase;
import com.ztgeo.entity.FJ;
import com.ztgeo.utils.FormateData;

public class HandleFJ {
	static Logger log = Logger.getLogger(HandleFJ.class);
	private StringBuffer rollBacksql = new StringBuffer();//回滚语句
	private boolean ZLZT;//增量状态 用来判定是否更新状态位
	private List<FJ> FJList;
	public void handleFJ() {
		//连接老库
				DoDatabase.getConnOld();
				//查询增量数据 SELECT 增量 qlr
				String sqlSZLFJ = "select * from FC_FJXX t where t.zlzt = 1";
				//set集合进行保存到list中
				ResultSet setSZLFJ = DoDatabase.getData(sqlSZLFJ);
				fjSaveAsList(setSZLFJ);
				//关闭set 和prep
				DoDatabase.closeResource();
				DoDatabase.closeConn();
				//分级处理 
				toHandleFJ();
				
	}

	private void toHandleFJ() {
		for (FJ fj : FJList) {
			rollBacksql.setLength(0);//回滚语句的清空
			ZLZT=true;
			log.info("");
			log.info("");
			log.info("※※※※FC_FJXX FJID:"+fj.getFJID()+"※※※※");
			System.out.println("\n\n※※※※※※※※※※※※FC_FJXX FJID:"+fj.getFJID()+"※※※※※※※※※※※※\n");
			//判断是否有权属信息 有责插入 无则退出
			//查询房证信息： 有则插入 无则不插入（状态值要改正）
			String SLBH = "";
			String YWZH = PublicDo.getYWZH(fj.getSLBH());	
			//查看是否是ywzh本身为空
			if("".equals(YWZH)||YWZH==null){
				//本身为空
				log.error("※检测到FC_FJXX对应的YWZH为空，导致DJ_DJB_FJ导入失败※");
				System.out.println("※ERROR:检测到FC_FJXX对应的YWZH为空，导致DJ_DJB_FJ导入失败※");
				ZLZT = false;
				//写入日志
				PublicDo.writeLogT("FC_FJXX", fj.getFJID(), "DJ_DJB_FJ", "FC_FJXX中该条信息对应的YWZH为空，数据不规范！");
			}else{
				SLBH = PublicDo.getSLBHWithDJB(YWZH);
				//查询对于属性库中是否有业务对应
				if(PublicDo.isHaveData("dj_djb", "slbh", SLBH)==0){
					//说明无权属信息 ，暂时不需要导入
					log.error("※检测到FC_FJXX的SLBH在DJ_DJB中无对应，无权证信息，导致导入失败...※");
					System.out.println("※ERROR:检测到FC_FJXX的SLBH在DJ_DJB中无对应，无权证信息，导致导入失败...※");
					ZLZT = false;
					PublicDo.writeLogT("FC_FJXX", fj.getFJID(), "DJ_DJB_JS", "FC_FJXX的SLBH在DJ_DJB中无对应，无权证信息，导致导入失败。。。");
				}else{
					//有权证信息。可以导入
					insertDJ_DJB_JS(fj,SLBH);
			}
			}
			//判断状态码 进行改变数据状态
			
			PublicDo.changeState("fc_fjxx", "fjid", fj.getFJID(), ZLZT);
			log.info("※回滚语句："+rollBacksql.toString());
			System.out.println("※INFO:回滚语句："+rollBacksql.toString());
			//根据回滚语句和回滚方式进行自动回滚
			PublicDo.rollbackNewD(rollBacksql.toString(),ZLZT);
			//数据状态
			log.info("※该条数据导入结果:※"+ (ZLZT==true?"成功!!!":"失败!!!"));
			System.out.println("※INFO:该条数据导入结果:※"+ (ZLZT==true?"成功!!!":"失败!!!"));
		}
	}

	private void insertDJ_DJB_JS(FJ fj,String SLBH) {
		//拿到业务宗号
			//可以拿到证书信息 进行信息的插入
			String DJBJSID= fj.getFJID();//登记簿计事ID--附记ID
			//SLBH直接插入
			String JSRQ = FormateData.subTime(fj.getFJRQ());//记事日期--附记日期
			String DJJS = fj.getFJXX();//登记记事--附记信息
			String DJKJBR = fj.getJBR();//登记卡记簿人--经办人
			String DJKSHR = fj.getSHR();//登记卡审核人--审核人
			
			String JSXH = fj.getFJXH();//记事序号--附记序号
			log.info("※对应的登记簿中有权属信息，准备导入DJ_DJB_JS...※");
			System.out.println("※INFO:对应的登记簿中有权属信息，准备导入DJ_DJB_JS...※");
			Object[] insDJ_DJB_JSparams = new String[8];
			insDJ_DJB_JSparams[0]=DJBJSID;
			insDJ_DJB_JSparams[1]=SLBH;
			insDJ_DJB_JSparams[2]=JSRQ;
			insDJ_DJB_JSparams[3]=DJJS;
			insDJ_DJB_JSparams[4]=DJKJBR;
			insDJ_DJB_JSparams[5]= DJKSHR;
			insDJ_DJB_JSparams[6]=SLBH;
			insDJ_DJB_JSparams[7]=JSXH;
			String insDJ_DJB_JS = "insert into dj_djb_js(djbjsid,slbh,jsrq,djjs,djkjbr,djkshr,djslbh,jsxh,transnum) values(?,?,to_date(?,'yyyy/mm/dd HH24:MI:SS'),?,?,?,?,?,57)";
			try {
				DoDatabase.getConnNew();
				DoDatabase.doExecuteUpdate(insDJ_DJB_JS, insDJ_DJB_JSparams);
				log.info("※FC_FJXX信息导入成功生成的DJBJSID(主键)为:"+DJBJSID+"※");
				System.out.println("※INFO:FC_FJXX信息导入成功生成的DJBJSID(主键)为:"+DJBJSID+"※");
				rollBacksql.append("delete from dj_djb_js where djbjsid ='"+DJBJSID+"';\n");
			} catch (SQLException e) {
				//sql有问题时进行捕捉并保存到数据库 
				ZLZT=false;
				log.info("※FC_FJXX信息导入时发生错误:"+e.getLocalizedMessage()+"※");
				System.out.println("※INFO:FC_FJXX信息导入时发生错误:"+e.getLocalizedMessage()+"※");
				PublicDo.writeLogT("FC_FJXX",fj.getFJID(),"DJ_DJB_JS",e.getLocalizedMessage());
			}finally {
				DoDatabase.closeResource();
				DoDatabase.closeConn();
			}
	}

	private void fjSaveAsList(ResultSet set) {
		FJList = new ArrayList<>();
		try {
			while(set.next()){
				//取值
				FJ fj = new FJ();
				fj.setDJSLBH(set.getString("DJSLBH"));
				fj.setFJID(set.getString("FJID"));
				fj.setSLBH(set.getString("SLBH"));
				fj.setFJXH(set.getString("FJXH"));
				fj.setFJRQ(set.getString("FJRQ"));
				fj.setFJXX(set.getString("FJXX"));
				fj.setJBR(set.getString("JBR"));
				fj.setSHR(set.getString("SHR"));
				fj.setFJLX(set.getString("FJLX"));
				//加入list
				FJList.add(fj);
			}
			
		} catch (SQLException e) {
			log.error("--获取fj增量数据处理set集合异常");
			System.out.println("--获取fj增量数据处理set集合异常");
			e.printStackTrace();
		}
	}

}
