package com.metropolitan.VibeOn.config;

import com.metropolitan.VibeOn.repository.ChatRepository;
import com.metropolitan.VibeOn.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebSocketAuthorizationTest {

    private JwtTokenProvider jwtTokenProvider;
    private UserDetailsService userDetailsService;
    private ChatRepository chatRepository;
    private ChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = mock(JwtTokenProvider.class);
        userDetailsService = mock(UserDetailsService.class);
        chatRepository = mock(ChatRepository.class);

        WebSocketConfig config = new WebSocketConfig(jwtTokenProvider, userDetailsService, chatRepository);
        CapturingChannelRegistration registration = new CapturingChannelRegistration();
        config.configureClientInboundChannel(registration);
        interceptor = registration.interceptor;
    }

    @Test
    void connectRequiresAValidBearerTokenAndSetsThePrincipal() {
        when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtTokenProvider.getUsername("valid-token")).thenReturn("alice");
        when(userDetailsService.loadUserByUsername("alice"))
                .thenReturn(User.withUsername("alice").password("password").roles("USER").build());

        Message<?> result = interceptor.preSend(
                stompMessage(StompCommand.CONNECT, null, null, "Bearer valid-token"),
                null
        );

        assertThat(StompHeaderAccessor.wrap(result).getUser())
                .isNotNull()
                .extracting(java.security.Principal::getName)
                .isEqualTo("alice");

        assertThatThrownBy(() -> interceptor.preSend(
                stompMessage(StompCommand.CONNECT, null, null, null),
                null
        )).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    void chatSubscriptionIsAllowedForMembers() {
        UsernamePasswordAuthenticationToken alice =
                new UsernamePasswordAuthenticationToken("alice", null, List.of());
        when(chatRepository.existsByIdAndUsersUsername(10L, "alice")).thenReturn(true);

        Message<?> message = stompMessage(
                StompCommand.SUBSCRIBE,
                "/topic/chat/10",
                alice,
                null
        );

        assertThat(interceptor.preSend(message, null)).isSameAs(message);
    }

    @Test
    void chatSubscriptionIsRejectedForNonMembersAndAnonymousUsers() {
        UsernamePasswordAuthenticationToken mallory =
                new UsernamePasswordAuthenticationToken("mallory", null, List.of());
        when(chatRepository.existsByIdAndUsersUsername(10L, "mallory")).thenReturn(false);

        assertThatThrownBy(() -> interceptor.preSend(
                stompMessage(StompCommand.SUBSCRIBE, "/topic/chat/10", mallory, null),
                null
        )).isInstanceOf(AccessDeniedException.class);

        assertThatThrownBy(() -> interceptor.preSend(
                stompMessage(StompCommand.SUBSCRIBE, "/topic/chat/10", null, null),
                null
        )).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    private Message<byte[]> stompMessage(
            StompCommand command,
            String destination,
            UsernamePasswordAuthenticationToken user,
            String authorization
    ) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setLeaveMutable(true);
        if (destination != null) {
            accessor.setDestination(destination);
        }
        if (user != null) {
            accessor.setUser(user);
        }
        if (authorization != null) {
            accessor.setNativeHeader("Authorization", authorization);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private static final class CapturingChannelRegistration extends ChannelRegistration {
        private ChannelInterceptor interceptor;

        @Override
        public ChannelRegistration interceptors(ChannelInterceptor... interceptors) {
            this.interceptor = interceptors[0];
            return this;
        }
    }
}
