package com.project.userservice.mapper;

import com.project.userservice.dto.response.PositionResponse;
import com.project.userservice.entity.Position;
import org.springframework.stereotype.Component;

@Component
public class PositionMapper {

    public PositionResponse toResponse(Position position) {
        return new PositionResponse(position.getId(), position.getCode(),
                position.getTitle(), position.getDescription());
    }
}
