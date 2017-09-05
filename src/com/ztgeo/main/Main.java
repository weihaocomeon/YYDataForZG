package com.ztgeo.main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;

import org.apache.log4j.Logger;

import com.ztgeo.servicesimpl.serviceimpl;
import com.ztgeo.staticParams.StaticParams;
import com.ztgeo.utils.Email;
import com.ztgeo.utils.FormateData;
import com.ztgeo.utils.Quartz;
import com.ztgeo.utils.ReadXml;


public class Main extends Thread {
	Logger log = Logger.getLogger(Main.class);
	JFrame jf;
	JPanel jpanel;
	JScrollPane jscrollPane;

	public Main() {
		UIManager.put("RootPane.setupButtonVisible", false);
		try {
			org.jb2011.lnf.beautyeye.BeautyEyeLNFHelper.launchBeautyEyeLNF();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		  
		jf = new JFrame("预约系统加密数据推送For中庚(建国西路数据)");
		Container contentPane = jf.getContentPane();
		contentPane.setLayout(new BorderLayout());
		StringStr.jta=new JTextArea(10,15);
		StringStr.jta.setTabSize(4);
		StringStr.jta.setFont(new Font("标楷体", Font.BOLD, 16));
		StringStr.jta.setLineWrap(true);// 激活自动换行功能
		StringStr.jta.setWrapStyleWord(true);// 激活断行不断字功能
		StringStr.jta.setBackground(Color.WHITE);	
		jscrollPane = new JScrollPane(StringStr.jta);
		jpanel = new JPanel();
		jpanel.setLayout(new GridLayout(1, 3));
		contentPane.add(jscrollPane, BorderLayout.CENTER);
		contentPane.add(jpanel, BorderLayout.SOUTH);
		jf.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		jf.setSize(800, 600);
		jf.setLocation(400, 200);
		jf.setVisible(true);
		jf.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				//System.out.println("本程序不允许被终止!");
				System.exit(0);
			}
		});
	//
	}
	@Override
	public void run() {
			String dateS = FormateData.getNowTime();
			System.out.println("※程序启动中-----启动日期:※"+dateS+"※");
			log.info("※程序启动中-----启动日期:※"+dateS+"※");
			File directory = new File("xml");//设定为当前文件夹 
			String path="";
			
			
		    path = directory.getAbsolutePath();//获取标准的路径 
		    //开发环境 该目录可用
		    ReadXml.readXmlProperty(path);
		    
			//调用定时程序
			if("true".equals(StaticParams.isUseQuartz)){//启动定时程序
				 Quartz.startQuartz();
			}else{
				//邮件数据清空
				StaticParams.sbForEmail.setLength(0);
				serviceimpl im = new serviceimpl();
				im.ToDo();
				System.out.println("※程序运行结束----※");
				log.info("※程序运行结束----※");
				//判断sb长度 是否发送邮件
				if(StaticParams.sbForEmail.length()>0){
					//写入邮件系统
					try {
						Email.setEmail(StaticParams.sbForEmail.toString());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			
		    
			
			
			
	}

	public static void main(String[] args) {
		new SystemPrintln();
		Main m = new Main();
		m.run();
	}
}


