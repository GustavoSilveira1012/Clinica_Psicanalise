package com.psicogest.psicogest.security.authorization;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Serviço auxiliar para extrair informações do contexto de segurança.
 * Centraliza a lógica de acesso ao Authentication.
 */
@Service
public class SecurityContextService {

    /**
     * Extrai o userId do principal do Authentication
     */
    public Optional<Long> userId(
            Authentication authentication
    ) {

        if ( authentication == null ) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();

        if ( principal instanceof org.springframework.security.core.userdetails.UserDetails ) {

            // Se for UserDetails customizado com getId()
            try {

                return Optional.of(
                        (Long) principal.getClass()
                                .getMethod( "getId" )
                                .invoke( principal )
                );

            } catch ( Exception exception ) {

                // Fallback para username
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    /**
     * Verifica se o usuário possui uma role específica
     */
    public boolean hasRole(
            Authentication authentication,
            String role
    ) {

        if ( authentication == null ) {
            return false;
        }

        String rolePrefix = "ROLE_";

        if ( !role.startsWith( rolePrefix ) ) {
            role = rolePrefix + role;
        }

        String finalRole = role;

        return authentication.getAuthorities()
                .stream()
                .map( GrantedAuthority::getAuthority )
                .anyMatch( auth -> auth.equals( finalRole ) );
    }
}
