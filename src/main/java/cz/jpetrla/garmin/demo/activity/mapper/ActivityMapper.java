package cz.jpetrla.garmin.demo.activity.mapper;

import cz.jpetrla.garmin.demo.activity.api.ActivityResponse;
import cz.jpetrla.garmin.demo.activity.domain.ActivityData;
import cz.jpetrla.garmin.demo.activity.infrastructure.repository.ActivityEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface ActivityMapper {

    @Mapping(target = "id", ignore = true)
    ActivityEntity toEntity(ActivityData data, String fileHash);

    ActivityResponse toResponse(ActivityEntity entity);
}
