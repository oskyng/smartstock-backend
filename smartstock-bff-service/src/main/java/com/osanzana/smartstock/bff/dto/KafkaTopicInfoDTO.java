package com.osanzana.smartstock.bff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KafkaTopicInfoDTO {
    private String nombre;
    private int particiones;
    private int factorReplicacion;
    private List<ConsumerGroupInfoDTO> gruposConsumidores;
}
