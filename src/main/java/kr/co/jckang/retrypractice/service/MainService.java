package kr.co.jckang.retrypractice.service;

import org.springframework.stereotype.Service;

@Service
public class MainService {
    public String add(String param, int count) {
        return String.valueOf(param).repeat(count);
    }
}
