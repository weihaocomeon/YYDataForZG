package com.ztgeo.services;

import com.ztgeo.services.handle.HandleDY;
import com.ztgeo.services.handle.HandleFJ;
import com.ztgeo.services.handle.HandleQLR;
import com.ztgeo.services.handle.HandleZS;

//管理数据库连接 断开 回滚 提交
public class Services {
	
	//整理权利人
	public void handleQLR(){
		HandleQLR handleqlr = new HandleQLR();
		handleqlr.handleQLR();
	}
	
	//整理附记
	public void handleFJ(){
		HandleFJ handlefj = new HandleFJ();
		handlefj.handleFJ();
	}
	
	//整理抵押
	public void handleDY(){
		HandleDY handledy = new HandleDY();
		handledy.handleDY();
	}
	
	//整理证书
	public void handleZS(){
		HandleZS handlezs = new HandleZS();
		handlezs.handleZS();
	}
	
	
}
