package com.example.travel_project.config.filter;

import com.example.travel_project.apiPayload.code.ErrorReasonDTO;
import com.example.travel_project.apiPayload.exception.GeneralException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
@Component
public class ExceptionHandlerFilter extends OncePerRequestFilter {
    private final ObjectMapper mapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (GeneralException e) {
            handleException(response, e);
        }
    }

    private void handleException (HttpServletResponse response, GeneralException e) throws IOException {
        response.setStatus(e.getErrorReasonHttpStatus().getHttpStatus().value());
        response.setContentType("application/json; charset=UTF-8");

        ErrorReasonDTO errorReasonDTO = ErrorReasonDTO.builder()
                .httpStatus(e.getErrorReasonHttpStatus().getHttpStatus())
                .isSuccess(false)
                .code(e.getErrorReason().getCode())
                .message(e.getErrorReason().getMessage())
                .build();

        response.getWriter().write(mapper.writeValueAsString(errorReasonDTO));
    }
}
