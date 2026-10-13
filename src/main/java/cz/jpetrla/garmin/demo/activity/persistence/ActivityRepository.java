package cz.jpetrla.garmin.demo.activity.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRepository extends JpaRepository<ActivityEntity, Long> {

    boolean existsByFileHash(String fileHash);
}
