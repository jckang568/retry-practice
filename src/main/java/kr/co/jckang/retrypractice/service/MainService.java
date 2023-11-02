package kr.co.jckang.retrypractice.service;

import kr.co.jckang.retrypractice.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;

@Slf4j
@Service
public class MainService {

    @Retry(attempts = 3
            , delay = 3000
            , backoff = 3
            , value = NumberFormatException.class)
    public String add(String param, String count) {
        log.info("MainService add executed");
        if(getRandomBoolean()) {
            log.info("NumberFormat Exception Occurred : going to retry");
            return String.valueOf(param).repeat(Integer.parseInt(count));
        }
        log.info("Exception does not occurred");
        return "Exception does not occurred";
    }
    public static boolean getRandomBoolean() {
        Random random = new Random();
        return random.nextBoolean();
    }
}
