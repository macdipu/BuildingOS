package com.buildingos.building.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.buildingos.building.outbox.application.publishpendingevents.PublishPendingEventsUseCase;
import com.buildingos.building.shared.application.port.out.UnitOfWork;
import com.buildingos.building.shared.domain.model.OutboxEvent;
import com.buildingos.building.shared.domain.repository.OutboxRepository;
import com.buildingos.building.support.JwtFixtures;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** F4-T5a: committed outbox events reach Kafka in the envelope contract and survive a broker outage. */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class OutboxKafkaIntegrationTest {
    private static final String TOPIC = "ownership.transferred";
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static JwtFixtures fixtures;

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres@sha256:b0f9560a2de083e2cc7382e75f808c7381a32852a7ec49117deedb300e552b24")
                    .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("building_db").withUsername("building_app").withPassword("test-only-password");

    /** Same image as infra/docker/compose.yaml; Kafka 4 refuses Testcontainers' 0.0.0.0 controller listener. */
    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("apache/kafka@sha256:fbc7d7c428e3755cf36518d4976596002477e4c052d1f80b5b9eafd06d0fff2f")
                    .asCompatibleSubstituteFor("apache/kafka"))
            .withEnv("KAFKA_LISTENERS", "PLAINTEXT://:9092,BROKER://:9093,CONTROLLER://:9094");

    @Autowired
    private PublishPendingEventsUseCase publish;

    @Autowired
    private OutboxRepository outbox;

    @Autowired
    private UnitOfWork uow;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID building;

    @BeforeAll
    static void startFixtures() throws Exception {
        fixtures = new JwtFixtures();
    }

    @AfterAll
    static void stopFixtures() {
        fixtures.close();
    }

    @DynamicPropertySource
    static void dynamicProps(DynamicPropertyRegistry registry) {
        registry.add("platform.security.issuer", fixtures::issuer);
        registry.add("platform.security.audience", () -> "building-platform");
        registry.add("platform.security.jwk-set-uri", () -> fixtures.jwkSetUri);
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("spring.kafka.producer.properties.max.block.ms", () -> "2000");
        registry.add("spring.kafka.producer.properties.request.timeout.ms", () -> "1500");
        registry.add("spring.kafka.producer.properties.delivery.timeout.ms", () -> "3000");
        registry.add("buildingos.outbox.enabled", () -> "true");
        registry.add("buildingos.outbox.poll-interval", () -> "PT1H");
        registry.add("buildingos.outbox.retry-base", () -> "PT0S");
        registry.add("buildingos.outbox.send-timeout", () -> "PT10S");
    }

    @BeforeEach
    void seed() {
        jdbc.update("UPDATE building_outbox SET delivered_at = now() WHERE delivered_at IS NULL");
        Instant now = Instant.now();
        UUID application = UUID.randomUUID();
        building = UUID.randomUUID();
        jdbc.update("INSERT INTO building_application (id, application_number, applicant_user_id, source, status, "
                + "version, created_at, updated_at) VALUES (?, ?, ?, 'SELF_SERVICE', 'APPROVED', 0, ?, ?)",
                application, UUID.randomUUID().toString().substring(0, 16), UUID.randomUUID(), Timestamp.from(now),
                Timestamp.from(now));
        jdbc.update("INSERT INTO building (id, application_id, name, building_type, address, area, district, "
                + "contact_phone, status, version, created_at, updated_at) VALUES (?, ?, 'Rose Tower', 'RESIDENTIAL', "
                + "'Road 1', 'Mirpur', 'Dhaka', '01712345678', 'ACTIVE', 0, ?, ?)",
                building, application, Timestamp.from(now), Timestamp.from(now));
    }

    @Test
    void deliversTheEnvelopeOnceAndMarksTheRowDelivered() {
        OutboxEvent event = append(UUID.randomUUID());

        assertThat(publish.execute().published()).isEqualTo(1);

        List<ConsumerRecord<String, String>> records = consume(event.eventId(), 1);
        assertThat(records).hasSize(1);
        assertThat(records.get(0).key()).isEqualTo(event.aggregateId().toString());
        JsonNode envelope = JSON.readTree(records.get(0).value());
        assertThat(envelope.propertyNames()).containsExactlyInAnyOrder("eventId", "eventType", "eventVersion",
                "occurredAt", "producer", "correlationId", "buildingId", "data");
        assertThat(envelope.path("eventType").asString()).isEqualTo(TOPIC);
        assertThat(envelope.path("eventVersion").asInt()).isEqualTo(1);
        assertThat(envelope.path("producer").asString()).isEqualTo("building-service");
        assertThat(envelope.path("correlationId").asString()).isEqualTo(event.eventId().toString());
        assertThat(envelope.path("buildingId").asString()).isEqualTo(building.toString());
        assertThat(Instant.parse(envelope.path("occurredAt").asString())).isEqualTo(event.occurredAt());
        assertThat(envelope.path("data").path("share").toString()).isEqualTo("25.5");
        assertThat(envelope.path("data").path("revision").asLong()).isEqualTo(2);
        assertThat(row(event.eventId()).get("delivered_at")).isNotNull();

        assertThat(publish.execute().published()).isZero();
    }

    @Test
    void brokerOutageKeepsTheEventUntilKafkaRecovers() {
        OutboxEvent event = append(UUID.randomUUID());
        var docker = KAFKA.getDockerClient();
        docker.pauseContainerCmd(KAFKA.getContainerId()).exec();
        try {
            assertThat(publish.execute().failed()).isEqualTo(1);
        } finally {
            docker.unpauseContainerCmd(KAFKA.getContainerId()).exec();
        }
        Map<String, Object> failed = row(event.eventId());
        assertThat(failed.get("delivered_at")).isNull();
        assertThat(failed.get("attempts")).isEqualTo(1);
        assertThat((String) failed.get("last_error")).isNotBlank();

        await().atMost(Duration.ofSeconds(60)).pollInterval(Duration.ofSeconds(1))
                .until(() -> publish.execute().published() == 1);

        List<ConsumerRecord<String, String>> records = consume(event.eventId(), 1);
        assertThat(records).isNotEmpty().allSatisfy(r -> assertThat(JSON.readTree(r.value()).path("eventId")
                .asString()).isEqualTo(event.eventId().toString()));
        assertThat(row(event.eventId()).get("delivered_at")).isNotNull();
    }

    private OutboxEvent append(UUID unit) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("transferId", UUID.randomUUID().toString());
        data.put("buildingId", building.toString());
        data.put("unitId", unit.toString());
        data.put("share", new BigDecimal("25.5"));
        data.put("revision", 2L);
        OutboxEvent event = new OutboxEvent(UUID.randomUUID(), TOPIC, 1, building, "UNIT", unit, 2, data,
                Instant.now().truncatedTo(ChronoUnit.MICROS));
        uow.inTransaction(() -> {
            outbox.append(event);
            return null;
        });
        return event;
    }

    private Map<String, Object> row(UUID eventId) {
        return jdbc.queryForMap("SELECT delivered_at, attempts, last_error FROM building_outbox WHERE event_id = ?",
                eventId);
    }

    private static List<ConsumerRecord<String, String>> consume(UUID eventId, int expected) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "outbox-it-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        List<ConsumerRecord<String, String>> matching = new ArrayList<>();
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(TOPIC));
            Instant deadline = Instant.now().plusSeconds(20);
            while (Instant.now().isBefore(deadline) && matching.size() < expected) {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
                    if (record.value().contains(eventId.toString())) {
                        matching.add(record);
                    }
                }
            }
            for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofSeconds(2))) {
                if (record.value().contains(eventId.toString())) {
                    matching.add(record);
                }
            }
        }
        return matching;
    }
}
