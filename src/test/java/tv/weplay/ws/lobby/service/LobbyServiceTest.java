package tv.weplay.ws.lobby.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import tv.weplay.ws.lobby.AbstractEnd2EndTestBase;
import tv.weplay.ws.lobby.model.dto.*;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class LobbyServiceTest extends AbstractEnd2EndTestBase {

    private static final Long DEFAULT_ID = 10L;

    @Test
    public void createLobby() {
        Lobby lobby = getLobby(DEFAULT_ID);
        lobbyService.create(lobby);

        Lobby actual = lobbyService.findById(lobby.getId());

        assertThat(actual).isNotNull();
        assertThat(actual.getStartDatetime()).isNotNull();
        assertThat(actual.getId()).isEqualTo(lobby.getId());
    }

    @Test
    public void updateLobby() {
        Lobby lobby = getLobby(DEFAULT_ID);
        lobbyService.create(lobby);

        Lobby toUpdate = lobby.toBuilder()
                .status(LobbyStatus.ONGOING)
                .build();
        lobbyService.update(toUpdate);

        Lobby actual = lobbyService.findById(lobby.getId());

        assertThat(actual).isNotNull();
        assertThat(actual.getId()).isEqualTo(lobby.getId());
        assertThat(actual.getStatus()).isEqualTo(toUpdate.getStatus());
    }

    @Test
    public void deleteLobby() {
        Lobby lobby = getLobby(DEFAULT_ID);
        lobbyService.create(lobby);

        lobbyService.delete(lobby.getId());

        Lobby actual = lobbyService.findById(lobby.getId());

        assertThat(actual).isNull();
    }

    @Test
    public void startVotingWithAllMembersOnline() {
        Lobby lobby = getLobby(DEFAULT_ID);
        lobby.getMatch().getMembers().forEach(member -> member.setStatus(MemberStatus.ONLINE));
        lobbyService.create(lobby);

        lobbyService.startVoting(lobby.getId());

        Lobby actual = lobbyService.findById(lobby.getId());

        assertThat(actual).isNotNull();
        assertThat(actual.getId()).isEqualTo(lobby.getId());
        assertThat(actual.getStatus()).isEqualTo(LobbyStatus.ONGOING);
    }

    @Test
    public void updateMemberStatus() {
        Lobby lobby = getLobby(DEFAULT_ID);
        lobbyService.create(lobby);

        lobbyService.updateMemberStatus(lobby.getId(), DEFAULT_ID);

        Lobby actual = lobbyService.findById(lobby.getId());

        assertThat(actual).isNotNull();
        assertThat(actual.getId()).isEqualTo(lobby.getId());
        assertThat(getMatchMemberById(actual, DEFAULT_ID).getStatus()).isEqualTo(MemberStatus.ONLINE);
    }

    @Test
    public void voteRandomCard() {
        Lobby lobby = getLobby(DEFAULT_ID);
        lobbyService.create(lobby);

        lobbyService.voteRandomCard(lobby.getId(), LobbyMapStatus.SERVER_PICK);

        Lobby actual = lobbyService.findById(lobby.getId());

        assertThat(actual).isNotNull();
        assertThat(actual.getId()).isEqualTo(lobby.getId());
        assertThat(getLobbyMapById(actual, DEFAULT_ID).getStatus()).isEqualTo(LobbyMapStatus.SERVER_PICK);
    }

    @Test
    public void voteCardByUser() {
        Lobby lobby = getLobby(DEFAULT_ID);
        lobbyService.create(lobby);

        LobbyMap map = getLobbyMapById(lobby, DEFAULT_ID).toBuilder()
                .voteItem(new VoteItem(1L))
                .build();

        lobbyService.voteCardByUser(lobby.getId(), map, DEFAULT_ID);

        Lobby actual = lobbyService.findById(lobby.getId());

        assertThat(actual).isNotNull();
        assertThat(actual.getId()).isEqualTo(lobby.getId());
        assertThat(getLobbyMapById(actual, DEFAULT_ID).getStatus()).isEqualTo(LobbyMapStatus.USER_PICK);
    }

    @Test
    public void voteCardByServer() {
        Lobby lobby = getLobby(DEFAULT_ID);
        Lobby actual = lobbyService.create(lobby);

        lobbyService.voteCardByServer(lobby.getId(), 1L, LobbyMapStatus.SERVER_PICK);

        actual = lobbyService.findById(lobby.getId());

        assertThat(actual).isNotNull();
        assertThat(actual.getId()).isEqualTo(lobby.getId());
        assertThat(getLobbyMapById(actual, DEFAULT_ID).getStatus()).isEqualTo(LobbyMapStatus.SERVER_PICK);
    }

    private MatchMember getMatchMemberById(Lobby lobby, Long memberId) {
        return lobby.getMatch().getMembers().stream()
                .filter(member -> member.getId().equals(memberId))
                .findFirst().orElse(null);
    }

    private LobbyMap getLobbyMapById(Lobby lobby, Long mapId) {
        return lobby.getLobbyMap().stream()
                .filter(map -> map.getId().equals(mapId))
                .findFirst().orElse(null);
    }

    private Lobby getLobby(Long lobbyId) {
        return Lobby.builder()
                .id(lobbyId)
                .status(LobbyStatus.UPCOMING)
                .duration(5L)
                .settings(Settings.builder()
                        .votePool(Arrays.asList(1L, 2L, 3L))
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
        LobbyMap third = LobbyMap.builder()
                .id(DEFAULT_ID + 2)
                .status(LobbyMapStatus.NONE)
                .member(Member.builder().id(DEFAULT_ID + 2).build())
                .build();
        return Arrays.asList(first, second, third);
    }

    private TournamentMember getTournamentMember(Long id) {
        return TournamentMember.builder()
                .id(id)
                .role(TournamentMemberRole.CAPTAIN)
                .member(Member.builder().id(id).build())
                .build();
    }

}
