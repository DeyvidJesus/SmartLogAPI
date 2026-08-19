package br.com.smartlog.api;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ApiInfoController {

    @GetMapping
    public Map<String, String> info() {
        return Map.of(
                "name", "smartlog-api",
                "version", "v1",
                "status", "UP");
    }
}
