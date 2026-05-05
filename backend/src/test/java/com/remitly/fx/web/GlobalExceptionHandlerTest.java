package com.remitly.fx.web;

import com.remitly.fx.service.RateNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handlesNotFound() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleNotFound(new RateNotFoundException("missing"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("error", "missing");
        assertThat(response.getBody()).containsEntry("status", 404);
    }

    @Test
    void handlesIllegalArgument() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleBadInput(new IllegalArgumentException("bad"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "bad");
        assertThat(response.getBody()).containsEntry("status", 400);
    }
}
