package io.github.eggy03.papertrail.api.controller;

import io.github.eggy03.papertrail.api.dto.MessageDTO;
import io.github.eggy03.papertrail.api.entity.Message;
import io.github.eggy03.papertrail.api.mapper.MessageMapper;
import io.github.eggy03.papertrail.api.repository.MessageRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import lombok.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class MessageControllerTest {

    private static final String BASE_PATH = "/api/v2/message";

    private static final Long MESSAGE_ID = 111111111111111L;
    private static final String MESSAGE_CONTENT = "message-one";
    private static final Long AUTHOR_ID = 222222222222222L;

    private static final String UPDATED_MESSAGE_CONTENT = "message-two";
    private static final Long UPDATED_AUTHOR_ID = 333333333333333L;

    private static final Long NEGATIVE_MESSAGE_ID = -111111111111111L;
    private static final Long NEGATIVE_AUTHOR_ID = -222222222222222L;

    @Inject
    MessageRepository repository;
    @Inject
    MessageMapper mapper;

    // RedisDataSource while not annotated for CDI, does get injected because Quarkus handles this synthetic bean
    // Or IntelliJ does not see the dependencies
    // see https://github.com/quarkiverse/quarkus-minio/issues/413 and https://github.com/quarkusio/quarkus/discussions/25120
    @Inject
    RedisDataSource redisDataSource;

    // prep a stream of negative DTOs
    public static @NonNull Stream<MessageDTO> negativeDTOs() {
        return Stream.of(
                new MessageDTO(NEGATIVE_MESSAGE_ID, MESSAGE_CONTENT, AUTHOR_ID),
                new MessageDTO(MESSAGE_ID, MESSAGE_CONTENT, NEGATIVE_AUTHOR_ID)
        );
    }

    // prep a stream of null DTOs
    public static @NonNull Stream<MessageDTO> nullDTOs() {
        return Stream.of(
                new MessageDTO(null, MESSAGE_CONTENT, AUTHOR_ID),
                new MessageDTO(MESSAGE_ID, null, AUTHOR_ID),
                new MessageDTO(MESSAGE_ID, MESSAGE_CONTENT, null)
        );
    }

    public static @NonNull MessageDTO validDTO() {
        return new MessageDTO(MESSAGE_ID, MESSAGE_CONTENT, AUTHOR_ID);
    }

    public static @NonNull MessageDTO validUpdatedDTO() {
        return new MessageDTO(MESSAGE_ID, UPDATED_MESSAGE_CONTENT, UPDATED_AUTHOR_ID);
    }

    // repeated rest-assured calls
    ValidatableResponse postMessage(@NonNull MessageDTO messageDTO) {
        return given()
                .contentType("application/json")
                .body(messageDTO)
                .when()
                .post(BASE_PATH)
                .then();
    }

    ValidatableResponse getMessage(long messageId) {
        return given()
                .contentType("application/json")
                .when()
                .get(BASE_PATH + "/" + messageId)
                .then();
    }

    ValidatableResponse patchMessage(@NonNull MessageDTO messageDTO) {
        return given()
                .contentType("application/json")
                .body(messageDTO)
                .when()
                .patch(BASE_PATH)
                .then();
    }

    ValidatableResponse deleteMessage(long messageId) {
        return given()
                .contentType("application/json")
                .when()
                .delete(BASE_PATH + "/" + messageId)
                .then();
    }

    // repeated assertion calls
    void assertMessageExistsAndValidateIt(@NonNull MessageDTO dto) {

        Optional<Message> entityOptional = QuarkusTransaction
                .requiringNew()
                .call(() -> repository.findByIdOptional(dto.getMessageId()));

        assertThat(entityOptional)
                .isPresent()
                .get()
                .satisfies(message -> {
                    assertThat(message.getMessageId()).isEqualTo(dto.getMessageId());
                    assertThat(message.getMessageContent()).isEqualTo(dto.getMessageContent());
                    assertThat(message.getAuthorId()).isEqualTo(dto.getAuthorId());
                });
    }

    void assertMessageDoesNotExist(@NonNull MessageDTO dto) {

        Optional<Message> entityOptional = QuarkusTransaction
                .requiringNew()
                .call(() -> repository.findByIdOptional(dto.getMessageId()));

        assertThat(entityOptional).isEmpty();
    }

    void assertRepositoryEmpty() {
        assertThat(repository.findAll().list()).isEmpty();
    }

    // repeated repository calls
    void persistMessage(@NonNull MessageDTO messageDTO) {
        QuarkusTransaction.requiringNew().run(() -> repository.persistAndFlush(mapper.toEntity(messageDTO)));
    }

    @BeforeEach
    void cleanState() {
        QuarkusTransaction.requiringNew().run(repository::deleteAll);
        redisDataSource.flushall();
    }

    @Test
    void saveMessage_success() {
        postMessage(validDTO()).statusCode(201);
        // assert that save was a success
        assertMessageExistsAndValidateIt(validDTO());
    }

    @Test
    void saveMessage_alreadyExists_conflicts() {
        // save once, expect success
        postMessage(validDTO()).statusCode(201);
        // save again, expect 409 conflict
        postMessage(validDTO()).statusCode(409);
        // assert that the data wasn't modified
        assertMessageExistsAndValidateIt(validDTO());

    }

    @ParameterizedTest
    @MethodSource({"negativeDTOs", "nullDTOs"})
    void saveMessage_negativeAndNullDTOs_validationFails_badRequest(MessageDTO dto) {
        postMessage(dto).statusCode(400);
        assertRepositoryEmpty();
    }

    @Test
    void getMessage_success() {
        // save
        persistMessage(validDTO());
        // get registered message - expect success
        getMessage(validDTO().getMessageId()).statusCode(200);
        // validate
        assertMessageExistsAndValidateIt(validDTO());
    }

    @Test
    void getMessage_notSaved_notFound() {
        getMessage(validDTO().getMessageId()).statusCode(404);
        assertMessageDoesNotExist(validDTO());
    }

    @Test
    void getMessage_invalidParameters() {
        getMessage(NEGATIVE_MESSAGE_ID).statusCode(400);
    }

    @Test
    void updateMessage_success() {
        // assert message did not exist before
        assertMessageDoesNotExist(validDTO());
        // save
        persistMessage(validDTO());
        // validate its existence
        assertMessageExistsAndValidateIt(validDTO());
        // update
        patchMessage(validUpdatedDTO()).statusCode(204);
        // verify update
        assertMessageExistsAndValidateIt(validUpdatedDTO());
    }

    @Test
    void updateMessage_doesNotExist_notFound() {
        // update without saving
        patchMessage(validUpdatedDTO()).statusCode(404);
        // verify update didn't register a new guild
        assertMessageDoesNotExist(validUpdatedDTO());

    }

    @ParameterizedTest
    @MethodSource({"negativeDTOs", "nullDTOs"})
    void updateMessage_negativeAndNullDTOs_validationFails_badRequest(MessageDTO dto) {
        // persist a message
        persistMessage(validDTO());
        // try updating with invalid parameters
        patchMessage(dto).statusCode(400);
        // verify nothing was changed
        assertMessageExistsAndValidateIt(validDTO());
    }

    @Test
    void deleteMessage_success() {
        // save
        persistMessage(validDTO());
        // verify existence
        assertMessageExistsAndValidateIt(validDTO());
        // delete
        deleteMessage(validDTO().getMessageId()).statusCode(204);
        // verify deletion
        assertMessageDoesNotExist(validDTO());

    }

    @Test
    void deleteMessage_doesNotExist_notFound() {
        // attempt to delete without persisting
        deleteMessage(validDTO().getMessageId()).statusCode(404);
        // verify guild actually does not exist
        assertMessageDoesNotExist(validDTO());

    }

    @Test
    void deleteMessage_invalidParameters() {
        deleteMessage(NEGATIVE_MESSAGE_ID).statusCode(400);

    }

}
