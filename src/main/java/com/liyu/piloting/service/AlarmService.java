package com.liyu.piloting.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.liyu.piloting.config.AlarmConf;
import com.liyu.piloting.model.*;
import com.liyu.piloting.websocket.model.WebSocketMessage;
import com.liyu.piloting.websocket.util.WebSocketSender;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.liyu.piloting.websocket.constant.WebSocketConstant.VIDEO_PILOTING_ALARM;
import static com.liyu.piloting.websocket.constant.WebSocketConstant.VIDEO_PILOTING_CAMERA;

/**
 * @author liyu
 * date 2022/9/28 18:28
 * description
 */
@Service
@Slf4j(topic = "AlarmService")
public class AlarmService {
    @Autowired
    RestTemplate restTemplate;

    @Autowired
    AlarmConf alarmConf;


    private static final String AppKey = "9bf4cdbcf95f428f91bc22fc568197c0";
    private static final String Secret = "664382388b3f6a2ea625d3d31d27e53a";
    private static final String url_access_token = "https://open.ys7.com/api/lapp/token/get";
    private static final String url_device_alarm = "https://open.ys7.com/api/lapp/alarm/device/list";


    private AccessToken accessToken;

    /**
     * 上次的告警信息
     */
    private Alarm preAlarm;

    /**
     * 按照设备获取告警消息列表
     *
     * @param deviceSerial 设备序列号
     * @link https://open.ys7.com/doc/zh/book/index/device_alarm.html
     */
    public AlarmResp queryAlarmDeviceList(String deviceSerial, Integer alarmType, Integer status, Integer pageSize, Integer pageStart) {
        String token = getAccessToken(false);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("accessToken", token);
        map.add("deviceSerial", deviceSerial);//设备序列号,存在英文字母的设备序列号，字母需为大写
        alarmType = alarmType == null ? -1 : alarmType;
        map.add("alarmType", alarmType.toString());//告警类型，默认为-1（全部）
        status = status == null ? 0 : status;
        map.add("status", status.toString());//告警消息状态：2-所有，1-已读，0-未读，默认为0（未读状态）
        map.add("startTime", System.currentTimeMillis() - alarmConf.getSearchTimeForwardMillis() + "");//告警查询开始时间，时间格式为1457420564508，精确到毫秒，默认为今日0点，最多查询当前时间起前推7天以内的数据
//        map.add("endTime", "0");//告警查询结束时间，时间格式为1457420771029，默认为当前时间
        pageSize = pageSize == null ? 20 : pageSize;
        pageStart = pageStart == null ? 0 : pageStart;
        map.add("pageSize", pageSize.toString());//分页大小，默认为10，最大为,50
        map.add("pageStart", pageStart.toString());//分页起始页，从0开始
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);
        ResponseEntity<AlarmResp> exchange = restTemplate.exchange(url_device_alarm, HttpMethod.POST, entity, AlarmResp.class);
        AlarmResp body = exchange.getBody();
        if (exchange.getStatusCode() != HttpStatus.OK || body == null) {
            log.error("queryAlarmDeviceList error exchange={}", exchange.toString());
            return null;
        }
        if (StringUtils.equals("10002", body.getCode())) {
            log.info("re getAccessToken");
            getAccessToken(true);
            return queryAlarmDeviceList(deviceSerial, alarmType, status, pageSize, pageStart);
        }

        if (!StringUtils.equals("200", body.getCode())) {
            log.error("queryAlarmDeviceList error exchange={}", exchange.toString());
            return null;
        }

        return body;
    }

    public List<Alarm> getAlarmDeviceList(String deviceSerial, Integer alarmType, Integer status) {
        AlarmResp body = queryAlarmDeviceList(deviceSerial, alarmType, status, null, null);
        if (body == null) {
            return new ArrayList<>();
        }
        Page page = body.getPage();
        List<Alarm> alarms = body.getData();
        int pages = page.getTotal() / page.getSize();
        int gets = page.getTotal() % page.getSize();
        if (pages > 0) {
            if (gets > 0) {
                pages++;
            }
            //需要分页查
            for (int i = 1; i < pages; i++) {
                alarms.addAll(queryAlarmDeviceList(deviceSerial, alarmType, status, null, i).getData());
            }
        }
        return alarms;
    }

    public void processAlarm(String deviceSerial) {
        List<Alarm> alarmDeviceList = null;
        for (Integer alarmType : alarmConf.getAlarmType()) {
            alarmDeviceList = getAlarmDeviceList(deviceSerial, alarmType, alarmConf.getStatus());
        }
        if (alarmDeviceList == null || alarmDeviceList.isEmpty()) {
            return;
        }
        alarmDeviceList.sort((a, b) -> (int) (b.getAlarmTime() - a.getAlarmTime()));

        //只处理最新的一条
        Alarm alarm = alarmDeviceList.get(0);
        if (preAlarm != null && StringUtils.equals(preAlarm.getAlarmId(), alarm.getAlarmId())) {
            log.info("processAlarm query repeat");
            return;
        }
        log.info("processAlarm alarm={}", alarm.toString());
        preAlarm = alarm;
        WebSocketMessage<Alarm> message = new WebSocketMessage<>();
        message.setContent(alarm)
                .setMsgType(VIDEO_PILOTING_ALARM);
        WebSocketSender.pushMessageToAll(message);
    }


    /**
     * 获取token
     */
    public void accessToken() {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("appKey", AppKey);
        map.add("appSecret", Secret);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);

        ResponseEntity<String> exchange = restTemplate.exchange(url_access_token, HttpMethod.POST, entity, String.class);
        if (exchange.getStatusCode() != HttpStatus.OK || exchange.getBody() == null) {
            log.error("get accessToken error exchange={}", exchange.toString());
            return;
        }
        JSONObject jsonObject = JSONObject.parseObject(exchange.getBody());
        if (!StringUtils.equals("200", jsonObject.getString("code"))) {
            log.error("get accessToken error exchange={}", exchange.toString());
            return;
        }
        JSONObject data = jsonObject.getJSONObject("data");
        accessToken = data.toJavaObject(AccessToken.class);
    }

    public String getAccessToken(boolean flush) {
        if (flush || accessToken == null || (System.currentTimeMillis() + 1000 * 60 * 60) > accessToken.getExpireTime()) {
            log.info("accessToken start");
            accessToken();
            log.info("accessToken success");
        }
        return accessToken.getAccessToken();
    }
}
