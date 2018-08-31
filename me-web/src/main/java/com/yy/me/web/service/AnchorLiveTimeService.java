package com.yy.me.web.service;

import static com.yy.me.http.BaseServletUtil.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.Lists;
import com.mongodb.DBObject;
import com.yy.me.web.dao.AnchorLiveTimeMongoDBMapper;
import com.yy.me.web.entity.LiveShowDuration;
import com.yy.me.web.entity.LiveShowH5DurationDto;

@Service
public class AnchorLiveTimeService {

    @Autowired
    private AnchorLiveTimeMongoDBMapper anchorLiveTimeMongoDBMapper;

    private static Logger logger = LoggerFactory.getLogger(AnchorLiveTimeService.class);
    
    private static final FastDateFormat ZERO_DATA_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd");
    private static final FastDateFormat MATH_DATE_FORMAT = FastDateFormat.getInstance("yyyyMM");
    private static final FastDateFormat LIVE_DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd");
    
    /**
     * 返回h5主播的开播记录
     * @param uid
     * @param Date
     * @return
     */
    public void doSetLiveShowH5DurationDto(Long uid, Date date, HttpServletRequest request, HttpServletResponse response) {
        Date startDate = null;
        Date endDate = null;
        boolean isShouldGreater = false;
        boolean isGreaterMonth = false;
        //date判断是否为空,如果为空为当月1号，默认展示前一个月的开播记录；不为1号，那么显示当月的记录
        if(date == null) {
            //如果没有传数据
                //current month firstDay
            boolean isMonthFirstDay = isMonthFirstDay(new Date());
            //如果是第一天
            if(isMonthFirstDay) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(new Date());
                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.add(Calendar.MONTH, -1);
                date = cal.getTime();
                try {
                    date = ZERO_DATA_FORMAT.parse(ZERO_DATA_FORMAT.format(date));
                } catch (ParseException e) {
                    //未知错误
                    logger.error(e.getMessage(), e);
                }
                //得到查询时间的起始时间和结束时间
                startDate = getMonthFirstDay(date);
                endDate = getMonthLastDay(date);
            } else {
                isShouldGreater = true;
                try {
                    date = ZERO_DATA_FORMAT.parse(ZERO_DATA_FORMAT.format(new Date()));
                } catch (ParseException e) {
                    //未知错误
                    logger.error(e.getMessage(), e);
                }
                //如果不是当月第一天，返回当月第一天 到 现在的时间
                startDate = getMonthFirstDay(date);
                try {
                    endDate = ZERO_DATA_FORMAT.parse(ZERO_DATA_FORMAT.format(new Date()));
                } catch (ParseException e) {
                    endDate = new Date();
                    //未知错误
                    logger.error(e.getMessage(), e);
                }
            }
        } else {
            //如果传入的当月
            try {
                //如果大于当月,肯定没有数据，那么直接返回空
                if(isGreaterThanCurrentMonth(date)) {
                    isGreaterMonth = true;
                }
                boolean isCurrentMonth = isCurrentMonth(date);
                if(isCurrentMonth) {
                    isShouldGreater = true;
                    startDate = getMonthFirstDay(ZERO_DATA_FORMAT.parse(ZERO_DATA_FORMAT.format(date)));
                    endDate = ZERO_DATA_FORMAT.parse(ZERO_DATA_FORMAT.format(new Date()));
                } else {
                    //如果传入的是非当月的
                    startDate = getMonthFirstDay(ZERO_DATA_FORMAT.parse(ZERO_DATA_FORMAT.format(date)));
                    endDate = getMonthLastDay(ZERO_DATA_FORMAT.parse(ZERO_DATA_FORMAT.format(date)));
                }
            } catch (ParseException e) {
                startDate = new Date();
                endDate = new Date();
                //未知错误
                logger.error(e.getMessage(), e);
            }
        }
        LiveShowH5DurationDto liveShowH5DurationDto = new LiveShowH5DurationDto();
        liveShowH5DurationDto.setCurrentMonth(MATH_DATE_FORMAT.format(date));
        List<LiveShowDuration> LiveShowDurationList = Lists.newArrayList();
        logger.info("doSetLiveShowH5DurationDto uid: {}, startDate : {}, endDate : {}", uid, startDate, endDate);
        List<DBObject> anchorLiveList = anchorLiveTimeMongoDBMapper.getLiveTimeDBObjectByUidAndTime(uid, startDate, endDate);
        //如果不为空,为空的话 要补零
        if(!CollectionUtils.isEmpty(anchorLiveList)) {
            for(DBObject dBObject : anchorLiveList) {
                LiveShowDuration liveShowDuration = new LiveShowDuration();
                Date liveShowDate = (Date) (dBObject.get(AnchorLiveTimeMongoDBMapper.FIELD_LIVE_SHOW_DATE) != null ? dBObject.get(AnchorLiveTimeMongoDBMapper.FIELD_LIVE_SHOW_DATE) : new Date());
                liveShowDuration.setUid(uid);
                liveShowDuration.setDuration((Long)(dBObject.get(AnchorLiveTimeMongoDBMapper.FIELD_DURATION) != null ? 
                		(!dBObject.containsField(AnchorLiveTimeMongoDBMapper.FIELD_DURATION_VIEW)||dBObject.get(AnchorLiveTimeMongoDBMapper.FIELD_DURATION_VIEW)==null)? 
                		//dBObject.containsField(AnchorLiveTimeMongoDBMapper.FIELD_DURATION_VIEW)&&dBObject.get(AnchorLiveTimeMongoDBMapper.FIELD_DURATION_VIEW)!=null ?
                				dBObject.get(AnchorLiveTimeMongoDBMapper.FIELD_DURATION):
                				dBObject.get(AnchorLiveTimeMongoDBMapper.FIELD_DURATION_VIEW)	
                						 : new Long(0l)));
                liveShowDuration.setLiveShowDate(liveShowDate);
                LiveShowDurationList.add(liveShowDuration);
            }
        }
        //补零处理
        Date currentDate = startDate;
        
        List<LiveShowDuration> orderedDurationList = Lists.newArrayList();
        //这里没有等于，不补全最后一天数据
        if(!isGreaterMonth) {
            if(isShouldGreater) {
                while(currentDate.getTime() < endDate.getTime()) {
                    LiveShowDuration currentDuration = getLiveShowDurationByLiveShowDate(LiveShowDurationList, currentDate);
                    if(currentDuration == null) {
                        currentDuration = new LiveShowDuration(); 
                        currentDuration.setUid(uid);
                        currentDuration.setDuration(0l);
                        currentDuration.setLiveShowDate(currentDate);
                        String dateStr=LIVE_DATE_FORMAT.format(currentDate);
                        currentDuration.setLiveShowDateStr(dateStr);
                        
                    }
                    orderedDurationList.add(currentDuration);
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(currentDate);
                    cal.add(Calendar.DAY_OF_YEAR, 1);
                    currentDate = cal.getTime();
                }
            } else {
                while(currentDate.getTime() <= endDate.getTime()) {
                    LiveShowDuration currentDuration = getLiveShowDurationByLiveShowDate(LiveShowDurationList, currentDate);
                    if(currentDuration == null) {
                        currentDuration = new LiveShowDuration(); 
                        currentDuration.setUid(uid);
                        currentDuration.setDuration(0l);
                        currentDuration.setLiveShowDate(currentDate);
                        String dateStr=LIVE_DATE_FORMAT.format(currentDate);
                        currentDuration.setLiveShowDateStr(dateStr);
                    }
                    orderedDurationList.add(currentDuration);
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(currentDate);
                    cal.add(Calendar.DAY_OF_YEAR, 1);
                    currentDate = cal.getTime();
                }
            }
        }
        liveShowH5DurationDto.setLiveShowDurationList(orderedDurationList);
        sendResponseAuto(request, response, genMsgObj(SUCCESS, null, liveShowH5DurationDto));
    }
    
    private LiveShowDuration getLiveShowDurationByLiveShowDate(List<LiveShowDuration> durationList, Date liveShowDate) {
        
    	LiveShowDuration liveShowDuration = null;
        
        if(CollectionUtils.isEmpty(durationList) || liveShowDate == null) {
            return null;
        }
        for(LiveShowDuration duration : durationList) {
            if(duration != null && duration.getLiveShowDate() != null) {
                if(liveShowDate.getTime() == duration.getLiveShowDate().getTime()) {
                	duration.setLiveShowDateStr(LIVE_DATE_FORMAT.format(liveShowDate));
                    liveShowDuration = duration;
                }
            }
        }
        return liveShowDuration;
    }
    
    public static void main(String[] args) {
    	    	
    	SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
        Date currentDate = new Date();
        LiveShowDuration currentDuration = new LiveShowDuration(); 
        currentDuration.setUid(1000l);
        currentDuration.setDuration(0l);
        currentDuration.setLiveShowDate(currentDate);
        String time =sdf.format(currentDate);
        currentDuration.setLiveShowDateStr(time);
        Calendar cal = Calendar.getInstance();
        cal.setTime(currentDate);
        cal.add(Calendar.DAY_OF_YEAR, 1);
        currentDate = cal.getTime();
        System.out.println("currentDate == " + currentDate);
        System.out.println("liveShowDate == " + currentDuration.getLiveShowDate());
    }
    
    
    private Date getMonthFirstDay(Date date) {
        Calendar c = Calendar.getInstance(); 
        c.setTime(date);
        c.add(Calendar.MONTH, 0);
        c.set(Calendar.DAY_OF_MONTH,1);
        return c.getTime();
    }
    
    private Date getMonthLastDay(Date date) {
        Calendar c = Calendar.getInstance(); 
        c.setTime(date);
        c.set(Calendar.DAY_OF_MONTH,c.getActualMaximum(Calendar.DAY_OF_MONTH));
        return c.getTime();
    }
    
    private boolean isMonthFirstDay(Date date) {
        boolean flag = false;
        Calendar cal  = Calendar.getInstance();
        cal.setTime(date);
        int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
        flag = dayOfMonth == 1;
        return flag;
    }
    
    private boolean isCurrentMonth(Date date) throws ParseException {
        boolean flag = false;
        Date currentMonthDay = MATH_DATE_FORMAT.parse(MATH_DATE_FORMAT.format(new Date()));
        Date inputMonthDay = MATH_DATE_FORMAT.parse(MATH_DATE_FORMAT.format(date));
        flag = currentMonthDay.getTime() == inputMonthDay.getTime();
        return flag;
    }
    
    private boolean isGreaterThanCurrentMonth(Date date) throws ParseException {
        boolean flag = false;
        Date currentMonthDay = MATH_DATE_FORMAT.parse(MATH_DATE_FORMAT.format(new Date()));
        Date inputMonthDay = MATH_DATE_FORMAT.parse(MATH_DATE_FORMAT.format(date));
        flag = currentMonthDay.getTime() < inputMonthDay.getTime();
        return flag;
    }
    
}
