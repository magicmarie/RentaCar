package com.rentacar.dto.reservation;

import java.time.LocalDateTime;

public record CheckOutRequest(LocalDateTime pickupDateTime) {
}
