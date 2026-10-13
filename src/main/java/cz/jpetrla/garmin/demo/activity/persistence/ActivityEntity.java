package cz.jpetrla.garmin.demo.activity.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "activity")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_hash", nullable = false, unique = true, length = 64)
    private String fileHash;

    @Column(name = "sport")
    private String sport;

    @Column(name = "average_heart_rate")
    private Short heartRateAvg;

    @Column(name = "aerobic_training_effect")
    private Float trainingEffectAerobic;

    @Column(name = "anaerobic_training_effect")
    private Float trainingEffectAnaerobic;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "total_distance")
    private Float totalDistance;

    @Column(name = "total_timer_time")
    private Float totalTimerTime;

    @Column(name = "total_moving_time")
    private Float totalMovingTime;

    @Column(name = "total_elapsed_time")
    private Float totalElapsedTime;

    @Column(name = "max_heart_rate")
    private Short maxHeartRate;

    @Column(name = "avg_power")
    private Integer avgPower;

    @Column(name = "max_power")
    private Integer maxPower;

    @Column(name = "left_right_balance")
    private Integer leftRightBalance;

    @Column(name = "avg_left_torque_effectiveness")
    private Float avgLeftTorqueEffectiveness;

    @Column(name = "avg_right_torque_effectiveness")
    private Float avgRightTorqueEffectiveness;

    @Column(name = "avg_left_pedal_smoothness")
    private Float avgLeftPedalSmoothness;

    @Column(name = "avg_right_pedal_smoothness")
    private Float avgRightPedalSmoothness;

    @Column(name = "normalized_power")
    private Integer normalizedPower;

    @Column(name = "intensity_factor")
    private Float intensityFactor;

    @Column(name = "training_stress_score")
    private Float trainingStressScore;
}
