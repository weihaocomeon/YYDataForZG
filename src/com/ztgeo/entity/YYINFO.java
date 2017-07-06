package com.ztgeo.entity;

//预约人信息的实体类
public class YYINFO {
	private String QNO; //预约顺序号
	
	private String NAME; //预约人姓名
	
	private String IDENTITYCARDNUM; //身份证信息
	
	private String CARDID; //预约人联系方式
	
	public YYINFO() {
		// TODO Auto-generated constructor stub
	}

	
	
	public YYINFO(String qNO, String nAME, String iDENTITYCARDNUM, String cARDID) {
		super();
		QNO = qNO;
		NAME = nAME;
		IDENTITYCARDNUM = iDENTITYCARDNUM;
		CARDID = cARDID;
	}



	public String getQNO() {
		return QNO;
	}

	public void setQNO(String qNO) {
		QNO = qNO;
	}

	public String getNAME() {
		return NAME;
	}

	public void setNAME(String nAME) {
		NAME = nAME;
	}

	public String getIDENTITYCARDNUM() {
		return IDENTITYCARDNUM;
	}

	public void setIDENTITYCARDNUM(String iDENTITYCARDNUM) {
		IDENTITYCARDNUM = iDENTITYCARDNUM;
	}

	public String getCARDID() {
		return CARDID;
	}

	public void setCARDID(String cARDID) {
		CARDID = cARDID;
	}



	@Override
	public String toString() {
		return "预约人信息 [预约人顺序号=" + QNO + ", 姓名=" + NAME + ", 身份证=" + IDENTITYCARDNUM + ", 号码=" + CARDID
				+ "]";
	}
	
	
	
}
