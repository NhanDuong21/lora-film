package com.project.userservice.mapper;

import com.project.userservice.dto.response.PositionResponse;
import com.project.userservice.entity.Position;
import org.springframework.stereotype.Component;

@Component
public class PositionMapper {

    public PositionResponse toResponse(Position position) {
        return toResponse(position, 0);
    }

    public PositionResponse toResponse(Position position, long employeeCount) {
        return new PositionResponse(position.getId(), position.getCode(),
                position.getTitle(), position.getDescription(),
                position.getDepartment().getId(), position.getDepartment().getCode(),
                position.getDepartment().getName(), employeeCount);
    }
}
