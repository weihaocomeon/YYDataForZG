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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.apache.log4j.Logger;

import com.ztgeo.services.Services;
import com.ztgeo.services.handle.HandleDY;
import com.ztgeo.utils.Quartz;
import com.ztgeo.utils.ReadXml;

import oracle.sql.DATE;

public class Main extends Thread {
	static Logger log = Logger.getLogger(Main.class);
	JFrame jf;
	JPanel jpanel;
	JButton jb1, jb2, jb3;
	JScrollPane jscrollPane;

	public Main() {
		
		//JFrame.setDefaultLookAndFeelDecorated(true);
		UIManager.put("RootPane.setupButtonVisible", false);
		try {
			org.jb2011.lnf.beautyeye.BeautyEyeLNFHelper.launchBeautyEyeLNF();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		  
		jf = new JFrame("徐州增量数据处理工具");
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
		jb1 = new JButton("开始");
		jb2 = new JButton("结束并退出");
		/*jb3 = new JButton("程序运行结束");*/
		jb1.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				jb1.setEnabled(false);
				start();
				
			}
		});

		jb2.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
				
			}
		});
		
		jpanel.add(jb1);
		jpanel.add(jb2);
		contentPane.add(jscrollPane, BorderLayout.CENTER);
		contentPane.add(jpanel, BorderLayout.SOUTH);
		jf.setSize(800, 600);
		jf.setLocation(400, 200);
		jf.setVisible(true);
		jf.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
	//
	}
	@Override
	public void run() {
			Date date = new Date();
			SimpleDateFormat fm = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS");
			String dateS = fm.format(date);
			System.out.println("※程序启动中-----※"+dateS);
			log.info("※程序启动中-----※"+dateS);
			//read xml for static property
			ReadXml.readXmlProperty();
			//调用定时程序
			//Quartz.startQuartz();
			//c用来测试
			Services service = new Services();
			service.handleZS();  
			service.handleQLR();            
			service.handleFJ();                                               
			service.handleDY(); 
			System.out.println("※程序运行结束----※");
			log.info("※程序运行结束----※");
			
	}

	public static void main(String[] args) {
		new SystemPrintln();
		new Main();
		
		
	}
}


