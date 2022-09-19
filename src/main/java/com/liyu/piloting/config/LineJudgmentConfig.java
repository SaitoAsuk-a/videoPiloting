package com.liyu.piloting.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * @author liyu
 * date 2022/8/24 9:57
 * description 线路判断参数
 */
@Data
@ConfigurationProperties("piloting.judgment")
@Configuration
public class LineJudgmentConfig {


    private PositionConf position;
    private StartConf start;
    private EndConf end;
    private CameraConf camera;
    private StopConf stop;


    public int LineEndSatisfyDistanceCount() {
        return this.end.getLineEndSatisfyDistanceCount();
    }

    public int getLineEndJudgmentPositionCount() {
        return this.end.getLineEndJudgmentPositionCount();
    }

    public int getLineEndSatisfyDistanceMeter() {
        return this.end.getLineEndSatisfyDistanceMeter();
    }


    public int getPullCameraJudgmentPositionCount() {
        return this.camera.getPullCameraJudgmentPositionCount();
    }

    public int getPullCameraSatisfyDistanceMeter() {
        return this.camera.getPullCameraSatisfyDistanceMeter();
    }

    public int getPullCameraSatisfyDistanceCount() {
        return this.camera.getPullCameraSatisfyDistanceCount();
    }

    public int getPullCameraJudgmentIntervalMeter() {
        return this.camera.getPullCameraJudgmentIntervalMeter();
    }

    public int getPullCameraOverPositionCount() {
        return this.camera.getPullCameraOverPositionCount();
    }

    public int getPullCameraDirectionScoreThreshold() {
        return this.camera.getPullCameraDirectionScoreThreshold();
    }

    public int getCameraOverSatisfyDistanceMeter() {
        return this.camera.getCameraOverSatisfyDistanceMeter();
    }

    public int getCameraOverSatisfyDistanceCount() {
        return this.camera.getCameraOverSatisfyDistanceCount();
    }


    public int getLineStartJudgmentPositionCount() {
        return this.start.getLineStartJudgmentPositionCount();
    }

    public int getStartJudgmentIntervalMeter() {
        return this.start.getStartJudgmentIntervalMeter();
    }

    public int getStartDirectionScoreThreshold() {
        return this.start.getStartDirectionScoreThreshold();
    }


    public int getPositionExpireTime() {
        return this.position.getPositionExpireTime();
    }

    public int getPositionStoreInterval() {
        return this.position.getPositionStoreInterval();
    }

    public int getPositionQueueCapacity() {
        return this.position.getPositionQueueCapacity();
    }


    public int getLineStopJudgmentPositionCount() {
        return this.stop.getLineStopJudgmentPositionCount();
    }

    public double getLineStopJudgmentVariance() {
        return this.stop.getLineStopJudgmentVariance();
    }

    public long getLineStopJudgmentTimeInterval() {
        return this.stop.getLineStopJudgmentTimeInterval();
    }
}
