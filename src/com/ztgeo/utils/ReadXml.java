package com.ztgeo.utils;
import java.io.File;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.ztgeo.dao.DoDatabase;
import com.ztgeo.staticParams.StaticParams;

//读取xml
public class ReadXml {
	static Logger log = Logger.getLogger(ReadXml.class);
	//!地址暂时写死 后期更改	
	public static void readXmlProperty(String path){
		SAXReader reader = new SAXReader();
		//声明xml文档
		Document doc = null;
		//读取获得对象
		try {
		doc = reader.read(new File(path+"\\setProperty.xml"));
		Element root = doc.getRootElement();
		//获得所有根节点下的子节点集合
		List<Element> elements = root.elements();
		for (Element e : elements) {
			String elementName = e.getName();
			switch (elementName) {
			case "username1":
					StaticParams.username1 = e.getText();
				break;
			case "password1":
					StaticParams.password1 = e.getText();
				break;
			case "url1":
				StaticParams.url1 = e.getText();
				break;
			case "username2":
				StaticParams.username2 = e.getText();
			break;
		case "password2":
				StaticParams.password2 = e.getText();
			break;
		case "url2":
			StaticParams.url2 = e.getText();
			break;
		case "startTime":
			StaticParams.QuartzTime = e.getText();
			break;
		case "rollbackType":
			StaticParams.rollbackType = e.getText();
			break;
		case "isUseQuartz":
			StaticParams.isUseQuartz = e.getText();
			break;
		default:
			break;
		}
		}
		
		
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		log.info("xml获取得到的参数:"+StaticParams.username1
				+"\n"+StaticParams.username1
				+"\n"+StaticParams.password1
				+"\n"+StaticParams.url1
				+"\n"+StaticParams.username2
				+"\n"+StaticParams.password2
				+"\n"+StaticParams.url2
				+"\n"+StaticParams.rollbackType
				+"\n"+StaticParams.QuartzTime
				);
		System.out.println("xml获取得到的参数:"
				+"\n串联库用户名:"+StaticParams.username1
				+"\n串联库密码:"+StaticParams.password1
				+"\n房产库url:"+StaticParams.url1
				+"\n不动产库用户名:"+StaticParams.username2
				+"\n不动产库密码:"+StaticParams.password2
				+"\n不动产库url:"+StaticParams.url2
				+"\n"+StaticParams.rollbackType
				+"\n"+StaticParams.QuartzTime
				);
		
	}
	
}
