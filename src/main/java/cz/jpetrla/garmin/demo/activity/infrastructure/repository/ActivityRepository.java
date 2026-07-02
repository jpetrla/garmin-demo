package cz.jpetrla.garmin.demo.activity.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRepository extends JpaRepository<ActivityEntity, Long> {

    boolean existsByFitId(Long fitId);
}
