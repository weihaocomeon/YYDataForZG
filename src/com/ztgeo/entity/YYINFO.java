package com.ztgeo.entity;

//预约人信息的实体类
public class YYINFO {
	private String NI_NUMBER; //预约顺序号
	
	private String NI_NAME; //预约人姓名
	
	private String NI_SFZ; //身份证信息
	
	private String NI_PHONE; //预约人联系方式
	private String NI_Noon;//预约时间段
	
	public YYINFO() {
		// TODO Auto-generated constructor stub
	}

	public YYINFO(String nI_NUMBER, String nI_NAME, String nI_SFZ, String nI_PHONE, String nI_Noon) {
		super();
		NI_NUMBER = nI_NUMBER;
		NI_NAME = nI_NAME;
		NI_SFZ = nI_SFZ;
		NI_PHONE = nI_PHONE;
		NI_Noon = nI_Noon;
	}

	public String getNI_NUMBER() {
		return NI_NUMBER;
	}

	public void setNI_NUMBER(String nI_NUMBER) {
		NI_NUMBER = nI_NUMBER;
	}

	public String getNI_NAME() {
		return NI_NAME;
	}

	public void setNI_NAME(String nI_NAME) {
		NI_NAME = nI_NAME;
	}

	public String getNI_SFZ() {
		return NI_SFZ;
	}

	public void setNI_SFZ(String nI_SFZ) {
		NI_SFZ = nI_SFZ;
	}

	public String getNI_PHONE() {
		return NI_PHONE;
	}

	public void setNI_PHONE(String nI_PHONE) {
		NI_PHONE = nI_PHONE;
	}

	public String getNI_Noon() {
		return NI_Noon;
	}

	public void setNI_Noon(String nI_Noon) {
		NI_Noon = nI_Noon;
	}

	
	



	
}
