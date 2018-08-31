package com.yy.me.web.service;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@Ignore
@ContextConfiguration(locations = { "/spring/appContext.xml"})
public class ArrangeAnchorScheServiceTest extends AbstractJUnit4SpringContextTests {

	@Autowired
	private ArrangeAnchorScheService arrangeAnchorScheService;
	
	@Test 
	public void testGetAnchorArrangeScheList(){
		//arrangeAnchorScheService.getAnchorArrangeScheList(0l, null, null);
		
		
	}
}
