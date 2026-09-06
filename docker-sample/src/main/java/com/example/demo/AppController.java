package com.example.demo;

import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class AppController {

    private final Environment environment;
    private final JdbcTemplate jdbcTemplate;

    public AppController(Environment environment, JdbcTemplate jdbcTemplate) {
        this.environment = environment;
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 루트 — 머스태시 템플릿을 렌더링한다. DB 를 건드리지 않는다. */
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("version", version());
        model.addAttribute("host", host());
        return "index";
    }

    /**
     * 문서의 실습이 결과를 대조하는 지점.
     * 응답에 호스트명을 실어 어느 컨테이너·파드가 답했는지 눈으로 구분한다.
     */
    @GetMapping("/hello")
    @ResponseBody
    public String hello() {
        return "Hello World! " + version() + " (Host = " + host() + ")";
    }

    /**
     * DB 에 실제로 붙어 본다.
     * 10장(네트워크)에서 jdbc:mysql://localhost 가 왜 틀렸는지를 이 엔드포인트로 확인한다.
     * 루트 페이지는 DB 를 건드리지 않으므로 설정이 틀려도 "잘 도는 것처럼" 보인다.
     */
    @GetMapping("/db")
    @ResponseBody
    public String db() {
        try {
            String v = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);
            return "DB OK — MySQL " + v;
        } catch (Exception e) {
            return "DB 연결 실패 — " + e.getClass().getSimpleName() + ": "
                    + String.valueOf(e.getMessage()).lines().findFirst().orElse("");
        }
    }

    private String version() {
        // 8.6절: 빌드 인자로 주입한다. 값이 없으면 v1.
        String v = environment.getProperty("APP_VERSION");
        return (v == null || v.isBlank()) ? "v1" : v;
    }

    private String host() {
        String h = environment.getProperty("HOSTNAME");
        return (h == null || h.isBlank()) ? "unknown" : h;
    }
}
