package cz.jpetrla.garmin.demo.activity.mapper;

import cz.jpetrla.garmin.demo.activity.api.ActivityUploadResponse;
import cz.jpetrla.garmin.demo.activity.domain.ActivityData;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ActivityMapper {
    ActivityUploadResponse toResponse(ActivityData data);
}
