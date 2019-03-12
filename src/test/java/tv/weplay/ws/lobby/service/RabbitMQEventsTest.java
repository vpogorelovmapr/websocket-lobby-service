package tv.weplay.ws.lobby.service;

import com.github.jasminb.jsonapi.JSONAPIDocument;
import com.github.jasminb.jsonapi.exceptions.DocumentSerializationException;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;
import tv.weplay.ws.lobby.AbstractEnd2EndTestBase;
import tv.weplay.ws.lobby.converter.JsonApiConverter;
import tv.weplay.ws.lobby.model.dto.*;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static tv.weplay.ws.lobby.common.EventTypes.LOBBY_CREATED;
import static tv.weplay.ws.lobby.common.EventTypes.MEMBER_EVENT;
import static tv.weplay.ws.lobby.common.EventTypes.VOTE_EVENT;
import static tv.weplay.ws.lobby.model.dto.LobbyStatus.CANCELED;
import static tv.weplay.ws.lobby.model.dto.LobbyStatus.UPCOMING;

@Slf4j
public class RabbitMQEventsTest extends AbstractEnd2EndTestBase {

    private static final Long DEFAULT_TIMEOUT = 15000L;
    private static final Long DEFAULT_ID = 100L;

    @Autowired
    private EventSenderService eventSenderService;

    @Autowired
    private JsonApiConverter converter;

    @Test
    public void cancelOneToOneLobby() throws Exception {
        // Send lobby creation event
        sendLobbyCreationEvent();

        // Check that lobby service receive lobby creation event
        checkLobbyStatusEvent(rabbitmqProperties.getOutcomingTournamentsQueueName(), UPCOMING);
        checkLobbyStatusEvent(rabbitmqProperties.getOutcomingPrivateQueueName(), UPCOMING);

        // Check that lobby service sent canceled event as some users are offline
        checkLobbyStatusEvent(rabbitmqProperties.getOutcomingTournamentsQueueName(), CANCELED);
        checkLobbyStatusEvent(rabbitmqProperties.getOutcomingUiQueueName(), CANCELED);
    }

    @Test
    public void playOneToOneLobby() throws Exception {
        // Send lobby creation event
        sendLobbyCreationEvent();

        // Check that lobby service receive lobby creation event
        checkLobbyStatusEvent(rabbitmqProperties.getOutcomingTournamentsQueueName(), UPCOMING);
        checkLobbyStatusEvent(rabbitmqProperties.getOutcomingPrivateQueueName(), UPCOMING);

        // Send match member event that user is online
        sendAndCheckMemberEvent(DEFAULT_ID);
        sendAndCheckMemberEvent(DEFAULT_ID + 1);

        // Check that lobby service sent ongoing event as all users are online
        receiveAndCheckLobbyEvent(LobbyStatus.ONGOING);

        // Send vote event
        sendLobbyMapEvent();

        // Check lobby vote results from lobby service
        receiveAndCheckLobbyMapEvent(DEFAULT_ID, 1L);
        receiveAndCheckLobbyMapEvent(DEFAULT_ID + 1, 2L);

        // Check that lobby service sent ended event
        receiveAndCheckLobbyEvent(LobbyStatus.ENDED);
    }

    private void receiveAndCheckLobbyMapEvent(Long mapId, Long voteId) {
        receiveAndCheckLobbyMapEvent(rabbitmqProperties.getOutcomingUiQueueName(), mapId, voteId);
        receiveAndCheckLobbyMapEvent(rabbitmqProperties.getOutcomingTournamentsQueueName(), mapId, voteId);
    }

    private void receiveAndCheckLobbyMapEvent(String queue, Long mapId, Long voteId) {
        String response = eventSenderService.receiveAndConvert(queue, DEFAULT_TIMEOUT);
        LobbyMap lobbyMapEvent = converter.readDocument(response, LobbyMap.class).get();

        assertThat(lobbyMapEvent).isNotNull();
        assertThat(lobbyMapEvent.getId()).isEqualTo(mapId);
        assertThat(lobbyMapEvent.getVoteItem()).isEqualTo(new VoteItem(voteId));
    }

    private void sendLobbyMapEvent() throws DocumentSerializationException {
        LobbyMap map = LobbyMap.builder()
                .lobby(Lobby.builder().id(DEFAULT_ID).build())
                .id(DEFAULT_ID)
                .voteItem(new VoteItem(1L))
                .build();

        String message = new String(converter.writeDocument(new JSONAPIDocument<>(map)));
        eventSenderService.prepareAndSendEvent("", message, rabbitmqProperties.getIncomingUiQueueName(), VOTE_EVENT, getDefaultHeaders(DEFAULT_ID));
    }

    private void receiveAndCheckLobbyEvent(LobbyStatus status) {
        checkLobbyStatusEvent(rabbitmqProperties.getOutcomingUiQueueName(), status);
        checkLobbyStatusEvent(rabbitmqProperties.getOutcomingTournamentsQueueName(), status);
    }

    private void sendAndCheckMemberEvent(Long memberId) throws DocumentSerializationException {
        MatchMember member = MatchMember.builder()
                .lobby(Lobby.builder().id(DEFAULT_ID).build())
                .id(memberId)
                .build();

        String message = new String(converter.writeDocument(new JSONAPIDocument<>(member)));
        eventSenderService.prepareAndSendEvent("", message, rabbitmqProperties.getIncomingUiQueueName(), MEMBER_EVENT, getDefaultHeaders(memberId));

        checkMatchMemberEvent(rabbitmqProperties.getOutcomingUiQueueName(), memberId);
        checkMatchMemberEvent(rabbitmqProperties.getOutcomingTournamentsQueueName(), memberId);
    }

    private void checkMatchMemberEvent(String queue, Long memberId) {
        String response = eventSenderService.receiveAndConvert(queue, DEFAULT_TIMEOUT);
        MatchMember memberEvent = converter.readDocument(response, MatchMember.class).get();

        assertThat(memberEvent).isNotNull();
        assertThat(memberEvent.getId()).isEqualTo(memberId);
    }

    private void checkLobbyStatusEvent(String queueName, LobbyStatus status) {
        String response = eventSenderService.receiveAndConvert(queueName, DEFAULT_TIMEOUT);
        Lobby lobbyCanceledEvent = converter.readDocument(response, Lobby.class).get();

        assertThat(lobbyCanceledEvent).isNotNull();
        assertThat(lobbyCanceledEvent.getStatus()).isEqualTo(status);
    }

    private Map<String, Long> getDefaultHeaders(Long defaultId) {
        return ImmutableMap.of("user_id", defaultId);
    }

    private void sendLobbyCreationEvent() throws DocumentSerializationException {
        Lobby lobby = getLobby();
        String message = new String(converter.writeDocument(new JSONAPIDocument<>(lobby)));
        eventSenderService.prepareAndSendEvent("", message, rabbitmqProperties.getIncomingTournamentsQueueName(), LOBBY_CREATED, new HashMap<>());
    }

    private Lobby getLobby() {
        return Lobby.builder()
                .id(RabbitMQEventsTest.DEFAULT_ID)
                .status(UPCOMING)
                .duration(5L)
                .settings(Settings.builder()
                        .votePool(Arrays.asList(1L, 2L))
                        .voteTime(15)
                        .build())
                .match(Match.builder()
                        .id(DEFAULT_ID)
                        .members(getMatchMemberList())
                        .build())
                .lobbyMap(getLobbyMapList())
                .build();
    }

    private List<MatchMember> getMatchMemberList() {
        MatchMember first = MatchMember.builder()
                .id(DEFAULT_ID)
                .status(MemberStatus.OFFLINE)
                .participationType(ParticipationType.HOME)
                .tournamentMember(getTournamentMember(DEFAULT_ID))
                .build();
        MatchMember second = MatchMember.builder()
                .id(DEFAULT_ID + 1)
                .status(MemberStatus.OFFLINE)
                .participationType(ParticipationType.AWAY)
                .tournamentMember(getTournamentMember(DEFAULT_ID + 1))
                .build();
        return Arrays.asList(first, second);
    }

    private List<LobbyMap> getLobbyMapList() {
        LobbyMap first = LobbyMap.builder()
                .id(DEFAULT_ID)
                .status(LobbyMapStatus.NONE)
                .member(Member.builder().id(DEFAULT_ID).build())
                .build();
        LobbyMap second = LobbyMap.builder()
                .id(DEFAULT_ID + 1)
                .status(LobbyMapStatus.NONE)
                .member(Member.builder().id(DEFAULT_ID + 1).build())
                .build();
        return Arrays.asList(first, second);
    }

    private TournamentMember getTournamentMember(Long id) {
        return TournamentMember.builder()
                .id(id)
                .role(TournamentMemberRole.CAPTAIN)
                .member(Member.builder().id(id).build())
                .build();
    }

}