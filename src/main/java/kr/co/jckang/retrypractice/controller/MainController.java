package kr.co.jckang.retrypractice.controller;

import kr.co.jckang.retrypractice.annotation.LogExecutionTime;
import kr.co.jckang.retrypractice.annotation.Retry;
import kr.co.jckang.retrypractice.service.MainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class MainController {

    private final MainService mainService;

    @LogExecutionTime
    @Retry(attempts = 1
            , delay = 200
            , backoff = 3
            , value = NumberFormatException.class)
    @RequestMapping("/{param}/{count}")
    public ResponseEntity<String> test(
            @PathVariable String param
            , @PathVariable String count) {
        return ResponseEntity.ok(mainService.add(param, count));
    }
}
