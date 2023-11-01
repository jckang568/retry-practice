package kr.co.jckang.retrypractice.service;

import org.springframework.stereotype.Service;

@Service
public class MainService {
    public String add(String param, String count) {
        return String.valueOf(param).repeat(Integer.parseInt(count));
    }
}
