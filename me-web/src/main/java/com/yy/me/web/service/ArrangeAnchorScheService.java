package com.yy.me.web.service;

import static com.yy.me.http.BaseServletUtil.FAILED;
import static com.yy.me.http.BaseServletUtil.GRABING_EXCEPTION;
import static com.yy.me.http.BaseServletUtil.GRABING_HAS_FAILE_PEOPLE_FULL;
import static com.yy.me.http.BaseServletUtil.IS_NOT_ARRANGED;
import static com.yy.me.http.BaseServletUtil.IS_NOT_WEEKEND;
import static com.yy.me.http.BaseServletUtil.PARAM_DAY_STR_FORMAT_ERROR;
import static com.yy.me.http.BaseServletUtil.PARAM_ERROR;
import static com.yy.me.http.BaseServletUtil.PARAM_NOW_BIGGER_DAYTIME;
import static com.yy.me.http.BaseServletUtil.SUCCESS;
import static com.yy.me.http.BaseServletUtil.genMsgObj;
import static com.yy.me.http.BaseServletUtil.sendResponseAuto;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import com.yy.cs.common.service.util.StringUtils;
import com.yy.me.http.BaseServletUtil;
import com.yy.me.http.BaseServletUtil.RetMsgObj;
import com.yy.me.metrics.MetricsClient;
import com.yy.me.mongo.MongoUtil;
import com.yy.me.time.DateTimeUtil;
import com.yy.me.time.MaskClock;
import com.yy.me.web.dao.ArrangeAnchorScheMongoDBMapper;

/**
 * 主播排班
 * 
 * @author dudusmida
 *
 */
@Service
public class ArrangeAnchorScheService {

	public static final Logger logger = LoggerFactory
			.getLogger(ArrangeAnchorScheService.class);

	private static final String FIELD_DAY_TIME_STR = "dayTimeStr";
	private static final String FIELD_ARRANGE_START_TIME = "arrangeStartTime";
	private static final String FIELD_ARRANGE_END_TIME = "arrangeEndTime";
	private static final String FIELD_ISVALID = "isValid";
	private static final String FIELD_CITY = "city";
	private static final String FIELD_BID = "bid";
	
	@Value(value = "#{settings['node.productEnv']}")
	private boolean productEnv;

	@Autowired
	private ArrangeAnchorScheMongoDBMapper arrangeAnchorMongoDBMapper;

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private MetricsClient metricsClient;

	/**
	 * 获取本周主播排班考勤列表
	 * 
	 * @throws Exception
	 */
	public void thisweekAttendanceList(Long uid, int isNext,
			HttpServletRequest request, HttpServletResponse response) {
		long startTime = MaskClock.getCurtime();
		int resCode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码

		try {
			if (!arrangeAnchorMongoDBMapper.isArrangeAnchor(uid)) {
				sendResponseAuto(
						request,
						response,
						genMsgObj(IS_NOT_ARRANGED, "the anchor is not arranged"));
				return;
			}

			// 获取今天的星期值
			Date nowDate = new Date();
			int weekday = isDayOfWeek(nowDate);
			boolean isAllow = true;
			// 如果为下周，判断时间是否小于星期6 测试环境一律不走
			if (productEnv && (isNext == 1 && isDayOfWeek(nowDate) < 6)) {
				isAllow = false;
			}

			// 如果今天是周六 测试环境一律不走
			if (productEnv && (isNext == 1 && isDayOfWeek(nowDate) == 6)) {
				// 判断时间是否是12：00
				int hour = nowDate.getHours();
				if (hour < 12) {
					isAllow = false;
				}

			}

			// 获取本周头尾时间段
			Calendar cal = Calendar.getInstance();
			cal.setFirstDayOfWeek(Calendar.MONDAY);
			cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
			String weekStartStr = null;
			Date weekStartDate = null;

			if (0 == isNext) {
				weekStartStr = DateTimeUtil.formatDate(cal.getTime());
				weekStartDate = DateTimeUtil.parseDate(weekStartStr);
				cal.add(Calendar.DATE, 7);
			} else if (1 == isNext) {
				cal.add(Calendar.DATE, 7);
				weekStartStr = DateTimeUtil.formatDate(cal.getTime());
				weekStartDate = DateTimeUtil.parseDate(weekStartStr);
				cal.add(Calendar.DATE, 7);
			}

			String weekEndStr = DateTimeUtil.formatDate(cal.getTime());
			Date weekEndDate = DateTimeUtil.parseDate(weekEndStr);

			String city = null;
			DBCursor dbCursor = arrangeAnchorMongoDBMapper
					.getWeekAttendanceList(uid, weekStartDate, weekEndDate);
			ArrayNode resultLiveState = BaseServletUtil.getLocalObjMapper()
					.createArrayNode();
			while (dbCursor.hasNext()) {
				DBObject dbObject = dbCursor.next();

				String dayStr = (String) dbObject.get(FIELD_DAY_TIME_STR);
				city = (String) dbObject.get(FIELD_CITY);
				Date arrangeStartDate = (Date) dbObject
						.get(FIELD_ARRANGE_START_TIME);
				Date arrangeEndDate = (Date) dbObject
						.get(FIELD_ARRANGE_END_TIME);

				int resultLiveStateSize = resultLiveState.size();
				JsonNode livestateItem = resultLiveState
						.get(resultLiveStateSize - 1);
				if (resultLiveStateSize != 0
						&& livestateItem.get("date").asText().equals(dayStr)) {
					// 装填数组json结果集live元素
					ObjectNode liveItem = BaseServletUtil.getLocalObjMapper()
							.createObjectNode();
					liveItem.put("startTimeStr", DateTimeUtil.formatDate(
							arrangeStartDate, "HH:mm"));
					liveItem.put("endTimeStr",
					        DateTimeUtil.formatDate(arrangeEndDate, "HH:mm"));
					liveItem.put(
							"status",
							isLiveTimeQualified(
									uid,
									dayStr,
									arrangeStartDate,
									arrangeEndDate,
									MongoUtil.getInt(
											dbObject.get(FIELD_ISVALID), 0)));
					liveItem.put("bid", dbObject.get(FIELD_BID).toString());
					((ArrayNode) livestateItem.get("live")).add(liveItem);
					continue;
				}

				// 装填数组json结果集liveState元素
				ObjectNode liveStateItem = BaseServletUtil.getLocalObjMapper()
						.createObjectNode();
				liveStateItem.put("weekday", isDayOfWeek(DateTimeUtil
						.parseDate(dayStr)));
				liveStateItem.put("date", dayStr);
				// 装填数组json结果集live元素
				ObjectNode liveItem = BaseServletUtil.getLocalObjMapper()
						.createObjectNode();
				liveItem.put("startTimeStr",
				        DateTimeUtil.formatDate(arrangeStartDate, "HH:mm"));
				liveItem.put("endTimeStr",
				        DateTimeUtil.formatDate(arrangeEndDate, "HH:mm"));
				liveItem.put(
						"status",
						isLiveTimeQualified(uid, dayStr, arrangeStartDate,
								arrangeEndDate, MongoUtil.getInt(
										dbObject.get(FIELD_ISVALID), 0)));
				liveItem.put("bid", dbObject.get(FIELD_BID).toString());
				ArrayNode liveArray = BaseServletUtil.getLocalObjMapper()
						.createArrayNode();
				liveArray.add(liveItem);
				liveStateItem.put("live", liveArray);
				resultLiveState.add(liveStateItem);
			}
			ObjectNode resultObj = BaseServletUtil.getLocalObjMapper()
					.createObjectNode();
			resultObj.put("isAllow", isAllow);
			resultObj.put("city", city);
			cal.add(Calendar.DATE, -1);
			resultObj.put(
					"weekTimeStr",
					weekStartStr.concat("~").concat(
					        DateTimeUtil.formatDate(cal.getTime())));
			resultObj.put("weekday", weekday);
			resultObj.put("liveState", resultLiveState);
			sendResponseAuto(request, response,
					genMsgObj(SUCCESS, null, resultObj));
		} catch (Exception e) {
			logger.error(
					"[thisweekAttendanceList] the uid is {}, and there are some error:-->"
							+ e.getMessage(), uid);
			sendResponseAuto(request, response, genMsgObj(FAILED));
			resCode = MetricsClient.RESCODE_FAIL;
		} finally {
			metricsClient.report(MetricsClient.ProtocolType.HTTP,
					"ArrangeAnchorSche", this.getClass(), "attendance",
					MaskClock.getCurtime() - startTime, resCode);
		}
	}

	/**
	 * 获取主播下周抢班列表
	 * 
	 * @throws Exception
	 */
	public void weekGrablist(Long uid, int isNext, String city, String weekday,
			HttpServletRequest request, HttpServletResponse response) {

		long startTime = MaskClock.getCurtime();
		int resCode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
		try {
			if (!arrangeAnchorMongoDBMapper.isArrangeAnchor(uid)) {
				sendResponseAuto(
						request,
						response,
						genMsgObj(IS_NOT_ARRANGED, "the anchor is not arranged"));
				return;
			}
			// 如果为下周，判断时间是否小于星期6
			Date nowDate = new Date();
			if (productEnv && (isNext == 1 && isDayOfWeek(nowDate) < 6)) {
				sendResponseAuto(
						request,
						response,
						genMsgObj(IS_NOT_WEEKEND,
								"the day of grabing is not allow"));
				return;
			}
			// 如果今天是周六
			if (productEnv && (isNext == 1 && isDayOfWeek(nowDate) == 6)) {
				// 判断时间是否是12：00
				int hour = nowDate.getHours();
				if (hour < 12) {
					sendResponseAuto(
							request,
							response,
							genMsgObj(IS_NOT_WEEKEND,
									"the day of grabing is not allow"));
					return;
				}

			}
			// 如果为本周 抢班列表的weekday的时间小于当前时间，则进到判断

			if (isNext == 0
					&& (new Date()).compareTo(DateTimeUtil.parseDate(weekday)) >= 0) {
				sendResponseAuto(
						request,
						response,
						genMsgObj(PARAM_NOW_BIGGER_DAYTIME,
								"the day of editting is little than now time"));
				return;
			}

			// 根据weekday获取当天的排班bucket
			ArrayNode resultArray = arrangeAnchorMongoDBMapper
					.weekBucketForday(city, weekday);

			// 如果为本周，则要获取weekday当天该用户之前选的bucket记录明细
			if (isNext == 0) {
				List<String> bidList = arrangeAnchorMongoDBMapper
						.getAnchorWeekBucketForday(uid, city, weekday);
				Iterator iterator = resultArray.iterator();
				while (iterator.hasNext()) {
					ObjectNode a = (ObjectNode) iterator.next();
					if (bidList.contains(a.get("id").asText())) {
						a.put("isGrabed", 1);
					} else {
						a.put("isGrabed", 0);
					}
				}
			}
			sendResponseAuto(request, response,
					genMsgObj(SUCCESS, null, resultArray));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error(
					"[weekGrablist] the anchorUid is {}, isNext is {},city is {}, weekday is {} and isthere are some error:-->"
							+ e.getMessage(), uid, isNext, city, weekday);
			sendResponseAuto(request, response, genMsgObj(FAILED));
			resCode = MetricsClient.RESCODE_FAIL;
		} finally {
			metricsClient.report(MetricsClient.ProtocolType.HTTP,
					"ArrangeAnchorSche", this.getClass(), "grablist",
					MaskClock.getCurtime() - startTime, resCode);
		}
	}

	/**
	 * 获取周列表
	 * 
	 * @param uid
	 * @param isNext
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	public void weekList(int isNext, HttpServletRequest request,
			HttpServletResponse response) {
		long startTime = MaskClock.getCurtime();
		int resCode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
		try {
			Calendar cal = Calendar.getInstance();
			cal.setFirstDayOfWeek(Calendar.MONDAY);
			cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

			ArrayNode weekList = BaseServletUtil.getLocalObjMapper()
					.createArrayNode();
			// 下周周列表
			if (1 == isNext) {
				if (productEnv && isDayOfWeek(new Date()) < 6) {
					sendResponseAuto(
							request,
							response,
							genMsgObj(IS_NOT_WEEKEND,
									"the day of grabing is not allow"));
					return;
				}
				cal.add(Calendar.DATE, 7);
				for (int i = 0; i < 7; i++) {
					ObjectNode day = BaseServletUtil.getLocalObjMapper()
							.createObjectNode();
					day.put("dayCn", i + 1);
					day.put("isinValid", 1);
					day.put("time", DateTimeUtil.formatDate(cal.getTime()));
					weekList.add(day);
					cal.add(Calendar.DATE, 1);
				}
			} else if (0 == isNext) {
				for (int i = 0; i < 7; i++) {
					ObjectNode day = BaseServletUtil.getLocalObjMapper()
							.createObjectNode();
					day.put("dayCn", i + 1);
					Date todayDate = DateTimeUtil.parseDate(
					        DateTimeUtil.formatDate(new Date()));
					Date weekDate = DateTimeUtil.parseDate(DateTimeUtil.formatDate(cal.getTime()));
					day.put("isinValid", todayDate.compareTo(weekDate) >= 0 ? 0
							: 1);
					day.put("time", DateTimeUtil.formatDate(cal.getTime()));
					weekList.add(day);
					cal.add(Calendar.DATE, 1);
				}
			}

			sendResponseAuto(request, response,
					genMsgObj(SUCCESS, null, weekList));

		} catch (Exception e) {
			logger.error(
					"[weekList] getnextweek? -> {},there are some error:-->"
							+ e.getMessage(), isNext);
			sendResponseAuto(request, response, genMsgObj(FAILED));
			resCode = MetricsClient.RESCODE_FAIL;
		} finally {
			metricsClient.report(MetricsClient.ProtocolType.HTTP,
					"ArrangeAnchorSche", this.getClass(), "weeklist",
					MaskClock.getCurtime() - startTime, resCode);
		}
	}

	/**
	 * 抢班(是否需要异步返回) 在出现的异常中，针对明细的改动，在异常中进行了数据回滚，但在bucket人数的改动，若出现异常，暂时不作处理
	 * 
	 * @param uid
	 * @param bucids
	 * @param city
	 * @param dayTimeStr
	 * @param request
	 * @param response
	 */
	public void weekGrab(Long uid, ArrayNode bucids, String dayTimeStr,
			String city, HttpServletRequest request,
			HttpServletResponse response) {
		long startTime = MaskClock.getCurtime();
		int resCode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
		try {

			if (!arrangeAnchorMongoDBMapper.isArrangeAnchor(uid)) {
				sendResponseAuto(
						request,
						response,
						genMsgObj(IS_NOT_ARRANGED, "the anchor is not arranged"));
				return;
			}

			// 判断是否为下周排班 时间是否小于6
			Date nowDate = new Date();
			if (productEnv && isDayOfWeek(nowDate) < 6) {
				sendResponseAuto(
						request,
						response,
						genMsgObj(IS_NOT_WEEKEND,
								"the day of grabing is not allow"));
				return;
			}

			// 如果今天是周六
			if (productEnv && isDayOfWeek(nowDate) == 6) {
				// 判断时间是否是12：00
				int hour = nowDate.getHours();
				if (hour < 12) {
					sendResponseAuto(
							request,
							response,
							genMsgObj(IS_NOT_WEEKEND,
									"the day of grabing is not allow"));
					return;
				}

			}

			// 日期是否符合YYYY-MM-DD
			if (!isValidDate(dayTimeStr)) {
				sendResponseAuto(
						request,
						response,
						genMsgObj(PARAM_DAY_STR_FORMAT_ERROR,
								"the day time is not right"));
				return;
			}

			// 获取直播地址
			String bucid0 = bucids.get(0).asText();
			String bucid1 = bucids.get(1).asText();
			DBObject[] grabDBObjetctArray = new DBObject[2];
			// 判断bucids是否有效

			RetMsgObj retBucketIdObj = isvalidForBucktId(bucids, dayTimeStr,
					city, false, uid);
			if (retBucketIdObj != null) {
				logger.info(
						"[weekGrab] uid {},grabbucid{},and the brabing bucketid is invalid",
						uid, retBucketIdObj.toString());
				sendResponseAuto(request, response, retBucketIdObj);
				return;
			}

			DBCursor oldArrangeScheDbcur = arrangeAnchorMongoDBMapper
					.getArrangeScheInfoFromDayTimeStr(uid, dayTimeStr);

			if (oldArrangeScheDbcur.count() != 0) {
				// 已经存在bucketID
				sendResponseAuto(
						request,
						response,
						genMsgObj(FAILED,
								"Req Param Not Right: bucketid is null"));
				return;
			}
			// 先对两个抢排班进行+1
			for (int i = 0; i < bucids.size(); i++) {
				String bucidi = bucids.get(i).asText();
				try {

					// 排班+1
					grabDBObjetctArray[i] = arrangeAnchorMongoDBMapper
							.grabScheForLive(bucidi);

					if (grabDBObjetctArray[i] == null) {
						// 如果更新失败，则throw异常
						throw new Exception(
								"update arrange sche faile and the bid is ->"
										+ bucidi);
					}
				} catch (Exception e) {
					// catch 异常并手动回滚-1
					logger.error(
							"[weekGrab] uid:{} ,bucids:{}、{} ,dayTimeStr:{},city:{},grab arrange sche count has faile-->"
									+ e.getMessage(), uid, bucid0, bucid1,
							dayTimeStr, city);
					// 检查抢班结果 若无效 手动回滚减一
					RetMsgObj roolbackResult = rollBackReduceGrabCount(
							grabDBObjetctArray[i], i, uid, bucid0);
					if (roolbackResult != null
							&& roolbackResult.getCode() == GRABING_HAS_FAILE_PEOPLE_FULL) {
						sendResponseAuto(request, response, roolbackResult);
						resCode = MetricsClient.RESCODE_FAIL;
						return;
					}
				}
			}
			// 排班+1成功

			for (int i = 0; i < bucids.size(); i++) {
				// 若抢班成功
				try {

					WriteResult writeResult = arrangeAnchorMongoDBMapper
							.grabScheForLiveSaveInfo(uid, bucids.get(i)
									.asText(), null, city,
									grabDBObjetctArray[i]);

					if (writeResult.getN() == 0) {
						throw new Exception("save arrange sche info is failed");
					}
				} catch (Exception e) {
					logger.error(
							"grabing sche is exceptional ,uid->{},dayTimeStr->{},newbucketId->{} and error is ",
							uid, dayTimeStr, bucids.get(i).asText(),
							e.getMessage());
					// 回滚删除今天刚插入的数据，(不减数量)
					arrangeAnchorMongoDBMapper.removeArrangeGrabFromDayStr(uid,
							city, dayTimeStr);
					sendResponseAuto(
							request,
							response,
							genMsgObj(GRABING_EXCEPTION,
									"grabing sche is exceptional ,please contact about staff to solve it!"));
					resCode = MetricsClient.RESCODE_FAIL;
					return;
				}

			}

			sendResponseAuto(request, response, genMsgObj(SUCCESS));
			return;
		} catch (Exception e) {
			logger.error(
					"grabing sche is exceptional ,uid->{},dayTimeStr->{},newbucketId0->{},newbucketId1->{} and error is ",
					uid, dayTimeStr, bucids.get(0).asText(), e.getMessage());
			sendResponseAuto(
					request,
					response,
					genMsgObj(GRABING_EXCEPTION,
							"grabing sche is exceptional ,please contact about staff to solve it!"));
			resCode = MetricsClient.RESCODE_FAIL;
		} finally {
			metricsClient.report(MetricsClient.ProtocolType.HTTP,
					"ArrangeAnchorSche", this.getClass(), "grab",
					MaskClock.getCurtime() - startTime, resCode);
		}
	}

	/**
	 * 改班 在出现的异常中，针对明细的改动，在异常中进行了数据回滚，但在bucket人数的改动，若出现异常，暂时不作处理
	 * 
	 * @throws Exception
	 */
	public void weekEditGrab(Long uid, ArrayNode bucIds, ArrayNode bucOldIds,
			String city, String dayTimeStr, HttpServletRequest request,
			HttpServletResponse response) {
		long startTime = MaskClock.getCurtime();
		int resCode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码

		if (!arrangeAnchorMongoDBMapper.isArrangeAnchor(uid)) {
			sendResponseAuto(request, response,
					genMsgObj(IS_NOT_ARRANGED, "the anchor is not arranged"));
			return;
		}

		// 日期是否符合YYYY-MM-DD
		if (!isValidDate(dayTimeStr)) {
			sendResponseAuto(
					request,
					response,
					genMsgObj(PARAM_DAY_STR_FORMAT_ERROR,
							"the day time is not right"));
			return;
		}
		String bucid0 = bucIds.get(0).asText();
		String bucid1 = bucIds.get(1).asText();

		String bucOldId0 = null;
		String bucOldId1 = null;

		try {
			// 判断提交时间是否大于今天
			if (productEnv
					&& ((new Date()).compareTo(DateTimeUtil.parseDate(
							dayTimeStr)) >= 0)) {
				sendResponseAuto(
						request,
						response,
						genMsgObj(PARAM_NOW_BIGGER_DAYTIME,
								"the day of editting is little than now time"));
				return;
			}
			// 若提交过来的旧bucketid数组为null
			DBCursor oldArrangeScheDbcur = arrangeAnchorMongoDBMapper
					.getArrangeScheInfoFromDayTimeStr(uid, dayTimeStr);

			if (bucOldIds == null || bucOldIds.size() == 0) {
				if (oldArrangeScheDbcur.count() != 0) {
					// 提交过来的旧bid参数和实际的不一样，实际明细中存在旧的排班明细
					sendResponseAuto(
							request,
							response,
							genMsgObj(FAILED,
									"Req Param Not Right: bucketid is null"));
					return;
				} else {
					bucOldIds = BaseServletUtil.getLocalObjMapper()
							.createArrayNode();
					bucOldIds.add("");
					bucOldIds.add("");
				}
			} else {
				// 判断oldbucket数组的值是否有效
				RetMsgObj retBucketIdObj = isvalidForBucktId(bucOldIds,
						dayTimeStr, city, true, uid);
				if (retBucketIdObj != null) {
					logger.info(
							"uid {},grabbucid{},and the oldbucketid is invalid",
							uid, retBucketIdObj.toString());
					sendResponseAuto(request, response, retBucketIdObj);
					return;
				}

				if (oldArrangeScheDbcur.count() == 0) {
					// 提交过来的旧bid参数和实际的不一样，实际明细中存在旧的排班明细
					sendResponseAuto(
							request,
							response,
							genMsgObj(FAILED,
									"Req Param Not Right: bucketid is null"));
					return;
				}
			}

			// 判断bid是否有效
			RetMsgObj retBucketIdObj = isvalidForBucktId(bucIds, dayTimeStr,
					city, false, uid);

			if (retBucketIdObj != null) {
				logger.info(
						"[weekGrab] uid {},grabbucid{},and the editing bucketid is invalid",
						uid, retBucketIdObj.toString());
				sendResponseAuto(request, response, retBucketIdObj);
				return;
			}

			bucOldId0 = bucOldIds.get(0).asText();
			bucOldId1 = bucOldIds.get(1).asText();
			DBObject[] grabDBObjetctArray = new DBObject[2];
			// 判断两个bid是否是同一个 有相同的 则重构新旧bid数组
			if (bucid0.equals(bucOldId0) && !bucid1.equals(bucOldId1)) {
				bucOldIds = bucOldIds.removeAll();
				bucOldIds.add(bucOldId1);
				bucIds = bucIds.removeAll();
				bucIds.add(bucid1);
			} else if (!bucid0.equals(bucOldId0) && bucid1.equals(bucOldId1)) {
				bucOldIds = bucOldIds.removeAll();
				bucOldIds.add(bucOldId0);
				bucIds = bucIds.removeAll();
				bucIds.add(bucid0);
			} else if (!bucid1.equals(bucOldId0) && bucid0.equals(bucOldId1)) {
				bucOldIds = bucOldIds.removeAll();
				bucOldIds.add(bucOldId0);
				bucIds = bucIds.removeAll();
				bucIds.add(bucid1);
			} else if (bucid1.equals(bucOldId0) && !bucid0.equals(bucOldId1)) {
				bucOldIds = bucOldIds.removeAll();
				bucOldIds.add(bucOldId1);
				bucIds = bucIds.removeAll();
				bucIds.add(bucid0);
			}
			// 先抢班
			for (int i = 0; i < bucIds.size(); i++) {
				String bucidi = bucIds.get(i).asText();
				try {
					// 先抢班+1
					grabDBObjetctArray[i] = arrangeAnchorMongoDBMapper
							.grabScheForLive(bucidi);

					if (grabDBObjetctArray[i] == null) {
						// 如果更新失败，则throw异常
						throw new Exception(
								"update arrange sche faile and the bid is ->"
										+ bucidi);
					}
				} catch (Exception e) {
					// catch 异常并手动回滚-1
					logger.error("grab arrange sche count has faile-->"
							+ e.getMessage());
					// 检查抢班结果 若无效 手动回滚减一
					RetMsgObj roolbackResult = rollBackReduceGrabCount(
							grabDBObjetctArray[i], i, uid, bucid0);
					resCode = MetricsClient.RESCODE_FAIL;
					if (roolbackResult != null
							&& roolbackResult.getCode() == GRABING_HAS_FAILE_PEOPLE_FULL) {
						sendResponseAuto(request, response, roolbackResult);
						return;
					}
				}
			}
			// 若数量添加成功
			for (int i = 0; i < bucIds.size(); i++) {
				// 若抢班成功
				try {
					WriteResult writeResult = arrangeAnchorMongoDBMapper
							.grabScheForLiveSaveInfo(uid, bucIds.get(i)
									.asText(), bucOldIds.get(i).asText(), city,
									grabDBObjetctArray[i]);
					if (writeResult.getN() == 0) {
						throw new Exception("save arrange sche info is failed");
					}
				} catch (Exception e) {
					logger.error(
							"grabing sche is exceptional ,uid->{},dayTimeStr->{},newbucketId->{} and error is ",
							uid, dayTimeStr, bucIds.get(i).asText(),
							e.getMessage());
					// 数量不减一，只针对明细进行修改或还原，如果参数bucOldIds为空，表明旧明细没有，则删除新插入的即可，（按照uid，城市和dayTime即可）
					// 如果bucOldIds不为空，表示是更新，则将之前保存的旧明细更新上去即可
					if (StringUtils.isBlank(bucOldIds.get(i).asText())) {// 当前旧bid为空表明当前改班行为是误终生用，则仅仅删除刚刚插入的新数据即可
						// 回滚删除今天刚插入的数据，并减少数量
						arrangeAnchorMongoDBMapper.removeArrangeGrabFromDayStr(
								uid, city, dayTimeStr);
					} else {// 表明是更新旧数据，则将之前查询保留的旧数据更新的新数据上
						if (1 == i) {
							arrangeAnchorMongoDBMapper.grabScheForLiveSaveInfo(
									uid, bucOldIds.get(0).asText(),
									bucIds.get(0).asText(), city,
									grabDBObjetctArray[i]);
						}

					}

					sendResponseAuto(
							request,
							response,
							genMsgObj(GRABING_EXCEPTION,
									"grabing sche is exceptional ,please contact about staff to solve it!"));
					resCode = MetricsClient.RESCODE_FAIL;
					return;
				}

			}

			// 对原来的排班数量实行减操作
			for (int i = 0; i < bucOldIds.size(); i++) {
				String bucOldIdi = bucOldIds.get(i).asText();
				// 修改原来的排班数量 -1
				if (StringUtils.isNotBlank(bucOldIdi)) {

					DBObject reduceDBObjetct = arrangeAnchorMongoDBMapper
							.reduceArrangeGrabCount(bucOldIdi, 1);

					if (reduceDBObjetct == null
							&& !StringUtils.isBlank(bucOldIdi)) {// 若提交的旧bucketid不为空且
																	// 更新后返回的结果为空，则更新异常(不会滚了)
						logger.error(
								"grabing editting  sche is exceptional ,uid->{},dayTimeStr->{},newbucketId->{} old bucketId->{}"
										+ "error: reduce old arrangeSche count is faile",
								uid, dayTimeStr, bucIds.get(i).asText(),
								bucOldIdi);
						sendResponseAuto(
								request,
								response,
								genMsgObj(
										GRABING_EXCEPTION,
										"grabing editting  sche is exceptional ,please contact about staff to solve it!"));
						return;
					}

				}
			}
			sendResponseAuto(request, response, genMsgObj(SUCCESS));
		} catch (Exception e) {
			// 对原来的bucket抢班数量减一，若出异常暂不处理
			logger.error(
					"grabing editting sche is exceptional ,uid->{},dayTimeStr->{},newbucketId->{},{} old bucketId->{},{},and the error is {},cause:{}",
					uid, dayTimeStr, bucIds.get(0).asText(), bucid1, bucOldId0,
					bucOldId1, e.getMessage(), e.getCause());
			sendResponseAuto(
					request,
					response,
					genMsgObj(
							GRABING_EXCEPTION,
							"grabing editting  sche is exceptional ,please contact about staff to solve it!"));
			resCode = MetricsClient.RESCODE_FAIL;
			return;
		} finally {
			metricsClient.report(MetricsClient.ProtocolType.HTTP,
					"ArrangeAnchorSche", this.getClass(), "editgrab",
					MaskClock.getCurtime() - startTime, resCode);

		}

	}

	/**
	 * 判断周几
	 * 
	 * @param date
	 * @return
	 */
	private int isDayOfWeek(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		int weekday = cal.get(Calendar.DAY_OF_WEEK) - 1;
		weekday = weekday <= 0 ? 7 : weekday;
		return weekday;

	}

	/**
	 * 判断时间格式
	 * 
	 * @param str
	 * @return
	 */
	private boolean isValidDate(String str) {
		try {
			Date date = (Date) DateTimeUtil.parseDate(str);
			return str.equals(DateTimeUtil.formatDate(date));
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 判断直播时间
	 * 
	 * @param date
	 * @param arrangeStartDate
	 * @param arrangeEndDate
	 * @param liveStartDate
	 * @param liveEndDate
	 * @return
	 * @throws Exception
	 */
	private String isLiveTimeQualified(Long uid, String dayTimeStr,
			Date arrangeStartDate, Date arrangeEndDate, int isValid)
			throws Exception {

		// 开始时间和结束时间都是在今天
		Date zeroDateArrange = DateTimeUtil.parseDate(DateTimeUtil
				.formatDate(arrangeStartDate));
		Date zeronowTime = DateTimeUtil.parseDate(DateTimeUtil.formatDate(
				new Date()));

		if (zeroDateArrange.equals(zeronowTime)) {
			return "统计中...";
		} else if (zeroDateArrange.compareTo(zeronowTime) < 0) {// 判断当前时间是否大于arrange时间
			if (1 == isValid) {
				return "正常";
			} else {
				return "缺勤";
			}
		}

		return "";
	}

	private RetMsgObj rollBackReduceGrabCount(DBObject grabDBObjetct,
			int arryIndex, Long uid, String bucidZero) {
		try {
			// 如果抢排班失败则手动回滚
			if (grabDBObjetct == null && 0 == arryIndex) {
				return genMsgObj(GRABING_HAS_FAILE_PEOPLE_FULL,
						"grab arrange sche is failed,people count is full");
			} else if (grabDBObjetct == null && 1 == arryIndex) {
				// (自己滚回去 ,对抢班数减一)
				arrangeAnchorMongoDBMapper.reduceArrangeGrabCount(bucidZero, 1);
				return genMsgObj(GRABING_HAS_FAILE_PEOPLE_FULL,
						"grab arrange sche is failed,people count is full");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error("the anchor's uid is ({}) and " + e.getMessage(), uid);

			return genMsgObj(GRABING_EXCEPTION,
					"rollback is failed ,please contact about staff to solve that");
		}
		return null;
	}

	/**
	 * 判断bid是否有效
	 * 
	 * @param bucIds
	 * @param dayTimeStr
	 * @param city
	 * @param isOld
	 * @param uid
	 * @return
	 */
	private RetMsgObj isvalidForBucktId(ArrayNode bucIds, String dayTimeStr,
			String city, boolean isOld, Long uid) {
		// 判断bucOldIds是否有效
		for (int i = 0; i < bucIds.size(); i++) {
			if (!arrangeAnchorMongoDBMapper.isTheBidIsValid(bucIds.get(i)
					.asText(), dayTimeStr, city)) {

				return genMsgObj(PARAM_ERROR,
						"the brabing bucketid is invalid", bucIds.get(i));
			}

			if (isOld
					&& !arrangeAnchorMongoDBMapper.isTheOldBidInGrab(uid,
							bucIds.get(i).asText())) {
				return genMsgObj(PARAM_ERROR,
						"the brabing bucketid is invalid", bucIds.get(i));
			}

		}
		return null;
	}

}
