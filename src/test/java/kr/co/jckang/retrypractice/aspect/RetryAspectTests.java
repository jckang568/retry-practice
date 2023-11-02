package kr.co.jckang.retrypractice.aspect;

import kr.co.jckang.retrypractice.service.MainService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class RetryAspectTests {
    @Autowired
    private MainService mainService;
    @Test
    @DisplayName("Exception이 발생하지 않으면 그대로 통과해야한다.")
    public void normalProcessTest() {
        // Retry 의 각 항목에 대해서 제대로 작동하는지 테스트가 필요.
    }
}
