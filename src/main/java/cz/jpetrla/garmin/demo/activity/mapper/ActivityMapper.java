package cz.jpetrla.garmin.demo.activity.mapper;

import cz.jpetrla.garmin.demo.activity.api.ActivityResponse;
import cz.jpetrla.garmin.demo.activity.domain.ActivityData;
import cz.jpetrla.garmin.demo.activity.infrastructure.repository.ActivityEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ActivityMapper {

    ActivityEntity toEntity(ActivityData data);

    ActivityResponse toResponse(ActivityEntity entity);
}
