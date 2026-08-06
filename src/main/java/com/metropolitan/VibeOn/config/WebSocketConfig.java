package com.metropolitan.VibeOn.config;

import com.metropolitan.VibeOn.repository.ChatRepository;
import com.metropolitan.VibeOn.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final ChatRepository chatRepository;

    @Value("${app.frontend-origin}")
    private String frontendOrigin;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry
                .addEndpoint("/ws")
                .setAllowedOrigins(frontendOrigin)
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
    }
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null) {
                    return message;
                }

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    authenticate(accessor);
                } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    authorizeSubscription(accessor);
                }
                return message;
            }
        });
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new AuthenticationCredentialsNotFoundException("Authentication required");
        }

        String token = authorization.substring(7);
        if (!jwtTokenProvider.validateToken(token)) {
            throw new BadCredentialsException("Invalid authentication token");
        }

        String username = jwtTokenProvider.getUsername(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        accessor.setUser(new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        ));
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        if (accessor.getUser() == null) {
            throw new AuthenticationCredentialsNotFoundException("Authentication required");
        }

        String destination = accessor.getDestination();
        String chatPrefix = "/topic/chat/";
        if (destination == null || !destination.startsWith(chatPrefix)) {
            return;
        }

        try {
            Long chatId = Long.valueOf(destination.substring(chatPrefix.length()));
            if (!chatRepository.existsByIdAndUsersUsername(chatId, accessor.getUser().getName())) {
                throw new AccessDeniedException("Not authorized for this chat");
            }
        } catch (NumberFormatException exception) {
            throw new AccessDeniedException("Invalid chat destination");
        }
    }
}

