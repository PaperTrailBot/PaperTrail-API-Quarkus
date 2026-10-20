package io.github.eggy03.papertrail.api.controller;

import io.github.eggy03.papertrail.api.dto.GuildDTO;
import io.github.eggy03.papertrail.api.entity.Guild;
import io.github.eggy03.papertrail.api.mapper.GuildMapper;
import io.github.eggy03.papertrail.api.repository.GuildRepository;
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
class GuildControllerTest {

    private static final String BASE_PATH = "/api/v2/guild";

    private static final Long GUILD_ID = 111111111111111L;

    private static final Long GUILD_EVENT_CHANNEL_ID = 222222222222222L;
    private static final Long MEMBER_EVENT_CHANNEL_ID = 333333333333333L;
    private static final Long MESSAGE_EVENT_CHANNEL_ID = 444444444444444L;

    private static final Long UPDATED_GUILD_EVENT_CHANNEL_ID = 555555555555555L;
    private static final Long UPDATED_MEMBER_EVENT_CHANNEL_ID = 666666666666666L;
    private static final Long UPDATED_MESSAGE_EVENT_CHANNEL_ID = 777777777777777L;

    private static final Long NEGATIVE_GUILD_ID = -111111111111111L;
    private static final Long NEGATIVE_GUILD_EVENT_CHANNEL_ID = -222222222222222L;
    private static final Long NEGATIVE_MEMBER_EVENT_CHANNEL_ID = -333333333333333L;
    private static final Long NEGATIVE_MESSAGE_EVENT_CHANNEL_ID = -444444444444444L;

    @Inject
    GuildRepository repository;

    @Inject
    GuildMapper mapper;

    // RedisDataSource while not annotated for CDI, does get injected because Quarkus handles this synthetic bean
    // Or IntelliJ does not see the dependencies
    // see https://github.com/quarkiverse/quarkus-minio/issues/413 and https://github.com/quarkusio/quarkus/discussions/25120
    @Inject
    RedisDataSource redisDataSource;

    // prep a stream of valid DTOs
    public static @NonNull Stream<GuildDTO> validDTOs() {

        return Stream.of(
                // no nulls
                new GuildDTO(GUILD_ID, GUILD_EVENT_CHANNEL_ID, MEMBER_EVENT_CHANNEL_ID, MESSAGE_EVENT_CHANNEL_ID),

                // at-most one null
                new GuildDTO(GUILD_ID, null, MEMBER_EVENT_CHANNEL_ID, MESSAGE_EVENT_CHANNEL_ID),
                new GuildDTO(GUILD_ID, GUILD_EVENT_CHANNEL_ID, null, MESSAGE_EVENT_CHANNEL_ID),
                new GuildDTO(GUILD_ID, GUILD_EVENT_CHANNEL_ID, MEMBER_EVENT_CHANNEL_ID, null),

                // at-most 2 nulls
                new GuildDTO(GUILD_ID, null, null, GUILD_EVENT_CHANNEL_ID),
                new GuildDTO(GUILD_ID, null, MEMBER_EVENT_CHANNEL_ID, null),
                new GuildDTO(GUILD_ID, null, null, MESSAGE_EVENT_CHANNEL_ID)
        );
    }

    // prep a stream of negative DTOs
    public static @NonNull Stream<GuildDTO> negativeDTOs() {

        return Stream.of(
                new GuildDTO(NEGATIVE_GUILD_ID, GUILD_EVENT_CHANNEL_ID, MEMBER_EVENT_CHANNEL_ID, MESSAGE_EVENT_CHANNEL_ID),
                new GuildDTO(GUILD_ID, NEGATIVE_GUILD_EVENT_CHANNEL_ID, MEMBER_EVENT_CHANNEL_ID, MESSAGE_EVENT_CHANNEL_ID),
                new GuildDTO(GUILD_ID, GUILD_EVENT_CHANNEL_ID, NEGATIVE_MEMBER_EVENT_CHANNEL_ID, MESSAGE_EVENT_CHANNEL_ID),
                new GuildDTO(GUILD_ID, GUILD_EVENT_CHANNEL_ID, MEMBER_EVENT_CHANNEL_ID, NEGATIVE_MESSAGE_EVENT_CHANNEL_ID)
        );
    }

    // prep a stream of null DTOs
    public static @NonNull Stream<GuildDTO> nullDTOs() {

        return Stream.of(
                new GuildDTO(null, GUILD_EVENT_CHANNEL_ID, MEMBER_EVENT_CHANNEL_ID, MESSAGE_EVENT_CHANNEL_ID),
                new GuildDTO(GUILD_ID, null, null, null)
        );
    }

    // individual DTOs
    public static @NonNull GuildDTO validDTO() {
        return new GuildDTO(GUILD_ID, GUILD_EVENT_CHANNEL_ID, MEMBER_EVENT_CHANNEL_ID, MESSAGE_EVENT_CHANNEL_ID);
    }

    public static @NonNull GuildDTO updatedDTO() {
        return new GuildDTO(GUILD_ID, UPDATED_GUILD_EVENT_CHANNEL_ID, UPDATED_MEMBER_EVENT_CHANNEL_ID, UPDATED_MESSAGE_EVENT_CHANNEL_ID);
    }

    @BeforeEach
    void cleanState() {
        QuarkusTransaction.requiringNew().run(repository::deleteAll);
        redisDataSource.flushall();
    }

    // repeated rest-assured calls
    ValidatableResponse postGuild(@NonNull GuildDTO guildDTO) {
        return given()
                .contentType("application/json")
                .body(guildDTO)
                .when().post(BASE_PATH)
                .then();
    }

    ValidatableResponse getGuild(long guildId) {
        return given()
                .contentType("application/json")
                .when()
                .get(BASE_PATH + "/" + guildId)
                .then();
    }

    ValidatableResponse patchGuild(@NonNull GuildDTO guildDTO) {
        return given()
                .contentType("application/json")
                .body(guildDTO)
                .when()
                .patch(BASE_PATH)
                .then();
    }

    ValidatableResponse deleteGuild(long guildId) {
        return given()
                .contentType("application/json")
                .when()
                .delete(BASE_PATH + "/" + guildId)
                .then();
    }

    // repeated assertion calls
    void assertGuildExistsAndValidateIt(@NonNull GuildDTO dto) {

        Optional<Guild> entityOptional = QuarkusTransaction
                .requiringNew()
                .call(() -> repository.findByIdOptional(dto.getGuildId()));

        assertThat(entityOptional)
                .isPresent()
                .get()
                .satisfies(guild -> {
                    assertThat(guild.getGuildId()).isEqualTo(dto.getGuildId());
                    assertThat(guild.getGuildEventChannelId()).isEqualTo(dto.getGuildEventChannelId());
                    assertThat(guild.getMemberEventChannelId()).isEqualTo(dto.getMemberEventChannelId());
                    assertThat(guild.getMessageEventChannelId()).isEqualTo(dto.getMessageEventChannelId());
                });
    }

    void assertGuildDoesNotExist(@NonNull GuildDTO dto) {

        Optional<Guild> entityOptional = QuarkusTransaction
                .requiringNew()
                .call(() -> repository.findByIdOptional(dto.getGuildId()));

        assertThat(entityOptional).isEmpty();
    }

    void assertRepositoryEmpty() {
        assertThat(repository.findAll().list()).isEmpty();
    }

    // repeated repository calls
    void persistGuild(@NonNull GuildDTO guildDTO) {
        QuarkusTransaction.requiringNew().run(() -> repository.persistAndFlush(mapper.toEntity(guildDTO)));
    }

    @ParameterizedTest
    @MethodSource("validDTOs")
    void saveGuild_success(GuildDTO dto) {
        postGuild(dto).statusCode(201);
        // assert that save was a success
        assertGuildExistsAndValidateIt(dto);
    }

    @ParameterizedTest
    @MethodSource("validDTOs")
    void saveGuild_alreadyExists_conflicts(GuildDTO dto) {
        // save once, expect success
        postGuild(dto).statusCode(201);
        // save again, expect 409 conflict
        postGuild(dto).statusCode(409);
        // assert that the data wasn't modified
        assertGuildExistsAndValidateIt(dto);

    }

    @ParameterizedTest
    @MethodSource({"negativeDTOs", "nullDTOs"})
    void saveGuild_negativeAndNullDTOs_validationFails_badRequest(GuildDTO dto) {
        postGuild(dto).statusCode(400);
        assertRepositoryEmpty();
    }

    @ParameterizedTest
    @MethodSource("validDTOs")
    void getGuild_success(GuildDTO dto) {
        // save
        persistGuild(dto);
        // get saved guild - expect success
        getGuild(dto.getGuildId()).statusCode(200);
        // validate
        assertGuildExistsAndValidateIt(dto);

    }

    @ParameterizedTest
    @MethodSource("validDTOs")
    void getGuild_notRegistered_notFound(@NonNull GuildDTO dto) {
        getGuild(dto.getGuildId()).statusCode(404);
        assertGuildDoesNotExist(dto);
    }

    @Test
    void getGuild_invalidParameters() {
        getGuild(NEGATIVE_GUILD_ID).statusCode(400);
    }

    @ParameterizedTest
    @MethodSource("validDTOs")
    void updateGuild_success(GuildDTO dto) {
        // assert guild did not exist before
        assertGuildDoesNotExist(dto);
        // save
        persistGuild(dto);
        // validate its existence
        assertGuildExistsAndValidateIt(dto);
        // update
        patchGuild(updatedDTO()).statusCode(204);
        // verify update
        assertGuildExistsAndValidateIt(updatedDTO());
    }

    @ParameterizedTest
    @MethodSource("validDTOs")
    void updateGuild_doesNotExist(GuildDTO dto) {
        // update without saving
        patchGuild(dto).statusCode(404);
        // verify update didn't save a new guild
        assertGuildDoesNotExist(dto);

    }

    @ParameterizedTest
    @MethodSource({"negativeDTOs", "nullDTOs"})
    void updateGuild_negativeAndNullDTOs_validationFails_badRequest(GuildDTO dto) {
        // persist a guild
        persistGuild(validDTO());
        // try updating with invalid parameters
        patchGuild(dto).statusCode(400);
        // verify nothing was changed
        assertGuildExistsAndValidateIt(validDTO());
    }

    @ParameterizedTest
    @MethodSource("validDTOs")
    void deleteGuild_success(GuildDTO dto) {
        // save
        persistGuild(dto);
        // verify existence
        assertGuildExistsAndValidateIt(dto);
        // delete
        deleteGuild(dto.getGuildId()).statusCode(204);
        // verify deletion
        assertGuildDoesNotExist(dto);
    }

    @ParameterizedTest
    @MethodSource("validDTOs")
    void deleteGuild_doesNotExist_notFound(@NonNull GuildDTO dto) {
        // attempt to delete without persisting
        deleteGuild(dto.getGuildId()).statusCode(404);
        // verify guild actually does not exist
        assertGuildDoesNotExist(dto);

    }

    @Test
    void deleteGuild_invalidParameters() {
        deleteGuild(NEGATIVE_GUILD_ID).statusCode(400);
    }

}
