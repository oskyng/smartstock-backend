package com.osanzana.smartstock.bff.service;

import com.osanzana.smartstock.bff.dto.ConsumerGroupInfoDTO;
import com.osanzana.smartstock.bff.dto.InfraStatusDTO;
import com.osanzana.smartstock.bff.dto.KafkaTopicInfoDTO;
import com.osanzana.smartstock.bff.dto.ServiceHealthDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class InfraDiagService {

    private static final List<String> TOPICOS_MONITOREADOS = List.of("lote-creado", "alerta-etiqueta", "alerta-escalada");

    private final AdminClient kafkaAdminClient;
    private final WebClient healthWebClient;

    @Value("${smartstock.auth-service.url}")
    private String authServiceUrl;
    @Value("${smartstock.inventory-service.url}")
    private String inventoryServiceUrl;
    @Value("${smartstock.finance-service.url}")
    private String financeServiceUrl;
    @Value("${smartstock.alert-service.url}")
    private String alertServiceUrl;
    @Value("${smartstock.commerce-service.url}")
    private String commerceServiceUrl;

    public InfraDiagService(AdminClient kafkaAdminClient, @Qualifier("healthWebClient") WebClient healthWebClient) {
        this.kafkaAdminClient = kafkaAdminClient;
        this.healthWebClient = healthWebClient;
    }

    @SuppressWarnings("unchecked")
    public Mono<InfraStatusDTO> obtenerEstadoInfraestructura() {
        Map<String, String> servicios = new LinkedHashMap<>();
        servicios.put("auth-service", authServiceUrl);
        servicios.put("inventory-service", inventoryServiceUrl);
        servicios.put("finance-service", financeServiceUrl);
        servicios.put("alert-service", alertServiceUrl);
        servicios.put("commerce-service", commerceServiceUrl);

        List<Mono<ServiceHealthDTO>> checks = servicios.entrySet().stream()
                .map(e -> verificarSalud(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        return Flux.merge(checks)
                .collectList()
                .map(lista -> {
                    lista.sort(Comparator.comparing(ServiceHealthDTO::getNombre));
                    return InfraStatusDTO.builder()
                            .timestamp(LocalDateTime.now())
                            .servicios(lista)
                            .kafkaEstado(obtenerEstadoKafka() ? "OPERACIONAL" : "NO_DISPONIBLE")
                            .kafkaNodos(obtenerNodosKafka())
                            .topicos(obtenerTopicos())
                            .build();
                });
    }

    @SuppressWarnings("unchecked")
    private Mono<ServiceHealthDTO> verificarSalud(String nombre, String baseUrl) {
        long inicio = System.currentTimeMillis();
        return healthWebClient.get()
                .uri(baseUrl + "/actuator/health")
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(3))
                .map(body -> {
                    String estado = String.valueOf(body.getOrDefault("status", "DESCONOCIDO"));
                    String estadoDb = extraerEstadoBaseDatos((Map<String, Object>) body);
                    return ServiceHealthDTO.builder()
                            .nombre(nombre)
                            .url(baseUrl)
                            .estado(estado)
                            .estadoBaseDatos(estadoDb)
                            .latenciaMs(System.currentTimeMillis() - inicio)
                            .build();
                })
                .onErrorResume(ex -> Mono.just(ServiceHealthDTO.builder()
                        .nombre(nombre)
                        .url(baseUrl)
                        .estado("DOWN")
                        .latenciaMs(System.currentTimeMillis() - inicio)
                        .detalle(ex.getClass().getSimpleName())
                        .build()));
    }

    @SuppressWarnings("unchecked")
    private String extraerEstadoBaseDatos(Map<String, Object> body) {
        Object componentsObj = body.get("components");
        if (!(componentsObj instanceof Map)) {
            return null;
        }
        Object dbObj = ((Map<String, Object>) componentsObj).get("db");
        if (!(dbObj instanceof Map)) {
            return null;
        }
        Object status = ((Map<String, Object>) dbObj).get("status");
        return status != null ? status.toString() : null;
    }

    private boolean obtenerEstadoKafka() {
        try {
            kafkaAdminClient.describeCluster().nodes().get(3, TimeUnit.SECONDS);
            return true;
        } catch (Exception ex) {
            log.warn("No se pudo conectar al clúster Kafka: {}", ex.getMessage());
            return false;
        }
    }

    private int obtenerNodosKafka() {
        try {
            return kafkaAdminClient.describeCluster().nodes().get(3, TimeUnit.SECONDS).size();
        } catch (Exception ex) {
            return 0;
        }
    }

    private List<KafkaTopicInfoDTO> obtenerTopicos() {
        try {
            Set<String> topicosExistentes = kafkaAdminClient.listTopics().names().get(3, TimeUnit.SECONDS);
            List<String> topicosAMonitorear = TOPICOS_MONITOREADOS.stream()
                    .filter(topicosExistentes::contains)
                    .collect(Collectors.toList());

            if (topicosAMonitorear.isEmpty()) {
                return Collections.emptyList();
            }

            Map<String, TopicDescription> descripciones = kafkaAdminClient.describeTopics(topicosAMonitorear)
                    .allTopicNames().get(5, TimeUnit.SECONDS);

            Map<String, List<ConsumerGroupInfoDTO>> gruposPorTopico = obtenerGruposConsumidoresPorTopico(topicosAMonitorear);

            return topicosAMonitorear.stream()
                    .map(nombre -> {
                        TopicDescription desc = descripciones.get(nombre);
                        int particiones = desc != null ? desc.partitions().size() : 0;
                        int replicas = desc != null && !desc.partitions().isEmpty()
                                ? desc.partitions().get(0).replicas().size() : 0;
                        return KafkaTopicInfoDTO.builder()
                                .nombre(nombre)
                                .particiones(particiones)
                                .factorReplicacion(replicas)
                                .gruposConsumidores(gruposPorTopico.getOrDefault(nombre, Collections.emptyList()))
                                .build();
                    })
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            log.warn("No se pudo obtener metadata de tópicos Kafka: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    private Map<String, List<ConsumerGroupInfoDTO>> obtenerGruposConsumidoresPorTopico(List<String> topicos) {
        Map<String, List<ConsumerGroupInfoDTO>> resultado = new LinkedHashMap<>();
        try {
            Collection<ConsumerGroupListing> grupos = kafkaAdminClient.listConsumerGroups().all().get(3, TimeUnit.SECONDS);
            List<String> groupIds = grupos.stream().map(ConsumerGroupListing::groupId).collect(Collectors.toList());
            if (groupIds.isEmpty()) {
                return resultado;
            }

            Map<String, ConsumerGroupDescription> descripciones = kafkaAdminClient.describeConsumerGroups(groupIds)
                    .all().get(5, TimeUnit.SECONDS);

            for (String groupId : groupIds) {
                Map<TopicPartition, OffsetAndMetadata> offsets;
                try {
                    offsets = kafkaAdminClient.listConsumerGroupOffsets(groupId)
                            .partitionsToOffsetAndMetadata().get(3, TimeUnit.SECONDS);
                } catch (Exception ex) {
                    continue;
                }
                Set<String> topicosDelGrupo = offsets.keySet().stream()
                        .map(TopicPartition::topic)
                        .collect(Collectors.toCollection(HashSet::new));

                for (String topico : topicos) {
                    if (topicosDelGrupo.contains(topico)) {
                        ConsumerGroupDescription desc = descripciones.get(groupId);
                        resultado.computeIfAbsent(topico, k -> new ArrayList<>())
                                .add(ConsumerGroupInfoDTO.builder()
                                        .groupId(groupId)
                                        .estado(desc != null ? desc.state().toString() : "DESCONOCIDO")
                                        .build());
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("No se pudieron listar los grupos consumidores de Kafka: {}", ex.getMessage());
        }
        return resultado;
    }
}
