package com.osanzana.smartstock.bff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InfraStatusDTO {
    private LocalDateTime timestamp;
    private List<ServiceHealthDTO> servicios;
    private String kafkaEstado;
    private int kafkaNodos;
    private List<KafkaTopicInfoDTO> topicos;
}
