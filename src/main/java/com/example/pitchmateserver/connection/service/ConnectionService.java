package com.example.pitchmateserver.connection.service;

import com.example.pitchmateserver.common.exception.BusinessException;
import com.example.pitchmateserver.common.exception.ErrorCode;
import com.example.pitchmateserver.connection.dto.ConnectionRequest;
import com.example.pitchmateserver.connection.dto.ConnectionResponse;
import com.example.pitchmateserver.connection.dto.MentorSearchResponse;
import com.example.pitchmateserver.connection.entity.MentorConnection;
import com.example.pitchmateserver.connection.repository.ConnectionRepository;
import com.example.pitchmateserver.user.entity.User;
import com.example.pitchmateserver.user.repository.UserRepository;
import com.example.pitchmateserver.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConnectionService {

    private final ConnectionRepository connectionRepository;
    private final UserService userService;
    private final UserRepository userRepository;

    public List<MentorSearchResponse> searchMentors(Long menteeId, String nickname) {
        List<User> mentors = userRepository.findByNicknameContainingAndRole(nickname, "MENTOR");

        Map<Long, String> statusMap = connectionRepository.findByMenteeIdOrderByCreatedAtDesc(menteeId)
                .stream()
                .collect(Collectors.toMap(
                        c -> c.getMentor().getId(),
                        c -> c.getStatus().name()
                ));

        return mentors.stream()
                .map(m -> MentorSearchResponse.of(m, statusMap.get(m.getId())))
                .toList();
    }

    @Transactional
    public ConnectionResponse requestConnection(Long menteeId, ConnectionRequest request) {
        User mentee = userService.findUser(menteeId);
        User mentor = userService.findUser(request.getMentorId());

        if (!"MENTOR".equals(mentor.getRole())) {
            throw new BusinessException(ErrorCode.NOT_MENTOR);
        }
        if (connectionRepository.existsByMenteeIdAndMentorId(menteeId, mentor.getId())) {
            throw new BusinessException(ErrorCode.CONNECTION_ALREADY_EXISTS);
        }

        long menteeRequestCount = connectionRepository.countByMenteeIdAndStatus(menteeId, MentorConnection.ConnectionStatus.PENDING)
                + connectionRepository.countByMenteeIdAndStatus(menteeId, MentorConnection.ConnectionStatus.ACCEPTED);
        if (menteeRequestCount >= 5) {
            throw new BusinessException(ErrorCode.MENTOR_LIMIT_EXCEEDED);
        }

        long mentorAcceptedCount = connectionRepository.countByMentorIdAndStatus(mentor.getId(), MentorConnection.ConnectionStatus.ACCEPTED);
        if (mentorAcceptedCount >= 10) {
            throw new BusinessException(ErrorCode.MENTEE_LIMIT_EXCEEDED);
        }

        MentorConnection connection = connectionRepository.save(MentorConnection.builder()
                .mentee(mentee)
                .mentor(mentor)
                .status(MentorConnection.ConnectionStatus.PENDING)
                .menteeIntro(mentee.getBio())
                .build());

        return ConnectionResponse.from(connection, menteeId);
    }

    public List<ConnectionResponse> getMyConnections(Long userId) {
        User user = userService.findUser(userId);
        List<MentorConnection> connections = "MENTOR".equals(user.getRole())
                ? connectionRepository.findByMentorIdOrderByCreatedAtDesc(userId)
                : connectionRepository.findByMenteeIdOrderByCreatedAtDesc(userId);

        return connections.stream().map(c -> ConnectionResponse.from(c, userId)).toList();
    }

    @Transactional
    public ConnectionResponse acceptConnection(Long mentorId, Long connectionId) {
        MentorConnection connection = findAndValidate(mentorId, connectionId, true);
        if (connection.getStatus() != MentorConnection.ConnectionStatus.PENDING) {
            throw new BusinessException(ErrorCode.CONNECTION_ACCESS_DENIED);
        }
        connection.accept();
        return ConnectionResponse.from(connection, mentorId);
    }

    @Transactional
    public ConnectionResponse rejectConnection(Long mentorId, Long connectionId) {
        MentorConnection connection = findAndValidate(mentorId, connectionId, true);
        if (connection.getStatus() != MentorConnection.ConnectionStatus.PENDING) {
            throw new BusinessException(ErrorCode.CONNECTION_ACCESS_DENIED);
        }
        connection.reject();
        return ConnectionResponse.from(connection, mentorId);
    }

    @Transactional
    public void deleteConnection(Long userId, Long connectionId) {
        MentorConnection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONNECTION_NOT_FOUND));

        boolean isMentor = connection.getMentor().getId().equals(userId);
        boolean isMentee = connection.getMentee().getId().equals(userId);
        if (!isMentor && !isMentee) {
            throw new BusinessException(ErrorCode.CONNECTION_ACCESS_DENIED);
        }
        connectionRepository.delete(connection);
    }

    public List<ConnectionResponse> getAcceptedMentees(Long mentorId) {
        return connectionRepository.findByMentorIdAndStatus(mentorId, MentorConnection.ConnectionStatus.ACCEPTED)
                .stream().map(c -> ConnectionResponse.from(c, mentorId)).toList();
    }

    public List<ConnectionResponse> getAcceptedMentors(Long menteeId) {
        return connectionRepository.findByMenteeIdAndStatus(menteeId, MentorConnection.ConnectionStatus.ACCEPTED)
                .stream().map(c -> ConnectionResponse.from(c, menteeId)).toList();
    }

    private MentorConnection findAndValidate(Long mentorId, Long connectionId, boolean requireMentor) {
        MentorConnection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONNECTION_NOT_FOUND));
        if (requireMentor && !connection.getMentor().getId().equals(mentorId)) {
            throw new BusinessException(ErrorCode.CONNECTION_ACCESS_DENIED);
        }
        return connection;
    }
}
